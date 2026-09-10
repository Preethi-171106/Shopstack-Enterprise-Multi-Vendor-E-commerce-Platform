import React, { useEffect, useState, useCallback } from 'react';
import {
  Warehouse, Layers, Truck, AlertCircle, RefreshCw,
  Package, ArrowUpDown, CheckCircle2, ClipboardList,
  Search, Check, X, Box, ArrowRight, ShieldCheck,
  Send, History, Plus, Edit2, Clock, Filter, Eye,
  Building, MapPin, CheckCircle, Navigation, Archive,
  RotateCcw, AlertTriangle, FileCheck, CheckSquare, ShieldAlert
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useApp } from '../../context/AppContext';
import warehouseService from '../../services/warehouseService';
import Badge from '../../components/common/Badge';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import Loading from '../../components/common/Loading';

const shipmentStatusColor = {
  PROCESSING: 'amber',
  READY_TO_SHIP: 'indigo',
  SHIPPED: 'violet',
  OUT_FOR_DELIVERY: 'emerald',
  DELIVERED: 'emerald',
  CANCELLED: 'rose',
  RETURNED: 'rose',
};

const allocationStatusColor = {
  ALLOCATED: 'blue',
  PICKED: 'amber',
  PACKED: 'indigo',
  READY_FOR_SHIPMENT: 'emerald',
};

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

const VALID_NEXT_STATUS = {
  PROCESSING: ['READY_TO_SHIP', 'CANCELLED'],
  READY_TO_SHIP: ['SHIPPED', 'CANCELLED'],
  SHIPPED: ['OUT_FOR_DELIVERY'],
  OUT_FOR_DELIVERY: ['DELIVERED'],
  DELIVERED: ['RETURNED'],
  CANCELLED: [],
  RETURNED: [],
};

const WarehouseDashboard = () => {
  const { user } = useAuth();
  const { showNotification } = useApp();

  // Active tab: 'overview' | 'allocations' | 'picking' | 'packing' | 'shipments' | 'inventory' | 'facilities' | 'lowstock' | 'movements' | 'returns_receiving' | 'returns_qc' | 'damaged_inventory'
  const [activeTab, setActiveTab] = useState('overview');
  const [loading, setLoading] = useState(true);

  // Multi-Warehouse Facility Filter State
  const [warehouses, setWarehouses] = useState([]);
  const [selectedWarehouseId, setSelectedWarehouseId] = useState('');

  // Data States
  const [dashboardMetrics, setDashboardMetrics] = useState(null);
  const [allocations, setAllocations] = useState([]);
  const [orders, setOrders] = useState([]);
  const [inventories, setInventories] = useState([]);
  const [warehouseInventories, setWarehouseInventories] = useState([]);
  const [lowStock, setLowStock] = useState([]);
  const [shipments, setShipments] = useState([]);
  const [stockMovements, setStockMovements] = useState([]);
  const [pendingReceivingReturns, setPendingReceivingReturns] = useState([]);
  const [pendingQcReturns, setPendingQcReturns] = useState([]);
  const [damagedInventories, setDamagedInventories] = useState([]);

  // Search and Filter States
  const [allocationStatusFilter, setAllocationStatusFilter] = useState('ALL');
  const [allocationSearch, setAllocationSearch] = useState('');
  const [orderFilter, setOrderFilter] = useState('ALL');
  const [orderSearch, setOrderSearch] = useState('');
  const [inventorySearch, setInventorySearch] = useState('');
  const [inventoryStatusFilter, setInventoryStatusFilter] = useState('ALL');

  // Multi-Warehouse Allocation Action Modals
  const [activePackAllocation, setActivePackAllocation] = useState(null);
  const [allocationPackingForm, setAllocationPackingForm] = useState({ packageWeight: '1.2 kg', packageDimensions: '25x18x12 cm', notes: 'Standard protective box' });

  const [activeStageAllocation, setActiveStageAllocation] = useState(null);
  const [allocationStagingForm, setAllocationStagingForm] = useState({ carrier: 'BlueDart Express', notes: 'Staged in Bay-A' });

  // Facility Management Modal State (Admin)
  const [facilityModal, setFacilityModal] = useState(false);
  const [newFacility, setNewFacility] = useState({ warehouseCode: '', name: '', address: '', city: '', state: '', postalCode: '', country: 'India' });
  const [isSavingFacility, setIsSavingFacility] = useState(false);

  // Restock / Adjustment Modal State
  const [restockModal, setRestockModal] = useState(null);
  const [adjustmentType, setAdjustmentType] = useState('ADD'); // 'ADD' | 'REMOVE' | 'DAMAGE' | 'SET' | 'THRESHOLD'
  const [restockQty, setRestockQty] = useState('');
  const [thresholdVal, setThresholdVal] = useState('10');
  const [restockReason, setRestockReason] = useState('Inbound replenishment');
  const [isSavingStock, setIsSavingStock] = useState(false);

  // Return Receiving & Quality Check Modals
  const [activeReceiveReturn, setActiveReceiveReturn] = useState(null);
  const [receiveForm, setReceiveForm] = useState({ packageCondition: 'Good', receivingNotes: 'Product arrived safely at intake bay' });
  const [isReceivingReturn, setIsReceivingReturn] = useState(false);

  const [activeQcReturn, setActiveQcReturn] = useState(null);
  const [qcForm, setQcForm] = useState({
    result: 'ACCEPTED',
    conditionNotes: 'Inspected: Seals intact, SKU verified, excellent condition.',
    acceptedQuantity: 1,
    damagedQuantity: 0,
    moveToQuarantine: false
  });
  const [isSubmittingQc, setIsSubmittingQc] = useState(false);

  // Fetch all warehouse and multi-warehouse data
  const fetchAllData = useCallback(async () => {
    setLoading(true);
    try {
      const whIdParam = selectedWarehouseId ? Number(selectedWarehouseId) : null;

      const [whRes, dashRes, allocRes, ordersRes, invRes, lowRes, shipRes, moveRes, recvRetRes, qcRetRes, damRes, whInvRes] = await Promise.allSettled([
        warehouseService.getWarehouses(),
        warehouseService.getDashboard(),
        warehouseService.getAllocations(whIdParam),
        warehouseService.getOrders(),
        warehouseService.getInventories(),
        warehouseService.getLowStock(),
        warehouseService.getShipments(),
        warehouseService.getStockMovements(0, 50),
        warehouseService.getPendingReceivingReturns(whIdParam),
        warehouseService.getPendingQcReturns(whIdParam),
        warehouseService.getDamagedInventories(whIdParam),
        whIdParam ? warehouseService.getWarehouseInventoryByWarehouse(whIdParam) : warehouseService.getAllWarehouseInventories(),
      ]);

      if (whRes.status === 'fulfilled') setWarehouses(Array.isArray(whRes.value) ? whRes.value : (whRes.value?.content || []));
      if (dashRes.status === 'fulfilled') setDashboardMetrics(dashRes.value || null);
      if (allocRes.status === 'fulfilled') setAllocations(Array.isArray(allocRes.value) ? allocRes.value : (allocRes.value?.content || []));
      if (ordersRes.status === 'fulfilled') setOrders(Array.isArray(ordersRes.value) ? ordersRes.value : (ordersRes.value?.content || []));
      if (invRes.status === 'fulfilled') setInventories(Array.isArray(invRes.value) ? invRes.value : (invRes.value?.content || []));
      if (lowRes.status === 'fulfilled') setLowStock(Array.isArray(lowRes.value) ? lowRes.value : (lowRes.value?.content || []));
      if (shipRes.status === 'fulfilled') setShipments(Array.isArray(shipRes.value) ? shipRes.value : (shipRes.value?.content || []));
      if (moveRes.status === 'fulfilled') setStockMovements(Array.isArray(moveRes.value) ? moveRes.value : (moveRes.value?.content || []));
      if (recvRetRes.status === 'fulfilled') setPendingReceivingReturns(Array.isArray(recvRetRes.value) ? recvRetRes.value : (recvRetRes.value?.content || []));
      if (qcRetRes.status === 'fulfilled') setPendingQcReturns(Array.isArray(qcRetRes.value) ? qcRetRes.value : (qcRetRes.value?.content || []));
      if (damRes.status === 'fulfilled') setDamagedInventories(Array.isArray(damRes.value) ? damRes.value : (damRes.value?.content || []));
      if (whInvRes.status === 'fulfilled') setWarehouseInventories(Array.isArray(whInvRes.value) ? whInvRes.value : (whInvRes.value?.content || []));
    } catch (err) {
      console.error('Warehouse load error:', err);
      showNotification('Failed to load some warehouse metrics', 'error');
    } finally {
      setLoading(false);
    }
  }, [selectedWarehouseId, showNotification]);

  useEffect(() => {
    fetchAllData();
  }, [fetchAllData]);

  const handleUpdateShipmentStatus = async (shipmentId, nextStatus) => {
    try {
      await warehouseService.updateShipmentStatus(shipmentId, { status: nextStatus });
      showNotification(`Shipment status updated to ${nextStatus}`, 'success');
      fetchAllData();
    } catch (err) {
      showNotification(err.response?.data?.message || 'Failed to update shipment status', 'error');
    }
  };

  // ── Allocation Workflow Handlers ──────────────────────────────────────────
  const handlePickAllocation = async (allocationId) => {
    try {
      const updated = await warehouseService.pickAllocation(allocationId);
      setAllocations((prev) => prev.map((a) => (a.id === allocationId ? updated : a)));
      showNotification(`Item picked from ${updated.warehouseCode}! Moved to Packing.`, 'success');
      fetchAllData();
    } catch (err) {
      showNotification(err.response?.data?.message || 'Failed to pick allocation', 'error');
    }
  };

  const handlePackAllocation = async (e) => {
    e.preventDefault();
    if (!activePackAllocation) return;
    try {
      const updated = await warehouseService.packAllocation(activePackAllocation.id, allocationPackingForm);
      setAllocations((prev) => prev.map((a) => (a.id === activePackAllocation.id ? updated : a)));
      setActivePackAllocation(null);
      showNotification(`Item packed at ${updated.warehouseCode}! Ready for dispatch staging.`, 'success');
      fetchAllData();
    } catch (err) {
      showNotification(err.response?.data?.message || 'Failed to pack allocation', 'error');
    }
  };

  const handleStageAllocation = async (e) => {
    e.preventDefault();
    if (!activeStageAllocation) return;
    try {
      const updated = await warehouseService.readyAllocationForShipment(activeStageAllocation.id, allocationStagingForm);
      setAllocations((prev) => prev.map((a) => (a.id === activeStageAllocation.id ? updated : a)));
      setActiveStageAllocation(null);
      showNotification(`Allocation staged as READY FOR SHIPMENT!`, 'success');
      fetchAllData();
    } catch (err) {
      showNotification(err.response?.data?.message || 'Failed to stage allocation', 'error');
    }
  };

  // ── Facility Management Handlers (Admin) ──────────────────────────────────
  const handleCreateFacility = async (e) => {
    e.preventDefault();
    if (!newFacility.warehouseCode.trim() || !newFacility.name.trim()) {
      showNotification('Warehouse code and name are required', 'error');
      return;
    }
    setIsSavingFacility(true);
    try {
      const created = await warehouseService.createWarehouse(newFacility);
      setWarehouses((prev) => [...prev, created]);
      setFacilityModal(false);
      setNewFacility({ warehouseCode: '', name: '', address: '', city: '', state: '', postalCode: '', country: 'India' });
      showNotification(`Warehouse ${created.warehouseCode} created successfully!`, 'success');
      fetchAllData();
    } catch (err) {
      showNotification(err.response?.data?.message || 'Failed to create facility', 'error');
    } finally {
      setIsSavingFacility(false);
    }
  };

  const handleToggleFacilityActive = async (wh) => {
    try {
      if (wh.active) {
        await warehouseService.deactivateWarehouse(wh.id);
        showNotification(`Warehouse ${wh.warehouseCode} deactivated`, 'info');
      } else {
        await warehouseService.activateWarehouse(wh.id);
        showNotification(`Warehouse ${wh.warehouseCode} activated`, 'success');
      }
      fetchAllData();
    } catch (err) {
      showNotification(err.response?.data?.message || 'Failed to toggle status', 'error');
    }
  };

  // ── Stock Replenishment & Adjustment Handler ─────────────────────────────
  const handleStockAdjustmentSubmit = async (e) => {
    e.preventDefault();
    if (adjustmentType === 'THRESHOLD') {
      if (!thresholdVal || parseInt(thresholdVal) < 0) {
        showNotification('Please enter a valid threshold value', 'error');
        return;
      }
      setIsSavingStock(true);
      try {
        await warehouseService.updateThreshold(restockModal.productId, thresholdVal);
        showNotification(`Low-stock threshold updated for ${restockModal.productName}`, 'success');
        setRestockModal(null);
        fetchAllData();
      } catch (err) {
        showNotification(err.response?.data?.message || 'Failed to update threshold', 'error');
      } finally {
        setIsSavingStock(false);
      }
      return;
    }

    if (!restockQty || parseInt(restockQty) < 0) {
      showNotification('Please enter a valid stock quantity', 'error');
      return;
    }
    setIsSavingStock(true);
    try {
      if (adjustmentType === 'ADD') {
        if (selectedWarehouseId) {
          await warehouseService.addWarehouseProductStock(selectedWarehouseId, {
            productId: restockModal.productId,
            initialQuantity: parseInt(restockQty),
            lowStockThreshold: 10,
          });
        } else {
          await warehouseService.addStock(restockModal.productId, restockQty, restockReason);
        }
        showNotification(`Added ${restockQty} units to ${restockModal.productName}`, 'success');
      } else {
        if (selectedWarehouseId) {
          await warehouseService.adjustWarehouseProductStock(selectedWarehouseId, restockModal.productId, {
            adjustmentType,
            quantity: parseInt(restockQty),
            reason: restockReason,
          });
        } else {
          await warehouseService.adjustStock(restockModal.productId, {
            adjustmentType,
            quantity: parseInt(restockQty),
            newTotalStock: adjustmentType === 'SET' ? parseInt(restockQty) : null,
            reason: restockReason,
          });
        }
        showNotification(`Stock adjusted (${adjustmentType}) for ${restockModal.productName}`, 'success');
      }
      setRestockModal(null);
      setRestockQty('');
      fetchAllData();
    } catch (err) {
      showNotification(err.response?.data?.message || 'Failed to adjust stock', 'error');
    } finally {
      setIsSavingStock(false);
    }
  };

  // ── Returns & Quality Check Handlers (Warehouse Staff) ───────────────────
  const handleOpenReceiveModal = (ret) => {
    setActiveReceiveReturn(ret);
    setReceiveForm({
      packageCondition: 'Good',
      receivingNotes: `Intake verified for ${ret.returnNumber} (${ret.productName || 'Item'})`
    });
  };

  const handleReceiveReturnSubmit = async (e) => {
    e.preventDefault();
    if (!activeReceiveReturn) return;
    setIsReceivingReturn(true);
    try {
      await warehouseService.receiveReturn(activeReceiveReturn.id, receiveForm);
      showNotification(`Return ${activeReceiveReturn.returnNumber} received! Moved to Quality Check queue.`, 'success');
      setActiveReceiveReturn(null);
      fetchAllData();
    } catch (err) {
      showNotification(err.response?.data?.message || 'Failed to receive return', 'error');
    } finally {
      setIsReceivingReturn(false);
    }
  };

  const handleOpenQcModal = (ret) => {
    setActiveQcReturn(ret);
    const qty = ret.returnQuantity || 1;
    setQcForm({
      result: 'ACCEPTED',
      conditionNotes: 'All items in original packaging, verified functional.',
      acceptedQuantity: qty,
      damagedQuantity: 0,
      moveToQuarantine: false,
    });
  };

  const handlePerformQcSubmit = async (e) => {
    e.preventDefault();
    if (!activeQcReturn) return;
    setIsSubmittingQc(true);
    try {
      await warehouseService.performQualityCheck(activeQcReturn.id, qcForm);
      const resMsg = qcForm.result === 'ACCEPTED'
        ? `Quality check passed: Restocked ${qcForm.acceptedQuantity} units & initiated refund!`
        : `Quality check completed: ${qcForm.damagedQuantity} units isolated to damaged/quarantine stock.`;
      showNotification(resMsg, 'success');
      setActiveQcReturn(null);
      fetchAllData();
    } catch (err) {
      showNotification(err.response?.data?.message || 'Failed to submit quality check', 'error');
    } finally {
      setIsSubmittingQc(false);
    }
  };

  // Filtered lists
  const filteredAllocations = (allocations || []).filter((a) => {
    let matchesStatus =
      allocationStatusFilter === 'ALL' ||
      a.allocationStatus === allocationStatusFilter;
    if (activeTab === 'picking') {
      matchesStatus = a.allocationStatus === 'ALLOCATED';
    } else if (activeTab === 'packing') {
      matchesStatus = a.allocationStatus === 'PICKED';
    }

    const matchesSearch =
      !allocationSearch ||
      a.orderNumber?.toLowerCase().includes(allocationSearch.toLowerCase()) ||
      a.productName?.toLowerCase().includes(allocationSearch.toLowerCase()) ||
      a.productSku?.toLowerCase().includes(allocationSearch.toLowerCase()) ||
      a.warehouseCode?.toLowerCase().includes(allocationSearch.toLowerCase()) ||
      a.warehouseName?.toLowerCase().includes(allocationSearch.toLowerCase());
    return matchesStatus && matchesSearch;
  });

  const displayInventories = warehouseInventories.length > 0 || selectedWarehouseId
    ? warehouseInventories
    : inventories;

  const filteredInventories = (displayInventories || []).filter((inv) => {
    const isOutOfStock = (inv.availableQuantity !== undefined ? inv.availableQuantity : inv.availableStock) <= 0;
    const isLow = inv.lowStock || inv.isLowStock;
    const matchesStatus =
      inventoryStatusFilter === 'ALL' ||
      (inventoryStatusFilter === 'IN_STOCK' && !isOutOfStock && !isLow) ||
      (inventoryStatusFilter === 'LOW_STOCK' && isLow && !isOutOfStock) ||
      (inventoryStatusFilter === 'OUT_OF_STOCK' && isOutOfStock);

    const matchesSearch =
      !inventorySearch ||
      inv.productName?.toLowerCase().includes(inventorySearch.toLowerCase()) ||
      inv.productSku?.toLowerCase().includes(inventorySearch.toLowerCase()) ||
      inv.warehouseCode?.toLowerCase().includes(inventorySearch.toLowerCase());
    return matchesStatus && matchesSearch;
  });

  const allocatedItemsCount = (allocations || []).filter((a) => a.allocationStatus === 'ALLOCATED').length;
  const pickedItemsCount = (allocations || []).filter((a) => a.allocationStatus === 'PICKED').length;
  const packedItemsCount = (allocations || []).filter((a) => a.allocationStatus === 'PACKED').length;
  const readyShipItemsCount = (allocations || []).filter((a) => a.allocationStatus === 'READY_FOR_SHIPMENT').length;

  return (
    <div className="flex-1 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 w-full space-y-8">
      {/* ── Top Header ────────────────────────────────────────────────────── */}
      <div className="p-6 sm:p-8 rounded-3xl bg-slate-950/90 border border-slate-800 flex flex-col md:flex-row items-start md:items-center justify-between gap-6 shadow-2xl backdrop-blur-xl">
        <div className="space-y-1.5">
          <div className="flex items-center gap-3">
            <h1 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight">
              Warehouse Command Center
            </h1>
            <Badge color="amber" size="md" icon={Warehouse}>MULTI-WAREHOUSE</Badge>
          </div>
          <p className="text-xs sm:text-sm text-slate-400">
            Multi-facility routing, allocations, picking/packing, returns intake & quality inspection • Operator: <span className="text-amber-300 font-medium">{user?.email}</span>
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-3">
          {/* Facility Selector */}
          <div className="flex items-center gap-2 bg-slate-900/90 px-3 py-2 rounded-xl border border-slate-700/80">
            <Building className="w-4 h-4 text-amber-400" />
            <select
              value={selectedWarehouseId}
              onChange={(e) => setSelectedWarehouseId(e.target.value)}
              className="bg-transparent text-xs font-semibold text-white focus:outline-none cursor-pointer"
            >
              <option value="" className="bg-slate-900 text-white">All Physical Facilities ({warehouses.length})</option>
              {warehouses.map((w) => (
                <option key={w.id} value={w.id} className="bg-slate-900 text-white">
                  {w.warehouseCode} — {w.city} ({w.name})
                </option>
              ))}
            </select>
          </div>

          <Button variant="outline" size="md" icon={RefreshCw} onClick={fetchAllData} disabled={loading}>
            {loading ? 'Syncing...' : 'Sync Network'}
          </Button>
        </div>
      </div>

      {/* ── Tab Navigation ────────────────────────────────────────────────── */}
      <div className="flex items-center gap-2 overflow-x-auto pb-2 border-b border-slate-800 scrollbar-none">
        {[
          { id: 'overview', label: 'Network Overview', icon: Warehouse },
          { id: 'allocations', label: 'Allocations Queue', icon: Package, count: allocations.length },
          { id: 'picking', label: 'Picking Station', icon: ClipboardList, count: allocatedItemsCount },
          { id: 'packing', label: 'Packing Station', icon: Box, count: pickedItemsCount },
          { id: 'shipments', label: 'Shipments & Staging', icon: Truck, count: shipments.length },
          { id: 'returns_receiving', label: 'Return Intake', icon: RotateCcw, count: pendingReceivingReturns.length },
          { id: 'returns_qc', label: 'Quality Check', icon: ShieldCheck, count: pendingQcReturns.length },
          { id: 'damaged_inventory', label: 'Damaged & Quarantine', icon: AlertTriangle, count: damagedInventories.length },
          { id: 'inventory', label: 'Warehouse Stock', icon: Layers, count: (warehouseInventories.length > 0 ? warehouseInventories.length : inventories.length) },
          { id: 'facilities', label: 'Physical Facilities', icon: Building, count: warehouses.length },
          { id: 'lowstock', label: 'Low Stock Alerts', icon: AlertCircle, count: lowStock.length },
          { id: 'movements', label: 'Stock Movement Audit', icon: History },
        ].map((tab) => {
          const Icon = tab.icon;
          const isActive = activeTab === tab.id;
          return (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`flex items-center gap-2 px-4 py-2.5 rounded-xl text-xs sm:text-sm font-semibold transition-all whitespace-nowrap ${
                isActive
                  ? 'bg-amber-600 text-white shadow-lg shadow-amber-600/30'
                  : 'bg-slate-900/60 text-slate-400 hover:text-white hover:bg-slate-800/80 border border-slate-800'
              }`}
            >
              <Icon className="w-4 h-4" />
              <span>{tab.label}</span>
              {tab.count !== undefined && tab.count > 0 && (
                <span className={`px-1.5 py-0.5 rounded-full text-[10px] ${isActive ? 'bg-white/20 text-white' : 'bg-slate-800 text-amber-400'}`}>
                  {tab.count}
                </span>
              )}
            </button>
          );
        })}
      </div>

      {loading ? (
        <Loading message="Synchronizing multi-warehouse allocation records & facilities..." />
      ) : (
        <>
          {/* ══════════════════════════════════════════════════════════════════ */}
          {/* TAB 1: NETWORK OVERVIEW                                            */}
          {/* ══════════════════════════════════════════════════════════════════ */}
          {activeTab === 'overview' && (
            <div className="space-y-8">
              {/* Metric KPI Cards */}
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-4">
                <div className="p-5 rounded-2xl bg-slate-950/70 border border-slate-800/90 hover:border-amber-500/50 transition-all">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">Total Facilities</span>
                    <div className="p-2 rounded-xl bg-amber-500/10 text-amber-400">
                      <Building className="w-4 h-4" />
                    </div>
                  </div>
                  <p className="text-2xl font-extrabold text-white mt-2">{warehouses.length}</p>
                  <p className="text-[11px] text-slate-400 mt-1 font-medium">{warehouses.filter((w) => w.active).length} Active Warehouses</p>
                </div>

                <div className="p-5 rounded-2xl bg-slate-950/70 border border-slate-800/90 hover:border-blue-500/50 transition-all">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">Allocated Items</span>
                    <div className="p-2 rounded-xl bg-blue-500/10 text-blue-400">
                      <Package className="w-4 h-4" />
                    </div>
                  </div>
                  <p className="text-2xl font-extrabold text-white mt-2">{allocatedItemsCount}</p>
                  <p className="text-[11px] text-blue-300 mt-1 font-medium">Awaiting Physical Pick</p>
                </div>

                <div className="p-5 rounded-2xl bg-slate-950/70 border border-slate-800/90 hover:border-amber-500/50 transition-all">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">Picked Queue</span>
                    <div className="p-2 rounded-xl bg-amber-500/10 text-amber-400">
                      <ClipboardList className="w-4 h-4" />
                    </div>
                  </div>
                  <p className="text-2xl font-extrabold text-white mt-2">{pickedItemsCount}</p>
                  <p className="text-[11px] text-amber-300 mt-1 font-medium">Ready For Packing</p>
                </div>

                <div className="p-5 rounded-2xl bg-slate-950/70 border border-slate-800/90 hover:border-indigo-500/50 transition-all">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">Packed Items</span>
                    <div className="p-2 rounded-xl bg-indigo-500/10 text-indigo-400">
                      <Box className="w-4 h-4" />
                    </div>
                  </div>
                  <p className="text-2xl font-extrabold text-white mt-2">{packedItemsCount}</p>
                  <p className="text-[11px] text-indigo-300 mt-1 font-medium">Ready For Staging</p>
                </div>

                <div className="p-5 rounded-2xl bg-slate-950/70 border border-slate-800/90 hover:border-emerald-500/50 transition-all">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">Ready to Ship</span>
                    <div className="p-2 rounded-xl bg-emerald-500/10 text-emerald-400">
                      <Truck className="w-4 h-4" />
                    </div>
                  </div>
                  <p className="text-2xl font-extrabold text-white mt-2">{readyShipItemsCount}</p>
                  <p className="text-[11px] text-emerald-300 mt-1 font-medium">Staged in Bay</p>
                </div>

                <div className="p-5 rounded-2xl bg-slate-950/70 border border-slate-800/90 hover:border-rose-500/50 transition-all">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">Low Stock SKUs</span>
                    <div className="p-2 rounded-xl bg-rose-500/10 text-rose-400">
                      <AlertCircle className="w-4 h-4" />
                    </div>
                  </div>
                  <p className="text-2xl font-extrabold text-white mt-2">{lowStock.length}</p>
                  <p className="text-[11px] text-rose-300 mt-1 font-medium">Threshold Alerts</p>
                </div>
              </div>

              {/* Physical Facilities Grid */}
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <h2 className="text-lg font-bold text-white flex items-center gap-2">
                    <Building className="w-5 h-5 text-amber-400" />
                    Physical Warehouse Locations
                  </h2>
                  {user?.role === 'ADMIN' && (
                    <Button variant="primary" size="sm" icon={Plus} onClick={() => setFacilityModal(true)}>
                      Add Facility
                    </Button>
                  )}
                </div>

                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                  {(warehouses || []).length === 0 ? (
                    <div className="col-span-full py-8 text-center text-slate-500 text-xs">
                      No physical warehouse facilities registered yet.
                    </div>
                  ) : (
                    (warehouses || []).map((wh) => (
                      <div key={wh.id} className="p-5 rounded-2xl bg-slate-900/80 border border-slate-800 hover:border-slate-700 transition-all space-y-3">
                        <div className="flex items-center justify-between">
                          <span className="px-2.5 py-1 rounded-lg bg-amber-500/10 text-amber-400 text-xs font-bold font-mono">
                            {wh.warehouseCode}
                          </span>
                          <Badge color={wh.active ? 'emerald' : 'rose'} size="sm">
                            {wh.active ? 'ACTIVE' : 'DISABLED'}
                          </Badge>
                        </div>
                        <div>
                          <h3 className="text-base font-bold text-white">{wh.name}</h3>
                          <p className="text-xs text-slate-400 flex items-center gap-1 mt-1">
                            <MapPin className="w-3.5 h-3.5 text-slate-500" />
                            {wh.city}, {wh.state} ({wh.postalCode})
                          </p>
                        </div>
                        <div className="flex items-center justify-between text-xs text-slate-400 pt-2 border-t border-slate-800/80">
                          <span>{wh.totalSkus || 0} Stocked SKUs</span>
                          <span className="font-semibold text-white">{wh.totalStock || 0} Units On-Hand</span>
                        </div>
                      </div>
                    ))
                  )}
                </div>
              </div>
            </div>
          )}

          {/* ══════════════════════════════════════════════════════════════════ */}
          {/* TAB 2: ALLOCATIONS QUEUE                                           */}
          {/* ══════════════════════════════════════════════════════════════════ */}
          {(activeTab === 'allocations' || activeTab === 'picking' || activeTab === 'packing') && (
            <div className="space-y-6">
              {/* Filter controls */}
              <div className="flex flex-col sm:flex-row items-center justify-between gap-4 p-4 rounded-2xl bg-slate-900/60 border border-slate-800">
                <div className="relative flex-1 w-full">
                  <Search className="w-4 h-4 text-slate-500 absolute left-3 top-1/2 -translate-y-1/2" />
                  <input
                    type="text"
                    placeholder="Search by Order #, SKU, Product, or Warehouse..."
                    value={allocationSearch}
                    onChange={(e) => setAllocationSearch(e.target.value)}
                    className="w-full pl-9 pr-4 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-white placeholder-slate-500 focus:outline-none focus:border-amber-500"
                  />
                </div>

                {activeTab === 'allocations' && (
                  <div className="flex items-center gap-2 overflow-x-auto w-full sm:w-auto">
                    {['ALL', 'ALLOCATED', 'PICKED', 'PACKED', 'READY_FOR_SHIPMENT'].map((st) => (
                      <button
                        key={st}
                        onClick={() => setAllocationStatusFilter(st)}
                        className={`px-3 py-1.5 rounded-lg text-xs font-semibold whitespace-nowrap transition-all ${
                          allocationStatusFilter === st
                            ? 'bg-amber-600 text-white'
                            : 'bg-slate-950 text-slate-400 hover:text-white border border-slate-800'
                        }`}
                      >
                        {st.replace('_', ' ')}
                      </button>
                    ))}
                  </div>
                )}
              </div>

              {/* Allocations Table */}
              <div className="rounded-2xl border border-slate-800 overflow-hidden bg-slate-950/70">
                <div className="overflow-x-auto">
                  <table className="w-full text-left text-xs text-slate-300">
                    <thead className="bg-slate-900/90 text-slate-400 text-[11px] uppercase tracking-wider font-semibold border-b border-slate-800">
                      <tr>
                        <th className="py-3.5 px-4">Order #</th>
                        <th className="py-3.5 px-4">Product & SKU</th>
                        <th className="py-3.5 px-4">Assigned Warehouse</th>
                        <th className="py-3.5 px-4">Qty</th>
                        <th className="py-3.5 px-4">Lifecycle Status</th>
                        <th className="py-3.5 px-4 text-right">Workflow Action</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-800/80">
                      {filteredAllocations.length === 0 ? (
                        <tr>
                          <td colSpan={6} className="py-8 text-center text-slate-500 text-xs">
                            No warehouse allocations found for the selected criteria.
                          </td>
                        </tr>
                      ) : (
                        filteredAllocations.map((alloc) => (
                          <tr key={alloc.id} className="hover:bg-slate-900/50 transition-colors">
                            <td className="py-3.5 px-4">
                              <span className="font-mono font-bold text-white">{alloc.orderNumber}</span>
                              <p className="text-[10px] text-slate-500 mt-0.5">{alloc.customerName}</p>
                            </td>
                            <td className="py-3.5 px-4">
                              <p className="font-semibold text-white">{alloc.productName}</p>
                              <span className="text-[10px] font-mono text-slate-400">{alloc.productSku}</span>
                            </td>
                            <td className="py-3.5 px-4">
                              <span className="px-2 py-0.5 rounded bg-slate-800 text-amber-300 font-mono font-semibold text-[11px]">
                                {alloc.warehouseCode}
                              </span>
                              <p className="text-[10px] text-slate-400 mt-0.5">{alloc.warehouseCity}</p>
                            </td>
                            <td className="py-3.5 px-4 font-bold text-white">
                              {alloc.allocatedQuantity} units
                            </td>
                            <td className="py-3.5 px-4">
                              <Badge color={allocationStatusColor[alloc.allocationStatus] || 'slate'} size="sm">
                                {alloc.allocationStatus}
                              </Badge>
                            </td>
                            <td className="py-3.5 px-4 text-right">
                              {alloc.allocationStatus === 'ALLOCATED' && (
                                <button
                                  onClick={() => handlePickAllocation(alloc.id)}
                                  className="px-3 py-1.5 rounded-lg bg-amber-600 hover:bg-amber-500 text-white text-xs font-semibold transition-all inline-flex items-center gap-1.5 shadow-md shadow-amber-600/20"
                                >
                                  <ClipboardList className="w-3.5 h-3.5" />
                                  <span>Mark Picked</span>
                                </button>
                              )}

                              {alloc.allocationStatus === 'PICKED' && (
                                <button
                                  onClick={() => setActivePackAllocation(alloc)}
                                  className="px-3 py-1.5 rounded-lg bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold transition-all inline-flex items-center gap-1.5 shadow-md shadow-indigo-600/20"
                                >
                                  <Box className="w-3.5 h-3.5" />
                                  <span>Pack Package</span>
                                </button>
                              )}

                              {alloc.allocationStatus === 'PACKED' && (
                                <button
                                  onClick={() => setActiveStageAllocation(alloc)}
                                  className="px-3 py-1.5 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-semibold transition-all inline-flex items-center gap-1.5 shadow-md shadow-emerald-600/20"
                                >
                                  <Truck className="w-3.5 h-3.5" />
                                  <span>Ready for Dispatch</span>
                                </button>
                              )}

                              {alloc.allocationStatus === 'READY_FOR_SHIPMENT' && (
                                <span className="inline-flex items-center gap-1 text-emerald-400 text-xs font-semibold">
                                  <CheckCircle2 className="w-4 h-4" />
                                  Staged in Bay
                                </span>
                              )}
                            </td>
                          </tr>
                        ))
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          )}

          {/* ══════════════════════════════════════════════════════════════════ */}
          {/* TAB 6: WAREHOUSE STOCK                                             */}
          {/* ══════════════════════════════════════════════════════════════════ */}
          {activeTab === 'inventory' && (
            <div className="space-y-6">
              <div className="flex flex-col sm:flex-row items-center justify-between gap-4 p-4 rounded-2xl bg-slate-900/60 border border-slate-800">
                <div className="relative flex-1 w-full">
                  <Search className="w-4 h-4 text-slate-500 absolute left-3 top-1/2 -translate-y-1/2" />
                  <input
                    type="text"
                    placeholder="Search SKU or product in warehouse catalog..."
                    value={inventorySearch}
                    onChange={(e) => setInventorySearch(e.target.value)}
                    className="w-full pl-9 pr-4 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-white placeholder-slate-500 focus:outline-none focus:border-amber-500"
                  />
                </div>

                <div className="flex items-center gap-2 overflow-x-auto w-full sm:w-auto">
                  {['ALL', 'IN_STOCK', 'LOW_STOCK', 'OUT_OF_STOCK'].map((st) => (
                    <button
                      key={st}
                      onClick={() => setInventoryStatusFilter(st)}
                      className={`px-3 py-1.5 rounded-lg text-xs font-semibold whitespace-nowrap transition-all ${
                        inventoryStatusFilter === st
                          ? 'bg-amber-600 text-white'
                          : 'bg-slate-950 text-slate-400 hover:text-white border border-slate-800'
                      }`}
                    >
                      {st.replace('_', ' ')}
                    </button>
                  ))}
                </div>
              </div>

              {/* Inventory Table */}
              <div className="rounded-2xl border border-slate-800 overflow-hidden bg-slate-950/70">
                <table className="w-full text-left text-xs text-slate-300">
                  <thead className="bg-slate-900/90 text-slate-400 text-[11px] uppercase tracking-wider font-semibold border-b border-slate-800">
                    <tr>
                      <th className="py-3.5 px-4">Product SKU & Name</th>
                      <th className="py-3.5 px-4">Facility</th>
                      <th className="py-3.5 px-4 text-center">Total Stock</th>
                      <th className="py-3.5 px-4 text-center">Reserved</th>
                      <th className="py-3.5 px-4 text-center">Available Stock</th>
                      <th className="py-3.5 px-4 text-center">Threshold</th>
                      <th className="py-3.5 px-4 text-right">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800/80">
                    {(filteredInventories || []).length === 0 ? (
                      <tr>
                        <td colSpan={7} className="py-8 text-center text-slate-500 text-xs">
                          No warehouse inventory items found matching the selected criteria.
                        </td>
                      </tr>
                    ) : (
                      (filteredInventories || []).map((inv) => {
                        const total = inv.totalQuantity !== undefined ? inv.totalQuantity : (inv.totalStock !== undefined ? inv.totalStock : 0);
                        const reserved = inv.reservedQuantity !== undefined ? inv.reservedQuantity : (inv.reservedStock !== undefined ? inv.reservedStock : 0);
                        const avail = inv.availableQuantity !== undefined ? inv.availableQuantity : (inv.availableStock !== undefined ? inv.availableStock : 0);
                        const thresh = inv.lowStockThreshold !== undefined ? inv.lowStockThreshold : 10;

                        return (
                          <tr key={inv.id || inv.productId} className="hover:bg-slate-900/50 transition-colors">
                            <td className="py-3.5 px-4">
                              <p className="font-bold text-white">{inv.productName}</p>
                              <span className="text-[10px] font-mono text-slate-400">{inv.productSku}</span>
                            </td>
                            <td className="py-3.5 px-4">
                              <span className="px-2 py-0.5 rounded bg-slate-800 text-amber-300 font-mono text-[11px]">
                                {inv.warehouseCode || 'GLOBAL POOL'}
                              </span>
                            </td>
                            <td className="py-3.5 px-4 text-center font-bold text-white">{total}</td>
                            <td className="py-3.5 px-4 text-center text-amber-400 font-bold">{reserved}</td>
                            <td className="py-3.5 px-4 text-center">
                              <span className={`font-extrabold ${avail <= 0 ? 'text-rose-400' : avail <= thresh ? 'text-amber-400' : 'text-emerald-400'}`}>
                                {avail}
                              </span>
                            </td>
                            <td className="py-3.5 px-4 text-center text-slate-400">{thresh}</td>
                            <td className="py-3.5 px-4 text-right">
                              <button
                                onClick={() => {
                                  setRestockModal(inv);
                                  setAdjustmentType('ADD');
                                  setRestockQty('50');
                                }}
                                className="px-2.5 py-1 rounded-lg bg-amber-500/10 hover:bg-amber-500/20 text-amber-300 text-xs font-semibold transition-all border border-amber-500/30"
                              >
                                Restock
                              </button>
                            </td>
                          </tr>
                        );
                      })
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* ══════════════════════════════════════════════════════════════════ */}
          {/* TAB 7: PHYSICAL FACILITIES                                         */}
          {/* ══════════════════════════════════════════════════════════════════ */}
          {activeTab === 'facilities' && (
            <div className="space-y-6">
              <div className="flex items-center justify-between">
                <div>
                  <h2 className="text-xl font-extrabold text-white">Physical Warehouse Facilities</h2>
                  <p className="text-xs text-slate-400 mt-0.5">Enterprise distribution centers with isolated physical stock pools</p>
                </div>
                {user?.role === 'ADMIN' && (
                  <Button variant="primary" size="md" icon={Plus} onClick={() => setFacilityModal(true)}>
                    Register New Warehouse
                  </Button>
                )}
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {(warehouses || []).length === 0 ? (
                  <div className="col-span-full py-8 text-center text-slate-500 text-xs">
                    No physical warehouse facilities registered.
                  </div>
                ) : (
                  (warehouses || []).map((wh) => (
                    <div key={wh.id} className="p-6 rounded-3xl bg-slate-950/80 border border-slate-800 space-y-4 shadow-xl">
                      <div className="flex items-center justify-between">
                        <span className="px-3 py-1 rounded-xl bg-amber-500/10 text-amber-400 font-mono font-bold text-xs">
                          {wh.warehouseCode}
                        </span>
                        <Badge color={wh.active ? 'emerald' : 'rose'} size="sm">
                          {wh.active ? 'OPERATIONAL' : 'DEACTIVATED'}
                        </Badge>
                      </div>

                      <div>
                        <h3 className="text-lg font-bold text-white">{wh.name}</h3>
                        <p className="text-xs text-slate-400 flex items-start gap-1.5 mt-1.5">
                          <MapPin className="w-4 h-4 text-slate-500 shrink-0 mt-0.5" />
                          <span>{wh.address}, {wh.city}, {wh.state} — {wh.postalCode}, {wh.country}</span>
                        </p>
                      </div>

                      <div className="grid grid-cols-2 gap-3 pt-3 border-t border-slate-800">
                        <div className="p-3 rounded-xl bg-slate-900/60 border border-slate-800">
                          <span className="text-[10px] text-slate-500 uppercase font-bold">Tracked SKUs</span>
                          <p className="text-base font-extrabold text-white mt-0.5">{wh.totalSkus || 0}</p>
                        </div>
                        <div className="p-3 rounded-xl bg-slate-900/60 border border-slate-800">
                          <span className="text-[10px] text-slate-500 uppercase font-bold">Units On-Hand</span>
                          <p className="text-base font-extrabold text-amber-300 mt-0.5">{wh.totalStock || 0}</p>
                        </div>
                      </div>

                      {user?.role === 'ADMIN' && (
                        <div className="pt-2 flex items-center justify-end gap-2">
                          <button
                            onClick={() => handleToggleFacilityActive(wh)}
                            className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${
                              wh.active
                                ? 'bg-rose-500/10 text-rose-300 hover:bg-rose-500/20 border border-rose-500/30'
                                : 'bg-emerald-500/10 text-emerald-300 hover:bg-emerald-500/20 border border-emerald-500/30'
                            }`}
                          >
                            {wh.active ? 'Deactivate' : 'Activate'}
                          </button>
                        </div>
                      )}
                    </div>
                  ))
                )}
              </div>
            </div>
          )}

          {/* ══════════════════════════════════════════════════════════════════ */}
          {/* TAB 8: STOCK MOVEMENT AUDIT                                        */}
          {/* ══════════════════════════════════════════════════════════════════ */}
          {activeTab === 'movements' && (
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <h2 className="text-lg font-bold text-white flex items-center gap-2">
                  <History className="w-5 h-5 text-amber-400" />
                  Immutable Stock Movement Audit Trail
                </h2>
              </div>

              <div className="rounded-2xl border border-slate-800 overflow-hidden bg-slate-950/70">
                <table className="w-full text-left text-xs text-slate-300">
                  <thead className="bg-slate-900/90 text-slate-400 text-[11px] uppercase tracking-wider font-semibold border-b border-slate-800">
                    <tr>
                      <th className="py-3.5 px-4">Date & Time</th>
                      <th className="py-3.5 px-4">Product SKU</th>
                      <th className="py-3.5 px-4">Facility</th>
                      <th className="py-3.5 px-4">Movement Type</th>
                      <th className="py-3.5 px-4 text-center">Delta Qty</th>
                      <th className="py-3.5 px-4 text-center">Stock Snapshot</th>
                      <th className="py-3.5 px-4">Reference & Notes</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800/80">
                    {(stockMovements || []).length === 0 ? (
                      <tr>
                        <td colSpan={7} className="py-8 text-center text-slate-500 text-xs">
                          No stock movement audit records found.
                        </td>
                      </tr>
                    ) : (
                      (stockMovements || []).map((m) => (
                        <tr key={m.id} className="hover:bg-slate-900/50 transition-colors">
                          <td className="py-3.5 px-4 text-slate-400 font-mono text-[11px]">
                            {m.createdAt ? new Date(m.createdAt).toLocaleString() : 'N/A'}
                          </td>
                          <td className="py-3.5 px-4 font-semibold text-white">{m.productSku || m.productName}</td>
                          <td className="py-3.5 px-4">
                            <span className="px-2 py-0.5 rounded bg-slate-800 text-amber-300 font-mono text-[10px]">
                              {m.warehouseCode || 'CENTRAL'}
                            </span>
                          </td>
                          <td className="py-3.5 px-4">
                            <Badge color={m.movementType === 'IN' || m.movementType === 'STOCK_IN' ? 'emerald' : m.movementType === 'RESERVED' ? 'amber' : 'blue'} size="sm">
                              {m.movementType}
                            </Badge>
                          </td>
                          <td className="py-3.5 px-4 text-center font-bold text-white">{m.quantity}</td>
                          <td className="py-3.5 px-4 text-center font-mono text-slate-400">
                            {m.previousStock} &rarr; <span className="text-white font-bold">{m.newStock}</span>
                          </td>
                          <td className="py-3.5 px-4 text-slate-400 text-[11px]">
                            {m.referenceId && <span className="font-mono text-indigo-300 mr-2">{m.referenceId}</span>}
                            {m.notes}
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* ══════════════════════════════════════════════════════════════════ */}
          {/* TAB 10: RETURN INTAKE QUEUE                                        */}
          {/* ══════════════════════════════════════════════════════════════════ */}
          {activeTab === 'returns_receiving' && (
            <div className="space-y-6">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                <div>
                  <h2 className="text-lg font-bold text-white flex items-center gap-2">
                    <RotateCcw className="w-5 h-5 text-amber-400" />
                    Inbound Return Receiving Bay
                  </h2>
                  <p className="text-xs text-slate-400">
                    Physical intake station for customer returns routed to this fulfillment center
                  </p>
                </div>
              </div>

              <div className="bg-slate-950/70 border border-slate-800/90 rounded-2xl overflow-hidden shadow-xl">
                <table className="w-full text-left text-xs">
                  <thead className="bg-slate-900/90 text-slate-400 font-semibold border-b border-slate-800">
                    <tr>
                      <th className="py-3.5 px-4">Return #</th>
                      <th className="py-3.5 px-4">Order / Customer</th>
                      <th className="py-3.5 px-4">Returned Item</th>
                      <th className="py-3.5 px-4 text-center">Qty</th>
                      <th className="py-3.5 px-4">Origin / Warehouse</th>
                      <th className="py-3.5 px-4">Status</th>
                      <th className="py-3.5 px-4 text-right">Intake Action</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800/60">
                    {pendingReceivingReturns.length === 0 ? (
                      <tr>
                        <td colSpan={7} className="py-8 text-center text-slate-500">
                          <RotateCcw className="w-8 h-8 mx-auto mb-2 opacity-30 text-amber-400" />
                          No pending return parcels awaiting physical receipt at this bay.
                        </td>
                      </tr>
                    ) : (
                      pendingReceivingReturns.map((ret) => (
                        <tr key={ret.id} className="hover:bg-slate-900/40 transition-colors">
                          <td className="py-3.5 px-4 font-mono font-bold text-amber-300">
                            {ret.returnNumber}
                          </td>
                          <td className="py-3.5 px-4">
                            <span className="font-mono text-white">#{ret.orderNumber}</span>
                            <div className="text-[11px] text-slate-400">{ret.customerName || ret.userEmail}</div>
                          </td>
                          <td className="py-3.5 px-4">
                            <span className="font-semibold text-white">{ret.productName || 'Order Item'}</span>
                            {ret.productSku && <span className="block font-mono text-[10px] text-slate-400">{ret.productSku}</span>}
                          </td>
                          <td className="py-3.5 px-4 text-center font-bold text-white">
                            {ret.returnQuantity || 1}
                          </td>
                          <td className="py-3.5 px-4">
                            <span className="px-2 py-0.5 rounded bg-slate-800 text-amber-300 font-mono text-[10px]">
                              {ret.returnWarehouseCode || ret.originalWarehouseCode || 'WH-CENTRAL'}
                            </span>
                          </td>
                          <td className="py-3.5 px-4">
                            <Badge color={returnStatusColor[ret.status] || 'amber'} size="sm">
                              {ret.status}
                            </Badge>
                          </td>
                          <td className="py-3.5 px-4 text-right">
                            <Button
                              variant="primary"
                              size="sm"
                              icon={CheckSquare}
                              onClick={() => handleOpenReceiveModal(ret)}
                            >
                              Receive Parcel
                            </Button>
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* ══════════════════════════════════════════════════════════════════ */}
          {/* TAB 11: QUALITY CHECK QUEUE                                         */}
          {/* ══════════════════════════════════════════════════════════════════ */}
          {activeTab === 'returns_qc' && (
            <div className="space-y-6">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                <div>
                  <h2 className="text-lg font-bold text-white flex items-center gap-2">
                    <ShieldCheck className="w-5 h-5 text-teal-400" />
                    Quality Inspection & Restock Station
                  </h2>
                  <p className="text-xs text-slate-400">
                    Inspect physical items: restock accepted units, quarantine or write-off damages, and trigger customer refunds
                  </p>
                </div>
              </div>

              <div className="bg-slate-950/70 border border-slate-800/90 rounded-2xl overflow-hidden shadow-xl">
                <table className="w-full text-left text-xs">
                  <thead className="bg-slate-900/90 text-slate-400 font-semibold border-b border-slate-800">
                    <tr>
                      <th className="py-3.5 px-4">Return #</th>
                      <th className="py-3.5 px-4">Product Inspected</th>
                      <th className="py-3.5 px-4 text-center">Return Qty</th>
                      <th className="py-3.5 px-4">Received Condition</th>
                      <th className="py-3.5 px-4">Customer Reason</th>
                      <th className="py-3.5 px-4">QC Status</th>
                      <th className="py-3.5 px-4 text-right">Inspection Action</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800/60">
                    {pendingQcReturns.length === 0 ? (
                      <tr>
                        <td colSpan={7} className="py-8 text-center text-slate-500">
                          <ShieldCheck className="w-8 h-8 mx-auto mb-2 opacity-30 text-teal-400" />
                          No returned parcels pending quality inspection.
                        </td>
                      </tr>
                    ) : (
                      pendingQcReturns.map((ret) => (
                        <tr key={ret.id} className="hover:bg-slate-900/40 transition-colors">
                          <td className="py-3.5 px-4 font-mono font-bold text-teal-300">
                            {ret.returnNumber}
                          </td>
                          <td className="py-3.5 px-4">
                            <span className="font-semibold text-white">{ret.productName || 'Returned Item'}</span>
                            <div className="text-[10px] font-mono text-slate-400">{ret.productSku}</div>
                          </td>
                          <td className="py-3.5 px-4 text-center font-bold text-white">
                            {ret.returnQuantity || 1}
                          </td>
                          <td className="py-3.5 px-4 text-slate-300">
                            <span className="px-2 py-0.5 rounded bg-slate-800 text-slate-200 font-medium text-[11px]">
                              {ret.packageCondition || 'Received'}
                            </span>
                            {ret.receivingNotes && <div className="text-[10px] text-slate-400 mt-0.5">{ret.receivingNotes}</div>}
                          </td>
                          <td className="py-3.5 px-4 text-slate-400">
                            <span className="text-slate-200">{ret.reason}</span>
                            {ret.description && <div className="text-[10px] text-slate-400 line-clamp-1">{ret.description}</div>}
                          </td>
                          <td className="py-3.5 px-4">
                            <Badge color="amber" size="sm">
                              {ret.qcStatus || 'QC_PENDING'}
                            </Badge>
                          </td>
                          <td className="py-3.5 px-4 text-right">
                            <Button
                              variant="primary"
                              size="sm"
                              icon={FileCheck}
                              onClick={() => handleOpenQcModal(ret)}
                            >
                              Perform QC
                            </Button>
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* ══════════════════════════════════════════════════════════════════ */}
          {/* TAB 12: DAMAGED & QUARANTINE INVENTORY                              */}
          {/* ══════════════════════════════════════════════════════════════════ */}
          {activeTab === 'damaged_inventory' && (
            <div className="space-y-6">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                <div>
                  <h2 className="text-lg font-bold text-white flex items-center gap-2">
                    <AlertTriangle className="w-5 h-5 text-rose-400" />
                    Damaged & Quarantine Isolation Storage
                  </h2>
                  <p className="text-xs text-slate-400">
                    Defective, damaged, and quarantined items strictly isolated from sellable customer inventory
                  </p>
                </div>
              </div>

              <div className="bg-slate-950/70 border border-slate-800/90 rounded-2xl overflow-hidden shadow-xl">
                <table className="w-full text-left text-xs">
                  <thead className="bg-slate-900/90 text-slate-400 font-semibold border-b border-slate-800">
                    <tr>
                      <th className="py-3.5 px-4">Warehouse Code</th>
                      <th className="py-3.5 px-4">Product Name</th>
                      <th className="py-3.5 px-4">SKU</th>
                      <th className="py-3.5 px-4 text-center">Sellable Available</th>
                      <th className="py-3.5 px-4 text-center text-rose-400">Damaged Stock</th>
                      <th className="py-3.5 px-4 text-center text-amber-400">Quarantine Stock</th>
                      <th className="py-3.5 px-4 text-right">Isolation Status</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800/60">
                    {damagedInventories.length === 0 ? (
                      <tr>
                        <td colSpan={7} className="py-8 text-center text-slate-500">
                          <ShieldCheck className="w-8 h-8 mx-auto mb-2 opacity-30 text-emerald-400" />
                          No damaged or quarantined items recorded across warehouses.
                        </td>
                      </tr>
                    ) : (
                      damagedInventories.map((item) => (
                        <tr key={item.id} className="hover:bg-slate-900/40 transition-colors">
                          <td className="py-3.5 px-4 font-mono font-bold text-amber-300">
                            {item.warehouseCode}
                          </td>
                          <td className="py-3.5 px-4 font-semibold text-white">
                            {item.productName}
                          </td>
                          <td className="py-3.5 px-4 font-mono text-slate-400">
                            {item.productSku}
                          </td>
                          <td className="py-3.5 px-4 text-center font-bold text-emerald-400">
                            {item.availableQuantity}
                          </td>
                          <td className="py-3.5 px-4 text-center font-bold text-rose-400">
                            {item.damagedQuantity || 0}
                          </td>
                          <td className="py-3.5 px-4 text-center font-bold text-amber-400">
                            {item.quarantineQuantity || 0}
                          </td>
                          <td className="py-3.5 px-4 text-right">
                            <span className="px-2.5 py-1 rounded-lg bg-rose-500/10 text-rose-300 text-[11px] font-semibold">
                              ISOLATED FROM SELLABLE
                            </span>
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* ══════════════════════════════════════════════════════════════════ */}
          {/* TAB: SHIPMENTS & STAGING                                          */}
          {/* ══════════════════════════════════════════════════════════════════ */}
          {activeTab === 'shipments' && (
            <div className="space-y-8">
              {/* Ready for Dispatch Staged Queue */}
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <h2 className="text-lg font-bold text-white flex items-center gap-2">
                    <Truck className="w-5 h-5 text-emerald-400" />
                    Staged Dispatch Bay (Ready For Carrier Pickup)
                  </h2>
                </div>

                <div className="rounded-2xl border border-slate-800 overflow-hidden bg-slate-950/70 shadow-xl">
                  <table className="w-full text-left text-xs text-slate-300">
                    <thead className="bg-slate-900/90 text-slate-400 text-[11px] uppercase tracking-wider font-semibold border-b border-slate-800">
                      <tr>
                        <th className="py-3.5 px-4">Order #</th>
                        <th className="py-3.5 px-4">Product & SKU</th>
                        <th className="py-3.5 px-4">Facility Bay</th>
                        <th className="py-3.5 px-4 text-center">Allocated Qty</th>
                        <th className="py-3.5 px-4">Staging Status</th>
                        <th className="py-3.5 px-4 text-right">Staged Carrier</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-800/80">
                      {(allocations || []).filter((a) => a.allocationStatus === 'READY_FOR_SHIPMENT').length === 0 ? (
                        <tr>
                          <td colSpan={6} className="py-8 text-center text-slate-500 text-xs">
                            No allocations currently staged in the dispatch bay.
                          </td>
                        </tr>
                      ) : (
                        (allocations || [])
                          .filter((a) => a.allocationStatus === 'READY_FOR_SHIPMENT')
                          .map((alloc) => (
                            <tr key={alloc.id} className="hover:bg-slate-900/50 transition-colors">
                              <td className="py-3.5 px-4">
                                <span className="font-mono font-bold text-white">{alloc.orderNumber}</span>
                                <p className="text-[10px] text-slate-500 mt-0.5">{alloc.customerName}</p>
                              </td>
                              <td className="py-3.5 px-4">
                                <p className="font-semibold text-white">{alloc.productName}</p>
                                <span className="text-[10px] font-mono text-slate-400">{alloc.productSku}</span>
                              </td>
                              <td className="py-3.5 px-4">
                                <span className="px-2 py-0.5 rounded bg-slate-800 text-amber-300 font-mono font-semibold text-[11px]">
                                  {alloc.warehouseCode}
                                </span>
                              </td>
                              <td className="py-3.5 px-4 text-center font-bold text-white">
                                {alloc.allocatedQuantity} units
                              </td>
                              <td className="py-3.5 px-4">
                                <Badge color="emerald" size="sm">
                                  READY FOR SHIPMENT
                                </Badge>
                              </td>
                              <td className="py-3.5 px-4 text-right text-slate-300 font-medium">
                                <span className="px-2.5 py-1 rounded-lg bg-emerald-500/10 text-emerald-300 font-mono text-xs">
                                  {alloc.carrier || 'Staged in Bay-A'}
                                </span>
                              </td>
                            </tr>
                          ))
                      )}
                    </tbody>
                  </table>
                </div>
              </div>

              {/* Dispatched & In-Transit Carrier Shipments */}
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <h2 className="text-lg font-bold text-white flex items-center gap-2">
                    <Navigation className="w-5 h-5 text-indigo-400" />
                    Carrier Shipments & Logistics Tracking
                  </h2>
                </div>

                <div className="rounded-2xl border border-slate-800 overflow-hidden bg-slate-950/70 shadow-xl">
                  <table className="w-full text-left text-xs text-slate-300">
                    <thead className="bg-slate-900/90 text-slate-400 text-[11px] uppercase tracking-wider font-semibold border-b border-slate-800">
                      <tr>
                        <th className="py-3.5 px-4">Tracking #</th>
                        <th className="py-3.5 px-4">Carrier</th>
                        <th className="py-3.5 px-4">Order Ref</th>
                        <th className="py-3.5 px-4">Shipping Address</th>
                        <th className="py-3.5 px-4">Status</th>
                        <th className="py-3.5 px-4 text-right">Update Status</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-800/80">
                      {(shipments || []).length === 0 ? (
                        <tr>
                          <td colSpan={6} className="py-8 text-center text-slate-500 text-xs">
                            No outbound shipments registered yet.
                          </td>
                        </tr>
                      ) : (
                        (shipments || []).map((s) => (
                          <tr key={s.id} className="hover:bg-slate-900/50 transition-colors">
                            <td className="py-3.5 px-4 font-mono font-bold text-indigo-300">
                              {s.trackingNumber || `TRK-${s.id}`}
                            </td>
                            <td className="py-3.5 px-4 font-semibold text-white">
                              {s.carrier || 'Express Delivery'}
                            </td>
                            <td className="py-3.5 px-4 font-mono text-slate-300">
                              #{s.orderNumber || s.orderId}
                            </td>
                            <td className="py-3.5 px-4 text-slate-400">
                              {s.shippingAddress ? `${s.shippingAddress.city || ''}, ${s.shippingAddress.state || ''}` : 'Fulfillment Hub'}
                            </td>
                            <td className="py-3.5 px-4">
                              <Badge color={shipmentStatusColor[s.status] || 'slate'} size="sm">
                                {s.status}
                              </Badge>
                            </td>
                            <td className="py-3.5 px-4 text-right">
                              {VALID_NEXT_STATUS[s.status] && VALID_NEXT_STATUS[s.status].length > 0 ? (
                                <div className="flex items-center justify-end gap-1.5">
                                  {VALID_NEXT_STATUS[s.status].map((nextSt) => (
                                    <button
                                      key={nextSt}
                                      onClick={() => handleUpdateShipmentStatus(s.id, nextSt)}
                                      className="px-2 py-1 rounded-lg bg-indigo-500/10 hover:bg-indigo-500/20 text-indigo-300 text-[11px] font-semibold border border-indigo-500/30 transition-all"
                                    >
                                      &rarr; {nextSt.replace(/_/g, ' ')}
                                    </button>
                                  ))}
                                </div>
                              ) : (
                                <span className="text-[11px] text-slate-500 font-medium">Finalized</span>
                              )}
                            </td>
                          </tr>
                        ))
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          )}

          {/* ══════════════════════════════════════════════════════════════════ */}
          {/* TAB: LOW STOCK ALERTS                                              */}
          {/* ══════════════════════════════════════════════════════════════════ */}
          {activeTab === 'lowstock' && (
            <div className="space-y-6">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                <div>
                  <h2 className="text-lg font-bold text-white flex items-center gap-2">
                    <AlertCircle className="w-5 h-5 text-rose-400" />
                    Low Stock Threshold Alerts
                  </h2>
                  <p className="text-xs text-slate-400">
                    Products currently at or below minimum threshold requiring replenishment
                  </p>
                </div>
              </div>

              <div className="bg-slate-950/70 border border-slate-800/90 rounded-2xl overflow-hidden shadow-xl">
                <table className="w-full text-left text-xs">
                  <thead className="bg-slate-900/90 text-slate-400 font-semibold border-b border-slate-800">
                    <tr>
                      <th className="py-3.5 px-4">Product SKU & Name</th>
                      <th className="py-3.5 px-4">Warehouse</th>
                      <th className="py-3.5 px-4 text-center">Available Stock</th>
                      <th className="py-3.5 px-4 text-center">Threshold</th>
                      <th className="py-3.5 px-4">Status</th>
                      <th className="py-3.5 px-4 text-right">Replenishment Action</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800/60">
                    {(lowStock || []).length === 0 ? (
                      <tr>
                        <td colSpan={6} className="py-8 text-center text-slate-500">
                          <CheckCircle2 className="w-8 h-8 mx-auto mb-2 opacity-30 text-emerald-400" />
                          No low stock alerts. All inventory levels are healthy!
                        </td>
                      </tr>
                    ) : (
                      (lowStock || []).map((item) => (
                        <tr key={item.id || item.productId} className="hover:bg-slate-900/40 transition-colors">
                          <td className="py-3.5 px-4">
                            <p className="font-bold text-white">{item.productName}</p>
                            <span className="text-[10px] font-mono text-slate-400">{item.productSku}</span>
                          </td>
                          <td className="py-3.5 px-4">
                            <span className="px-2 py-0.5 rounded bg-slate-800 text-amber-300 font-mono text-[10px]">
                              {item.warehouseCode || 'GLOBAL POOL'}
                            </span>
                          </td>
                          <td className="py-3.5 px-4 text-center font-bold text-rose-400">
                            {item.availableQuantity !== undefined ? item.availableQuantity : item.availableStock} units
                          </td>
                          <td className="py-3.5 px-4 text-center font-bold text-slate-300">
                            {item.lowStockThreshold || 10}
                          </td>
                          <td className="py-3.5 px-4">
                            <Badge color="rose" size="sm">
                              LOW STOCK
                            </Badge>
                          </td>
                          <td className="py-3.5 px-4 text-right">
                            <Button
                              variant="primary"
                              size="sm"
                              icon={Plus}
                              onClick={() => {
                                setRestockModal(item);
                                setAdjustmentType('ADD');
                                setRestockQty('50');
                              }}
                            >
                              Restock SKU
                            </Button>
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </>
      )}

      {/* ── Modal: Pack Allocation ─────────────────────────────────────────── */}
      {activePackAllocation && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm">
          <div className="bg-slate-950 border border-slate-800 rounded-3xl p-6 max-w-md w-full space-y-5 shadow-2xl">
            <div className="flex items-center justify-between">
              <h3 className="text-lg font-bold text-white flex items-center gap-2">
                <Box className="w-5 h-5 text-indigo-400" />
                Pack Order Item
              </h3>
              <button onClick={() => setActivePackAllocation(null)} className="text-slate-400 hover:text-white">
                <X className="w-5 h-5" />
              </button>
            </div>

            <p className="text-xs text-slate-400">
              Packing <span className="text-white font-semibold">{activePackAllocation.allocatedQuantity}x {activePackAllocation.productName}</span> at <span className="text-amber-300 font-mono">{activePackAllocation.warehouseCode}</span>.
            </p>

            <form onSubmit={handlePackAllocation} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">Package Weight</label>
                <input
                  type="text"
                  value={allocationPackingForm.packageWeight}
                  onChange={(e) => setAllocationPackingForm({ ...allocationPackingForm, packageWeight: e.target.value })}
                  className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-xl text-xs text-white focus:outline-none focus:border-indigo-500"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">Package Dimensions</label>
                <input
                  type="text"
                  value={allocationPackingForm.packageDimensions}
                  onChange={(e) => setAllocationPackingForm({ ...allocationPackingForm, packageDimensions: e.target.value })}
                  className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-xl text-xs text-white focus:outline-none focus:border-indigo-500"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">Packing Inspection Notes</label>
                <input
                  type="text"
                  value={allocationPackingForm.notes}
                  onChange={(e) => setAllocationPackingForm({ ...allocationPackingForm, notes: e.target.value })}
                  className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-xl text-xs text-white focus:outline-none focus:border-indigo-500"
                />
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <Button variant="outline" size="sm" type="button" onClick={() => setActivePackAllocation(null)}>
                  Cancel
                </Button>
                <Button variant="primary" size="sm" type="submit">
                  Seal & Mark Packed
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ── Modal: Stage Dispatch Ready ───────────────────────────────────── */}
      {activeStageAllocation && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm">
          <div className="bg-slate-950 border border-slate-800 rounded-3xl p-6 max-w-md w-full space-y-5 shadow-2xl">
            <div className="flex items-center justify-between">
              <h3 className="text-lg font-bold text-white flex items-center gap-2">
                <Truck className="w-5 h-5 text-emerald-400" />
                Stage for Carrier Pickup
              </h3>
              <button onClick={() => setActiveStageAllocation(null)} className="text-slate-400 hover:text-white">
                <X className="w-5 h-5" />
              </button>
            </div>

            <p className="text-xs text-slate-400">
              Stage item for order <span className="text-white font-mono font-bold">#{activeStageAllocation.orderNumber}</span> in dispatch staging bay at <span className="text-amber-300 font-mono">{activeStageAllocation.warehouseCode}</span>.
            </p>

            <form onSubmit={handleStageAllocation} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">Carrier Name</label>
                <input
                  type="text"
                  value={allocationStagingForm.carrier}
                  onChange={(e) => setAllocationStagingForm({ ...allocationStagingForm, carrier: e.target.value })}
                  className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-xl text-xs text-white focus:outline-none focus:border-emerald-500"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">Staging Bay / Dispatch Notes</label>
                <input
                  type="text"
                  value={allocationStagingForm.notes}
                  onChange={(e) => setAllocationStagingForm({ ...allocationStagingForm, notes: e.target.value })}
                  className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-xl text-xs text-white focus:outline-none focus:border-emerald-500"
                />
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <Button variant="outline" size="sm" type="button" onClick={() => setActiveStageAllocation(null)}>
                  Cancel
                </Button>
                <Button variant="primary" size="sm" type="submit">
                  Confirm Staging
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ── Modal: Register New Facility (Admin) ──────────────────────────── */}
      {facilityModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm">
          <div className="bg-slate-950 border border-slate-800 rounded-3xl p-6 max-w-lg w-full space-y-5 shadow-2xl">
            <div className="flex items-center justify-between">
              <h3 className="text-lg font-bold text-white flex items-center gap-2">
                <Building className="w-5 h-5 text-amber-400" />
                Register New Physical Warehouse
              </h3>
              <button onClick={() => setFacilityModal(false)} className="text-slate-400 hover:text-white">
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleCreateFacility} className="space-y-4">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1">Warehouse Code *</label>
                  <input
                    type="text"
                    placeholder="e.g. WH-KOL-01"
                    value={newFacility.warehouseCode}
                    onChange={(e) => setNewFacility({ ...newFacility, warehouseCode: e.target.value })}
                    className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-xl text-xs text-white uppercase focus:outline-none focus:border-amber-500 font-mono"
                    required
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1">Facility Name *</label>
                  <input
                    type="text"
                    placeholder="e.g. Kolkata East Depot"
                    value={newFacility.name}
                    onChange={(e) => setNewFacility({ ...newFacility, name: e.target.value })}
                    className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-xl text-xs text-white focus:outline-none focus:border-amber-500"
                    required
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">Street Address *</label>
                <input
                  type="text"
                  placeholder="Street / Industrial Area"
                  value={newFacility.address}
                  onChange={(e) => setNewFacility({ ...newFacility, address: e.target.value })}
                  className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-xl text-xs text-white focus:outline-none focus:border-amber-500"
                  required
                />
              </div>

              <div className="grid grid-cols-3 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1">City *</label>
                  <input
                    type="text"
                    placeholder="City"
                    value={newFacility.city}
                    onChange={(e) => setNewFacility({ ...newFacility, city: e.target.value })}
                    className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-xl text-xs text-white focus:outline-none focus:border-amber-500"
                    required
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1">State *</label>
                  <input
                    type="text"
                    placeholder="State"
                    value={newFacility.state}
                    onChange={(e) => setNewFacility({ ...newFacility, state: e.target.value })}
                    className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-xl text-xs text-white focus:outline-none focus:border-amber-500"
                    required
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1">Postal Code *</label>
                  <input
                    type="text"
                    placeholder="Pincode"
                    value={newFacility.postalCode}
                    onChange={(e) => setNewFacility({ ...newFacility, postalCode: e.target.value })}
                    className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-xl text-xs text-white focus:outline-none focus:border-amber-500 font-mono"
                    required
                  />
                </div>
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <Button variant="outline" size="sm" type="button" onClick={() => setFacilityModal(false)}>
                  Cancel
                </Button>
                <Button variant="primary" size="sm" type="submit" disabled={isSavingFacility}>
                  {isSavingFacility ? 'Registering...' : 'Register Facility'}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ── Modal: Stock Replenishment / Adjustment ──────────────────────── */}
      {restockModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm">
          <div className="bg-slate-950 border border-slate-800 rounded-3xl p-6 max-w-md w-full space-y-5 shadow-2xl">
            <div className="flex items-center justify-between">
              <h3 className="text-lg font-bold text-white flex items-center gap-2">
                <Layers className="w-5 h-5 text-amber-400" />
                Adjust Stock — {restockModal.productName}
              </h3>
              <button onClick={() => setRestockModal(null)} className="text-slate-400 hover:text-white">
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleStockAdjustmentSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">Adjustment Mode</label>
                <select
                  value={adjustmentType}
                  onChange={(e) => setAdjustmentType(e.target.value)}
                  className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-xl text-xs text-white focus:outline-none focus:border-amber-500"
                >
                  <option value="ADD">Stock In / Replenishment (+)</option>
                  <option value="REMOVE">Stock Out / Removal (-)</option>
                  <option value="DAMAGE">Damage / Scrap Write-off</option>
                  <option value="SET">Direct Physical Count Override</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">Quantity</label>
                <input
                  type="number"
                  min="1"
                  value={restockQty}
                  onChange={(e) => setRestockQty(e.target.value)}
                  className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-xl text-xs text-white focus:outline-none focus:border-amber-500"
                  required
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">Audit Reason</label>
                <input
                  type="text"
                  value={restockReason}
                  onChange={(e) => setRestockReason(e.target.value)}
                  className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-xl text-xs text-white focus:outline-none focus:border-amber-500"
                  required
                />
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <Button variant="outline" size="sm" type="button" onClick={() => setRestockModal(null)}>
                  Cancel
                </Button>
                <Button variant="primary" size="sm" type="submit" disabled={isSavingStock}>
                  {isSavingStock ? 'Saving...' : 'Apply Adjustment'}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ── Modal: Inbound Return Physical Intake ──────────────────────────── */}
      {activeReceiveReturn && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm">
          <div className="bg-slate-950 border border-slate-800 rounded-3xl p-6 max-w-md w-full space-y-5 shadow-2xl">
            <div className="flex items-center justify-between">
              <h3 className="text-lg font-bold text-white flex items-center gap-2">
                <RotateCcw className="w-5 h-5 text-amber-400" />
                Receive Returned Parcel
              </h3>
              <button onClick={() => setActiveReceiveReturn(null)} className="text-slate-400 hover:text-white">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-3.5 rounded-xl bg-slate-900/80 border border-slate-800 space-y-1 text-xs">
              <div className="flex justify-between">
                <span className="text-slate-400">Return Ref:</span>
                <span className="font-mono font-bold text-amber-300">{activeReceiveReturn.returnNumber}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Product:</span>
                <span className="font-semibold text-white">{activeReceiveReturn.productName}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Quantity:</span>
                <span className="font-bold text-white">{activeReceiveReturn.returnQuantity} unit(s)</span>
              </div>
            </div>

            <form onSubmit={handleReceiveReturnSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">Package Condition</label>
                <select
                  value={receiveForm.packageCondition}
                  onChange={(e) => setReceiveForm({ ...receiveForm, packageCondition: e.target.value })}
                  className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-xl text-xs text-white focus:outline-none focus:border-amber-500"
                >
                  <option value="Good">Good / Unopened Packaging</option>
                  <option value="Opened">Opened / Repackaged</option>
                  <option value="Damaged Box">Outer Box Crushed / Torn</option>
                  <option value="Defective">Suspected Defect / Broken</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">Intake Notes / Bay Staging</label>
                <input
                  type="text"
                  value={receiveForm.receivingNotes}
                  onChange={(e) => setReceiveForm({ ...receiveForm, receivingNotes: e.target.value })}
                  className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-xl text-xs text-white focus:outline-none focus:border-amber-500"
                  required
                />
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <Button variant="outline" size="sm" type="button" onClick={() => setActiveReceiveReturn(null)}>
                  Cancel
                </Button>
                <Button variant="primary" size="sm" type="submit" disabled={isReceivingReturn}>
                  {isReceivingReturn ? 'Confirming...' : 'Confirm Receipt & Stage for QC'}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ── Modal: Quality Check & Restock Inspection ─────────────────────── */}
      {activeQcReturn && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm">
          <div className="bg-slate-950 border border-slate-800 rounded-3xl p-6 max-w-lg w-full space-y-5 shadow-2xl">
            <div className="flex items-center justify-between">
              <h3 className="text-lg font-bold text-white flex items-center gap-2">
                <ShieldCheck className="w-5 h-5 text-teal-400" />
                Perform Quality Check Inspection
              </h3>
              <button onClick={() => setActiveQcReturn(null)} className="text-slate-400 hover:text-white">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-3.5 rounded-xl bg-slate-900/80 border border-slate-800 space-y-1 text-xs">
              <div className="flex justify-between">
                <span className="text-slate-400">Return #:</span>
                <span className="font-mono font-bold text-teal-300">{activeQcReturn.returnNumber}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Product:</span>
                <span className="font-semibold text-white">{activeQcReturn.productName} ({activeQcReturn.productSku})</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Total Return Qty:</span>
                <span className="font-bold text-white">{activeQcReturn.returnQuantity} unit(s)</span>
              </div>
            </div>

            <form onSubmit={handlePerformQcSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">Inspection Verdict</label>
                <select
                  value={qcForm.result}
                  onChange={(e) => {
                    const res = e.target.value;
                    const tot = activeQcReturn.returnQuantity || 1;
                    setQcForm({
                      ...qcForm,
                      result: res,
                      acceptedQuantity: res === 'ACCEPTED' ? tot : 0,
                      damagedQuantity: res === 'DAMAGED' || res === 'REJECTED' ? tot : 0,
                    });
                  }}
                  className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-xl text-xs text-white focus:outline-none focus:border-teal-500"
                >
                  <option value="ACCEPTED">ACCEPTED (Passes QC - Restock into Sellable Inventory & Refund)</option>
                  <option value="DAMAGED">DAMAGED (Defective - Isolate from Sellable Inventory)</option>
                  <option value="PARTIALLY_ACCEPTED">PARTIALLY ACCEPTED (Split units between sellable & damaged)</option>
                  <option value="REJECTED">REJECTED (Customer misuse / Warranty void)</option>
                </select>
              </div>

              {(qcForm.result === 'ACCEPTED' || qcForm.result === 'PARTIALLY_ACCEPTED') && (
                <div>
                  <label className="block text-xs font-semibold text-emerald-400 mb-1">Accepted Quantity (Restock to available stock)</label>
                  <input
                    type="number"
                    min="0"
                    max={activeQcReturn.returnQuantity || 1}
                    value={qcForm.acceptedQuantity}
                    onChange={(e) => setQcForm({ ...qcForm, acceptedQuantity: parseInt(e.target.value) || 0 })}
                    className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-xl text-xs text-white focus:outline-none focus:border-emerald-500"
                    required
                  />
                </div>
              )}

              {(qcForm.result === 'DAMAGED' || qcForm.result === 'PARTIALLY_ACCEPTED' || qcForm.result === 'REJECTED') && (
                <div className="space-y-3">
                  <div>
                    <label className="block text-xs font-semibold text-rose-400 mb-1">Damaged / Defective Quantity</label>
                    <input
                      type="number"
                      min="0"
                      max={activeQcReturn.returnQuantity || 1}
                      value={qcForm.damagedQuantity}
                      onChange={(e) => setQcForm({ ...qcForm, damagedQuantity: parseInt(e.target.value) || 0 })}
                      className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-xl text-xs text-white focus:outline-none focus:border-rose-500"
                      required
                    />
                  </div>
                  <div className="flex items-center gap-2">
                    <input
                      type="checkbox"
                      id="moveToQuarantine"
                      checked={qcForm.moveToQuarantine}
                      onChange={(e) => setQcForm({ ...qcForm, moveToQuarantine: e.target.checked })}
                      className="rounded bg-slate-900 border-slate-700 text-amber-500 focus:ring-amber-500"
                    />
                    <label htmlFor="moveToQuarantine" className="text-xs text-slate-300 cursor-pointer">
                      Flag as Quarantine Stock (Pending vendor RMA / forensic testing)
                    </label>
                  </div>
                </div>
              )}

              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">Inspector Notes & Condition Details</label>
                <textarea
                  rows="2"
                  value={qcForm.conditionNotes}
                  onChange={(e) => setQcForm({ ...qcForm, conditionNotes: e.target.value })}
                  className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-xl text-xs text-white focus:outline-none focus:border-teal-500"
                  required
                />
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <Button variant="outline" size="sm" type="button" onClick={() => setActiveQcReturn(null)}>
                  Cancel
                </Button>
                <Button variant="primary" size="sm" type="submit" disabled={isSubmittingQc}>
                  {isSubmittingQc ? 'Submitting QC...' : 'Finalize QC & Process'}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default WarehouseDashboard;
