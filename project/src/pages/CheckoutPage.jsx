import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { CreditCard, MapPin, CheckCircle2 } from 'lucide-react';
import { useCart } from '../context/CartContext';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { listAddresses, createAddress, createOrder, createPayment, createNotification, clearCart } from '../lib/api';
import { PageLoader, ButtonLoader } from '../components/Loader';
import { ErrorState, EmptyState } from '../components/States';

export function CheckoutPage() {
  const { cart, total, clear, itemCount } = useCart();
  const { user, profile } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();

  const [addresses, setAddresses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [placing, setPlacing] = useState(false);

  const [selectedAddress, setSelectedAddress] = useState(null);
  const [paymentMethod, setPaymentMethod] = useState('CARD');
  const [couponCode, setCouponCode] = useState('');

  const [showNewAddr, setShowNewAddr] = useState(false);
  const [addrForm, setAddrForm] = useState({
    full_name: profile?.full_name || '',
    line1: '', line2: '', city: '', state: '', postal_code: '', phone: '',
  });

  useEffect(() => {
    (async () => {
      if (!user) return;
      setLoading(true);
      try {
        const a = await listAddresses(user.id);
        setAddresses(a);
        if (a.length) setSelectedAddress(a[0].id);
      } catch (e) {
        setError(e.message);
      } finally {
        setLoading(false);
      }
    })();
  }, [user]);

  const saveAddress = async (e) => {
    e.preventDefault();
    try {
      const created = await createAddress({ ...addrForm, user_id: user.id, country: 'United States' });
      setAddresses((a) => [created, ...a]);
      setSelectedAddress(created.id);
      setShowNewAddr(false);
      toast.success('Address saved');
    } catch (e) {
      toast.error(e.message);
    }
  };

  const placeOrder = async (e) => {
    e.preventDefault();
    if (!selectedAddress) {
      toast.error('Please select a shipping address');
      return;
    }
    if (!cart || !cart.items?.length) {
      toast.error('Your cart is empty');
      return;
    }
    setPlacing(true);
    try {
      const firstItem = cart.items[0];
      const orderPayload = {
        user_id: user.id,
        vendor_id: firstItem.product?.vendor_id || null,
        status: 'PAID',
        total_amount: total,
        discount_amount: 0,
        coupon_code: couponCode || null,
        address_id: selectedAddress,
      };
      const items = cart.items.map((it) => ({
        product_id: it.product_id,
        product_name: it.product?.name || 'Product',
        quantity: it.quantity,
        unit_price: Number(it.product?.price || 0),
      }));
      const order = await createOrder(orderPayload, items);
      await createPayment({
        order_id: order.id,
        user_id: user.id,
        amount: total,
        status: 'SUCCESSFUL',
        payment_method: paymentMethod,
      });
      await createNotification({
        user_id: user.id,
        title: 'Order placed',
        message: `Your order #${order.id.slice(0, 8)} has been placed successfully.`,
        type: 'SUCCESS',
      });
      await clear();
      toast.success('Order placed successfully!');
      navigate(`/orders/${order.id}`);
    } catch (e) {
      toast.error(e.message || 'Checkout failed');
    } finally {
      setPlacing(false);
    }
  };

  if (loading) return <PageLoader label="Preparing checkout…" />;
  if (error) return <ErrorState message={error} />;
  if (!cart || !cart.items?.length) {
    return <EmptyState title="Your cart is empty" description="Add products before checking out." />;
  }

  const shipping = total > 75 ? 0 : 6.99;
  const tax = total * 0.08;
  const grand = total + shipping + tax;

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-slate-900">Checkout</h1>

      <form onSubmit={placeOrder} className="grid gap-6 lg:grid-cols-[1fr_360px]">
        <div className="space-y-6">
          {/* Address */}
          <section className="card space-y-4 p-5">
            <div className="flex items-center justify-between">
              <h2 className="flex items-center gap-2 text-lg font-semibold text-slate-800">
                <MapPin size={18} className="text-brand-600" /> Shipping address
              </h2>
              <button type="button" onClick={() => setShowNewAddr((s) => !s)} className="text-sm font-medium text-brand-600">
                {showNewAddr ? 'Cancel' : '+ Add new'}
              </button>
            </div>

            {addresses.length === 0 && !showNewAddr && (
              <p className="text-sm text-slate-500">No saved addresses. Add one to continue.</p>
            )}

            <div className="space-y-2">
              {addresses.map((a) => (
                <label
                  key={a.id}
                  className={`flex cursor-pointer items-start gap-3 rounded-lg p-3 ring-1 ${
                    selectedAddress === a.id ? 'ring-2 ring-brand-500 bg-brand-50' : 'ring-slate-200'
                  }`}
                >
                  <input
                    type="radio"
                    name="address"
                    checked={selectedAddress === a.id}
                    onChange={() => setSelectedAddress(a.id)}
                    className="mt-1 h-4 w-4 text-brand-600"
                  />
                  <div className="text-sm">
                    <p className="font-semibold text-slate-800">{a.full_name}</p>
                    <p className="text-slate-600">{a.line1}{a.line2 ? `, ${a.line2}` : ''}</p>
                    <p className="text-slate-600">{a.city}, {a.state} {a.postal_code}</p>
                    {a.phone && <p className="text-slate-500">{a.phone}</p>}
                  </div>
                </label>
              ))}
            </div>

            {showNewAddr && (
              <div className="grid gap-3 rounded-lg bg-slate-50 p-4 sm:grid-cols-2">
                <input className="input sm:col-span-2" placeholder="Full name" value={addrForm.full_name} onChange={(e) => setAddrForm((f) => ({ ...f, full_name: e.target.value }))} required />
                <input className="input sm:col-span-2" placeholder="Address line 1" value={addrForm.line1} onChange={(e) => setAddrForm((f) => ({ ...f, line1: e.target.value }))} required />
                <input className="input sm:col-span-2" placeholder="Address line 2 (optional)" value={addrForm.line2} onChange={(e) => setAddrForm((f) => ({ ...f, line2: e.target.value }))} />
                <input className="input" placeholder="City" value={addrForm.city} onChange={(e) => setAddrForm((f) => ({ ...f, city: e.target.value }))} required />
                <input className="input" placeholder="State" value={addrForm.state} onChange={(e) => setAddrForm((f) => ({ ...f, state: e.target.value }))} required />
                <input className="input" placeholder="Postal code" value={addrForm.postal_code} onChange={(e) => setAddrForm((f) => ({ ...f, postal_code: e.target.value }))} required />
                <input className="input" placeholder="Phone" value={addrForm.phone} onChange={(e) => setAddrForm((f) => ({ ...f, phone: e.target.value }))} />
                <button type="button" onClick={saveAddress} className="btn-primary sm:col-span-2">Save address</button>
              </div>
            )}
          </section>

          {/* Payment */}
          <section className="card space-y-4 p-5">
            <h2 className="flex items-center gap-2 text-lg font-semibold text-slate-800">
              <CreditCard size={18} className="text-brand-600" /> Payment method
            </h2>
            <div className="grid grid-cols-3 gap-2">
              {['CARD', 'PAYPAL', 'COD'].map((m) => (
                <label
                  key={m}
                  className={`cursor-pointer rounded-lg p-3 text-center text-sm font-medium ring-1 ${
                    paymentMethod === m ? 'ring-2 ring-brand-500 bg-brand-50 text-brand-700' : 'ring-slate-200 text-slate-600'
                  }`}
                >
                  <input type="radio" name="payment" checked={paymentMethod === m} onChange={() => setPaymentMethod(m)} className="hidden" />
                  {m === 'COD' ? 'Cash on delivery' : m.charAt(0) + m.slice(1).toLowerCase()}
                </label>
              ))}
            </div>
            {paymentMethod === 'CARD' && (
              <div className="grid gap-3 rounded-lg bg-slate-50 p-4 sm:grid-cols-2">
                <input className="input sm:col-span-2" placeholder="Card number" disabled />
                <input className="input" placeholder="Expiry (MM/YY)" disabled />
                <input className="input" placeholder="CVC" disabled />
                <p className="text-xs text-slate-400 sm:col-span-2">Demo checkout — no real card is charged.</p>
              </div>
            )}
          </section>

          {/* Coupon */}
          <section className="card space-y-3 p-5">
            <h2 className="text-lg font-semibold text-slate-800">Coupon code</h2>
            <div className="flex gap-2">
              <input
                className="input"
                placeholder="Enter coupon code"
                value={couponCode}
                onChange={(e) => setCouponCode(e.target.value)}
              />
              <button type="button" className="btn-secondary" onClick={() => toast.info('Coupon applied (demo)')}>Apply</button>
            </div>
          </section>
        </div>

        {/* Summary */}
        <aside className="card h-fit space-y-4 p-5 lg:sticky lg:top-22">
          <h2 className="text-lg font-semibold text-slate-800">Order summary</h2>
          <div className="max-h-48 space-y-2 overflow-y-auto">
            {cart.items.map((it) => (
              <div key={it.id} className="flex justify-between text-sm">
                <span className="text-slate-600">{it.product?.name} × {it.quantity}</span>
                <span className="font-medium">${(Number(it.product?.price || 0) * it.quantity).toFixed(2)}</span>
              </div>
            ))}
          </div>
          <div className="space-y-2 border-t border-slate-100 pt-3 text-sm">
            <div className="flex justify-between"><span className="text-slate-500">Subtotal</span><span>${total.toFixed(2)}</span></div>
            <div className="flex justify-between"><span className="text-slate-500">Shipping</span><span>{shipping === 0 ? 'Free' : `$${shipping.toFixed(2)}`}</span></div>
            <div className="flex justify-between"><span className="text-slate-500">Tax</span><span>${tax.toFixed(2)}</span></div>
            <div className="flex justify-between border-t border-slate-100 pt-2 text-base font-bold"><span>Total</span><span>${grand.toFixed(2)}</span></div>
          </div>
          <button type="submit" disabled={placing || !selectedAddress} className="btn-primary w-full">
            {placing ? <ButtonLoader /> : <><CheckCircle2 size={18} /> Place order</>}
          </button>
        </aside>
      </form>
    </div>
  );
}
