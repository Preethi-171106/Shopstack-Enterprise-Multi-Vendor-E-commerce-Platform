import React, { useEffect, useState, useCallback } from 'react';
import {
  Package, Truck, RotateCcw, User, Clock, CheckCircle2,
  AlertTriangle, Search, Eye, ArrowRight, ShieldCheck,
  ChevronRight, RefreshCw, X, FileText, CheckCircle,
  MapPin, Box, DollarSign
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useApp } from '../../context/AppContext';
import orderService from '../../services/orderService';
import shipmentService from '../../services/shipmentService';
import returnService from '../../services/returnService';
import Badge from '../../components/common/Badge';
import Button from '../../components/common/Button';
import Loading from '../../components/common/Loading';

const returnStatusColor = {
  REQUESTED: 'amber',
  APPROVED: 'blue',
  REJECTED: 'rose',
  IN_TRANSIT: 'violet',
  ITEM_RECEIVED: 'indigo',
  QC_PENDING: 'amber',
  QC_COMPLETED: 'teal',
  REFUND_INITIATED: 'emerald',
  REFUNDED: 'emerald',
  CLOSED: 'slate',
};

const CustomerDashboard = () => {
  const { user } = useAuth();
  const { showNotification } = useApp();

  const [activeTab, setActiveTab] = useState('orders');
  const [orders, setOrders] = useState([]);
  const [shipments, setShipments] = useState([]);
  const [returns, setReturns] = useState([]);
  const [loading, setLoading] = useState(true);

  // Return Request Modal State
  const [returnModalOrder, setReturnModalOrder] = useState(null);
  const [selectedOrderItemId, setSelectedOrderItemId] = useState('');
  const [returnQuantity, setReturnQuantity] = useState(1);
  const [returnReason, setReturnReason] = useState('DEFECTIVE');
  const [returnDescription, setReturnDescription] = useState('');
  const [isSubmittingReturn, setIsSubmittingReturn] = useState(false);

  // Selected Order Detail Modal / Drawer
  const [selectedOrder, setSelectedOrder] = useState(null);

  const fetchCustomerData = useCallback(async () => {
    setLoading(true);
    try {
      const [ordersRes, shipmentsRes, returnsRes] = await Promise.allSettled([
        orderService.getMyOrders(),
        shipmentService.getCustomerShipments(),
        returnService.getCustomerReturns(),
      ]);

      if (ordersRes.status === 'fulfilled') setOrders(ordersRes.value || []);
      if (shipmentsRes.status === 'fulfilled') setShipments(shipmentsRes.value || []);
      if (returnsRes.status === 'fulfilled') setReturns(returnsRes.value || []);
    } catch (err) {
      console.error('Customer dashboard load error:', err);
      showNotification('Failed to load some profile details', 'error');
    } finally {
      setLoading(false);
    }
  }, [showNotification]);

  useEffect(() => {
    fetchCustomerData();
  }, [fetchCustomerData]);

  const handleOpenReturnModal = (order) => {
    setReturnModalOrder(order);
    const firstItem = order.items && order.items.length > 0 ? order.items[0] : null;
    setSelectedOrderItemId(firstItem ? firstItem.id : '');
    setReturnQuantity(1);
    setReturnReason('DEFECTIVE');
    setReturnDescription('Product defective upon arrival or not matching description');
  };

  const handleSubmitReturn = async (e) => {
    e.preventDefault();
    if (!returnModalOrder) return;

    setIsSubmittingReturn(true);
    try {
      await returnService.createReturnRequest(returnModalOrder.id, {
        orderItemId: selectedOrderItemId ? Number(selectedOrderItemId) : null,
        quantity: Number(returnQuantity),
        reason: returnReason,
        description: returnDescription,
      });

      showNotification('Return request submitted successfully! Our fulfillment team will review it.', 'success');
      setReturnModalOrder(null);
      fetchCustomerData();
      setActiveTab('returns');
    } catch (err) {
      showNotification(err.response?.data?.message || 'Failed to submit return request', 'error');
    } finally {
      setIsSubmittingReturn(false);
    }
  };

  return (
    <div className="flex-1 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 w-full space-y-8">
      {/* ── Header ────────────────────────────────────────────────────────── */}
      <div className="p-6 sm:p-8 rounded-3xl bg-slate-950/90 border border-slate-800 flex flex-col md:flex-row items-start md:items-center justify-between gap-6 shadow-2xl backdrop-blur-xl">
        <div className="space-y-1">
          <div className="flex items-center gap-3">
            <h1 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight">
              Customer Account
            </h1>
            <Badge color="indigo" size="md" icon={User}>BUYER PORTAL</Badge>
          </div>
          <p className="text-xs sm:text-sm text-slate-400">
            Track live consignments, view order receipts, initiate returns & monitor refunds • <span className="text-indigo-300 font-medium">{user?.email}</span>
          </p>
        </div>

        <Button variant="outline" size="md" icon={RefreshCw} onClick={fetchCustomerData} disabled={loading}>
          {loading ? 'Refreshing...' : 'Refresh Activity'}
        </Button>
      </div>

      {/* ── Metrics Cards ─────────────────────────────────────────────────── */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-5">
        <div className="p-6 rounded-2xl bg-slate-950/70 border border-slate-800 hover:border-indigo-500/40 transition-all flex items-center justify-between">
          <div className="space-y-1">
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">Total Purchases</span>
            <p className="text-3xl font-extrabold text-white">{orders.length}</p>
            <p className="text-[11px] text-slate-500">{orders.filter(o => o.orderStatus === 'DELIVERED').length} Orders Delivered</p>
          </div>
          <div className="p-3 rounded-2xl bg-indigo-500/10 text-indigo-400">
            <Package className="w-6 h-6" />
          </div>
        </div>

        <div className="p-6 rounded-2xl bg-slate-950/70 border border-slate-800 hover:border-emerald-500/40 transition-all flex items-center justify-between">
          <div className="space-y-1">
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">Live Shipments</span>
            <p className="text-3xl font-extrabold text-emerald-400">{shipments.length}</p>
            <p className="text-[11px] text-slate-500">In-transit & staging</p>
          </div>
          <div className="p-3 rounded-2xl bg-emerald-500/10 text-emerald-400">
            <Truck className="w-6 h-6" />
          </div>
        </div>

        <div className="p-6 rounded-2xl bg-slate-950/70 border border-slate-800 hover:border-amber-500/40 transition-all flex items-center justify-between">
          <div className="space-y-1">
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">Returns & Refunds</span>
            <p className="text-3xl font-extrabold text-amber-400">{returns.length}</p>
            <p className="text-[11px] text-slate-500">{returns.filter(r => r.status === 'REFUNDED').length} Completed Refunds</p>
          </div>
          <div className="p-3 rounded-2xl bg-amber-500/10 text-amber-400">
            <RotateCcw className="w-6 h-6" />
          </div>
        </div>
      </div>

      {/* ── Navigation Tabs ───────────────────────────────────────────────── */}
      <div className="flex items-center gap-2 overflow-x-auto pb-2 border-b border-slate-800 scrollbar-none">
        {[
          { id: 'orders', label: 'Order History', icon: Package, count: orders.length },
          { id: 'shipments', label: 'Shipment Tracking', icon: Truck, count: shipments.length },
          { id: 'returns', label: 'Returns & QC Status', icon: RotateCcw, count: returns.length },
        ].map((tab) => {
          const Icon = tab.icon;
          const isActive = activeTab === tab.id;
          return (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`flex items-center gap-2 px-4 py-2.5 rounded-xl text-xs sm:text-sm font-semibold transition-all whitespace-nowrap ${
                isActive
                  ? 'bg-indigo-600 text-white shadow-lg shadow-indigo-600/30'
                  : 'bg-slate-900/60 text-slate-400 hover:text-white hover:bg-slate-800/80 border border-slate-800'
              }`}
            >
              <Icon className="w-4 h-4" />
              <span>{tab.label}</span>
              {tab.count !== undefined && tab.count > 0 && (
                <span className={`px-1.5 py-0.5 rounded-full text-[10px] ${isActive ? 'bg-white/20 text-white' : 'bg-slate-800 text-slate-400'}`}>
                  {tab.count}
                </span>
              )}
            </button>
          );
        })}
      </div>

      {loading ? (
        <Loading label="Fetching your order history and live parcel logs..." />
      ) : (
        <>
          {/* ══════════════════════════════════════════════════════════════════ */}
          {/* TAB 1: ORDER HISTORY                                               */}
          {/* ══════════════════════════════════════════════════════════════════ */}
          {activeTab === 'orders' && (
            <div className="space-y-4">
              {orders.length === 0 ? (
                <div className="p-12 text-center text-slate-500 bg-slate-950/40 rounded-2xl border border-slate-800">
                  <Package className="w-10 h-10 mx-auto mb-3 opacity-30 text-indigo-400" />
                  You haven't placed any orders yet.
                </div>
              ) : (
                orders.map((o) => (
                  <div key={o.id} className="p-6 rounded-2xl bg-slate-950/70 border border-slate-800 hover:border-slate-700 transition-all space-y-4">
                    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-slate-800/60 text-xs">
                      <div className="flex items-center gap-3">
                        <span className="font-mono font-bold text-white text-sm">#{o.orderNumber || o.id}</span>
                        <Badge color={o.orderStatus === 'DELIVERED' ? 'emerald' : o.orderStatus === 'RETURN_REQUESTED' || o.orderStatus === 'RETURNED' ? 'amber' : 'indigo'} size="sm">
                          {o.orderStatus}
                        </Badge>
                      </div>
                      <div className="text-slate-400 flex items-center gap-3">
                        <span>Placed: {new Date(o.createdAt || Date.now()).toLocaleDateString()}</span>
                        <span className="font-bold text-indigo-400 text-sm">₹{o.totalAmount}</span>
                      </div>
                    </div>

                    {/* Order Items List */}
                    <div className="space-y-2">
                      {(o.items || []).map((item) => (
                        <div key={item.id} className="flex items-center justify-between p-3 rounded-xl bg-slate-900/60 border border-slate-800/80 text-xs">
                          <div className="flex items-center gap-3">
                            {item.productImageUrlSnapshot && (
                              <img src={item.productImageUrlSnapshot} alt="" className="w-10 h-10 rounded-lg object-cover bg-slate-800" />
                            )}
                            <div>
                              <p className="font-semibold text-white">{item.productNameSnapshot || 'Product Item'}</p>
                              <p className="text-slate-400 text-[11px]">Quantity: <span className="text-white font-bold">{item.quantity}</span> • Unit Price: ₹{item.unitPriceSnapshot || item.price}</p>
                            </div>
                          </div>
                          <span className="font-bold text-white">₹{(item.unitPriceSnapshot || item.price) * item.quantity}</span>
                        </div>
                      ))}
                    </div>

                    {/* Action Bar */}
                    <div className="flex items-center justify-between pt-2">
                      <p className="text-xs text-slate-400 flex items-center gap-1">
                        <MapPin className="w-3.5 h-3.5 text-slate-500" />
                        Ship to: {o.shippingAddress}
                      </p>

                      {o.orderStatus === 'DELIVERED' && (
                        <Button
                          variant="outline"
                          size="sm"
                          icon={RotateCcw}
                          onClick={() => handleOpenReturnModal(o)}
                        >
                          Request Return / Refund
                        </Button>
                      )}
                    </div>
                  </div>
                ))
              )}
            </div>
          )}

          {/* ══════════════════════════════════════════════════════════════════ */}
          {/* TAB 2: SHIPMENT TRACKING                                           */}
          {/* ══════════════════════════════════════════════════════════════════ */}
          {activeTab === 'shipments' && (
            <div className="space-y-4">
              {shipments.length === 0 ? (
                <div className="p-12 text-center text-slate-500 bg-slate-950/40 rounded-2xl border border-slate-800">
                  <Truck className="w-10 h-10 mx-auto mb-3 opacity-30 text-emerald-400" />
                  No shipments currently recorded for your orders.
                </div>
              ) : (
                shipments.map((s) => (
                  <div key={s.id} className="p-6 rounded-2xl bg-slate-950/70 border border-slate-800 space-y-4">
                    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-slate-800 text-xs">
                      <div>
                        <span className="text-slate-400">Tracking #:</span>
                        <span className="font-mono font-bold text-emerald-400 ml-2">{s.trackingNumber || 'PENDING'}</span>
                      </div>
                      <div className="flex items-center gap-2">
                        <span className="text-slate-400">Carrier: {s.carrier || 'Standard Express'}</span>
                        <Badge color="emerald" size="sm">{s.status}</Badge>
                      </div>
                    </div>

                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
                      <div className="p-3 rounded-xl bg-slate-900/60 border border-slate-800 space-y-1">
                        <span className="text-slate-400">Origin Fulfillment Hub:</span>
                        <p className="font-bold text-white">{s.originWarehouse || 'Central Regional Warehouse'}</p>
                      </div>
                      <div className="p-3 rounded-xl bg-slate-900/60 border border-slate-800 space-y-1">
                        <span className="text-slate-400">Delivery Address:</span>
                        <p className="font-bold text-white">{s.shippingAddress || 'Customer Address'}</p>
                      </div>
                    </div>
                  </div>
                ))
              )}
            </div>
          )}

          {/* ══════════════════════════════════════════════════════════════════ */}
          {/* TAB 3: RETURNS & QC STATUS                                         */}
          {/* ══════════════════════════════════════════════════════════════════ */}
          {activeTab === 'returns' && (
            <div className="space-y-4">
              {returns.length === 0 ? (
                <div className="p-12 text-center text-slate-500 bg-slate-950/40 rounded-2xl border border-slate-800">
                  <RotateCcw className="w-10 h-10 mx-auto mb-3 opacity-30 text-amber-400" />
                  No returns submitted. Eligible delivered orders can request returns above.
                </div>
              ) : (
                returns.map((ret) => (
                  <div key={ret.id} className="p-6 rounded-2xl bg-slate-950/70 border border-slate-800 space-y-4">
                    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-slate-800 text-xs">
                      <div className="flex items-center gap-3">
                        <span className="font-mono font-bold text-amber-300">{ret.returnNumber}</span>
                        <span className="text-slate-400">Order #{ret.orderNumber}</span>
                      </div>
                      <div className="flex items-center gap-2">
                        <span className="text-slate-400">Refund Value:</span>
                        <span className="font-bold text-emerald-400">₹{ret.refundAmount}</span>
                        <Badge color={returnStatusColor[ret.status] || 'amber'} size="sm">{ret.status}</Badge>
                      </div>
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-xs">
                      <div className="p-3.5 rounded-xl bg-slate-900/60 border border-slate-800 space-y-1">
                        <span className="text-slate-400">Returned Item:</span>
                        <p className="font-semibold text-white">{ret.productName || 'Order Item'}</p>
                        <p className="text-[11px] text-slate-400">Quantity: <span className="text-white font-bold">{ret.returnQuantity}</span> • Reason: {ret.reason}</p>
                      </div>

                      <div className="p-3.5 rounded-xl bg-slate-900/60 border border-slate-800 space-y-1">
                        <span className="text-slate-400">Designated Return Hub:</span>
                        <p className="font-mono font-bold text-amber-300">{ret.returnWarehouseCode || ret.originalWarehouseCode || 'Fulfillment Center'}</p>
                        <p className="text-[11px] text-slate-400">{ret.returnWarehouseName || 'Regional Center'}</p>
                      </div>

                      <div className="p-3.5 rounded-xl bg-slate-900/60 border border-slate-800 space-y-1">
                        <span className="text-slate-400">Quality Inspection Result:</span>
                        <p className="font-bold text-teal-400">{ret.qcResult || (ret.qcStatus === 'QC_PENDING' ? 'Awaiting Inspection' : 'Under Review')}</p>
                        {ret.qcNotes && <p className="text-[11px] text-slate-400">{ret.qcNotes}</p>}
                      </div>
                    </div>

                    {ret.status === 'REFUNDED' && (
                      <div className="p-3 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-xs text-emerald-300 flex items-center gap-2">
                        <CheckCircle2 className="w-4 h-4 text-emerald-400" />
                        <span>Refund of ₹{ret.refundAmount} has been processed back to your original payment method.</span>
                      </div>
                    )}
                  </div>
                ))
              )}
            </div>
          )}
        </>
      )}

      {/* ── Modal: Request Return ─────────────────────────────────────────── */}
      {returnModalOrder && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm">
          <div className="bg-slate-950 border border-slate-800 rounded-3xl p-6 max-w-lg w-full space-y-5 shadow-2xl">
            <div className="flex items-center justify-between">
              <h3 className="text-lg font-bold text-white flex items-center gap-2">
                <RotateCcw className="w-5 h-5 text-indigo-400" />
                Submit Return Request
              </h3>
              <button onClick={() => setReturnModalOrder(null)} className="text-slate-400 hover:text-white">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-3.5 rounded-xl bg-slate-900/80 border border-slate-800 space-y-1 text-xs">
              <div className="flex justify-between">
                <span className="text-slate-400">Order:</span>
                <span className="font-mono font-bold text-white">#{returnModalOrder.orderNumber || returnModalOrder.id}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Order Total:</span>
                <span className="font-bold text-indigo-400">₹{returnModalOrder.totalAmount}</span>
              </div>
            </div>

            <form onSubmit={handleSubmitReturn} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">Select Item to Return</label>
                <select
                  value={selectedOrderItemId}
                  onChange={(e) => setSelectedOrderItemId(e.target.value)}
                  className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-xl text-xs text-white focus:outline-none focus:border-indigo-500"
                  required
                >
                  {(returnModalOrder.items || []).map((item) => (
                    <option key={item.id} value={item.id}>
                      {item.productNameSnapshot || 'Product'} (Qty: {item.quantity} • ₹{item.unitPriceSnapshot || item.price} each)
                    </option>
                  ))}
                </select>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1">Return Quantity</label>
                  <input
                    type="number"
                    min="1"
                    value={returnQuantity}
                    onChange={(e) => setReturnQuantity(parseInt(e.target.value) || 1)}
                    className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-xl text-xs text-white focus:outline-none focus:border-indigo-500"
                    required
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1">Primary Reason</label>
                  <select
                    value={returnReason}
                    onChange={(e) => setReturnReason(e.target.value)}
                    className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-xl text-xs text-white focus:outline-none focus:border-indigo-500"
                  >
                    <option value="DEFECTIVE">Defective / Malfunctioning</option>
                    <option value="DAMAGED_IN_SHIPPING">Damaged during shipping</option>
                    <option value="WRONG_ITEM">Wrong item delivered</option>
                    <option value="NOT_AS_DESCRIBED">Item not as described</option>
                    <option value="OTHER">Other reason</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">Detailed Description / Issue Details</label>
                <textarea
                  rows="3"
                  value={returnDescription}
                  onChange={(e) => setReturnDescription(e.target.value)}
                  placeholder="Please describe the condition of the item and reason for return..."
                  className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-xl text-xs text-white focus:outline-none focus:border-indigo-500"
                  required
                />
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <Button variant="outline" size="sm" type="button" onClick={() => setReturnModalOrder(null)}>
                  Cancel
                </Button>
                <Button variant="primary" size="sm" type="submit" disabled={isSubmittingReturn}>
                  {isSubmittingReturn ? 'Submitting...' : 'Confirm Return Request'}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default CustomerDashboard;
