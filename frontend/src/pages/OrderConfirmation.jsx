import React, { useEffect, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import {
  CheckCircle2,
  Package,
  MapPin,
  CreditCard,
  Truck,
  Smartphone,
  ShoppingBag,
  Calendar,
} from 'lucide-react';
import { clearCart, clearCartThunk } from '../store/slices/cartSlice';
import Button from '../components/common/Button';
import Badge from '../components/common/Badge';
import { useAuth } from '../context/AuthContext';

const generateOrderId = () => {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
  let result = '';
  for (let i = 0; i < 8; i++) {
    result += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return `ORD-${result}`;
};

const getEstimatedDelivery = () => {
  const date = new Date();
  date.setDate(date.getDate() + 5);
  return date.toLocaleDateString('en-US', {
    weekday: 'long',
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });
};

const PAYMENT_LABELS = {
  cod: { label: 'Cash on Delivery', icon: Truck },
  card: { label: 'Credit / Debit Card', icon: CreditCard },
  upi: { label: 'UPI / Digital Wallet', icon: Smartphone },
};

const OrderConfirmation = () => {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();

  const orderData = useMemo(() => {
    try {
      const raw = sessionStorage.getItem('shopstack_order');
      return raw ? JSON.parse(raw) : null;
    } catch {
      return null;
    }
  }, []);

  const orderId = useMemo(() => generateOrderId(), []);
  const estimatedDelivery = useMemo(() => getEstimatedDelivery(), []);

  useEffect(() => {
    if (isAuthenticated) {
      dispatch(clearCartThunk());
    } else {
      dispatch(clearCart());
    }
    return () => {
      sessionStorage.removeItem('shopstack_order');
    };
  }, [dispatch, isAuthenticated]);

  if (!orderData) {
    return (
      <div className="flex-1 max-w-4xl mx-auto px-4 py-20 text-center w-full">
        <p className="text-slate-400 text-sm mb-4">No order information found.</p>
        <Button variant="primary" size="md" onClick={() => navigate('/')}>
          Return Home
        </Button>
      </div>
    );
  }

  const { items, address, payment, subtotal, discount, shippingFee, grandTotal, coupon } = orderData;
  const paymentInfo = PAYMENT_LABELS[payment] || PAYMENT_LABELS.cod;
  const PaymentIcon = paymentInfo.icon;

  return (
    <div className="flex-1 max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8 w-full space-y-8">
      {/* SUCCESS BANNER */}
      <div className="relative overflow-hidden rounded-3xl p-8 bg-gradient-to-br from-emerald-950 via-slate-950 to-indigo-950 border border-emerald-500/30 text-center shadow-2xl">
        <div className="relative z-10 space-y-4">
          <div className="w-16 h-16 rounded-full bg-emerald-500/20 border border-emerald-500/40 flex items-center justify-center mx-auto shadow-lg shadow-emerald-500/20">
            <CheckCircle2 className="w-9 h-9 text-emerald-400" />
          </div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight">
            Order Placed Successfully!
          </h1>
          <p className="text-slate-300 text-sm max-w-lg mx-auto leading-relaxed">
            Thank you for shopping with ShopStack. Your order has been confirmed and will be
            dispatched from our verified vendor partners shortly.
          </p>
          <div className="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-emerald-500/15 border border-emerald-500/30 text-emerald-300 text-xs font-mono font-bold">
            <Package className="w-3.5 h-3.5" />
            Order #{orderId}
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 items-start">
        {/* LEFT: DETAILS */}
        <div className="lg:col-span-2 space-y-5">
          {/* Order Items */}
          <div className="p-5 rounded-2xl bg-slate-950/70 border border-slate-800/80 space-y-4">
            <h2 className="text-sm font-bold text-white flex items-center gap-2 border-b border-slate-800 pb-3">
              <Package className="w-4 h-4 text-indigo-400" />
              Order Items ({items.length})
            </h2>

            <div className="space-y-3">
              {items.map((item) => {
                const product = item.product;
                const quantity = item.quantity;
                return (
                  <div key={item.id || product.id} className="flex items-center gap-3">
                    <div className="w-12 h-12 rounded-xl border border-slate-800 shrink-0 overflow-hidden bg-slate-900 flex items-center justify-center">
                      <img
                        src={product.images?.[0] || product.imageUrl}
                        alt={product.name}
                        className="w-full h-full object-cover"
                        onError={(e) => {
                          e.currentTarget.style.display = 'none';
                          e.currentTarget.parentElement.innerHTML = '<span style="font-size:9px;color:#64748b;text-align:center;padding:2px;">No Image</span>';
                        }}
                      />
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="text-xs font-semibold text-white truncate">{product.name}</p>
                      <p className="text-[11px] text-slate-400">
                        {product.storeName} &bull; Qty: {quantity}
                      </p>
                    </div>
                    <span className="text-xs font-bold text-indigo-300 shrink-0">
                      ${((product.price || 0) * quantity).toFixed(2)}
                    </span>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Delivery Address */}
          <div className="p-5 rounded-2xl bg-slate-950/70 border border-slate-800/80 space-y-3">
            <h2 className="text-sm font-bold text-white flex items-center gap-2 border-b border-slate-800 pb-3">
              <MapPin className="w-4 h-4 text-indigo-400" />
              Delivery Address
            </h2>
            <div className="text-sm space-y-1 text-slate-300">
              <p className="font-semibold text-white">{address.fullName}</p>
              <p>{address.address}</p>
              <p>
                {address.city}, {address.state} — {address.postalCode}
              </p>
              <p className="text-slate-400">{address.phone}</p>
            </div>
          </div>

          {/* Payment Method */}
          <div className="p-5 rounded-2xl bg-slate-950/70 border border-slate-800/80 space-y-3">
            <h2 className="text-sm font-bold text-white flex items-center gap-2 border-b border-slate-800 pb-3">
              <PaymentIcon className="w-4 h-4 text-indigo-400" />
              Payment Method
            </h2>
            <div className="flex items-center gap-2">
              <Badge color="indigo" size="md">{paymentInfo.label}</Badge>
              {payment === 'cod' && (
                <span className="text-xs text-slate-400">Pay upon delivery</span>
              )}
            </div>
          </div>

          {/* Estimated Delivery */}
          <div className="flex items-center gap-3 p-4 rounded-2xl bg-indigo-500/5 border border-indigo-500/20">
            <Calendar className="w-5 h-5 text-indigo-400 shrink-0" />
            <div>
              <p className="text-xs font-semibold text-white">Estimated Delivery</p>
              <p className="text-xs text-slate-300">{estimatedDelivery}</p>
            </div>
          </div>
        </div>

        {/* RIGHT: SUMMARY */}
        <div className="lg:col-span-1 sticky top-24 space-y-4">
          <div className="p-5 rounded-2xl bg-slate-950/70 border border-slate-800/80 space-y-3">
            <h3 className="text-sm font-bold text-white border-b border-slate-800 pb-3">
              Price Summary
            </h3>

            <div className="space-y-2 text-xs">
              <div className="flex justify-between text-slate-300">
                <span>Subtotal</span>
                <span className="font-semibold text-white">${subtotal.toFixed(2)}</span>
              </div>

              {discount > 0 && (
                <div className="flex justify-between text-emerald-400">
                  <span>Discount ({coupon?.code})</span>
                  <span className="font-bold">−${discount.toFixed(2)}</span>
                </div>
              )}

              <div className="flex justify-between text-slate-300">
                <span>Shipping</span>
                <span className="font-semibold">
                  {shippingFee === 0 ? (
                    <span className="text-emerald-400 font-bold">FREE</span>
                  ) : (
                    `$${shippingFee.toFixed(2)}`
                  )}
                </span>
              </div>
            </div>

            <div className="border-t border-slate-800 pt-3 flex justify-between items-center">
              <span className="font-bold text-white text-sm">Total Paid</span>
              <span className="text-xl font-black text-emerald-400">${grandTotal.toFixed(2)}</span>
            </div>
          </div>

          <div className="space-y-3">
            <Button
              variant="primary"
              size="md"
              className="w-full"
              onClick={() => navigate('/products')}
              icon={ShoppingBag}
            >
              Continue Shopping
            </Button>
            {isAuthenticated && (
              <Button
                variant="outline"
                size="md"
                className="w-full"
                onClick={() => navigate('/profile')}
              >
                View Profile &amp; Deliveries
              </Button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default OrderConfirmation;
