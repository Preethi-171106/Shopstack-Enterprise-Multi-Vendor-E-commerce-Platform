import React, { useState, useEffect, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import {
  MapPin,
  CreditCard,
  Smartphone,
  Truck,
  ShoppingBag,
  AlertCircle,
  Package,
  Tag,
  Check,
  X,
  Sparkles,
} from 'lucide-react';
import {
  selectCartItems,
  selectCartSubtotal,
  selectCartShippingFee,
  selectCartGrandTotal,
  fetchCartThunk,
  clearCart,
} from '../store/slices/cartSlice';
import orderService from '../services/orderService';
import paymentService from '../services/paymentService';
import couponService from '../services/couponService';
import Button from '../components/common/Button';
import Input from '../components/common/Input';
import Badge from '../components/common/Badge';
import EmptyState from '../components/common/EmptyState';
import { useAuth } from '../context/AuthContext';
import { useApp } from '../context/AppContext';

const PAYMENT_METHODS = [
  {
    id: 'cod',
    label: 'Cash on Delivery',
    description: 'Pay when your order arrives',
    icon: Truck,
  },
  {
    id: 'card',
    label: 'Credit / Debit Card',
    description: 'Visa, Mastercard, Amex — Razorpay Gateway',
    icon: CreditCard,
  },
  {
    id: 'upi',
    label: 'UPI / Digital Wallet',
    description: 'GPay, PhonePe, Paytm — Instant Pay',
    icon: Smartphone,
  },
];

const Checkout = () => {
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const { user, isAuthenticated } = useAuth();
  const { showNotification } = useApp();

  const cartItems = useSelector(selectCartItems);
  const subtotal = useSelector(selectCartSubtotal);
  const shippingFee = useSelector(selectCartShippingFee);
  const grandTotal = useSelector(selectCartGrandTotal);

  useEffect(() => {
    if (isAuthenticated) {
      dispatch(fetchCartThunk());
    }
  }, [dispatch, isAuthenticated]);

  const [addressForm, setAddressForm] = useState({
    fullName: user ? `${user.firstName || ''} ${user.lastName || ''}`.trim() : '',
    phone: user?.phoneNumber || '',
    address: '',
    city: '',
    state: '',
    postalCode: '',
  });

  useEffect(() => {
    if (user) {
      setAddressForm((prev) => ({
        ...prev,
        fullName: prev.fullName || `${user.firstName || ''} ${user.lastName || ''}`.trim(),
        phone: prev.phone || user.phoneNumber || '',
      }));
    }
  }, [user]);

  const [addressErrors, setAddressErrors] = useState({});
  const [selectedPayment, setSelectedPayment] = useState('cod');
  const [paymentError, setPaymentError] = useState('');
  const [isPlacing, setIsPlacing] = useState(false);
  const [activeBackendOrder, setActiveBackendOrder] = useState(null);

  // ── Coupon State ──────────────────────────────────────────────────────────
  const [couponInput, setCouponInput] = useState('');
  const [appliedCoupon, setAppliedCoupon] = useState(null);
  const [couponLoading, setCouponLoading] = useState(false);
  const [couponError, setCouponError] = useState('');
  const [applicableCoupons, setApplicableCoupons] = useState([]);
  const [loadingApplicableCoupons, setLoadingApplicableCoupons] = useState(false);

  // Fetch applicable promotional coupons for the authenticated user's cart
  useEffect(() => {
    let isMounted = true;
    if (isAuthenticated && cartItems.length > 0) {
      setLoadingApplicableCoupons(true);
      couponService.getApplicableCoupons()
        .then((data) => {
          if (isMounted) setApplicableCoupons(data);
        })
        .catch((err) => {
          console.warn('Could not load applicable coupons:', err);
        })
        .finally(() => {
          if (isMounted) setLoadingApplicableCoupons(false);
        });
    } else {
      setApplicableCoupons([]);
    }
    return () => { isMounted = false; };
  }, [isAuthenticated, cartItems]);

  // Compute calculated discount amount from backend-validated coupon or formula
  const discountAmount = useMemo(() => {
    if (!appliedCoupon) return 0;
    if (typeof appliedCoupon.discountAmount === 'number' && appliedCoupon.discountAmount >= 0) {
      return Math.min(appliedCoupon.discountAmount, subtotal);
    }
    let disc = 0;
    const baseAmount = appliedCoupon.eligibleSubtotal !== undefined && appliedCoupon.eligibleSubtotal !== null
      ? appliedCoupon.eligibleSubtotal
      : subtotal;

    if (appliedCoupon.type === 'percentage' || appliedCoupon.discountType === 'PERCENTAGE') {
      disc = (baseAmount * (appliedCoupon.value || appliedCoupon.discountValue || 0)) / 100;
      if (appliedCoupon.maximumDiscount && disc > appliedCoupon.maximumDiscount) {
        disc = appliedCoupon.maximumDiscount;
      }
    } else {
      disc = appliedCoupon.value || appliedCoupon.discountValue || 0;
    }
    return Math.min(disc, subtotal);
  }, [appliedCoupon, subtotal]);

  // Compute final payable total with discount
  const finalGrandTotal = useMemo(() => {
    return Math.max(0, subtotal - discountAmount) + shippingFee;
  }, [subtotal, discountAmount, shippingFee]);

  // Handle Apply Coupon
  const handleApplyCoupon = async (codeToApply) => {
    const code = (codeToApply || couponInput).trim();
    if (!code) {
      setCouponError('Please enter a coupon code');
      return;
    }

    setCouponLoading(true);
    setCouponError('');

    try {
      const validated = await couponService.validateCoupon(code, subtotal, user?.email);
      setAppliedCoupon({
        ...validated,
        type: validated.discountType === 'PERCENTAGE' ? 'percentage' : 'fixed',
        value: validated.discountValue,
        discountAmount: validated.discountAmount,
      });
      setCouponInput(validated.code);
      showNotification(`Coupon "${validated.code}" applied: Saved ₹${validated.discountAmount?.toFixed(2) || '0.00'}!`, 'success');
    } catch (err) {
      const msg = err.response?.data?.message || err.userMessage || 'Invalid or inapplicable coupon code';
      setCouponError(msg);
      showNotification(msg, 'error');
    } finally {
      setCouponLoading(false);
    }
  };

  // Handle Remove Coupon
  const handleRemoveCoupon = () => {
    setAppliedCoupon(null);
    setCouponInput('');
    setCouponError('');
    showNotification('Coupon removed', 'info');
  };

  // If no items in cart and no pending order created yet, display empty state
  if (cartItems.length === 0 && !activeBackendOrder) {
    return (
      <div className="flex-1 max-w-4xl mx-auto px-4 py-16 w-full">
        <EmptyState
          icon={ShoppingBag}
          title="Your cart is empty"
          description="Add items to your cart before proceeding to checkout."
          actionLabel="Browse Products"
          onAction={() => navigate('/products')}
        />
      </div>
    );
  }

  const displayItems = cartItems.length > 0
    ? cartItems
    : (activeBackendOrder?.items || []).map((item) => ({
        id: item.id,
        quantity: item.quantity,
        product: {
          id: item.productId,
          name: item.productNameSnapshot || item.productName,
          imageUrl: item.productImageUrlSnapshot || item.imageUrl,
          price: item.unitPriceSnapshot || item.price,
          storeName: item.vendorProfileName || 'Verified Store',
        },
      }));

  const validateAddress = () => {
    const errors = {};
    if (!addressForm.fullName.trim()) errors.fullName = 'Full name is required';
    if (!addressForm.phone.trim()) {
      errors.phone = 'Phone number is required';
    } else if (!/^\+?[\d\s\-]{7,15}$/.test(addressForm.phone.trim())) {
      errors.phone = 'Enter a valid phone number';
    }
    if (!addressForm.address.trim()) errors.address = 'Address line is required';
    if (!addressForm.city.trim()) errors.city = 'City is required';
    if (!addressForm.state.trim()) errors.state = 'State is required';
    if (!addressForm.postalCode.trim()) {
      errors.postalCode = 'Postal code is required';
    }
    setAddressErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleAddressChange = (field, value) => {
    setAddressForm((prev) => ({ ...prev, [field]: value }));
    if (addressErrors[field]) {
      setAddressErrors((prev) => ({ ...prev, [field]: '' }));
    }
  };

  const handlePlaceOrder = async () => {
    if (isPlacing) return;
    if (cartItems.length === 0 && !activeBackendOrder) return;

    const addressValid = validateAddress();
    if (!addressValid) return;

    if (!selectedPayment) {
      setPaymentError('Please select a payment method to continue');
      return;
    }
    setPaymentError('');
    setIsPlacing(true);

    const fullShippingAddress = `${addressForm.fullName}, ${addressForm.address}, ${addressForm.city}, ${addressForm.state} ${addressForm.postalCode}, Phone: ${addressForm.phone}`;

    let backendOrder = activeBackendOrder;
    if (isAuthenticated && !backendOrder) {
      try {
        const couponCodeToSend = appliedCoupon ? appliedCoupon.code : null;
        backendOrder = await orderService.createOrder(fullShippingAddress, couponCodeToSend);
        setActiveBackendOrder(backendOrder);
      } catch (err) {
        console.error('Order creation error:', err);
        const userMsg = err.userMessage || err.response?.data?.message || err.message || 'Failed to place order. Please try again.';
        showNotification(userMsg, 'error');
        setIsPlacing(false);
        return;
      }
    }

    const orderData = {
      orderId: backendOrder?.orderNumber || backendOrder?.id || `ORD-${Date.now()}`,
      items: displayItems,
      address: addressForm,
      payment: selectedPayment,
      subtotal: backendOrder?.subtotalAmount ?? subtotal,
      discountAmount: backendOrder?.discountAmount ?? discountAmount,
      couponCode: backendOrder?.couponCode ?? (appliedCoupon ? appliedCoupon.code : null),
      shippingFee,
      grandTotal: backendOrder?.totalAmount ?? finalGrandTotal,
    };

    if (selectedPayment !== 'cod' && backendOrder?.id) {
      try {
        const pMethod = selectedPayment === 'upi' ? 'UPI' : 'CARD';
        const payResp = await paymentService.createPayment(backendOrder.id, {
          paymentMethod: pMethod
        });

        const loadScript = (src) =>
          new Promise((resolve) => {
            const script = document.createElement('script');
            script.src = src;
            script.onload = () => resolve(true);
            script.onerror = () => resolve(false);
            document.body.appendChild(script);
          });

        const ok = await loadScript('https://checkout.razorpay.com/v1/checkout.js');
        if (!ok || !window.Razorpay) {
          showNotification('Could not load Razorpay Checkout. Please try again.', 'error');
          setIsPlacing(false);
          return;
        }

        const options = {
          key: payResp.razorpayKeyId,
          amount: payResp.amountInPaise || Math.round(((backendOrder?.totalAmount ?? finalGrandTotal) || 0) * 100),
          currency: payResp.currency || 'INR',
          order_id: payResp.gatewayOrderId,
          name: 'ShopStack',
          description: `Order ${backendOrder.orderNumber || backendOrder.id}`,
          prefill: {
            name: `${user?.firstName || ''} ${user?.lastName || ''}`.trim() || addressForm.fullName,
            email: user?.email || '',
            contact: user?.phoneNumber || addressForm.phone
          },
          theme: {
            color: '#6366f1'
          },
          handler: async function (razorpayResponse) {
            try {
              const verifyResp = await paymentService.verifyPayment({
                orderId: backendOrder.id,
                razorpayOrderId: razorpayResponse.razorpay_order_id,
                razorpayPaymentId: razorpayResponse.razorpay_payment_id,
                razorpaySignature: razorpayResponse.razorpay_signature
              });

              if (verifyResp && verifyResp.status === 'SUCCESS') {
                dispatch(clearCart());
                setAppliedCoupon(null);
                sessionStorage.setItem('shopstack_order', JSON.stringify(orderData));
                setIsPlacing(false);
                navigate('/order-confirmation');
              } else {
                showNotification('Payment verification failed. Please contact support.', 'error');
                setIsPlacing(false);
              }
            } catch (verifyErr) {
              console.error('Payment verification error:', verifyErr);
              const userMsg = verifyErr.userMessage || verifyErr.response?.data?.message || 'Payment verification failed. Please contact support.';
              showNotification(userMsg, 'error');
              setIsPlacing(false);
            }
          },
          modal: {
            ondismiss: function () {
              showNotification('Payment cancelled or closed. You can retry paying for your order.', 'info');
              setIsPlacing(false);
            }
          }
        };

        const rzp = new window.Razorpay(options);
        rzp.open();
        return;
      } catch (payErr) {
        console.error('Payment creation error:', payErr);
        const userMsg = payErr.userMessage || payErr.response?.data?.message || payErr.message || 'Failed to initiate payment. Please try again.';
        showNotification(userMsg, 'error');
        setIsPlacing(false);
        return;
      }
    }

    dispatch(clearCart());
    setAppliedCoupon(null);
    sessionStorage.setItem('shopstack_order', JSON.stringify(orderData));
    setIsPlacing(false);
    navigate('/order-confirmation');
  };

  return (
    <div className="flex-1 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 w-full">
      {/* Page Title */}
      <div className="mb-8 border-b border-slate-800 pb-6">
        <h1 className="text-3xl font-extrabold text-white tracking-tight">Checkout</h1>
        <p className="text-xs text-slate-400 mt-1">
          Review your order and complete your purchase.
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 items-start">
        {/* MAIN CHECKOUT SECTIONS */}
        <div className="lg:col-span-2 space-y-6">
          
          {/* SECTION 1: DELIVERY ADDRESS */}
          <div className="p-6 rounded-2xl bg-slate-950/70 border border-slate-800/80 space-y-5">
            <div className="flex items-center gap-2.5 border-b border-slate-800 pb-4">
              <div className="w-7 h-7 rounded-lg bg-indigo-500/10 border border-indigo-500/20 flex items-center justify-center">
                <MapPin className="w-4 h-4 text-indigo-400" />
              </div>
              <h2 className="text-base font-bold text-white">Delivery Address</h2>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="sm:col-span-2">
                <Input
                  label="Full Name"
                  required
                  value={addressForm.fullName}
                  onChange={(e) => handleAddressChange('fullName', e.target.value)}
                  placeholder="Full Name"
                  error={addressErrors.fullName}
                />
              </div>
              <Input
                label="Phone Number"
                required
                value={addressForm.phone}
                onChange={(e) => handleAddressChange('phone', e.target.value)}
                placeholder="Phone Number"
                error={addressErrors.phone}
              />
              <div className="sm:col-span-2">
                <Input
                  label="Address Line"
                  required
                  value={addressForm.address}
                  onChange={(e) => handleAddressChange('address', e.target.value)}
                  placeholder="Street Address"
                  error={addressErrors.address}
                />
              </div>
              <Input
                label="City"
                required
                value={addressForm.city}
                onChange={(e) => handleAddressChange('city', e.target.value)}
                placeholder="City"
                error={addressErrors.city}
              />
              <Input
                label="State"
                required
                value={addressForm.state}
                onChange={(e) => handleAddressChange('state', e.target.value)}
                placeholder="State"
                error={addressErrors.state}
              />
              <Input
                label="Postal Code"
                required
                value={addressForm.postalCode}
                onChange={(e) => handleAddressChange('postalCode', e.target.value)}
                placeholder="Postal Code"
                error={addressErrors.postalCode}
              />
            </div>
          </div>

          {/* SECTION 2: ORDER ITEMS */}
          <div className="p-6 rounded-2xl bg-slate-950/70 border border-slate-800/80 space-y-4">
            <div className="flex items-center gap-2.5 border-b border-slate-800 pb-4">
              <div className="w-7 h-7 rounded-lg bg-indigo-500/10 border border-indigo-500/20 flex items-center justify-center">
                <Package className="w-4 h-4 text-indigo-400" />
              </div>
              <h2 className="text-base font-bold text-white">Order Items</h2>
              <Badge color="slate" size="sm">{displayItems.length} items</Badge>
            </div>

            <div className="space-y-3">
              {displayItems.map((item) => {
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
                      ₹{((product.price || 0) * quantity).toFixed(2)}
                    </span>
                  </div>
                );
              })}
            </div>
          </div>

          {/* SECTION 3: PAYMENT METHOD */}
          <div className="p-6 rounded-2xl bg-slate-950/70 border border-slate-800/80 space-y-4">
            <div className="flex items-center gap-2.5 border-b border-slate-800 pb-4">
              <div className="w-7 h-7 rounded-lg bg-indigo-500/10 border border-indigo-500/20 flex items-center justify-center">
                <CreditCard className="w-4 h-4 text-indigo-400" />
              </div>
              <h2 className="text-base font-bold text-white">Payment Method</h2>
            </div>

            <div className="space-y-3">
              {PAYMENT_METHODS.map((method) => {
                const IconComp = method.icon;
                const isSelected = selectedPayment === method.id;
                return (
                  <button
                    key={method.id}
                    type="button"
                    onClick={() => {
                      setSelectedPayment(method.id);
                      setPaymentError('');
                    }}
                    className={`w-full flex items-center gap-3 p-4 rounded-xl border text-left transition-all duration-200 ${
                      isSelected
                        ? 'border-indigo-500 bg-indigo-500/10 shadow-md shadow-indigo-500/10'
                        : 'border-slate-700/80 bg-slate-900/40 hover:border-slate-600'
                    }`}
                  >
                    <div
                      className={`w-8 h-8 rounded-lg flex items-center justify-center shrink-0 ${
                        isSelected ? 'bg-indigo-500/20 text-indigo-400' : 'bg-slate-800 text-slate-400'
                      }`}
                    >
                      <IconComp className="w-4 h-4" />
                    </div>
                    <div className="flex-1 min-w-0">
                      <p
                        className={`text-sm font-semibold ${
                          isSelected ? 'text-indigo-300' : 'text-white'
                        }`}
                      >
                        {method.label}
                      </p>
                      <p className="text-xs text-slate-400 truncate">{method.description}</p>
                    </div>
                    <div
                      className={`w-4 h-4 rounded-full border-2 shrink-0 ${
                        isSelected ? 'border-indigo-500 bg-indigo-500' : 'border-slate-600'
                      }`}
                    />
                  </button>
                );
              })}
            </div>

            {paymentError && (
              <div className="flex items-center gap-2 text-rose-400 text-xs mt-2">
                <AlertCircle className="w-4 h-4 shrink-0" />
                <span>{paymentError}</span>
              </div>
            )}
          </div>
        </div>

        {/* SIDEBAR SUMMARY WITH COUPON BOX */}
        <div className="lg:col-span-1 sticky top-24 space-y-4">
          
          {/* COUPON INPUT & PROMOTIONS CARD */}
          <div className="p-5 rounded-2xl bg-slate-950/70 border border-slate-800/80 space-y-4">
            <div className="flex items-center gap-2 border-b border-slate-800 pb-3">
              <Tag className="w-4 h-4 text-indigo-400" />
              <h3 className="text-sm font-bold text-white">Coupons & Offers</h3>
            </div>

            {appliedCoupon ? (
              <div className="p-3.5 rounded-xl bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-between">
                <div className="space-y-0.5">
                  <div className="flex items-center gap-1.5">
                    <Check className="w-4 h-4 text-emerald-400" />
                    <span className="font-mono font-bold text-sm text-emerald-300 uppercase">{appliedCoupon.code}</span>
                    <Badge color="emerald" size="sm">{appliedCoupon.applicabilityScope || 'PLATFORM'}</Badge>
                  </div>
                  <p className="text-xs text-emerald-400/90 font-medium">
                    {appliedCoupon.description || appliedCoupon.name} (Saved ₹{discountAmount.toFixed(2)})
                  </p>
                </div>
                <button
                  type="button"
                  onClick={handleRemoveCoupon}
                  className="p-1.5 rounded-lg text-slate-400 hover:text-rose-400 hover:bg-slate-900 transition-colors"
                  title="Remove coupon"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>
            ) : (
              <div className="space-y-3">
                <div className="flex gap-2">
                  <input
                    type="text"
                    placeholder="Enter coupon code"
                    value={couponInput}
                    onChange={(e) => {
                      setCouponInput(e.target.value.toUpperCase());
                      setCouponError('');
                    }}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter') {
                        e.preventDefault();
                        handleApplyCoupon();
                      }
                    }}
                    className="flex-1 bg-slate-900 text-white text-xs font-mono uppercase tracking-wider rounded-xl px-3.5 py-2.5 border border-slate-700/80 focus:outline-none focus:border-indigo-500"
                  />
                  <Button
                    variant="primary"
                    size="sm"
                    onClick={() => handleApplyCoupon()}
                    isLoading={couponLoading}
                    disabled={couponLoading || !couponInput.trim()}
                  >
                    Apply
                  </Button>
                </div>

                {couponError && (
                  <p className="text-xs text-rose-400 flex items-center gap-1 mt-1">
                    <AlertCircle className="w-3.5 h-3.5 shrink-0" />
                    <span>{couponError}</span>
                  </p>
                )}

                {/* Applicable Coupons - Available Offers for Your Order */}
                {loadingApplicableCoupons ? (
                  <div className="py-2 text-[11px] text-slate-400 flex items-center gap-1.5">
                    <Sparkles className="w-3.5 h-3.5 text-indigo-400 animate-spin" /> Checking eligible offers for your cart...
                  </div>
                ) : applicableCoupons.length > 0 ? (
                  <div className="pt-2 space-y-2 border-t border-slate-800/80">
                    <p className="text-[11px] text-slate-400 flex items-center gap-1 font-semibold uppercase tracking-wider">
                      <Sparkles className="w-3 h-3 text-amber-400" /> Available Offers for Your Order
                    </p>
                    <div className="space-y-1.5">
                      {applicableCoupons.map((ac) => {
                        const discountValText = ac.type === 'percentage' || ac.discountType === 'PERCENTAGE'
                          ? `${ac.value || ac.discountValue}% OFF`
                          : `₹${ac.value || ac.discountValue} OFF`;

                        return (
                          <div
                            key={ac.id || ac.code}
                            onClick={() => handleApplyCoupon(ac.code)}
                            className="p-2.5 rounded-xl bg-slate-900/90 hover:bg-indigo-950/40 border border-slate-800 hover:border-indigo-500/40 cursor-pointer transition-all flex items-center justify-between group"
                          >
                            <div className="space-y-0.5 min-w-0 pr-2">
                              <div className="flex items-center gap-1.5">
                                <span className="font-mono font-bold text-xs text-indigo-300 group-hover:text-indigo-200">
                                  ✓ {ac.code}
                                </span>
                                <span className="text-[10px] text-emerald-400 font-bold">
                                  — {discountValText}
                                </span>
                              </div>
                              <p className="text-[10px] text-slate-400 truncate">
                                {ac.message || ac.description || ac.name}
                              </p>
                            </div>
                            <Button
                              variant="outline"
                              size="sm"
                              className="shrink-0 text-[10px] py-1 px-2 group-hover:border-indigo-500 group-hover:text-white"
                              onClick={(e) => {
                                e.stopPropagation();
                                handleApplyCoupon(ac.code);
                              }}
                            >
                              Apply
                            </Button>
                          </div>
                        );
                      })}
                    </div>
                  </div>
                ) : (
                  <div className="pt-1 text-[11px] text-slate-500">
                    No targeted offers currently active for the items in your cart.
                  </div>
                )}
              </div>
            )}
          </div>

          {/* PRICE SUMMARY CARD */}
          <div className="p-5 rounded-2xl bg-slate-950/70 border border-slate-800/80 space-y-4">
            <h3 className="text-base font-bold text-white border-b border-slate-800 pb-3">
              Price Summary
            </h3>

            <div className="space-y-2.5 text-sm">
              <div className="flex justify-between text-slate-300">
                <span>Subtotal</span>
                <span className="font-semibold text-white">₹{subtotal.toFixed(2)}</span>
              </div>

              {discountAmount > 0 && (
                <div className="flex justify-between text-emerald-400 font-medium">
                  <span className="flex items-center gap-1">
                    <Tag className="w-3.5 h-3.5" /> Coupon Discount ({appliedCoupon?.code})
                  </span>
                  <span>-₹{discountAmount.toFixed(2)}</span>
                </div>
              )}

              <div className="flex justify-between text-slate-300">
                <span>Shipping</span>
                <span className="font-semibold">
                  {shippingFee === 0 ? (
                    <span className="text-emerald-400 font-bold">FREE</span>
                  ) : (
                    `₹${shippingFee.toFixed(2)}`
                  )}
                </span>
              </div>
            </div>

            <div className="border-t border-slate-800 pt-3 flex justify-between items-center">
              <span className="font-bold text-white">Total Payable</span>
              <span className="text-2xl font-black text-indigo-300">₹{finalGrandTotal.toFixed(2)}</span>
            </div>

            <Button
              variant="primary"
              size="lg"
              className="w-full shadow-lg shadow-indigo-600/25"
              onClick={handlePlaceOrder}
              isLoading={isPlacing}
              disabled={isPlacing}
            >
              {isPlacing ? 'Processing...' : (activeBackendOrder ? 'Retry Payment' : 'Place Order')}
            </Button>

            <p className="text-[11px] text-slate-500 text-center">
              By placing your order you agree to our Terms of Service.
            </p>
          </div>

          <div className="p-4 rounded-2xl bg-slate-950/40 border border-slate-800/60 text-xs text-slate-400 space-y-2">
            <div className="flex items-center gap-2">
              <span>🔒</span>
              <span>256-bit SSL encrypted checkout.</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Checkout;
