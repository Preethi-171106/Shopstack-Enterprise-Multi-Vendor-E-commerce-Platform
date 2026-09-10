import React, { useEffect, useState, useCallback } from 'react';
import {
  ShieldCheck, Package, ShoppingBag, Layers, RefreshCw, Users,
  Store, DollarSign, Percent, Activity, FileText, Download,
  CheckCircle, XCircle, AlertTriangle, Search, Filter, Eye,
  ChevronRight, ArrowUpRight, TrendingUp, Clock, HardDrive,
  Cpu, Server, Check, X, ShieldAlert, BarChart3, Tag, Plus,
  Edit2, Trash2, ToggleLeft, ToggleRight, Calendar,
  RotateCcw, Warehouse, Share2, CheckSquare
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useApp } from '../../context/AppContext';
import adminService from '../../services/adminService';
import couponService from '../../services/couponService';
import categoryService from '../../services/categoryService';
import productService from '../../services/productService';
import warehouseService from '../../services/warehouseService';
import returnService from '../../services/returnService';
import Badge from '../../components/common/Badge';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import Loading from '../../components/common/Loading';

// Scope helper functions for backward and forward compatibility
const isCategoryScope = (scope) => scope === 'CATEGORY' || scope === 'SPECIFIC_CATEGORIES';
const isProductScope = (scope) => scope === 'PRODUCT' || scope === 'SPECIFIC_PRODUCTS';
const isVendorScope = (scope) => scope === 'VENDOR' || scope === 'SPECIFIC_VENDORS';
const isPlatformScope = (scope) => !scope || scope === 'PLATFORM' || scope === 'ENTIRE_PLATFORM';

// Blank coupon form template
const BLANK_COUPON = {
  code: '',
  name: '',
  description: '',
  discountType: 'PERCENTAGE',
  discountValue: '',
  minimumOrderAmount: '',
  maximumDiscount: '',
  usageLimit: '',
  perUserLimit: '1',
  applicabilityScope: 'ENTIRE_PLATFORM',
  categoryIds: [],
  productIds: [],
  vendorIds: [],
  startDate: new Date().toISOString().slice(0, 16),
  expiryDate: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().slice(0, 16),
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

const AdminDashboard = () => {
  const { user } = useAuth();
  const { showNotification } = useApp();

  // Active tab state: 'overview' | 'vendors' | 'analytics' | 'orders' | 'commissions' | 'coupons' | 'distribution' | 'returns' | 'system' | 'reports'
  const [activeTab, setActiveTab] = useState('overview');
  const [loading, setLoading] = useState(true);

  // Data States
  const [stats, setStats] = useState(null);
  const [trends, setTrends] = useState(null);
  const [vendors, setVendors] = useState([]);
  const [orders, setOrders] = useState([]);
  const [commissions, setCommissions] = useState([]);
  const [commissionSummary, setCommissionSummary] = useState(null);
  const [systemHealth, setSystemHealth] = useState(null);
  const [adminCoupons, setAdminCoupons] = useState([]);

  // Returns Management State
  const [adminReturns, setAdminReturns] = useState([]);
  const [returnStatusFilter, setReturnStatusFilter] = useState('ALL');
  const [activeDecisionReturn, setActiveDecisionReturn] = useState(null);
  const [decisionAction, setDecisionAction] = useState('APPROVE');
  const [decisionRemarks, setDecisionRemarks] = useState('');
  const [isSavingDecision, setIsSavingDecision] = useState(false);

  // Multi-Warehouse Stock Distribution State
  const [distWarehouses, setDistWarehouses] = useState([]);
  const [selectedDistProductId, setSelectedDistProductId] = useState('');
  const [distOverview, setDistOverview] = useState(null);
  const [distAllocations, setDistAllocations] = useState({});
  const [isSavingDist, setIsSavingDist] = useState(false);
  const [loadingDistOverview, setLoadingDistOverview] = useState(false);

  // Filter & Search States
  const [vendorFilter, setVendorFilter] = useState('ALL');
  const [vendorSearch, setVendorSearch] = useState('');
  const [selectedVendorDetails, setSelectedVendorDetails] = useState(null);
  const [loadingVendorDetails, setLoadingVendorDetails] = useState(false);

  const [orderFilter, setOrderFilter] = useState('ALL');
  const [orderSearch, setOrderSearch] = useState('');
  const [selectedOrderDetails, setSelectedOrderDetails] = useState(null);

  // Coupon Management State
  const [couponFilter, setCouponFilter] = useState('ALL');
  const [couponSearch, setCouponSearch] = useState('');
  const [showCouponModal, setShowCouponModal] = useState(false);
  const [editingCoupon, setEditingCoupon] = useState(null);
  const [couponForm, setCouponForm] = useState(BLANK_COUPON);
  const [couponFormErrors, setCouponFormErrors] = useState({});
  const [savingCoupon, setSavingCoupon] = useState(false);
  const [couponAnalytics, setCouponAnalytics] = useState(null);
  const [selectedCouponUsage, setSelectedCouponUsage] = useState(null);
  const [loadingUsage, setLoadingUsage] = useState(false);
  const [showUsageModal, setShowUsageModal] = useState(false);
  const [availableCategories, setAvailableCategories] = useState([]);
  const [availableProducts, setAvailableProducts] = useState([]);

  // Business Reports State
  const [selectedReportType, setSelectedReportType] = useState('sales');
  const [reportData, setReportData] = useState(null);
  const [reportLoading, setReportLoading] = useState(false);
  const [exportingCsv, setExportingCsv] = useState(false);

  // Warehouse Staff State
  const [staffUsers, setStaffUsers] = useState([]);
  const [staffSearch, setStaffSearch] = useState('');
  const [staffStatusFilter, setStaffStatusFilter] = useState('ALL');
  const [selectedStaffDetails, setSelectedStaffDetails] = useState(null);
  const [showStaffModal, setShowStaffModal] = useState(false);
  const [showRejectStaffModal, setShowRejectStaffModal] = useState(false);
  const [rejectingStaffId, setRejectingStaffId] = useState(null);
  const [staffRejectReason, setStaffRejectReason] = useState('');
  const [staffForm, setStaffForm] = useState({
    firstName: '',
    lastName: '',
    email: '',
    phoneNumber: '',
    password: '',
    confirmPassword: '',
  });
  const [staffFormErrors, setStaffFormErrors] = useState({});
  const [savingStaff, setSavingStaff] = useState(false);

  // Fetch initial overview and all core data
  const fetchDashboardData = useCallback(async () => {
    setLoading(true);
    try {
      const [statsRes, trendsRes, vendorsRes, ordersRes, commsRes, commSummRes, healthRes, couponsRes, couponAnalyticsRes, categoriesRes, productsRes, returnsRes, whRes, staffRes] =
        await Promise.allSettled([
          adminService.getDashboardStats(),
          adminService.getAnalyticsTrends(),
          adminService.getVendors(),
          adminService.getAdminOrders(),
          adminService.getCommissions(),
          adminService.getCommissionSummary(),
          adminService.getSystemHealth(),
          couponService.getAllAdminCoupons(),
          couponService.getCouponAnalytics(),
          categoryService.getCategories(),
          productService.getProducts({ size: 100 }),
          returnService.getAllReturns(),
          warehouseService.getWarehouses(),
          adminService.getStaffUsers(),
        ]);

      if (statsRes.status === 'fulfilled') setStats(statsRes.value);
      if (trendsRes.status === 'fulfilled') setTrends(trendsRes.value);
      if (vendorsRes.status === 'fulfilled') setVendors(vendorsRes.value || []);
      if (ordersRes.status === 'fulfilled') setOrders(ordersRes.value || []);
      if (commsRes.status === 'fulfilled') setCommissions(commsRes.value || []);
      if (commSummRes.status === 'fulfilled') setCommissionSummary(commSummRes.value);
      if (healthRes.status === 'fulfilled') setSystemHealth(healthRes.value);
      if (couponsRes.status === 'fulfilled') setAdminCoupons(couponsRes.value || []);
      if (couponAnalyticsRes.status === 'fulfilled') setCouponAnalytics(couponAnalyticsRes.value);
      if (categoriesRes.status === 'fulfilled') setAvailableCategories(categoriesRes.value || []);
      if (productsRes.status === 'fulfilled') setAvailableProducts(productsRes.value?.content || []);
      if (returnsRes.status === 'fulfilled') setAdminReturns(returnsRes.value || []);
      if (whRes.status === 'fulfilled') setDistWarehouses(whRes.value || []);
      if (staffRes.status === 'fulfilled') setStaffUsers(staffRes.value || []);
    } catch (err) {
      console.error('Failed to load admin dashboard data:', err);
      showNotification('Failed to load some dashboard metrics', 'error');
    } finally {
      setLoading(false);
    }
  }, [showNotification]);

  useEffect(() => {
    fetchDashboardData();
  }, [fetchDashboardData]);

  // Handle Vendor Status Updates
  const handleVendorAction = async (vendorId, action) => {
    try {
      let updated;
      if (action === 'APPROVE') updated = await adminService.approveVendor(vendorId);
      if (action === 'REJECT') updated = await adminService.rejectVendor(vendorId);
      if (action === 'SUSPEND') updated = await adminService.suspendVendor(vendorId);

      setVendors((prev) => prev.map((v) => (v.id === vendorId ? { ...v, status: updated.status } : v)));
      if (selectedVendorDetails && selectedVendorDetails.id === vendorId) {
        setSelectedVendorDetails((prev) => ({ ...prev, status: updated.status }));
      }
      showNotification(`Vendor ${action.toLowerCase()}d successfully`, 'success');
      fetchDashboardData();
    } catch (err) {
      showNotification(err.response?.data?.message || `Failed to ${action.toLowerCase()} vendor`, 'error');
    }
  };

  // View Vendor Details Modal
  const handleViewVendor = async (vendorId) => {
    setLoadingVendorDetails(true);
    try {
      const details = await adminService.getVendorDetails(vendorId);
      setSelectedVendorDetails(details);
    } catch (err) {
      showNotification('Failed to load vendor details', 'error');
    } finally {
      setLoadingVendorDetails(false);
    }
  };

  // Handle Order Status Update
  const handleOrderStatusUpdate = async (orderId, newStatus) => {
    try {
      const updated = await adminService.updateOrderStatus(orderId, newStatus);
      setOrders((prev) => prev.map((o) => (o.id === orderId ? updated : o)));
      if (selectedOrderDetails && selectedOrderDetails.id === orderId) {
        setSelectedOrderDetails(updated);
      }
      showNotification(`Order status updated to ${newStatus}`, 'success');
      fetchDashboardData();
    } catch (err) {
      showNotification(err.response?.data?.message || 'Failed to update order status', 'error');
    }
  };

  // ── Coupon CRUD Actions ──────────────────────────────────────────────────
  const handleOpenCreateCoupon = () => {
    setEditingCoupon(null);
    setCouponForm(BLANK_COUPON);
    setCouponFormErrors({});
    setShowCouponModal(true);
  };

  const handleOpenEditCoupon = (coupon) => {
    setEditingCoupon(coupon);
    const catIds = Array.isArray(coupon.eligibleCategoryIds)
      ? coupon.eligibleCategoryIds
      : Array.from(coupon.eligibleCategoryIds || []);
    const prodIds = Array.isArray(coupon.eligibleProductIds)
      ? coupon.eligibleProductIds
      : Array.from(coupon.eligibleProductIds || []);
    const vendIds = Array.isArray(coupon.eligibleVendorIds)
      ? coupon.eligibleVendorIds
      : Array.from(coupon.eligibleVendorIds || []);

    setCouponForm({
      code: coupon.code || '',
      name: coupon.name || '',
      description: coupon.description || '',
      discountType: coupon.discountType || 'PERCENTAGE',
      discountValue: String(coupon.discountValue || ''),
      minimumOrderAmount: coupon.minimumOrderAmount ? String(coupon.minimumOrderAmount) : '',
      maximumDiscount: coupon.maximumDiscount ? String(coupon.maximumDiscount) : '',
      usageLimit: coupon.usageLimit ? String(coupon.usageLimit) : '',
      perUserLimit: coupon.perUserLimit ? String(coupon.perUserLimit) : '1',
      applicabilityScope: coupon.applicabilityScope || 'ENTIRE_PLATFORM',
      categoryIds: catIds.map(Number),
      productIds: prodIds.map(Number),
      vendorIds: vendIds.map(Number),
      startDate: coupon.startDate ? coupon.startDate.slice(0, 16) : new Date().toISOString().slice(0, 16),
      expiryDate: coupon.expiryDate ? coupon.expiryDate.slice(0, 16) : new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().slice(0, 16),
    });
    setCouponFormErrors({});
    setShowCouponModal(true);
  };

  const validateCouponForm = () => {
    const errs = {};
    if (!couponForm.code.trim()) errs.code = 'Coupon code is required';
    if (!couponForm.name.trim()) errs.name = 'Coupon name is required';
    if (!couponForm.discountValue || isNaN(parseFloat(couponForm.discountValue)) || parseFloat(couponForm.discountValue) <= 0) {
      errs.discountValue = 'Valid discount value (> 0) is required';
    }
    if (couponForm.discountType === 'PERCENTAGE' && parseFloat(couponForm.discountValue) > 100) {
      errs.discountValue = 'Percentage discount cannot exceed 100%';
    }
    if (isCategoryScope(couponForm.applicabilityScope) && (!couponForm.categoryIds || couponForm.categoryIds.length === 0)) {
      errs.categoryIds = 'Please select at least one category';
    }
    if (isProductScope(couponForm.applicabilityScope) && (!couponForm.productIds || couponForm.productIds.length === 0)) {
      errs.productIds = 'Please select at least one product';
    }
    if (isVendorScope(couponForm.applicabilityScope) && (!couponForm.vendorIds || couponForm.vendorIds.length === 0)) {
      errs.vendorIds = 'Please select at least one vendor';
    }
    if (!couponForm.startDate) errs.startDate = 'Start date is required';
    if (!couponForm.expiryDate) errs.expiryDate = 'Expiry date is required';
    if (couponForm.startDate && couponForm.expiryDate && new Date(couponForm.startDate) >= new Date(couponForm.expiryDate)) {
      errs.expiryDate = 'Expiry date must be after start date';
    }
    setCouponFormErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleSaveCoupon = async (e) => {
    e.preventDefault();
    if (!validateCouponForm()) return;
    setSavingCoupon(true);

    try {
      const payload = {
        code: couponForm.code.trim().toUpperCase(),
        name: couponForm.name.trim(),
        description: couponForm.description.trim() || null,
        discountType: couponForm.discountType,
        discountValue: parseFloat(couponForm.discountValue),
        minimumOrderAmount: couponForm.minimumOrderAmount ? parseFloat(couponForm.minimumOrderAmount) : null,
        maximumDiscount: couponForm.maximumDiscount ? parseFloat(couponForm.maximumDiscount) : null,
        usageLimit: couponForm.usageLimit ? parseInt(couponForm.usageLimit) : null,
        perUserLimit: couponForm.perUserLimit ? parseInt(couponForm.perUserLimit) : 1,
        applicabilityScope: couponForm.applicabilityScope || 'ENTIRE_PLATFORM',
        categoryIds: isCategoryScope(couponForm.applicabilityScope) ? couponForm.categoryIds.map(Number) : [],
        productIds: isProductScope(couponForm.applicabilityScope) ? couponForm.productIds.map(Number) : [],
        vendorIds: isVendorScope(couponForm.applicabilityScope) ? couponForm.vendorIds.map(Number) : [],
        startDate: couponForm.startDate.length === 16 ? `${couponForm.startDate}:00` : couponForm.startDate,
        expiryDate: couponForm.expiryDate.length === 16 ? `${couponForm.expiryDate}:00` : couponForm.expiryDate,
      };

      if (editingCoupon) {
        const updated = await couponService.updateCoupon(editingCoupon.id, payload);
        setAdminCoupons((prev) => prev.map((c) => (c.id === editingCoupon.id ? updated : c)));
        showNotification(`Coupon ${updated.code} updated successfully`, 'success');
      } else {
        const created = await couponService.createCoupon(payload);
        setAdminCoupons((prev) => [created, ...prev]);
        showNotification(`Coupon ${created.code} created successfully`, 'success');
      }

      setShowCouponModal(false);
    } catch (err) {
      showNotification(err.response?.data?.message || err.userMessage || 'Failed to save coupon', 'error');
    } finally {
      setSavingCoupon(false);
    }
  };

  const handleToggleCoupon = async (coupon) => {
    try {
      let updated;
      if (coupon.active) {
        updated = await couponService.deactivateCoupon(coupon.id);
        showNotification(`Coupon ${coupon.code} deactivated`, 'info');
      } else {
        updated = await couponService.activateCoupon(coupon.id);
        showNotification(`Coupon ${coupon.code} activated`, 'success');
      }
      setAdminCoupons((prev) => prev.map((c) => (c.id === coupon.id ? updated : c)));
      fetchDashboardData();
    } catch (err) {
      showNotification(err.response?.data?.message || 'Failed to update coupon status', 'error');
    }
  };

  const handleDeleteCoupon = async (couponId) => {
    if (!window.confirm('Are you sure you want to delete this coupon?')) return;
    try {
      await couponService.deleteCoupon(couponId);
      setAdminCoupons((prev) => prev.filter((c) => c.id !== couponId));
      showNotification('Coupon deleted successfully', 'success');
      fetchDashboardData();
    } catch (err) {
      showNotification(err.response?.data?.message || 'Failed to delete coupon', 'error');
    }
  };

  const handleViewUsage = async (coupon) => {
    setSelectedCouponUsage({ coupon, usages: [] });
    setShowUsageModal(true);
    setLoadingUsage(true);
    try {
      const usages = await couponService.getCouponUsages(coupon.id);
      setSelectedCouponUsage({ coupon, usages });
    } catch (err) {
      showNotification('Failed to load coupon usage records', 'error');
    } finally {
      setLoadingUsage(false);
    }
  };

  // Fetch Business Report
  const loadBusinessReport = useCallback(async (reportType) => {
    setReportLoading(true);
    try {
      let data;
      if (reportType === 'sales') data = await adminService.getSalesReport();
      if (reportType === 'orders') data = await adminService.getOrderReport();
      if (reportType === 'vendors') data = await adminService.getVendorReport();
      if (reportType === 'commissions') data = await adminService.getCommissionReport();
      if (reportType === 'products') data = await adminService.getProductReport();
      setReportData(data);
    } catch (err) {
      showNotification(`Failed to generate ${reportType} report`, 'error');
    } finally {
      setReportLoading(false);
    }
  }, [showNotification]);

  useEffect(() => {
    if (activeTab === 'reports') {
      loadBusinessReport(selectedReportType);
    }
  }, [activeTab, selectedReportType, loadBusinessReport]);

  // Export CSV
  const handleExportCsv = async (type) => {
    setExportingCsv(true);
    try {
      await adminService.downloadReportCsv(type);
      showNotification(`${type.toUpperCase()} report exported successfully`, 'success');
    } catch (err) {
      showNotification('Failed to export CSV report', 'error');
    } finally {
      setExportingCsv(false);
    }
  };

  // ── Stock Distribution Handlers (Admin) ──────────────────────────────────
  const handleSelectDistProduct = async (productId) => {
    setSelectedDistProductId(productId);
    if (!productId) {
      setDistOverview(null);
      setDistAllocations({});
      return;
    }
    setLoadingDistOverview(true);
    try {
      const overview = await warehouseService.getDistributionOverview(productId);
      setDistOverview(overview);
      const initAllocs = {};
      (overview.warehouseAllocations || []).forEach((w) => {
        initAllocs[w.warehouseId] = 0;
      });
      setDistAllocations(initAllocs);
    } catch (err) {
      showNotification('Failed to load product distribution overview', 'error');
    } finally {
      setLoadingDistOverview(false);
    }
  };

  const handleDistributeStockSubmit = async (e) => {
    e.preventDefault();
    if (!selectedDistProductId || !distOverview) return;

    const items = Object.entries(distAllocations)
      .filter(([_, qty]) => Number(qty) > 0)
      .map(([whId, qty]) => ({
        warehouseId: Number(whId),
        quantity: Number(qty),
      }));

    if (items.length === 0) {
      showNotification('Please allocate quantity (>0) to at least one warehouse', 'error');
      return;
    }

    const totalToDistribute = items.reduce((acc, i) => acc + i.quantity, 0);
    if (totalToDistribute > distOverview.unallocatedStock) {
      showNotification(`Total distribution (${totalToDistribute}) exceeds unallocated stock (${distOverview.unallocatedStock})`, 'error');
      return;
    }

    setIsSavingDist(true);
    try {
      await warehouseService.distributeStock({
        productId: Number(selectedDistProductId),
        distributions: items,
      });
      showNotification(`Successfully distributed ${totalToDistribute} units across warehouses!`, 'success');
      handleSelectDistProduct(selectedDistProductId);
      fetchDashboardData();
    } catch (err) {
      showNotification(err.response?.data?.message || 'Failed to distribute stock', 'error');
    } finally {
      setIsSavingDist(false);
    }
  };

  // ── Return Approval / Decision Handlers (Admin) ──────────────────────────
  const handleOpenDecisionModal = (ret, action) => {
    setActiveDecisionReturn(ret);
    setDecisionAction(action);
    setDecisionRemarks(action === 'APPROVE' ? 'Approved by admin for warehouse intake' : 'Return request declined as per store return policy');
  };

  const handleReturnDecisionSubmit = async (e) => {
    e.preventDefault();
    if (!activeDecisionReturn) return;
    setIsSavingDecision(true);
    try {
      if (decisionAction === 'APPROVE') {
        await returnService.approveReturn(activeDecisionReturn.id, { remarks: decisionRemarks });
        showNotification(`Return ${activeDecisionReturn.returnNumber} approved & assigned for intake!`, 'success');
      } else {
        await returnService.rejectReturn(activeDecisionReturn.id, { remarks: decisionRemarks });
        showNotification(`Return ${activeDecisionReturn.returnNumber} rejected`, 'info');
      }
      setActiveDecisionReturn(null);
      setDecisionRemarks('');
      fetchDashboardData();
    } catch (err) {
      showNotification(err.response?.data?.message || 'Failed to process return decision', 'error');
    } finally {
      setIsSavingDecision(false);
    }
  };

  // ── Staff Management Actions ─────────────────────────────────────────────
  const handleOpenCreateStaff = () => {
    setStaffForm({
      firstName: '',
      lastName: '',
      email: '',
      phoneNumber: '',
      password: '',
      confirmPassword: '',
    });
    setStaffFormErrors({});
    setShowStaffModal(true);
  };

  const validateStaffForm = () => {
    const errs = {};
    if (!staffForm.firstName.trim()) errs.firstName = 'First name is required';
    if (!staffForm.lastName.trim()) errs.lastName = 'Last name is required';
    if (!staffForm.email.trim()) {
      errs.email = 'Email address is required';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(staffForm.email.trim())) {
      errs.email = 'Invalid email address format';
    }
    if (!staffForm.password) {
      errs.password = 'Password is required';
    } else if (staffForm.password.length < 8) {
      errs.password = 'Password must be at least 8 characters long';
    }
    if (!staffForm.confirmPassword) {
      errs.confirmPassword = 'Confirm password is required';
    } else if (staffForm.password !== staffForm.confirmPassword) {
      errs.confirmPassword = 'Passwords do not match';
    }
    setStaffFormErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleSaveStaff = async (e) => {
    e.preventDefault();
    if (!validateStaffForm()) return;
    setSavingStaff(true);
    try {
      await adminService.createStaffUser({
        firstName: staffForm.firstName.trim(),
        lastName: staffForm.lastName.trim(),
        email: staffForm.email.trim(),
        phoneNumber: staffForm.phoneNumber.trim() || undefined,
        password: staffForm.password,
      });
      showNotification('Warehouse Staff created successfully', 'success');
      setShowStaffModal(false);
      const updatedStaff = await adminService.getStaffUsers();
      setStaffUsers(updatedStaff || []);
    } catch (err) {
      const msg = err.response?.data?.message || err.userMessage || 'Failed to create warehouse staff';
      showNotification(msg, 'error');
    } finally {
      setSavingStaff(false);
    }
  };

  const handleApproveStaff = async (staffId) => {
    try {
      await adminService.approveWarehouseStaff(staffId);
      showNotification('Warehouse Staff registration approved successfully', 'success');
      const updatedStaff = await adminService.getStaffUsers();
      setStaffUsers(updatedStaff || []);
    } catch (err) {
      showNotification(err.response?.data?.message || 'Failed to approve staff registration', 'error');
    }
  };

  const handleOpenRejectStaff = (staffId) => {
    setRejectingStaffId(staffId);
    setStaffRejectReason('');
    setShowRejectStaffModal(true);
  };

  const handleConfirmRejectStaff = async () => {
    if (!rejectingStaffId) return;
    try {
      await adminService.rejectWarehouseStaff(rejectingStaffId, staffRejectReason);
      showNotification('Warehouse Staff registration rejected', 'info');
      setShowRejectStaffModal(false);
      setRejectingStaffId(null);
      setStaffRejectReason('');
      const updatedStaff = await adminService.getStaffUsers();
      setStaffUsers(updatedStaff || []);
    } catch (err) {
      showNotification(err.response?.data?.message || 'Failed to reject staff registration', 'error');
    }
  };

  const handleSuspendStaff = async (staffId) => {
    try {
      await adminService.suspendWarehouseStaff(staffId);
      showNotification('Warehouse Staff account suspended', 'info');
      const updatedStaff = await adminService.getStaffUsers();
      setStaffUsers(updatedStaff || []);
    } catch (err) {
      showNotification(err.response?.data?.message || 'Failed to suspend staff account', 'error');
    }
  };

  const handleReactivateStaff = async (staffId) => {
    try {
      await adminService.reactivateWarehouseStaff(staffId);
      showNotification('Warehouse Staff account reactivated successfully', 'success');
      const updatedStaff = await adminService.getStaffUsers();
      setStaffUsers(updatedStaff || []);
    } catch (err) {
      showNotification(err.response?.data?.message || 'Failed to reactivate staff account', 'error');
    }
  };

  const handleToggleStaffStatus = async (userId, currentEnabled) => {
    try {
      const updated = await adminService.toggleUserStatus(userId, !currentEnabled);
      setStaffUsers((prev) => prev.map((u) => (u.id === userId ? updated : u)));
      showNotification(`Staff account ${!currentEnabled ? 'enabled' : 'disabled'} successfully`, 'success');
    } catch (err) {
      showNotification(err.response?.data?.message || 'Failed to update staff status', 'error');
    }
  };

  // Filtered lists
  const filteredStaff = staffUsers.filter((s) => {
    const fullName = `${s.firstName || s.name || ''} ${s.lastName || ''}`.toLowerCase();
    const matchesSearch =
      !staffSearch ||
      fullName.includes(staffSearch.toLowerCase()) ||
      s.email?.toLowerCase().includes(staffSearch.toLowerCase()) ||
      s.phoneNumber?.toLowerCase().includes(staffSearch.toLowerCase()) ||
      s.warehouseCode?.toLowerCase().includes(staffSearch.toLowerCase());
    const status = s.status || (s.enabled ? 'ACTIVE' : 'SUSPENDED');
    const matchesStatus = staffStatusFilter === 'ALL' || status === staffStatusFilter;
    return matchesSearch && matchesStatus;
  });

  const pendingStaffCount = staffUsers.filter((s) => s.status === 'PENDING').length;

  const filteredVendors = vendors.filter((v) => {
    const matchesFilter = vendorFilter === 'ALL' || v.status === vendorFilter;
    const matchesSearch =
      !vendorSearch ||
      v.storeName?.toLowerCase().includes(vendorSearch.toLowerCase()) ||
      v.businessEmail?.toLowerCase().includes(vendorSearch.toLowerCase());
    return matchesFilter && matchesSearch;
  });

  const filteredOrders = orders.filter((o) => {
    const matchesFilter = orderFilter === 'ALL' || o.orderStatus === orderFilter;
    const matchesSearch =
      !orderSearch ||
      o.orderNumber?.toLowerCase().includes(orderSearch.toLowerCase()) ||
      o.shippingAddress?.toLowerCase().includes(orderSearch.toLowerCase());
    return matchesFilter && matchesSearch;
  });

  const filteredCoupons = adminCoupons.filter((c) => {
    const now = new Date();
    const isExpired = c.expiryDate && new Date(c.expiryDate) < now;
    const matchesFilter =
      couponFilter === 'ALL' ||
      (couponFilter === 'ACTIVE' && c.active && !isExpired) ||
      (couponFilter === 'INACTIVE' && !c.active) ||
      (couponFilter === 'EXPIRED' && isExpired);
    const matchesSearch =
      !couponSearch ||
      c.code?.toLowerCase().includes(couponSearch.toLowerCase()) ||
      c.name?.toLowerCase().includes(couponSearch.toLowerCase()) ||
      c.description?.toLowerCase().includes(couponSearch.toLowerCase());
    return matchesFilter && matchesSearch;
  });

  const filteredReturns = adminReturns.filter((r) => {
    if (returnStatusFilter === 'ALL') return true;
    return r.status === returnStatusFilter;
  });

  const pendingReturnsCount = adminReturns.filter((r) => r.status === 'REQUESTED').length;

  return (
    <div className="flex-1 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 w-full space-y-8">
      {/* ── Top Header ────────────────────────────────────────────────────── */}
      <div className="p-6 sm:p-8 rounded-3xl bg-slate-950/90 border border-slate-800 flex flex-col md:flex-row items-start md:items-center justify-between gap-6 shadow-2xl backdrop-blur-xl">
        <div className="space-y-1.5">
          <div className="flex items-center gap-3">
            <h1 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight">
              Enterprise Admin Console
            </h1>
            <Badge color="rose" size="md" icon={ShieldCheck}>ADMINISTRATOR</Badge>
          </div>
          <p className="text-xs sm:text-sm text-slate-400">
            Marketplace supervision, warehouse distribution, staff management, return approvals & promotions • Signed in as <span className="text-indigo-300 font-medium">{user?.email}</span>
          </p>
        </div>
        <div className="flex items-center gap-3">
          <Button variant="outline" size="md" icon={RefreshCw} onClick={fetchDashboardData} disabled={loading}>
            {loading ? 'Syncing...' : 'Sync Live Data'}
          </Button>
        </div>
      </div>

      {/* ── Tab Navigation ────────────────────────────────────────────────── */}
      <div className="flex items-center gap-2 overflow-x-auto pb-2 border-b border-slate-800 scrollbar-none">
        {[
          { id: 'overview', label: 'Platform Overview', icon: Activity },
          { id: 'staff', label: 'Warehouse Staff', icon: Users, count: pendingStaffCount },
          { id: 'vendors', label: 'Vendor Management', icon: Store, count: vendors.length },
          { id: 'distribution', label: 'Stock Distribution', icon: Share2 },
          { id: 'returns', label: 'Returns & QC Oversight', icon: RotateCcw, count: pendingReturnsCount },
          { id: 'analytics', label: 'Marketplace Analytics', icon: BarChart3 },
          { id: 'orders', label: 'Order Monitoring', icon: ShoppingBag, count: orders.length },
          { id: 'commissions', label: 'Commission Hub', icon: Percent },
          { id: 'coupons', label: 'Coupons & Promos', icon: Tag, count: adminCoupons.length },
          { id: 'system', label: 'System Monitoring', icon: Server },
          { id: 'reports', label: 'Business Reports & CSV', icon: FileText },
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
        <Loading label="Aggregating live platform metrics from PostgreSQL..." />
      ) : (
        <>
          {/* ══════════════════════════════════════════════════════════════════ */}
          {/* TAB 1: OVERVIEW                                                    */}
          {/* ══════════════════════════════════════════════════════════════════ */}
          {activeTab === 'overview' && (
            <div className="space-y-8">
              {/* Metric KPI Cards */}
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
                <div className="p-6 rounded-2xl bg-slate-950/70 border border-slate-800/90 relative overflow-hidden group hover:border-indigo-500/50 transition-all">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">Total Gross Revenue</span>
                    <div className="p-2.5 rounded-xl bg-emerald-500/10 text-emerald-400">
                      <DollarSign className="w-5 h-5" />
                    </div>
                  </div>
                  <p className="text-3xl font-extrabold text-white mt-3">₹{stats?.totalRevenue ?? 0}</p>
                  <p className="text-[11px] text-emerald-400/90 flex items-center gap-1 mt-1 font-medium">
                    <TrendingUp className="w-3 h-3" /> Real PostgreSQL transactions
                  </p>
                </div>

                <div className="p-6 rounded-2xl bg-slate-950/70 border border-slate-800/90 relative overflow-hidden group hover:border-indigo-500/50 transition-all">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">Platform Commission (5%)</span>
                    <div className="p-2.5 rounded-xl bg-indigo-500/10 text-indigo-400">
                      <Percent className="w-5 h-5" />
                    </div>
                  </div>
                  <p className="text-3xl font-extrabold text-white mt-3">₹{stats?.totalPlatformCommission ?? 0}</p>
                  <p className="text-[11px] text-indigo-400/90 mt-1 font-medium">
                    Vendor Net Payout: ₹{stats?.totalVendorPayouts ?? 0}
                  </p>
                </div>

                <div className="p-6 rounded-2xl bg-slate-950/70 border border-slate-800/90 relative overflow-hidden group hover:border-indigo-500/50 transition-all">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">Total Orders</span>
                    <div className="p-2.5 rounded-xl bg-violet-500/10 text-violet-400">
                      <ShoppingBag className="w-5 h-5" />
                    </div>
                  </div>
                  <p className="text-3xl font-extrabold text-white mt-3">{stats?.totalOrders ?? 0}</p>
                  <div className="flex items-center gap-2 mt-1 text-[11px] text-slate-400">
                    <span className="text-emerald-400 font-semibold">{stats?.deliveredOrders ?? 0} Delivered</span> •{' '}
                    <span className="text-amber-400">{stats?.pendingOrders ?? 0} Pending</span>
                  </div>
                </div>

                <div className="p-6 rounded-2xl bg-slate-950/70 border border-slate-800/90 relative overflow-hidden group hover:border-indigo-500/50 transition-all">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">Registered Vendors</span>
                    <div className="p-2.5 rounded-xl bg-amber-500/10 text-amber-400">
                      <Store className="w-5 h-5" />
                    </div>
                  </div>
                  <p className="text-3xl font-extrabold text-white mt-3">{stats?.totalVendors ?? 0}</p>
                  <p className="text-[11px] text-amber-400/90 mt-1 font-medium">
                    {stats?.pendingVendorApprovals ?? 0} Pending Approvals
                  </p>
                </div>
              </div>

              {/* Secondary Stats Row */}
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
                <div className="p-4 rounded-xl bg-slate-900/80 border border-slate-800 flex items-center justify-between">
                  <div>
                    <p className="text-xs text-slate-400">Total Customers</p>
                    <p className="text-xl font-bold text-white mt-0.5">{stats?.totalCustomers ?? 0}</p>
                  </div>
                  <Users className="w-6 h-6 text-slate-500" />
                </div>
                <div className="p-4 rounded-xl bg-slate-900/80 border border-slate-800 flex items-center justify-between">
                  <div>
                    <p className="text-xs text-slate-400">Active Coupons</p>
                    <p className="text-xl font-bold text-indigo-400 mt-0.5">{adminCoupons.filter(c => c.active).length} / {adminCoupons.length}</p>
                  </div>
                  <Tag className="w-6 h-6 text-indigo-500" />
                </div>
                <div className="p-4 rounded-xl bg-slate-900/80 border border-slate-800 flex items-center justify-between">
                  <div>
                    <p className="text-xs text-slate-400">Live Products</p>
                    <p className="text-xl font-bold text-white mt-0.5">{stats?.activeProducts ?? 0} / {stats?.totalProducts ?? 0}</p>
                  </div>
                  <Package className="w-6 h-6 text-slate-500" />
                </div>
                <div className="p-4 rounded-xl bg-slate-900/80 border border-slate-800 flex items-center justify-between">
                  <div>
                    <p className="text-xs text-slate-400">Low Stock Alerts</p>
                    <p className="text-xl font-bold text-amber-400 mt-0.5">{stats?.lowStockProducts ?? 0}</p>
                  </div>
                  <AlertTriangle className="w-6 h-6 text-amber-500" />
                </div>
              </div>

              {/* Recent Orders Overview */}
              <div className="p-6 rounded-2xl bg-slate-950/70 border border-slate-800 space-y-4">
                <div className="flex items-center justify-between border-b border-slate-800 pb-3">
                  <h2 className="text-base font-bold text-white flex items-center gap-2">
                    <ShoppingBag className="w-4 h-4 text-indigo-400" /> Recent Marketplace Orders
                  </h2>
                  <button onClick={() => setActiveTab('orders')} className="text-xs text-indigo-400 hover:text-indigo-300 font-semibold flex items-center gap-1">
                    View All Orders <ChevronRight className="w-3.5 h-3.5" />
                  </button>
                </div>

                {orders.length === 0 ? (
                  <p className="text-xs text-slate-500 py-6 text-center">No orders recorded in database.</p>
                ) : (
                  <div className="divide-y divide-slate-800/80 overflow-x-auto">
                    <table className="w-full text-left text-xs">
                      <thead>
                        <tr className="text-slate-400 uppercase tracking-wider font-semibold">
                          <th className="pb-3 pr-4">Order Number</th>
                          <th className="pb-3 px-4">Customer ID</th>
                          <th className="pb-3 px-4">Amount</th>
                          <th className="pb-3 px-4">Coupon</th>
                          <th className="pb-3 px-4">Status</th>
                          <th className="pb-3 pl-4 text-right">Action</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-slate-800/50">
                        {orders.slice(0, 5).map((o) => (
                          <tr key={o.id} className="hover:bg-slate-900/50 transition-colors">
                            <td className="py-3 pr-4 font-mono font-bold text-white">{o.orderNumber || `ORD-${o.id}`}</td>
                            <td className="py-3 px-4 text-slate-300">User #{o.userId || 'N/A'}</td>
                            <td className="py-3 px-4 font-bold text-indigo-400">₹{o.totalAmount}</td>
                            <td className="py-3 px-4">
                              {o.couponCode ? <Badge color="violet" size="sm">{o.couponCode}</Badge> : <span className="text-slate-500">—</span>}
                            </td>
                            <td className="py-3 px-4">
                              <Badge
                                color={
                                  o.orderStatus === 'DELIVERED'
                                    ? 'emerald'
                                    : o.orderStatus === 'CANCELLED'
                                    ? 'rose'
                                    : o.orderStatus === 'PENDING'
                                    ? 'amber'
                                    : 'indigo'
                                }
                                size="sm"
                              >
                                {o.orderStatus}
                              </Badge>
                            </td>
                            <td className="py-3 pl-4 text-right">
                              <button
                                onClick={() => { setSelectedOrderDetails(o); setActiveTab('orders'); }}
                                className="px-2.5 py-1 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white transition-colors"
                              >
                                View
                              </button>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            </div>
          )}

          {/* ══════════════════════════════════════════════════════════════════ */}
          {/* TAB 2: VENDOR MANAGEMENT                                           */}
          {/* ══════════════════════════════════════════════════════════════════ */}
          {activeTab === 'vendors' && (
            <div className="space-y-6">
              {/* Vendor Filters */}
              <div className="flex flex-col sm:flex-row gap-4 justify-between items-start sm:items-center p-4 rounded-2xl bg-slate-950/70 border border-slate-800">
                <div className="flex items-center gap-2 w-full sm:w-auto">
                  <div className="relative flex-1 sm:w-72">
                    <Search className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400" />
                    <input
                      type="text"
                      placeholder="Search by store or email..."
                      value={vendorSearch}
                      onChange={(e) => setVendorSearch(e.target.value)}
                      className="w-full bg-slate-900 text-white text-xs rounded-xl pl-9 pr-3.5 py-2.5 border border-slate-700/80 focus:outline-none focus:border-indigo-500"
                    />
                  </div>
                </div>

                <div className="flex items-center gap-2 w-full sm:w-auto overflow-x-auto">
                  {['ALL', 'PENDING', 'APPROVED', 'REJECTED', 'SUSPENDED'].map((st) => (
                    <button
                      key={st}
                      onClick={() => setVendorFilter(st)}
                      className={`px-3 py-1.5 rounded-lg text-xs font-semibold whitespace-nowrap transition-colors ${
                        vendorFilter === st
                          ? 'bg-indigo-600 text-white'
                          : 'bg-slate-900 text-slate-400 hover:bg-slate-800'
                      }`}
                    >
                      {st}
                    </button>
                  ))}
                </div>
              </div>

              {/* Vendors List Table */}
              <div className="p-6 rounded-2xl bg-slate-950/70 border border-slate-800">
                <div className="overflow-x-auto">
                  <table className="w-full text-left text-xs">
                    <thead>
                      <tr className="text-slate-400 uppercase tracking-wider font-semibold border-b border-slate-800">
                        <th className="pb-3 pr-4">Store Name</th>
                        <th className="pb-3 px-4">Contact / Email</th>
                        <th className="pb-3 px-4">Location</th>
                        <th className="pb-3 px-4">Status</th>
                        <th className="pb-3 pl-4 text-right">Actions</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-800/50">
                      {filteredVendors.length === 0 ? (
                        <tr>
                          <td colSpan={5} className="py-8 text-center text-slate-500">
                            No vendors match the selected filter.
                          </td>
                        </tr>
                      ) : (
                        filteredVendors.map((v) => (
                          <tr key={v.id} className="hover:bg-slate-900/50 transition-colors">
                            <td className="py-4 pr-4">
                              <div className="font-bold text-white text-sm">{v.storeName}</div>
                              <div className="text-[11px] text-slate-400 line-clamp-1">{v.storeDescription || 'No description'}</div>
                            </td>
                            <td className="py-4 px-4 text-slate-300">
                              <div>{v.businessEmail || 'N/A'}</div>
                              <div className="text-[11px] text-slate-400">{v.businessPhone || 'N/A'}</div>
                            </td>
                            <td className="py-4 px-4 text-slate-300">
                              {v.city ? `${v.city}, ${v.country || ''}` : 'N/A'}
                            </td>
                            <td className="py-4 px-4">
                              <Badge
                                color={
                                  v.status === 'APPROVED'
                                    ? 'emerald'
                                    : v.status === 'PENDING'
                                    ? 'amber'
                                    : 'rose'
                                }
                                size="sm"
                              >
                                {v.status}
                              </Badge>
                            </td>
                            <td className="py-4 pl-4 text-right">
                              <div className="flex items-center justify-end gap-2">
                                <button
                                  onClick={() => handleViewVendor(v.id)}
                                  className="p-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white"
                                  title="View Details"
                                >
                                  <Eye className="w-4 h-4" />
                                </button>
                                {v.status !== 'APPROVED' && (
                                  <button
                                    onClick={() => handleVendorAction(v.id, 'APPROVE')}
                                    className="px-2.5 py-1 rounded-lg bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 text-[11px] font-semibold"
                                  >
                                    Approve
                                  </button>
                                )}
                                {v.status !== 'REJECTED' && (
                                  <button
                                    onClick={() => handleVendorAction(v.id, 'REJECT')}
                                    className="px-2.5 py-1 rounded-lg bg-rose-500/10 hover:bg-rose-500/20 text-rose-400 border border-rose-500/30 text-[11px] font-semibold"
                                  >
                                    Reject
                                  </button>
                                )}
                                {v.status === 'APPROVED' && (
                                  <button
                                    onClick={() => handleVendorAction(v.id, 'SUSPEND')}
                                    className="px-2.5 py-1 rounded-lg bg-amber-500/10 hover:bg-amber-500/20 text-amber-400 border border-amber-500/30 text-[11px] font-semibold"
                                  >
                                    Suspend
                                  </button>
                                )}
                              </div>
                            </td>
                          </tr>
                        ))
                      )}
                    </tbody>
                  </table>
                </div>
              </div>

              {/* Vendor Detail Modal */}
              {selectedVendorDetails && (
                <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm">
                  <div className="w-full max-w-2xl bg-slate-950 border border-slate-800 rounded-3xl p-6 sm:p-8 space-y-6 max-h-[90vh] overflow-y-auto">
                    <div className="flex items-center justify-between border-b border-slate-800 pb-4">
                      <div>
                        <h3 className="text-lg font-bold text-white">{selectedVendorDetails.storeName}</h3>
                        <p className="text-xs text-slate-400">Owner: {selectedVendorDetails.userFullName} ({selectedVendorDetails.userEmail})</p>
                      </div>
                      <button onClick={() => setSelectedVendorDetails(null)} className="p-2 rounded-xl text-slate-400 hover:bg-slate-800 hover:text-white">
                        <X className="w-5 h-5" />
                      </button>
                    </div>

                    <div className="grid grid-cols-3 gap-4">
                      <div className="p-4 rounded-xl bg-slate-900 border border-slate-800">
                        <p className="text-[11px] text-slate-400 uppercase font-semibold">Total Gross Sales</p>
                        <p className="text-lg font-bold text-emerald-400 mt-1">₹{selectedVendorDetails.totalGrossSales}</p>
                      </div>
                      <div className="p-4 rounded-xl bg-slate-900 border border-slate-800">
                        <p className="text-[11px] text-slate-400 uppercase font-semibold">Commission Paid</p>
                        <p className="text-lg font-bold text-indigo-400 mt-1">₹{selectedVendorDetails.totalCommissionPaid}</p>
                      </div>
                      <div className="p-4 rounded-xl bg-slate-900 border border-slate-800">
                        <p className="text-[11px] text-slate-400 uppercase font-semibold">Net Earnings</p>
                        <p className="text-lg font-bold text-white mt-1">₹{selectedVendorDetails.totalNetEarnings}</p>
                      </div>
                    </div>

                    <div className="space-y-2 text-xs text-slate-300">
                      <p><span className="font-semibold text-slate-400">Address:</span> {selectedVendorDetails.address || 'N/A'}</p>
                      <p><span className="font-semibold text-slate-400">Phone:</span> {selectedVendorDetails.contactNumber || 'N/A'}</p>
                      <p><span className="font-semibold text-slate-400">Postal/Tax Code:</span> {selectedVendorDetails.taxId || 'N/A'}</p>
                      <p><span className="font-semibold text-slate-400">Total Products:</span> {selectedVendorDetails.totalProducts} ({selectedVendorDetails.activeProducts} Active)</p>
                    </div>

                    <div className="flex justify-end gap-3 pt-4 border-t border-slate-800">
                      <Button variant="outline" size="sm" onClick={() => setSelectedVendorDetails(null)}>Close</Button>
                    </div>
                  </div>
                </div>
              )}
            </div>
          )}

          {/* ══════════════════════════════════════════════════════════════════ */}
          {/* TAB 3: MARKETPLACE ANALYTICS                                       */}
          {/* ══════════════════════════════════════════════════════════════════ */}
          {activeTab === 'analytics' && (
            <div className="space-y-8">
              {/* Daily Sales Trend Chart / Breakdown */}
              <div className="p-6 rounded-2xl bg-slate-950/70 border border-slate-800 space-y-4">
                <h2 className="text-base font-bold text-white flex items-center gap-2">
                  <TrendingUp className="w-4 h-4 text-emerald-400" /> Daily Revenue & Order Volume Trends
                </h2>
                {trends?.dailySalesTrend?.length === 0 ? (
                  <p className="text-xs text-slate-500 py-8 text-center">No time-series sales records available yet.</p>
                ) : (
                  <div className="space-y-3">
                    {trends?.dailySalesTrend?.map((t) => (
                      <div key={t.date} className="flex flex-col sm:flex-row sm:items-center justify-between p-3 rounded-xl bg-slate-900 border border-slate-800 gap-2">
                        <div className="flex items-center gap-3">
                          <span className="font-mono text-xs text-slate-400 font-semibold">{t.date}</span>
                          <span className="text-xs px-2 py-0.5 rounded-full bg-slate-800 text-slate-300">{t.orderCount} orders</span>
                        </div>
                        <div className="flex items-center gap-4">
                          <div className="w-32 bg-slate-800 rounded-full h-2 overflow-hidden hidden sm:block">
                            <div className="bg-emerald-500 h-full rounded-full" style={{ width: '100%' }} />
                          </div>
                          <span className="text-sm font-bold text-emerald-400">₹{t.revenue}</span>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              {/* Order Status Distribution */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div className="p-6 rounded-2xl bg-slate-950/70 border border-slate-800 space-y-4">
                  <h2 className="text-base font-bold text-white flex items-center gap-2">
                    <ShoppingBag className="w-4 h-4 text-indigo-400" /> Order Status Distribution
                  </h2>
                  <div className="grid grid-cols-2 gap-3">
                    {trends?.orderStatusDistribution &&
                      Object.entries(trends.orderStatusDistribution).map(([status, count]) => (
                        <div key={status} className="p-3.5 rounded-xl bg-slate-900 border border-slate-800">
                          <p className="text-[10px] text-slate-400 uppercase font-semibold truncate">{status}</p>
                          <p className="text-xl font-bold text-white mt-1">{count}</p>
                        </div>
                      ))}
                  </div>
                </div>

                {/* Vendor Performance Leaderboard */}
                <div className="p-6 rounded-2xl bg-slate-950/70 border border-slate-800 space-y-4">
                  <h2 className="text-base font-bold text-white flex items-center gap-2">
                    <Store className="w-4 h-4 text-violet-400" /> Vendor Sales Leaderboard
                  </h2>
                  <div className="space-y-2.5">
                    {trends?.topVendors?.map((v, idx) => (
                      <div key={v.vendorId} className="flex items-center justify-between p-3 rounded-xl bg-slate-900 border border-slate-800 text-xs">
                        <div className="flex items-center gap-2.5">
                          <span className="font-bold text-slate-500">#{idx + 1}</span>
                          <span className="font-bold text-white">{v.storeName}</span>
                        </div>
                        <div className="flex items-center gap-3">
                          <span className="text-slate-400">{v.orderCount} orders</span>
                          <span className="font-bold text-indigo-400">₹{v.totalSales}</span>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* ══════════════════════════════════════════════════════════════════ */}
          {/* TAB 4: ORDER MONITORING                                            */}
          {/* ══════════════════════════════════════════════════════════════════ */}
          {activeTab === 'orders' && (
            <div className="space-y-6">
              {/* Order Filter & Search */}
              <div className="flex flex-col sm:flex-row gap-4 justify-between items-start sm:items-center p-4 rounded-2xl bg-slate-950/70 border border-slate-800">
                <div className="relative flex-1 sm:w-72">
                  <Search className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400" />
                  <input
                    type="text"
                    placeholder="Search by order # or address..."
                    value={orderSearch}
                    onChange={(e) => setOrderSearch(e.target.value)}
                    className="w-full bg-slate-900 text-white text-xs rounded-xl pl-9 pr-3.5 py-2.5 border border-slate-700/80 focus:outline-none focus:border-indigo-500"
                  />
                </div>

                <div className="flex items-center gap-2 overflow-x-auto w-full sm:w-auto">
                  {['ALL', 'PENDING', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED', 'RETURN_REQUESTED'].map((st) => (
                    <button
                      key={st}
                      onClick={() => setOrderFilter(st)}
                      className={`px-3 py-1.5 rounded-lg text-xs font-semibold whitespace-nowrap transition-colors ${
                        orderFilter === st
                          ? 'bg-indigo-600 text-white'
                          : 'bg-slate-900 text-slate-400 hover:bg-slate-800'
                      }`}
                    >
                      {st}
                    </button>
                  ))}
                </div>
              </div>

              {/* Orders Table */}
              <div className="p-6 rounded-2xl bg-slate-950/70 border border-slate-800">
                <div className="overflow-x-auto">
                  <table className="w-full text-left text-xs">
                    <thead>
                      <tr className="text-slate-400 uppercase tracking-wider font-semibold border-b border-slate-800">
                        <th className="pb-3 pr-4">Order #</th>
                        <th className="pb-3 px-4">Date</th>
                        <th className="pb-3 px-4">Total Amount</th>
                        <th className="pb-3 px-4">Coupon</th>
                        <th className="pb-3 px-4">Status</th>
                        <th className="pb-3 pl-4 text-right">Manage</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-800/50">
                      {filteredOrders.length === 0 ? (
                        <tr>
                          <td colSpan={6} className="py-8 text-center text-slate-500">
                            No orders match this filter.
                          </td>
                        </tr>
                      ) : (
                        filteredOrders.map((o) => (
                          <tr key={o.id} className="hover:bg-slate-900/50 transition-colors">
                            <td className="py-4 pr-4">
                              <span className="font-mono font-bold text-white text-sm">{o.orderNumber || `ORD-${o.id}`}</span>
                              <p className="text-[11px] text-slate-400 line-clamp-1">{o.shippingAddress}</p>
                            </td>
                            <td className="py-4 px-4 text-slate-300">
                              {o.createdAt ? new Date(o.createdAt).toLocaleDateString() : 'N/A'}
                            </td>
                            <td className="py-4 px-4 font-bold text-indigo-400">
                              ₹{o.totalAmount}
                            </td>
                            <td className="py-4 px-4">
                              {o.couponCode ? (
                                <Badge color="violet" size="sm">{o.couponCode}</Badge>
                              ) : (
                                <span className="text-slate-500">—</span>
                              )}
                            </td>
                            <td className="py-4 px-4">
                              <Badge
                                color={
                                  o.orderStatus === 'DELIVERED'
                                    ? 'emerald'
                                    : o.orderStatus === 'CANCELLED'
                                    ? 'rose'
                                    : o.orderStatus === 'PENDING'
                                    ? 'amber'
                                    : 'indigo'
                                }
                                size="sm"
                              >
                                {o.orderStatus}
                              </Badge>
                            </td>
                            <td className="py-4 pl-4 text-right">
                              <button
                                onClick={() => setSelectedOrderDetails(o)}
                                className="px-3 py-1.5 rounded-lg bg-indigo-600/20 hover:bg-indigo-600/30 text-indigo-300 font-semibold border border-indigo-500/30"
                              >
                                Details & Status
                              </button>
                            </td>
                          </tr>
                        ))
                      )}
                    </tbody>
                  </table>
                </div>
              </div>

              {/* Order Details & Status Transition Modal */}
              {selectedOrderDetails && (
                <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm">
                  <div className="w-full max-w-xl bg-slate-950 border border-slate-800 rounded-3xl p-6 sm:p-8 space-y-6 max-h-[90vh] overflow-y-auto">
                    <div className="flex items-center justify-between border-b border-slate-800 pb-4">
                      <div>
                        <h3 className="text-lg font-bold text-white">Order {selectedOrderDetails.orderNumber}</h3>
                        <p className="text-xs text-slate-400">Created: {selectedOrderDetails.createdAt}</p>
                      </div>
                      <button onClick={() => setSelectedOrderDetails(null)} className="p-2 rounded-xl text-slate-400 hover:bg-slate-800 hover:text-white">
                        <X className="w-5 h-5" />
                      </button>
                    </div>

                    <div className="space-y-3">
                      <div className="p-3.5 rounded-xl bg-slate-900 border border-slate-800 text-xs space-y-1.5">
                        <p><span className="font-semibold text-slate-400">Shipping Address:</span> {selectedOrderDetails.shippingAddress}</p>
                        <p><span className="font-semibold text-slate-400">Subtotal:</span> ₹{selectedOrderDetails.subtotalAmount || selectedOrderDetails.totalAmount}</p>
                        {selectedOrderDetails.couponCode && (
                          <p><span className="font-semibold text-slate-400">Coupon Used:</span> <span className="font-mono text-violet-400 font-bold">{selectedOrderDetails.couponCode}</span></p>
                        )}
                        <p><span className="font-semibold text-slate-400">Discount:</span> ₹{selectedOrderDetails.discountAmount || 0}</p>
                        <p><span className="font-semibold text-slate-400">Total Charged:</span> <strong className="text-indigo-400">₹{selectedOrderDetails.totalAmount}</strong></p>
                      </div>

                      {/* Items */}
                      <div className="space-y-2">
                        <h4 className="text-xs font-bold text-slate-300 uppercase tracking-wider">Ordered Items</h4>
                        {selectedOrderDetails.items?.map((item) => (
                          <div key={item.id} className="p-3 rounded-xl bg-slate-900 border border-slate-800 flex items-center justify-between text-xs">
                            <div>
                              <p className="font-bold text-white">{item.productName || `Product #${item.productId}`}</p>
                              <p className="text-slate-400">Qty: {item.quantity} × ₹{item.price}</p>
                            </div>
                            <span className="font-bold text-indigo-400">₹{(item.quantity * item.price).toFixed(2)}</span>
                          </div>
                        ))}
                      </div>

                      {/* Status Transition Buttons */}
                      <div className="space-y-2 pt-2 border-t border-slate-800">
                        <h4 className="text-xs font-bold text-slate-300 uppercase tracking-wider">Update Order Lifecycle Status</h4>
                        <div className="flex flex-wrap gap-2">
                          {['CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED'].map((st) => (
                            <button
                              key={st}
                              disabled={selectedOrderDetails.orderStatus === st}
                              onClick={() => handleOrderStatusUpdate(selectedOrderDetails.id, st)}
                              className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${
                                selectedOrderDetails.orderStatus === st
                                  ? 'bg-slate-800 text-slate-500 cursor-not-allowed'
                                  : 'bg-indigo-600 hover:bg-indigo-500 text-white'
                              }`}
                            >
                              Move to {st}
                            </button>
                          ))}
                        </div>
                      </div>
                    </div>

                    <div className="flex justify-end gap-3 pt-4 border-t border-slate-800">
                      <Button variant="outline" size="sm" onClick={() => setSelectedOrderDetails(null)}>Close</Button>
                    </div>
                  </div>
                </div>
              )}
            </div>
          )}

          {/* ══════════════════════════════════════════════════════════════════ */}
          {/* TAB 5: COMMISSION MANAGEMENT                                       */}
          {/* ══════════════════════════════════════════════════════════════════ */}
          {activeTab === 'commissions' && (
            <div className="space-y-8">
              {/* Commission KPIs */}
              <div className="grid grid-cols-1 sm:grid-cols-4 gap-5">
                <div className="p-6 rounded-2xl bg-slate-950/70 border border-slate-800">
                  <p className="text-xs text-slate-400 uppercase font-semibold">Total Platform Commission</p>
                  <p className="text-2xl font-extrabold text-emerald-400 mt-2">
                    ₹{commissionSummary?.totalPlatformCommission ?? 0}
                  </p>
                </div>
                <div className="p-6 rounded-2xl bg-slate-950/70 border border-slate-800">
                  <p className="text-xs text-slate-400 uppercase font-semibold">Gross Sales Processed</p>
                  <p className="text-2xl font-extrabold text-white mt-2">
                    ₹{commissionSummary?.totalGrossSales ?? 0}
                  </p>
                </div>
                <div className="p-6 rounded-2xl bg-slate-950/70 border border-slate-800">
                  <p className="text-xs text-slate-400 uppercase font-semibold">Total Vendor Net Payouts</p>
                  <p className="text-2xl font-extrabold text-indigo-400 mt-2">
                    ₹{commissionSummary?.totalVendorPayouts ?? 0}
                  </p>
                </div>
                <div className="p-6 rounded-2xl bg-slate-950/70 border border-slate-800">
                  <p className="text-xs text-slate-400 uppercase font-semibold">Commission Rate</p>
                  <p className="text-2xl font-extrabold text-amber-400 mt-2">5.0%</p>
                </div>
              </div>

              {/* Per-Vendor Commission Breakdown Table */}
              <div className="p-6 rounded-2xl bg-slate-950/70 border border-slate-800 space-y-4">
                <h2 className="text-base font-bold text-white flex items-center gap-2">
                  <Store className="w-4 h-4 text-indigo-400" /> Vendor-wise Commission Breakdown
                </h2>
                <div className="overflow-x-auto">
                  <table className="w-full text-left text-xs">
                    <thead>
                      <tr className="text-slate-400 uppercase tracking-wider font-semibold border-b border-slate-800">
                        <th className="pb-3 pr-4">Vendor Store</th>
                        <th className="pb-3 px-4">Sales Amount</th>
                        <th className="pb-3 px-4">Commission (5%)</th>
                        <th className="pb-3 px-4">Net Payout</th>
                        <th className="pb-3 pl-4 text-right">Items Sold</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-800/50">
                      {commissionSummary?.vendorBreakdown?.map((item) => (
                        <tr key={item.vendorProfileId} className="hover:bg-slate-900/50 transition-colors">
                          <td className="py-3 pr-4 font-bold text-white">{item.vendorStoreName}</td>
                          <td className="py-3 px-4 text-slate-300">₹{item.totalSales}</td>
                          <td className="py-3 px-4 font-bold text-emerald-400">₹{item.commissionAmount}</td>
                          <td className="py-3 px-4 font-bold text-indigo-400">₹{item.netPayout}</td>
                          <td className="py-3 pl-4 text-right text-slate-300">{item.orderItemCount}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>

              {/* Granular Commission Item Audit */}
              <div className="p-6 rounded-2xl bg-slate-950/70 border border-slate-800 space-y-4">
                <h2 className="text-base font-bold text-white flex items-center gap-2">
                  <Percent className="w-4 h-4 text-violet-400" /> Item-Level Commission Audit Log
                </h2>
                <div className="overflow-x-auto">
                  <table className="w-full text-left text-xs">
                    <thead>
                      <tr className="text-slate-400 uppercase tracking-wider font-semibold border-b border-slate-800">
                        <th className="pb-3 pr-4">ID</th>
                        <th className="pb-3 px-4">Product</th>
                        <th className="pb-3 px-4">Store</th>
                        <th className="pb-3 px-4">Sale Amount</th>
                        <th className="pb-3 px-4">Platform (5%)</th>
                        <th className="pb-3 pl-4 text-right">Vendor Net</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-800/50">
                      {commissions.length === 0 ? (
                        <tr><td colSpan={6} className="py-6 text-center text-slate-500">No commission records yet.</td></tr>
                      ) : (
                        commissions.map((c) => (
                          <tr key={c.id} className="hover:bg-slate-900/50 transition-colors">
                            <td className="py-3 pr-4 font-mono text-slate-500">#{c.id}</td>
                            <td className="py-3 px-4 font-semibold text-white">{c.productName || 'Order Item'}</td>
                            <td className="py-3 px-4 text-slate-400">{c.vendorStoreName || 'N/A'}</td>
                            <td className="py-3 px-4 text-slate-300">₹{c.saleAmount}</td>
                            <td className="py-3 px-4 font-bold text-emerald-400">₹{c.commissionAmount}</td>
                            <td className="py-3 pl-4 text-right font-bold text-indigo-400">₹{c.vendorNetAmount}</td>
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
          {/* TAB 6: COUPONS & PROMOTIONS MANAGEMENT                             */}
          {/* ══════════════════════════════════════════════════════════════════ */}
          {activeTab === 'coupons' && (
            <div className="space-y-6">
              {/* Coupon Metrics */}
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-6 gap-4">
                <div className="p-4 rounded-2xl bg-slate-950/70 border border-slate-800">
                  <p className="text-[11px] text-slate-400 uppercase font-semibold">Total Campaigns</p>
                  <p className="text-2xl font-bold text-white mt-1">
                    {couponAnalytics?.totalCoupons ?? adminCoupons.length}
                  </p>
                </div>
                <div className="p-4 rounded-2xl bg-slate-950/70 border border-slate-800">
                  <p className="text-[11px] text-slate-400 uppercase font-semibold">Active Coupons</p>
                  <p className="text-2xl font-bold text-emerald-400 mt-1">
                    {couponAnalytics?.activeCoupons ?? adminCoupons.filter((c) => c.active).length}
                  </p>
                </div>
                <div className="p-4 rounded-2xl bg-slate-950/70 border border-slate-800">
                  <p className="text-[11px] text-slate-400 uppercase font-semibold">Expired Coupons</p>
                  <p className="text-2xl font-bold text-rose-400 mt-1">
                    {couponAnalytics?.expiredCoupons ?? adminCoupons.filter((c) => c.expiryDate && new Date(c.expiryDate) < new Date()).length}
                  </p>
                </div>
                <div className="p-4 rounded-2xl bg-slate-950/70 border border-slate-800">
                  <p className="text-[11px] text-slate-400 uppercase font-semibold">Total Redemptions</p>
                  <p className="text-2xl font-bold text-indigo-400 mt-1">
                    {couponAnalytics?.totalUsageCount ?? adminCoupons.reduce((acc, c) => acc + (c.usedCount || 0), 0)}
                  </p>
                </div>
                <div className="p-4 rounded-2xl bg-slate-950/70 border border-slate-800">
                  <p className="text-[11px] text-slate-400 uppercase font-semibold">Total Discount Given</p>
                  <p className="text-2xl font-bold text-amber-400 mt-1">
                    ₹{couponAnalytics?.totalDiscountGiven ?? 0}
                  </p>
                </div>
                <div className="p-4 rounded-2xl bg-slate-950/70 border border-slate-800">
                  <p className="text-[11px] text-slate-400 uppercase font-semibold">Top Performing</p>
                  <p className="text-lg font-mono font-bold text-violet-300 mt-1 truncate">
                    {couponAnalytics?.topCouponCode || 'None'}
                  </p>
                  <p className="text-[10px] text-slate-500">
                    {couponAnalytics?.topCouponUsageCount ? `${couponAnalytics.topCouponUsageCount} uses` : 'No redemptions'}
                  </p>
                </div>
              </div>

              {/* Coupon Controls Header */}
              <div className="flex flex-col sm:flex-row gap-4 justify-between items-start sm:items-center p-4 rounded-2xl bg-slate-950/70 border border-slate-800">
                <div className="flex items-center gap-3 w-full sm:w-auto">
                  <div className="relative flex-1 sm:w-72">
                    <Search className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400" />
                    <input
                      type="text"
                      placeholder="Search by code, title or description..."
                      value={couponSearch}
                      onChange={(e) => setCouponSearch(e.target.value)}
                      className="w-full bg-slate-900 text-white text-xs rounded-xl pl-9 pr-3.5 py-2.5 border border-slate-700/80 focus:outline-none focus:border-indigo-500"
                    />
                  </div>
                  <div className="flex items-center gap-1.5 overflow-x-auto scrollbar-none">
                    {['ALL', 'ACTIVE', 'INACTIVE', 'EXPIRED'].map((st) => (
                      <button
                        key={st}
                        onClick={() => setCouponFilter(st)}
                        className={`px-3 py-1.5 rounded-lg text-xs font-semibold whitespace-nowrap transition-colors ${
                          couponFilter === st
                            ? 'bg-indigo-600 text-white shadow-md shadow-indigo-600/30'
                            : 'bg-slate-900 text-slate-400 hover:bg-slate-800'
                        }`}
                      >
                        {st}
                      </button>
                    ))}
                  </div>
                </div>

                <Button
                  variant="primary"
                  size="md"
                  icon={Plus}
                  onClick={handleOpenCreateCoupon}
                >
                  Create Coupon
                </Button>
              </div>

              {/* Coupons Table */}
              <div className="p-6 rounded-2xl bg-slate-950/70 border border-slate-800">
                <div className="overflow-x-auto">
                  <table className="w-full text-left text-xs">
                    <thead>
                      <tr className="text-slate-400 uppercase tracking-wider font-semibold border-b border-slate-800">
                        <th className="pb-3 pr-4">Code / Campaign</th>
                        <th className="pb-3 px-4">Scope & Targets</th>
                        <th className="pb-3 px-4">Discount</th>
                        <th className="pb-3 px-4">Conditions</th>
                        <th className="pb-3 px-4">Validity</th>
                        <th className="pb-3 px-4">Usage</th>
                        <th className="pb-3 px-4">Status</th>
                        <th className="pb-3 pl-4 text-right">Actions</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-800/50">
                      {filteredCoupons.length === 0 ? (
                        <tr>
                          <td colSpan={8} className="py-8 text-center text-slate-500">
                            No coupon codes matching the criteria.
                          </td>
                        </tr>
                      ) : (
                        filteredCoupons.map((c) => {
                          const now = new Date();
                          const isExpired = c.expiryDate && new Date(c.expiryDate) < now;
                          const statusLabel = !c.active ? 'Disabled' : isExpired ? 'Expired' : 'Active';
                          const statusColor = !c.active ? 'slate' : isExpired ? 'rose' : 'emerald';

                          const scope = c.applicabilityScope || 'ENTIRE_PLATFORM';
                          const scopeBadgeLabel = isPlatformScope(scope)
                            ? 'Entire Platform'
                            : isCategoryScope(scope)
                            ? 'Categories'
                            : isProductScope(scope)
                            ? 'Products'
                            : 'Vendors';
                          const scopeColor = isPlatformScope(scope)
                            ? 'indigo'
                            : isCategoryScope(scope)
                            ? 'violet'
                            : isProductScope(scope)
                            ? 'amber'
                            : 'emerald';

                          return (
                            <tr key={c.id} className="hover:bg-slate-900/50 transition-colors">
                              <td className="py-4 pr-4">
                                <div className="font-mono font-bold text-white text-sm flex items-center gap-1.5">
                                  <Tag className="w-3.5 h-3.5 text-indigo-400" />
                                  {c.code}
                                </div>
                                <div className="text-[11px] text-slate-400 font-medium">{c.name}</div>
                                {c.description && (
                                  <div className="text-[10px] text-slate-500 line-clamp-1">{c.description}</div>
                                )}
                              </td>
                              <td className="py-4 px-4">
                                <Badge color={scopeColor} size="sm">
                                  {scopeBadgeLabel}
                                </Badge>
                                {isCategoryScope(scope) && c.eligibleCategoryNames?.length > 0 && (
                                  <div className="text-[10px] text-slate-400 mt-1 line-clamp-1">
                                    {c.eligibleCategoryNames.join(', ')}
                                  </div>
                                )}
                                {isProductScope(scope) && c.eligibleProductNames?.length > 0 && (
                                  <div className="text-[10px] text-slate-400 mt-1 line-clamp-1">
                                    {c.eligibleProductNames.join(', ')}
                                  </div>
                                )}
                                {isVendorScope(scope) && c.eligibleVendorNames?.length > 0 && (
                                  <div className="text-[10px] text-slate-400 mt-1 line-clamp-1">
                                    {c.eligibleVendorNames.join(', ')}
                                  </div>
                                )}
                              </td>
                              <td className="py-4 px-4 font-bold text-emerald-400 text-sm">
                                {c.type === 'percentage' || c.discountType === 'PERCENTAGE'
                                  ? `${c.value || c.discountValue}% OFF`
                                  : `₹${c.value || c.discountValue} OFF`}
                              </td>
                              <td className="py-4 px-4 text-slate-300">
                                <div>Min: {c.minimumOrderAmount ? `₹${c.minimumOrderAmount}` : 'None'}</div>
                                <div className="text-[11px] text-slate-400">
                                  {c.maximumDiscount ? `Cap: ₹${c.maximumDiscount}` : 'No Cap'}
                                </div>
                              </td>
                              <td className="py-4 px-4 text-slate-400 text-[11px]">
                                <div>{c.startDate ? new Date(c.startDate).toLocaleDateString() : 'Immediate'}</div>
                                <div className="text-slate-500">to {c.expiryDate ? new Date(c.expiryDate).toLocaleDateString() : 'Never'}</div>
                              </td>
                              <td className="py-4 px-4 text-slate-300">
                                <span className="font-bold text-white">{c.usedCount}</span> / {c.usageLimit || '∞'}
                                <div className="text-[10px] text-slate-500">Per User: {c.perUserLimit || '1'}</div>
                              </td>
                              <td className="py-4 px-4">
                                <Badge color={statusColor} size="sm">
                                  {statusLabel}
                                </Badge>
                              </td>
                              <td className="py-4 pl-4 text-right">
                                <div className="flex items-center justify-end gap-1.5">
                                  <button
                                    onClick={() => handleViewUsage(c)}
                                    className="p-1.5 rounded-lg text-slate-400 hover:text-indigo-300 hover:bg-slate-800 transition-colors"
                                    title="View Redemptions"
                                  >
                                    <Eye className="w-4 h-4" />
                                  </button>
                                  <button
                                    onClick={() => handleToggleCoupon(c)}
                                    className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
                                    title={c.active ? 'Deactivate Coupon' : 'Activate Coupon'}
                                  >
                                    {c.active ? <ToggleRight className="w-4 h-4 text-emerald-400" /> : <ToggleLeft className="w-4 h-4 text-slate-500" />}
                                  </button>
                                  <button
                                    onClick={() => handleOpenEditCoupon(c)}
                                    className="p-1.5 rounded-lg text-slate-400 hover:text-amber-400 hover:bg-slate-800 transition-colors"
                                    title="Edit Coupon"
                                  >
                                    <Edit2 className="w-4 h-4" />
                                  </button>
                                  <button
                                    onClick={() => handleDeleteCoupon(c.id)}
                                    className="p-1.5 rounded-lg text-slate-400 hover:text-rose-400 hover:bg-slate-800 transition-colors"
                                    title="Delete Coupon"
                                  >
                                    <Trash2 className="w-4 h-4" />
                                  </button>
                                </div>
                              </td>
                            </tr>
                          );
                        })
                      )}
                    </tbody>
                  </table>
                </div>
              </div>

              {/* Coupon Usage History Modal */}
              {showUsageModal && selectedCouponUsage && (
                <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-sm">
                  <div className="w-full max-w-2xl bg-slate-950 border border-slate-800 rounded-3xl p-6 sm:p-8 space-y-5 max-h-[85vh] overflow-y-auto">
                    <div className="flex items-center justify-between border-b border-slate-800 pb-4">
                      <div>
                        <h3 className="text-lg font-bold text-white flex items-center gap-2">
                          <Tag className="w-4 h-4 text-indigo-400" />
                          Redemption Audit: {selectedCouponUsage.coupon.code}
                        </h3>
                        <p className="text-xs text-slate-400">
                          {selectedCouponUsage.coupon.name} • Total Uses: {selectedCouponUsage.usages?.length ?? 0}
                        </p>
                      </div>
                      <button
                        onClick={() => setShowUsageModal(false)}
                        className="p-2 rounded-xl text-slate-400 hover:bg-slate-800 hover:text-white"
                      >
                        <X className="w-5 h-5" />
                      </button>
                    </div>

                    {loadingUsage ? (
                      <Loading label="Fetching redemption audit records..." />
                    ) : selectedCouponUsage.usages?.length === 0 ? (
                      <div className="py-12 text-center text-slate-500 space-y-2">
                        <Tag className="w-8 h-8 mx-auto text-slate-600" />
                        <p className="text-sm font-medium text-slate-400">No redemptions recorded yet</p>
                        <p className="text-xs text-slate-500">This coupon has not been applied to any placed orders.</p>
                      </div>
                    ) : (
                      <div className="overflow-x-auto">
                        <table className="w-full text-left text-xs">
                          <thead>
                            <tr className="text-slate-400 uppercase tracking-wider font-semibold border-b border-slate-800">
                              <th className="pb-3 pr-4">Order Number</th>
                              <th className="pb-3 px-4">Customer Email</th>
                              <th className="pb-3 px-4">Discount Given</th>
                              <th className="pb-3 pl-4 text-right">Used At</th>
                            </tr>
                          </thead>
                          <tbody className="divide-y divide-slate-800/50">
                            {selectedCouponUsage.usages.map((u) => (
                              <tr key={u.id} className="hover:bg-slate-900/50">
                                <td className="py-3 pr-4 font-mono font-bold text-white">
                                  {u.orderNumber || `#${u.orderId}`}
                                </td>
                                <td className="py-3 px-4 text-slate-300">
                                  {u.userEmail || `User #${u.userId}`}
                                </td>
                                <td className="py-3 px-4 font-bold text-emerald-400">
                                  ₹{u.discountAmount}
                                </td>
                                <td className="py-3 pl-4 text-right text-slate-400">
                                  {u.usedAt ? new Date(u.usedAt).toLocaleString() : 'N/A'}
                                </td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    )}

                    <div className="flex justify-end pt-4 border-t border-slate-800">
                      <Button
                        variant="outline"
                        size="md"
                        onClick={() => setShowUsageModal(false)}
                      >
                        Close
                      </Button>
                    </div>
                  </div>
                </div>
              )}

              {/* Create / Edit Coupon Modal */}
              {showCouponModal && (
                <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm">
                  <div className="w-full max-w-xl bg-slate-950 border border-slate-800 rounded-3xl p-6 sm:p-8 space-y-5 max-h-[90vh] overflow-y-auto">
                    <div className="flex items-center justify-between border-b border-slate-800 pb-4">
                      <h3 className="text-lg font-bold text-white">
                        {editingCoupon ? `Edit Coupon: ${editingCoupon.code}` : 'Create New Promotional Coupon'}
                      </h3>
                      <button
                        onClick={() => setShowCouponModal(false)}
                        className="p-2 rounded-xl text-slate-400 hover:bg-slate-800 hover:text-white"
                      >
                        <X className="w-5 h-5" />
                      </button>
                    </div>

                    <form onSubmit={handleSaveCoupon} className="space-y-4">
                      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                        <Input
                          label="Coupon Code"
                          required
                          value={couponForm.code}
                          onChange={(e) => setCouponForm({ ...couponForm, code: e.target.value.toUpperCase() })}
                          placeholder="e.g. FESTIVE20"
                          error={couponFormErrors.code}
                        />
                        <Input
                          label="Campaign Name"
                          required
                          value={couponForm.name}
                          onChange={(e) => setCouponForm({ ...couponForm, name: e.target.value })}
                          placeholder="e.g. Festive Holiday Sale"
                          error={couponFormErrors.name}
                        />
                      </div>

                      <div className="space-y-1.5">
                        <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300">
                          Description
                        </label>
                        <input
                          type="text"
                          value={couponForm.description}
                          onChange={(e) => setCouponForm({ ...couponForm, description: e.target.value })}
                          placeholder="e.g. Get 20% off on orders above ₹500"
                          className="w-full bg-slate-900 text-white text-xs rounded-xl px-3.5 py-2.5 border border-slate-700/80 focus:outline-none focus:border-indigo-500"
                        />
                      </div>

                      {/* ── Applicability Scope ("Applicable To") Selector ── */}
                      <div className="p-4 rounded-xl bg-slate-900 border border-slate-800 space-y-3">
                        <label className="block text-xs font-bold uppercase tracking-wider text-indigo-300">
                          Applicable To (Eligibility Scope)
                        </label>
                        <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
                          {[
                            { id: 'ENTIRE_PLATFORM', label: 'Entire Platform' },
                            { id: 'SPECIFIC_CATEGORIES', label: 'Specific Categories' },
                            { id: 'SPECIFIC_PRODUCTS', label: 'Specific Products' },
                            { id: 'SPECIFIC_VENDORS', label: 'Specific Vendors' },
                          ].map((scope) => {
                            const isSelected =
                              (scope.id === 'ENTIRE_PLATFORM' && isPlatformScope(couponForm.applicabilityScope)) ||
                              (scope.id === 'SPECIFIC_CATEGORIES' && isCategoryScope(couponForm.applicabilityScope)) ||
                              (scope.id === 'SPECIFIC_PRODUCTS' && isProductScope(couponForm.applicabilityScope)) ||
                              (scope.id === 'SPECIFIC_VENDORS' && isVendorScope(couponForm.applicabilityScope));

                            return (
                              <button
                                key={scope.id}
                                type="button"
                                onClick={() => setCouponForm({ ...couponForm, applicabilityScope: scope.id })}
                                className={`p-2.5 rounded-xl border text-xs font-semibold transition-all ${
                                  isSelected
                                    ? 'bg-indigo-600 text-white border-indigo-500 shadow-md shadow-indigo-600/30'
                                    : 'bg-slate-950 text-slate-400 border-slate-800 hover:border-slate-700'
                                }`}
                              >
                                {scope.label}
                              </button>
                            );
                          })}
                        </div>

                        {/* Category Selector */}
                        {isCategoryScope(couponForm.applicabilityScope) && (
                          <div className="space-y-1.5 pt-2 border-t border-slate-800">
                            <label className="block text-[11px] font-semibold text-slate-300">
                              Select Eligible Categories <span className="text-rose-400">*</span>
                            </label>
                            <div className="max-h-36 overflow-y-auto space-y-1 p-2 rounded-xl bg-slate-950 border border-slate-800">
                              {availableCategories.length === 0 ? (
                                <p className="text-[11px] text-slate-500 p-1">No categories available.</p>
                              ) : (
                                availableCategories.map((cat) => {
                                  const catId = Number(cat.rawId || cat.id);
                                  const isSelected = couponForm.categoryIds.includes(catId);
                                  return (
                                    <label
                                      key={catId}
                                      className="flex items-center gap-2 p-1.5 rounded-lg hover:bg-slate-900 cursor-pointer text-xs text-slate-300"
                                    >
                                      <input
                                        type="checkbox"
                                        checked={isSelected}
                                        onChange={(e) => {
                                          const next = e.target.checked
                                            ? [...couponForm.categoryIds, catId]
                                            : couponForm.categoryIds.filter((id) => id !== catId);
                                          setCouponForm({ ...couponForm, categoryIds: next });
                                        }}
                                        className="rounded border-slate-700 text-indigo-600 focus:ring-indigo-500"
                                      />
                                      <span>{cat.name}</span>
                                    </label>
                                  );
                                })
                              )}
                            </div>
                            {couponFormErrors.categoryIds && (
                              <p className="text-xs text-rose-400 mt-1">{couponFormErrors.categoryIds}</p>
                            )}
                          </div>
                        )}

                        {/* Product Selector */}
                        {isProductScope(couponForm.applicabilityScope) && (
                          <div className="space-y-1.5 pt-2 border-t border-slate-800">
                            <label className="block text-[11px] font-semibold text-slate-300">
                              Select Eligible Products <span className="text-rose-400">*</span>
                            </label>
                            <div className="max-h-36 overflow-y-auto space-y-1 p-2 rounded-xl bg-slate-950 border border-slate-800">
                              {availableProducts.length === 0 ? (
                                <p className="text-[11px] text-slate-500 p-1">No products available.</p>
                              ) : (
                                availableProducts.map((prod) => {
                                  const prodId = Number(prod.id);
                                  const isSelected = couponForm.productIds.includes(prodId);
                                  return (
                                    <label
                                      key={prodId}
                                      className="flex items-center gap-2 p-1.5 rounded-lg hover:bg-slate-900 cursor-pointer text-xs text-slate-300"
                                    >
                                      <input
                                        type="checkbox"
                                        checked={isSelected}
                                        onChange={(e) => {
                                          const next = e.target.checked
                                            ? [...couponForm.productIds, prodId]
                                            : couponForm.productIds.filter((id) => id !== prodId);
                                          setCouponForm({ ...couponForm, productIds: next });
                                        }}
                                        className="rounded border-slate-700 text-indigo-600 focus:ring-indigo-500"
                                      />
                                      <span className="truncate">{prod.name} (₹{prod.price})</span>
                                    </label>
                                  );
                                })
                              )}
                            </div>
                            {couponFormErrors.productIds && (
                              <p className="text-xs text-rose-400 mt-1">{couponFormErrors.productIds}</p>
                            )}
                          </div>
                        )}

                        {/* Vendor Selector */}
                        {isVendorScope(couponForm.applicabilityScope) && (
                          <div className="space-y-1.5 pt-2 border-t border-slate-800">
                            <label className="block text-[11px] font-semibold text-slate-300">
                              Select Eligible Vendors <span className="text-rose-400">*</span>
                            </label>
                            <div className="max-h-36 overflow-y-auto space-y-1 p-2 rounded-xl bg-slate-950 border border-slate-800">
                              {vendors.length === 0 ? (
                                <p className="text-[11px] text-slate-500 p-1">No vendors available.</p>
                              ) : (
                                vendors.map((v) => {
                                  const vId = Number(v.id);
                                  const isSelected = couponForm.vendorIds.includes(vId);
                                  return (
                                    <label
                                      key={vId}
                                      className="flex items-center gap-2 p-1.5 rounded-lg hover:bg-slate-900 cursor-pointer text-xs text-slate-300"
                                    >
                                      <input
                                        type="checkbox"
                                        checked={isSelected}
                                        onChange={(e) => {
                                          const next = e.target.checked
                                            ? [...couponForm.vendorIds, vId]
                                            : couponForm.vendorIds.filter((id) => id !== vId);
                                          setCouponForm({ ...couponForm, vendorIds: next });
                                        }}
                                        className="rounded border-slate-700 text-indigo-600 focus:ring-indigo-500"
                                      />
                                      <span>{v.storeName || `Vendor #${vId}`}</span>
                                    </label>
                                  );
                                })
                              )}
                            </div>
                            {couponFormErrors.vendorIds && (
                              <p className="text-xs text-rose-400 mt-1">{couponFormErrors.vendorIds}</p>
                            )}
                          </div>
                        )}
                      </div>

                      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                        <div className="space-y-1.5">
                          <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300">
                            Discount Type <span className="text-rose-400">*</span>
                          </label>
                          <select
                            value={couponForm.discountType}
                            onChange={(e) => setCouponForm({ ...couponForm, discountType: e.target.value })}
                            className="w-full bg-slate-900 text-white text-xs rounded-xl px-3.5 py-2.5 border border-slate-700/80 focus:outline-none focus:border-indigo-500"
                          >
                            <option value="PERCENTAGE">Percentage (%)</option>
                            <option value="FIXED_AMOUNT">Fixed Amount (₹)</option>
                          </select>
                        </div>

                        <Input
                          label={couponForm.discountType === 'PERCENTAGE' ? 'Discount Percentage (%)' : 'Fixed Discount (₹)'}
                          required
                          type="number"
                          step="0.01"
                          min="0.01"
                          value={couponForm.discountValue}
                          onChange={(e) => setCouponForm({ ...couponForm, discountValue: e.target.value })}
                          placeholder={couponForm.discountType === 'PERCENTAGE' ? 'e.g. 20' : 'e.g. 500'}
                          error={couponFormErrors.discountValue}
                        />
                      </div>

                      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                        <Input
                          label="Min Order Amount (₹)"
                          type="number"
                          step="0.01"
                          min="0"
                          value={couponForm.minimumOrderAmount}
                          onChange={(e) => setCouponForm({ ...couponForm, minimumOrderAmount: e.target.value })}
                          placeholder="Optional"
                        />
                        <Input
                          label="Max Discount Cap (₹)"
                          type="number"
                          step="0.01"
                          min="0"
                          value={couponForm.maximumDiscount}
                          onChange={(e) => setCouponForm({ ...couponForm, maximumDiscount: e.target.value })}
                          placeholder="Optional (for % discounts)"
                        />
                      </div>

                      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                        <Input
                          label="Total Platform Usage Limit"
                          type="number"
                          min="1"
                          value={couponForm.usageLimit}
                          onChange={(e) => setCouponForm({ ...couponForm, usageLimit: e.target.value })}
                          placeholder="Unlimited if blank"
                        />
                        <Input
                          label="Per User Limit"
                          type="number"
                          min="1"
                          value={couponForm.perUserLimit}
                          onChange={(e) => setCouponForm({ ...couponForm, perUserLimit: e.target.value })}
                          placeholder="Default 1"
                        />
                      </div>

                      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                        <Input
                          label="Start Date"
                          required
                          type="datetime-local"
                          value={couponForm.startDate}
                          onChange={(e) => setCouponForm({ ...couponForm, startDate: e.target.value })}
                          error={couponFormErrors.startDate}
                        />
                        <Input
                          label="Expiry Date"
                          required
                          type="datetime-local"
                          value={couponForm.expiryDate}
                          onChange={(e) => setCouponForm({ ...couponForm, expiryDate: e.target.value })}
                          error={couponFormErrors.expiryDate}
                        />
                      </div>

                      <div className="flex justify-end gap-3 pt-4 border-t border-slate-800">
                        <Button
                          type="button"
                          variant="outline"
                          size="md"
                          onClick={() => setShowCouponModal(false)}
                        >
                          Cancel
                        </Button>
                        <Button
                          type="submit"
                          variant="primary"
                          size="md"
                          isLoading={savingCoupon}
                          disabled={savingCoupon}
                        >
                          {savingCoupon ? 'Saving...' : editingCoupon ? 'Update Coupon' : 'Create Coupon'}
                        </Button>
                      </div>
                    </form>
                  </div>
                </div>
              )}
            </div>
          )}

          {/* ══════════════════════════════════════════════════════════════════ */}
          {/* TAB 7: SYSTEM MONITORING                                           */}
          {/* ══════════════════════════════════════════════════════════════════ */}
          {activeTab === 'system' && (
            <div className="space-y-8">
              {/* System Health Cards */}
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-6">
                <div className="p-6 rounded-2xl bg-slate-950/70 border border-slate-800 flex items-center gap-4">
                  <div className="p-3.5 rounded-2xl bg-emerald-500/10 text-emerald-400">
                    <Server className="w-7 h-7" />
                  </div>
                  <div>
                    <p className="text-xs text-slate-400 uppercase font-semibold">Backend Application</p>
                    <p className="text-2xl font-bold text-emerald-400 mt-1">{systemHealth?.applicationStatus || 'UP'}</p>
                    <p className="text-[11px] text-slate-500">Spring Boot REST API</p>
                  </div>
                </div>

                <div className="p-6 rounded-2xl bg-slate-950/70 border border-slate-800 flex items-center gap-4">
                  <div className="p-3.5 rounded-2xl bg-indigo-500/10 text-indigo-400">
                    <HardDrive className="w-7 h-7" />
                  </div>
                  <div>
                    <p className="text-xs text-slate-400 uppercase font-semibold">PostgreSQL Database</p>
                    <p className="text-2xl font-bold text-indigo-400 mt-1">{systemHealth?.databaseStatus || 'UP'}</p>
                    <p className="text-[11px] text-slate-500">HikariCP Pool Active</p>
                  </div>
                </div>

                <div className="p-6 rounded-2xl bg-slate-950/70 border border-slate-800 flex items-center gap-4">
                  <div className="p-3.5 rounded-2xl bg-violet-500/10 text-violet-400">
                    <Clock className="w-7 h-7" />
                  </div>
                  <div>
                    <p className="text-xs text-slate-400 uppercase font-semibold">Server Uptime</p>
                    <p className="text-2xl font-bold text-white mt-1">{systemHealth?.uptimeSeconds || 0}s</p>
                    <p className="text-[11px] text-slate-500">Java {systemHealth?.javaVersion}</p>
                  </div>
                </div>
              </div>

              {/* Memory & Diagnostic Details */}
              <div className="p-6 rounded-2xl bg-slate-950/70 border border-slate-800 space-y-6">
                <h2 className="text-base font-bold text-white flex items-center gap-2">
                  <Cpu className="w-4 h-4 text-indigo-400" /> JVM Memory & Thread Utilization
                </h2>

                <div className="space-y-3">
                  <div className="flex justify-between text-xs">
                    <span className="text-slate-400">JVM Memory In-Use</span>
                    <span className="text-white font-bold">
                      {systemHealth?.usedMemoryBytes ? (systemHealth.usedMemoryBytes / 1024 / 1024).toFixed(1) : 0} MB /{' '}
                      {systemHealth?.totalMemoryBytes ? (systemHealth.totalMemoryBytes / 1024 / 1024).toFixed(1) : 0} MB
                    </span>
                  </div>
                  <div className="w-full bg-slate-900 rounded-full h-3 overflow-hidden border border-slate-800">
                    <div
                      className="bg-gradient-to-r from-indigo-500 to-emerald-500 h-full rounded-full transition-all duration-500"
                      style={{
                        width: `${
                          systemHealth?.totalMemoryBytes
                            ? Math.round((systemHealth.usedMemoryBytes / systemHealth.totalMemoryBytes) * 100)
                            : 40
                        }%`,
                      }}
                    />
                  </div>
                </div>

                <div className="grid grid-cols-2 sm:grid-cols-3 gap-4 pt-4 border-t border-slate-800 text-xs">
                  <div className="p-3.5 rounded-xl bg-slate-900 border border-slate-800">
                    <span className="text-slate-400 font-medium">Active Threads:</span>
                    <p className="text-lg font-bold text-white mt-1">{systemHealth?.activeThreads || 0}</p>
                  </div>
                  <div className="p-3.5 rounded-xl bg-slate-900 border border-slate-800">
                    <span className="text-slate-400 font-medium">Environment:</span>
                    <p className="text-lg font-bold text-emerald-400 mt-1">{systemHealth?.environment || 'Production'}</p>
                  </div>
                  <div className="p-3.5 rounded-xl bg-slate-900 border border-slate-800 col-span-2 sm:col-span-1">
                    <span className="text-slate-400 font-medium">Diagnostic Safety:</span>
                    <p className="text-xs font-bold text-slate-300 mt-2">Zero credentials or secrets exposed</p>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* ══════════════════════════════════════════════════════════════════ */}
          {/* TAB 8: BUSINESS REPORTS & CSV EXPORT                              */}
          {/* ══════════════════════════════════════════════════════════════════ */}
          {activeTab === 'reports' && (
            <div className="space-y-8">
              {/* Report Selector Header */}
              <div className="p-6 rounded-2xl bg-slate-950/70 border border-slate-800 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
                <div className="flex items-center gap-2 overflow-x-auto w-full sm:w-auto">
                  {[
                    { id: 'sales', label: 'Sales Report' },
                    { id: 'orders', label: 'Order Report' },
                    { id: 'vendors', label: 'Vendor Performance' },
                    { id: 'commissions', label: 'Commission Report' },
                    { id: 'products', label: 'Product Inventory' },
                  ].map((r) => (
                    <button
                      key={r.id}
                      onClick={() => setSelectedReportType(r.id)}
                      className={`px-4 py-2 rounded-xl text-xs font-bold transition-all whitespace-nowrap ${
                        selectedReportType === r.id
                          ? 'bg-indigo-600 text-white shadow-md shadow-indigo-600/30'
                          : 'bg-slate-900 text-slate-400 hover:bg-slate-800 hover:text-white'
                      }`}
                    >
                      {r.label}
                    </button>
                  ))}
                </div>

                <Button
                  variant="primary"
                  size="md"
                  icon={Download}
                  onClick={() => handleExportCsv(selectedReportType)}
                  isLoading={exportingCsv}
                  disabled={exportingCsv}
                >
                  Export {selectedReportType.toUpperCase()} CSV
                </Button>
              </div>

              {/* Report Display Container */}
              {reportLoading ? (
                <Loading label={`Generating ${selectedReportType} report from PostgreSQL...`} />
              ) : reportData ? (
                <div className="p-6 rounded-2xl bg-slate-950/70 border border-slate-800 space-y-6">
                  {/* Summary Bar */}
                  <div className="flex flex-wrap items-center justify-between gap-4 pb-4 border-b border-slate-800">
                    <div>
                      <h3 className="text-base font-bold text-white capitalize">{selectedReportType} Report</h3>
                      <p className="text-xs text-slate-400">Generated: {reportData.generatedAt || new Date().toISOString()}</p>
                    </div>
                    <Badge color="emerald" size="md">Live PostgreSQL Data</Badge>
                  </div>

                  {/* Dynamic Table based on report type */}
                  <div className="overflow-x-auto">
                    <table className="w-full text-left text-xs">
                      {selectedReportType === 'sales' && (
                        <>
                          <thead>
                            <tr className="text-slate-400 uppercase tracking-wider font-semibold border-b border-slate-800">
                              <th className="pb-3 pr-4">Order #</th>
                              <th className="pb-3 px-4">Date</th>
                              <th className="pb-3 px-4">Customer</th>
                              <th className="pb-3 px-4">Subtotal</th>
                              <th className="pb-3 px-4">Discount</th>
                              <th className="pb-3 pl-4 text-right">Total Net</th>
                            </tr>
                          </thead>
                          <tbody className="divide-y divide-slate-800/50">
                            {reportData.records?.map((r, i) => (
                              <tr key={i} className="hover:bg-slate-900/50 transition-colors">
                                <td className="py-3 pr-4 font-mono font-bold text-white">{r.orderNumber}</td>
                                <td className="py-3 px-4 text-slate-300">{r.orderDate}</td>
                                <td className="py-3 px-4 text-slate-400">{r.customerEmail}</td>
                                <td className="py-3 px-4 text-slate-300">₹{r.subtotal}</td>
                                <td className="py-3 px-4 text-amber-400">₹{r.discount}</td>
                                <td className="py-3 pl-4 text-right font-bold text-emerald-400">₹{r.totalAmount}</td>
                              </tr>
                            ))}
                          </tbody>
                        </>
                      )}

                      {selectedReportType === 'orders' && (
                        <>
                          <thead>
                            <tr className="text-slate-400 uppercase tracking-wider font-semibold border-b border-slate-800">
                              <th className="pb-3 pr-4">Order #</th>
                              <th className="pb-3 px-4">Customer</th>
                              <th className="pb-3 px-4">Items</th>
                              <th className="pb-3 px-4">Amount</th>
                              <th className="pb-3 px-4">Status</th>
                              <th className="pb-3 pl-4 text-right">Payment</th>
                            </tr>
                          </thead>
                          <tbody className="divide-y divide-slate-800/50">
                            {reportData.records?.map((r) => (
                              <tr key={r.orderId} className="hover:bg-slate-900/50 transition-colors">
                                <td className="py-3 pr-4 font-mono font-bold text-white">{r.orderNumber}</td>
                                <td className="py-3 px-4 text-slate-300">{r.customerName}</td>
                                <td className="py-3 px-4 text-slate-400">{r.totalItems}</td>
                                <td className="py-3 px-4 font-bold text-indigo-400">₹{r.totalAmount}</td>
                                <td className="py-3 px-4"><Badge color="indigo" size="sm">{r.orderStatus}</Badge></td>
                                <td className="py-3 pl-4 text-right text-emerald-400 font-semibold">{r.paymentStatus}</td>
                              </tr>
                            ))}
                          </tbody>
                        </>
                      )}

                      {selectedReportType === 'vendors' && (
                        <>
                          <thead>
                            <tr className="text-slate-400 uppercase tracking-wider font-semibold border-b border-slate-800">
                              <th className="pb-3 pr-4">Store Name</th>
                              <th className="pb-3 px-4">Status</th>
                              <th className="pb-3 px-4">Products</th>
                              <th className="pb-3 px-4">Gross Sales</th>
                              <th className="pb-3 px-4">Commission</th>
                              <th className="pb-3 pl-4 text-right">Net Earnings</th>
                            </tr>
                          </thead>
                          <tbody className="divide-y divide-slate-800/50">
                            {reportData.records?.map((r) => (
                              <tr key={r.vendorId} className="hover:bg-slate-900/50 transition-colors">
                                <td className="py-3 pr-4 font-bold text-white">{r.storeName}</td>
                                <td className="py-3 px-4"><Badge color="emerald" size="sm">{r.status}</Badge></td>
                                <td className="py-3 px-4 text-slate-400">{r.totalProducts}</td>
                                <td className="py-3 px-4 font-bold text-white">₹{r.grossSales}</td>
                                <td className="py-3 px-4 text-emerald-400">₹{r.commissionPaid}</td>
                                <td className="py-3 pl-4 text-right font-bold text-indigo-400">₹{r.netEarnings}</td>
                              </tr>
                            ))}
                          </tbody>
                        </>
                      )}

                      {selectedReportType === 'commissions' && (
                        <>
                          <thead>
                            <tr className="text-slate-400 uppercase tracking-wider font-semibold border-b border-slate-800">
                              <th className="pb-3 pr-4">ID</th>
                              <th className="pb-3 px-4">Vendor Store</th>
                              <th className="pb-3 px-4">Product</th>
                              <th className="pb-3 px-4">Sale Amount</th>
                              <th className="pb-3 px-4">Commission</th>
                              <th className="pb-3 pl-4 text-right">Vendor Net</th>
                            </tr>
                          </thead>
                          <tbody className="divide-y divide-slate-800/50">
                            {reportData.records?.map((r) => (
                              <tr key={r.commissionId} className="hover:bg-slate-900/50 transition-colors">
                                <td className="py-3 pr-4 font-mono text-slate-500">#{r.commissionId}</td>
                                <td className="py-3 px-4 font-bold text-white">{r.vendorStoreName}</td>
                                <td className="py-3 px-4 text-slate-300">{r.productName}</td>
                                <td className="py-3 px-4 text-slate-400">₹{r.saleAmount}</td>
                                <td className="py-3 px-4 font-bold text-emerald-400">₹{r.commissionAmount}</td>
                                <td className="py-3 pl-4 text-right font-bold text-indigo-400">₹{r.vendorNetAmount}</td>
                              </tr>
                            ))}
                          </tbody>
                        </>
                      )}

                      {selectedReportType === 'products' && (
                        <>
                          <thead>
                            <tr className="text-slate-400 uppercase tracking-wider font-semibold border-b border-slate-800">
                              <th className="pb-3 pr-4">SKU</th>
                              <th className="pb-3 px-4">Product Name</th>
                              <th className="pb-3 px-4">Category</th>
                              <th className="pb-3 px-4">Price</th>
                              <th className="pb-3 px-4">Stock</th>
                              <th className="pb-3 pl-4 text-right">Status</th>
                            </tr>
                          </thead>
                          <tbody className="divide-y divide-slate-800/50">
                            {reportData.records?.map((r) => (
                              <tr key={r.productId} className="hover:bg-slate-900/50 transition-colors">
                                <td className="py-3 pr-4 font-mono text-slate-400">{r.sku}</td>
                                <td className="py-3 px-4 font-bold text-white">{r.name}</td>
                                <td className="py-3 px-4 text-slate-400">{r.categoryName}</td>
                                <td className="py-3 px-4 font-bold text-white">₹{r.price}</td>
                                <td className="py-3 px-4">
                                  <span className={r.lowStock ? 'text-amber-400 font-bold' : 'text-slate-300'}>
                                    {r.stockQuantity}
                                  </span>
                                </td>
                                <td className="py-3 pl-4 text-right">
                                  <Badge color={r.active ? 'emerald' : 'rose'} size="sm">
                                    {r.active ? 'Active' : 'Inactive'}
                                  </Badge>
                                </td>
                              </tr>
                            ))}
                          </tbody>
                        </>
                      )}
                    </table>
                  </div>
                </div>
              ) : (
                <p className="text-xs text-slate-500 text-center py-8">Select a report type to load data.</p>
              )}
            </div>
          )}

          {/* ══════════════════════════════════════════════════════════════════ */}
          {/* TAB 9: WAREHOUSE STOCK DISTRIBUTION                                */}
          {/* ══════════════════════════════════════════════════════════════════ */}
          {activeTab === 'distribution' && (
            <div className="space-y-6">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                <div>
                  <h2 className="text-lg font-bold text-white flex items-center gap-2">
                    <Share2 className="w-5 h-5 text-indigo-400" />
                    Multi-Warehouse Stock Distribution Console
                  </h2>
                  <p className="text-xs text-slate-400">
                    Allocate central vendor unallocated inventory across active physical fulfillment centers
                  </p>
                </div>
              </div>

              {/* Product Selector */}
              <div className="p-6 rounded-2xl bg-slate-950/70 border border-slate-800 space-y-4">
                <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400">
                  Select Product SKU to Distribute
                </label>
                <select
                  value={selectedDistProductId}
                  onChange={(e) => handleSelectDistProduct(e.target.value)}
                  className="w-full px-4 py-3 bg-slate-900 border border-slate-700 rounded-xl text-sm font-semibold text-white focus:outline-none focus:border-indigo-500"
                >
                  <option value="">-- Choose a Product ({availableProducts.length} items available) --</option>
                  {availableProducts.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.name} (SKU: {p.sku}) • Total Global Stock: {p.stockQuantity}
                    </option>
                  ))}
                </select>
              </div>

              {loadingDistOverview ? (
                <Loading label="Loading stock distribution metrics for selected SKU..." />
              ) : distOverview ? (
                <div className="space-y-6">
                  {/* Stock Metrics Row */}
                  <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                    <div className="p-5 rounded-2xl bg-slate-900/80 border border-slate-800">
                      <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Total Global Stock</span>
                      <p className="text-3xl font-extrabold text-white mt-2">{distOverview.globalTotalStock}</p>
                      <p className="text-xs text-slate-500 mt-1">Vendor uploaded quantity</p>
                    </div>
                    <div className="p-5 rounded-2xl bg-slate-900/80 border border-slate-800">
                      <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Allocated to Warehouses</span>
                      <p className="text-3xl font-extrabold text-indigo-400 mt-2">{distOverview.allocatedStock}</p>
                      <p className="text-xs text-slate-500 mt-1">Present in physical facilities</p>
                    </div>
                    <div className="p-5 rounded-2xl bg-slate-900/80 border border-slate-800">
                      <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Unallocated Balance</span>
                      <p className={`text-3xl font-extrabold mt-2 ${distOverview.unallocatedStock > 0 ? 'text-emerald-400' : 'text-slate-500'}`}>
                        {distOverview.unallocatedStock}
                      </p>
                      <p className="text-xs text-slate-500 mt-1">Available for distribution</p>
                    </div>
                  </div>

                  {/* Distribution Form */}
                  <form onSubmit={handleDistributeStockSubmit} className="p-6 rounded-2xl bg-slate-950/70 border border-slate-800 space-y-6">
                    <h3 className="text-base font-bold text-white flex items-center gap-2">
                      <Warehouse className="w-5 h-5 text-indigo-400" />
                      Warehouse Allocation Matrix
                    </h3>

                    <div className="space-y-3">
                      {(distOverview.warehouseAllocations || []).map((w) => (
                        <div key={w.warehouseId} className="flex flex-col sm:flex-row sm:items-center justify-between p-4 rounded-xl bg-slate-900/70 border border-slate-800 gap-4">
                          <div className="space-y-0.5">
                            <div className="flex items-center gap-2">
                              <span className="font-mono text-xs font-bold text-indigo-300 px-2 py-0.5 rounded bg-indigo-500/10">
                                {w.warehouseCode}
                              </span>
                              <span className="font-semibold text-sm text-white">{w.warehouseName}</span>
                            </div>
                            <p className="text-xs text-slate-400">
                              Current Total Stock: <span className="text-white font-bold">{w.totalQuantity ?? w.currentTotalQuantity ?? 0}</span> • Reserved: <span className="text-amber-400">{w.reservedQuantity ?? w.currentReservedQuantity ?? 0}</span> • Available: <span className="text-emerald-400">{w.availableQuantity ?? w.currentAvailableQuantity ?? 0}</span>
                            </p>
                          </div>

                          <div className="flex items-center gap-3">
                            <label className="text-xs font-medium text-slate-300 whitespace-nowrap">Distribute Qty:</label>
                            <input
                              type="number"
                              min="0"
                              max={distOverview.unallocatedStock}
                              value={distAllocations[w.warehouseId] || 0}
                              onChange={(e) => {
                                const val = parseInt(e.target.value) || 0;
                                setDistAllocations({ ...distAllocations, [w.warehouseId]: val });
                              }}
                              className="w-28 px-3 py-2 bg-slate-950 border border-slate-700 rounded-xl text-sm font-bold text-white text-center focus:outline-none focus:border-indigo-500"
                            />
                          </div>
                        </div>
                      ))}
                    </div>

                    <div className="flex flex-col sm:flex-row items-center justify-between gap-4 pt-4 border-t border-slate-800">
                      <div className="text-xs text-slate-400">
                        Total Units to Distribute: <span className="font-bold text-white">{Object.values(distAllocations).reduce((a, b) => a + Number(b || 0), 0)}</span> / {distOverview.unallocatedStock}
                      </div>

                      <Button
                        variant="primary"
                        size="md"
                        type="submit"
                        disabled={isSavingDist || distOverview.unallocatedStock <= 0 || Object.values(distAllocations).reduce((a, b) => a + Number(b || 0), 0) === 0}
                      >
                        {isSavingDist ? 'Distributing Stock...' : 'Confirm Stock Distribution'}
                      </Button>
                    </div>
                  </form>
                </div>
              ) : (
                <div className="p-12 text-center text-slate-500 bg-slate-950/40 rounded-2xl border border-slate-800/80">
                  <Share2 className="w-10 h-10 mx-auto mb-3 opacity-30 text-indigo-400" />
                  Please select a product from the dropdown above to view stock levels and distribute units to warehouses.
                </div>
              )}
            </div>
          )}

          {/* ══════════════════════════════════════════════════════════════════ */}
          {/* TAB 10: RETURNS & QC OVERSIGHT                                     */}
          {/* ══════════════════════════════════════════════════════════════════ */}
          {activeTab === 'returns' && (
            <div className="space-y-6">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                <div>
                  <h2 className="text-lg font-bold text-white flex items-center gap-2">
                    <RotateCcw className="w-5 h-5 text-indigo-400" />
                    Customer Returns & QC Lifecycle Oversight
                  </h2>
                  <p className="text-xs text-slate-400">
                    Review and authorize return requests, manage warehouse routing, and audit quality inspections
                  </p>
                </div>

                <div className="flex items-center gap-2">
                  <span className="text-xs font-semibold text-slate-400">Filter Status:</span>
                  <select
                    value={returnStatusFilter}
                    onChange={(e) => setReturnStatusFilter(e.target.value)}
                    className="px-3 py-2 bg-slate-900 border border-slate-700 rounded-xl text-xs font-semibold text-white focus:outline-none focus:border-indigo-500"
                  >
                    <option value="ALL">All Return Statuses ({adminReturns.length})</option>
                    <option value="REQUESTED">Pending Decision ({adminReturns.filter(r => r.status === 'REQUESTED').length})</option>
                    <option value="APPROVED">Approved / In Transit</option>
                    <option value="ITEM_RECEIVED">Received at Warehouse</option>
                    <option value="QC_COMPLETED">QC Completed</option>
                    <option value="REFUNDED">Refunded</option>
                    <option value="REJECTED">Rejected</option>
                    <option value="CLOSED">Closed</option>
                  </select>
                </div>
              </div>

              <div className="bg-slate-950/70 border border-slate-800/90 rounded-2xl overflow-hidden shadow-xl">
                <table className="w-full text-left text-xs">
                  <thead className="bg-slate-900/90 text-slate-400 font-semibold border-b border-slate-800">
                    <tr>
                      <th className="py-3.5 px-4">Return #</th>
                      <th className="py-3.5 px-4">Order / Customer</th>
                      <th className="py-3.5 px-4">Returned Item</th>
                      <th className="py-3.5 px-4">Target Warehouse</th>
                      <th className="py-3.5 px-4 text-center">Refund Value</th>
                      <th className="py-3.5 px-4">Status</th>
                      <th className="py-3.5 px-4 text-right">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800/60">
                    {filteredReturns.length === 0 ? (
                      <tr>
                        <td colSpan={7} className="py-8 text-center text-slate-500">
                          <RotateCcw className="w-8 h-8 mx-auto mb-2 opacity-30 text-indigo-400" />
                          No return requests match the selected filter.
                        </td>
                      </tr>
                    ) : (
                      filteredReturns.map((ret) => (
                        <tr key={ret.id} className="hover:bg-slate-900/40 transition-colors">
                          <td className="py-3.5 px-4 font-mono font-bold text-indigo-300">
                            {ret.returnNumber}
                          </td>
                          <td className="py-3.5 px-4">
                            <span className="font-mono text-white">#{ret.orderNumber}</span>
                            <div className="text-[11px] text-slate-400">{ret.customerName || ret.userEmail}</div>
                          </td>
                          <td className="py-3.5 px-4">
                            <span className="font-semibold text-white">{ret.productName || 'Order Item'}</span>
                            <div className="text-[10px] text-slate-400">Qty: {ret.returnQuantity || 1} • Reason: {ret.reason}</div>
                          </td>
                          <td className="py-3.5 px-4">
                            <span className="px-2 py-0.5 rounded bg-slate-800 text-amber-300 font-mono text-[10px]">
                              {ret.returnWarehouseCode || ret.originalWarehouseCode || 'WH-CENTRAL'}
                            </span>
                          </td>
                          <td className="py-3.5 px-4 text-center font-bold text-emerald-400">
                            ₹{ret.refundAmount}
                          </td>
                          <td className="py-3.5 px-4">
                            <Badge color={returnStatusColor[ret.status] || 'amber'} size="sm">
                              {ret.status}
                            </Badge>
                          </td>
                          <td className="py-3.5 px-4 text-right">
                            {ret.status === 'REQUESTED' ? (
                              <div className="flex items-center justify-end gap-2">
                                <Button
                                  variant="primary"
                                  size="sm"
                                  icon={CheckCircle}
                                  onClick={() => handleOpenDecisionModal(ret, 'APPROVE')}
                                >
                                  Approve
                                </Button>
                                <Button
                                  variant="outline"
                                  size="sm"
                                  icon={XCircle}
                                  onClick={() => handleOpenDecisionModal(ret, 'REJECT')}
                                >
                                  Reject
                                </Button>
                              </div>
                            ) : (
                              <span className="text-[11px] font-mono text-slate-400">
                                {ret.qcStatus ? `QC: ${ret.qcStatus}` : 'Processed'}
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
          )}

          {/* ── Tab: Warehouse Staff Management ──────────────────────────────── */}
          {activeTab === 'staff' && (
            <div className="space-y-6">
              {/* Header Actions */}
              <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 bg-slate-950/80 border border-slate-800/80 rounded-2xl p-5">
                <div className="space-y-1">
                  <h2 className="text-lg font-bold text-white flex items-center gap-2">
                    <Users className="w-5 h-5 text-indigo-400" />
                    Warehouse Staff Lifecycle & Authorizations
                  </h2>
                  <p className="text-xs text-slate-400">
                    Review public staff registrations, approve or reject pending applications, assign fulfillment hubs, and oversee operational credentials.
                  </p>
                </div>
                <div className="flex items-center gap-3">
                  <Button
                    variant="primary"
                    size="md"
                    icon={Plus}
                    onClick={handleOpenCreateStaff}
                  >
                    Direct Staff Provisioning
                  </Button>
                </div>
              </div>

              {/* Status Filter & Search Controls */}
              <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-4">
                {/* Status Tabs */}
                <div className="flex items-center gap-1.5 p-1 bg-slate-900/90 border border-slate-800 rounded-xl overflow-x-auto">
                  {[
                    { id: 'ALL', label: 'All Staff', count: staffUsers.length },
                    { id: 'PENDING', label: 'Pending Approval', count: pendingStaffCount },
                    { id: 'ACTIVE', label: 'Active', count: staffUsers.filter((s) => (s.status === 'ACTIVE' || (!s.status && s.enabled))).length },
                    { id: 'REJECTED', label: 'Rejected', count: staffUsers.filter((s) => s.status === 'REJECTED').length },
                    { id: 'SUSPENDED', label: 'Suspended', count: staffUsers.filter((s) => (s.status === 'SUSPENDED' || (!s.status && !s.enabled))).length },
                  ].map((tab) => (
                    <button
                      key={tab.id}
                      onClick={() => setStaffStatusFilter(tab.id)}
                      className={`flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs font-semibold transition-all whitespace-nowrap ${
                        staffStatusFilter === tab.id
                          ? 'bg-indigo-600 text-white shadow-md shadow-indigo-600/20'
                          : 'text-slate-400 hover:text-white hover:bg-slate-800/60'
                      }`}
                    >
                      <span>{tab.label}</span>
                      <span className={`px-1.5 py-0.2 rounded-full text-[10px] ${
                        staffStatusFilter === tab.id
                          ? 'bg-white/20 text-white'
                          : tab.id === 'PENDING' && tab.count > 0
                          ? 'bg-amber-500/20 text-amber-300 font-bold'
                          : 'bg-slate-800 text-slate-400'
                      }`}>
                        {tab.count}
                      </span>
                    </button>
                  ))}
                </div>

                {/* Search Bar */}
                <div className="relative w-full sm:w-80">
                  <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
                  <input
                    type="text"
                    value={staffSearch}
                    onChange={(e) => setStaffSearch(e.target.value)}
                    placeholder="Search name, email, warehouse..."
                    className="w-full bg-slate-900/90 text-white text-xs rounded-xl border border-slate-700/80 pl-9 pr-3.5 py-2.5 placeholder-slate-500 focus:outline-none focus:border-indigo-500 transition-all"
                  />
                </div>
              </div>

              {/* Staff Table */}
              <div className="bg-slate-950/80 border border-slate-800/80 rounded-2xl overflow-hidden shadow-xl">
                <table className="w-full text-left text-xs">
                  <thead className="bg-slate-900/90 text-slate-400 border-b border-slate-800 uppercase tracking-wider text-[11px]">
                    <tr>
                      <th className="py-3.5 px-4 font-semibold">Staff Member</th>
                      <th className="py-3.5 px-4 font-semibold">Contact Email & Phone</th>
                      <th className="py-3.5 px-4 font-semibold">Assigned Facility</th>
                      <th className="py-3.5 px-4 font-semibold">Registered</th>
                      <th className="py-3.5 px-4 font-semibold">Status</th>
                      <th className="py-3.5 px-4 font-semibold text-right">Operational Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800/60 text-slate-300">
                    {filteredStaff.length === 0 ? (
                      <tr>
                        <td colSpan="6" className="py-12 text-center text-slate-500">
                          <Users className="w-10 h-10 mx-auto mb-2 opacity-30" />
                          <p className="font-semibold text-sm">No warehouse staff members found</p>
                          <p className="text-xs text-slate-500 mt-1">
                            {staffStatusFilter !== 'ALL'
                              ? `No staff members matching the "${staffStatusFilter}" status filter.`
                              : 'No warehouse staff registrations recorded yet.'}
                          </p>
                        </td>
                      </tr>
                    ) : (
                      filteredStaff.map((staff) => {
                        const effectiveStatus = staff.status || (staff.enabled ? 'ACTIVE' : 'SUSPENDED');
                        const staffId = staff.userId || staff.id;
                        return (
                          <tr key={staff.id || staff.userId} className="hover:bg-slate-900/40 transition-colors">
                            <td className="py-3.5 px-4">
                              <div className="flex items-center gap-2.5">
                                <div className={`w-9 h-9 rounded-xl border flex items-center justify-center font-bold text-xs ${
                                  effectiveStatus === 'PENDING'
                                    ? 'bg-amber-500/10 border-amber-500/30 text-amber-400'
                                    : effectiveStatus === 'ACTIVE'
                                    ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-400'
                                    : effectiveStatus === 'REJECTED'
                                    ? 'bg-rose-500/10 border-rose-500/30 text-rose-400'
                                    : 'bg-slate-700/20 border-slate-700/40 text-slate-400'
                                }`}>
                                  {staff.firstName?.[0] || 'W'}{staff.lastName?.[0] || 'S'}
                                </div>
                                <div>
                                  <p className="font-semibold text-white">{staff.firstName} {staff.lastName}</p>
                                  <p className="text-[10px] text-slate-500 font-mono">User ID: #{staff.userId || staff.id}</p>
                                </div>
                              </div>
                            </td>
                            <td className="py-3.5 px-4">
                              <p className="font-mono text-slate-300">{staff.email}</p>
                              <p className="text-[11px] text-slate-500">{staff.phoneNumber || 'No phone recorded'}</p>
                            </td>
                            <td className="py-3.5 px-4">
                              {staff.warehouseCode || staff.warehouseName ? (
                                <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-slate-900 border border-slate-800 text-indigo-300 font-mono text-[11px]">
                                  <Warehouse className="w-3 h-3 text-indigo-400" />
                                  <span>{staff.warehouseCode || staff.warehouseName}</span>
                                </div>
                              ) : (
                                <span className="text-slate-500 italic">Central / Any Facility</span>
                              )}
                            </td>
                            <td className="py-3.5 px-4 text-slate-400 text-[11px]">
                              {staff.createdAt
                                ? new Date(staff.createdAt).toLocaleDateString('en-US', {
                                    month: 'short',
                                    day: 'numeric',
                                    year: 'numeric',
                                  })
                                : '—'}
                            </td>
                            <td className="py-3.5 px-4">
                              <Badge
                                color={
                                  effectiveStatus === 'ACTIVE'
                                    ? 'emerald'
                                    : effectiveStatus === 'PENDING'
                                    ? 'amber'
                                    : effectiveStatus === 'REJECTED'
                                    ? 'rose'
                                    : 'slate'
                                }
                                size="sm"
                              >
                                {effectiveStatus}
                              </Badge>
                            </td>
                            <td className="py-3.5 px-4 text-right">
                              <div className="inline-flex items-center justify-end gap-1.5">
                                {effectiveStatus === 'PENDING' && (
                                  <>
                                    <button
                                      onClick={() => handleApproveStaff(staffId)}
                                      title="Approve Staff Application"
                                      className="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-lg text-xs font-semibold border border-emerald-500/30 text-emerald-400 bg-emerald-500/10 hover:bg-emerald-500/20 transition-colors"
                                    >
                                      <Check className="w-3.5 h-3.5" />
                                      <span>Approve</span>
                                    </button>
                                    <button
                                      onClick={() => handleOpenRejectStaff(staffId)}
                                      title="Reject Staff Application"
                                      className="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-lg text-xs font-semibold border border-rose-500/30 text-rose-400 bg-rose-500/10 hover:bg-rose-500/20 transition-colors"
                                    >
                                      <X className="w-3.5 h-3.5" />
                                      <span>Reject</span>
                                    </button>
                                  </>
                                )}

                                {effectiveStatus === 'ACTIVE' && (
                                  <button
                                    onClick={() => handleSuspendStaff(staffId)}
                                    title="Suspend Staff Account"
                                    className="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-lg text-xs font-semibold border border-amber-500/30 text-amber-400 bg-amber-500/10 hover:bg-amber-500/20 transition-colors"
                                  >
                                    <ShieldAlert className="w-3.5 h-3.5" />
                                    <span>Suspend</span>
                                  </button>
                                )}

                                {effectiveStatus === 'SUSPENDED' && (
                                  <button
                                    onClick={() => handleReactivateStaff(staffId)}
                                    title="Reactivate Staff Account"
                                    className="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-lg text-xs font-semibold border border-emerald-500/30 text-emerald-400 bg-emerald-500/10 hover:bg-emerald-500/20 transition-colors"
                                  >
                                    <RefreshCw className="w-3.5 h-3.5" />
                                    <span>Reactivate</span>
                                  </button>
                                )}

                                <button
                                  onClick={() => setSelectedStaffDetails(staff)}
                                  title="View Staff Details"
                                  className="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-lg text-xs font-semibold border border-slate-700/80 text-slate-300 hover:text-white hover:bg-slate-800 transition-colors"
                                >
                                  <Eye className="w-3.5 h-3.5" />
                                  <span>Details</span>
                                </button>
                              </div>
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
        </>
      )}

      {/* ── Modal: Create Warehouse Staff ──────────────────────────────────── */}
      {showStaffModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-in fade-in duration-200">
          <div className="relative w-full max-w-lg bg-slate-900 border border-slate-800 rounded-3xl p-6 sm:p-8 shadow-2xl space-y-6">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-indigo-500/10 border border-indigo-500/20 text-indigo-400 flex items-center justify-center">
                  <Users className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-lg font-bold text-white">Create Warehouse Staff Account</h3>
                  <p className="text-xs text-slate-400">Internal fulfillment operations credential</p>
                </div>
              </div>
              <button
                onClick={() => setShowStaffModal(false)}
                className="text-slate-400 hover:text-white p-1 rounded-xl hover:bg-slate-800 transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleSaveStaff} className="space-y-4" autoComplete="off">
              {/* Role Indicator Banner */}
              <div className="p-3 rounded-xl bg-amber-500/10 border border-amber-500/20 flex items-center justify-between">
                <span className="text-xs font-semibold text-amber-300">System Role:</span>
                <Badge color="amber" size="sm">WAREHOUSE_STAFF (Server-Assigned)</Badge>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <Input
                  label="First Name"
                  required
                  value={staffForm.firstName}
                  onChange={(e) => {
                    setStaffForm((prev) => ({ ...prev, firstName: e.target.value }));
                    if (staffFormErrors.firstName) setStaffFormErrors((prev) => ({ ...prev, firstName: null }));
                  }}
                  error={staffFormErrors.firstName}
                  placeholder="First Name"
                  autoComplete="off"
                />
                <Input
                  label="Last Name"
                  required
                  value={staffForm.lastName}
                  onChange={(e) => {
                    setStaffForm((prev) => ({ ...prev, lastName: e.target.value }));
                    if (staffFormErrors.lastName) setStaffFormErrors((prev) => ({ ...prev, lastName: null }));
                  }}
                  error={staffFormErrors.lastName}
                  placeholder="Last Name"
                  autoComplete="off"
                />
              </div>

              <Input
                label="Email Address"
                type="email"
                required
                value={staffForm.email}
                onChange={(e) => {
                  setStaffForm((prev) => ({ ...prev, email: e.target.value }));
                  if (staffFormErrors.email) setStaffFormErrors((prev) => ({ ...prev, email: null }));
                }}
                error={staffFormErrors.email}
                placeholder="staff@warehouse.shopstack.com"
                autoComplete="off"
              />

              <Input
                label="Phone Number (Optional)"
                type="tel"
                value={staffForm.phoneNumber}
                onChange={(e) => setStaffForm((prev) => ({ ...prev, phoneNumber: e.target.value }))}
                placeholder="+91 9876543210"
                autoComplete="off"
              />

              <div className="grid grid-cols-2 gap-3">
                <Input
                  label="Password"
                  type="password"
                  required
                  value={staffForm.password}
                  onChange={(e) => {
                    setStaffForm((prev) => ({ ...prev, password: e.target.value }));
                    if (staffFormErrors.password) setStaffFormErrors((prev) => ({ ...prev, password: null }));
                  }}
                  error={staffFormErrors.password}
                  placeholder="Min 8 chars"
                  autoComplete="new-password"
                />
                <Input
                  label="Confirm Password"
                  type="password"
                  required
                  value={staffForm.confirmPassword}
                  onChange={(e) => {
                    setStaffForm((prev) => ({ ...prev, confirmPassword: e.target.value }));
                    if (staffFormErrors.confirmPassword) setStaffFormErrors((prev) => ({ ...prev, confirmPassword: null }));
                  }}
                  error={staffFormErrors.confirmPassword}
                  placeholder="Re-enter password"
                  autoComplete="new-password"
                />
              </div>

              <div className="flex justify-end gap-3 pt-3 border-t border-slate-800">
                <Button
                  variant="outline"
                  size="md"
                  type="button"
                  onClick={() => setShowStaffModal(false)}
                >
                  Cancel
                </Button>
                <Button
                  variant="primary"
                  size="md"
                  type="submit"
                  isLoading={savingStaff}
                  disabled={savingStaff}
                  icon={Plus}
                >
                  Create Staff Account
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ── Modal: Return Decision (Approve / Reject) ──────────────────────── */}
      {activeDecisionReturn && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm">
          <div className="bg-slate-950 border border-slate-800 rounded-3xl p-6 max-w-md w-full space-y-5 shadow-2xl">
            <div className="flex items-center justify-between">
              <h3 className="text-lg font-bold text-white flex items-center gap-2">
                {decisionAction === 'APPROVE' ? (
                  <CheckCircle className="w-5 h-5 text-emerald-400" />
                ) : (
                  <XCircle className="w-5 h-5 text-rose-400" />
                )}
                {decisionAction === 'APPROVE' ? 'Authorize Return Request' : 'Decline Return Request'}
              </h3>
              <button onClick={() => setActiveDecisionReturn(null)} className="text-slate-400 hover:text-white">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-3.5 rounded-xl bg-slate-900/80 border border-slate-800 space-y-1 text-xs">
              <div className="flex justify-between">
                <span className="text-slate-400">Return Ref:</span>
                <span className="font-mono font-bold text-indigo-300">{activeDecisionReturn.returnNumber}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Assigned Return Hub:</span>
                <span className="font-mono font-bold text-amber-300">
                  {activeDecisionReturn.returnWarehouseCode || activeDecisionReturn.originalWarehouseCode || 'WH-CENTRAL'}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Product:</span>
                <span className="font-semibold text-white">{activeDecisionReturn.productName}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Refund Amount:</span>
                <span className="font-bold text-emerald-400">₹{activeDecisionReturn.refundAmount}</span>
              </div>
            </div>

            <form onSubmit={handleReturnDecisionSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">Administrative Remarks / Instructions</label>
                <textarea
                  rows="3"
                  value={decisionRemarks}
                  onChange={(e) => setDecisionRemarks(e.target.value)}
                  className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-xl text-xs text-white focus:outline-none focus:border-indigo-500"
                  required
                />
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <Button variant="outline" size="sm" type="button" onClick={() => setActiveDecisionReturn(null)}>
                  Cancel
                </Button>
                <Button
                  variant={decisionAction === 'APPROVE' ? 'primary' : 'danger'}
                  size="sm"
                  type="submit"
                  disabled={isSavingDecision}
                >
                  {isSavingDecision ? 'Submitting...' : decisionAction === 'APPROVE' ? 'Confirm Approval' : 'Confirm Rejection'}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ── Modal: Reject Warehouse Staff ──────────────────────────────────── */}
      {showRejectStaffModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-in fade-in duration-200">
          <div className="bg-slate-950 border border-slate-800 rounded-3xl p-6 max-w-md w-full space-y-5 shadow-2xl">
            <div className="flex items-center justify-between">
              <h3 className="text-lg font-bold text-white flex items-center gap-2">
                <XCircle className="w-5 h-5 text-rose-400" />
                Reject Warehouse Staff Application
              </h3>
              <button
                onClick={() => {
                  setShowRejectStaffModal(false);
                  setRejectingStaffId(null);
                  setStaffRejectReason('');
                }}
                className="text-slate-400 hover:text-white"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <p className="text-xs text-slate-400">
              Please provide the official reason for rejecting this warehouse staff registration. This explanation will be recorded in the audit trail and displayed to the user upon login attempt.
            </p>

            <div className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">
                  Rejection Reason / Verification Notes <span className="text-rose-400">*</span>
                </label>
                <textarea
                  rows="3"
                  value={staffRejectReason}
                  onChange={(e) => setStaffRejectReason(e.target.value)}
                  placeholder="e.g., Unverified employment credentials, facility full capacity, invalid contact details..."
                  className="w-full px-3.5 py-2.5 bg-slate-900 border border-slate-700/80 rounded-xl text-xs text-white focus:outline-none focus:border-rose-500 placeholder-slate-500"
                  required
                />
              </div>

              <div className="flex justify-end gap-2 pt-2 border-t border-slate-800">
                <Button
                  variant="outline"
                  size="sm"
                  type="button"
                  onClick={() => {
                    setShowRejectStaffModal(false);
                    setRejectingStaffId(null);
                    setStaffRejectReason('');
                  }}
                >
                  Cancel
                </Button>
                <Button
                  variant="danger"
                  size="sm"
                  type="button"
                  disabled={!staffRejectReason.trim()}
                  onClick={handleConfirmRejectStaff}
                >
                  Confirm Rejection
                </Button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* ── Modal: Warehouse Staff Details ─────────────────────────────────── */}
      {selectedStaffDetails && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-in fade-in duration-200">
          <div className="relative w-full max-w-lg bg-slate-900 border border-slate-800 rounded-3xl p-6 sm:p-8 shadow-2xl space-y-6">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-indigo-500/10 border border-indigo-500/20 text-indigo-400 flex items-center justify-center font-bold text-sm">
                  {selectedStaffDetails.firstName?.[0] || 'W'}{selectedStaffDetails.lastName?.[0] || 'S'}
                </div>
                <div>
                  <h3 className="text-lg font-bold text-white">
                    {selectedStaffDetails.firstName} {selectedStaffDetails.lastName}
                  </h3>
                  <p className="text-xs text-slate-400 font-mono">
                    User #{selectedStaffDetails.userId || selectedStaffDetails.id} · Profile #{selectedStaffDetails.id || 'N/A'}
                  </p>
                </div>
              </div>
              <button
                onClick={() => setSelectedStaffDetails(null)}
                className="text-slate-400 hover:text-white p-1 rounded-xl hover:bg-slate-800 transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Status and Facility Cards */}
            <div className="grid grid-cols-2 gap-3">
              <div className="p-3.5 rounded-xl bg-slate-950/70 border border-slate-800/80 space-y-1">
                <span className="text-[11px] font-semibold text-slate-400">Account Lifecycle Status</span>
                <div>
                  <Badge
                    color={
                      selectedStaffDetails.status === 'ACTIVE' || (!selectedStaffDetails.status && selectedStaffDetails.enabled)
                        ? 'emerald'
                        : selectedStaffDetails.status === 'PENDING'
                        ? 'amber'
                        : selectedStaffDetails.status === 'REJECTED'
                        ? 'rose'
                        : 'slate'
                    }
                    size="sm"
                  >
                    {selectedStaffDetails.status || (selectedStaffDetails.enabled ? 'ACTIVE' : 'SUSPENDED')}
                  </Badge>
                </div>
              </div>

              <div className="p-3.5 rounded-xl bg-slate-950/70 border border-slate-800/80 space-y-1">
                <span className="text-[11px] font-semibold text-slate-400">Assigned Facility</span>
                <p className="text-xs font-bold text-indigo-300 font-mono">
                  {selectedStaffDetails.warehouseCode || selectedStaffDetails.warehouseName || 'Central Hub / Any'}
                </p>
              </div>
            </div>

            {/* Profile Information List */}
            <div className="p-4 rounded-2xl bg-slate-950/60 border border-slate-800/80 space-y-3 text-xs">
              <div className="flex justify-between items-center py-1 border-b border-slate-800/60">
                <span className="text-slate-400">Email Address</span>
                <span className="font-mono text-slate-200">{selectedStaffDetails.email}</span>
              </div>
              <div className="flex justify-between items-center py-1 border-b border-slate-800/60">
                <span className="text-slate-400">Phone Number</span>
                <span className="text-slate-200">{selectedStaffDetails.phoneNumber || 'Not provided'}</span>
              </div>
              <div className="flex justify-between items-center py-1 border-b border-slate-800/60">
                <span className="text-slate-400">Role Authorization</span>
                <Badge color="amber" size="sm">WAREHOUSE_STAFF</Badge>
              </div>
              <div className="flex justify-between items-center py-1 border-b border-slate-800/60">
                <span className="text-slate-400">Application Date</span>
                <span className="text-slate-300">
                  {selectedStaffDetails.createdAt
                    ? new Date(selectedStaffDetails.createdAt).toLocaleString()
                    : '—'}
                </span>
              </div>
              {selectedStaffDetails.approvedBy && (
                <div className="flex justify-between items-center py-1 border-b border-slate-800/60">
                  <span className="text-slate-400">Reviewer / Approver</span>
                  <span className="text-slate-200 font-semibold">{selectedStaffDetails.approvedBy}</span>
                </div>
              )}
              {selectedStaffDetails.approvedAt && (
                <div className="flex justify-between items-center py-1 border-b border-slate-800/60">
                  <span className="text-slate-400">Decision Timestamp</span>
                  <span className="text-slate-300">
                    {new Date(selectedStaffDetails.approvedAt).toLocaleString()}
                  </span>
                </div>
              )}
              {selectedStaffDetails.rejectionReason && (
                <div className="p-3 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-300 space-y-1">
                  <span className="font-semibold block text-[11px] text-rose-400">Rejection Reason:</span>
                  <p className="text-xs">{selectedStaffDetails.rejectionReason}</p>
                </div>
              )}
            </div>

            {/* Actions in details modal */}
            <div className="flex justify-between items-center pt-2 border-t border-slate-800">
              <div className="flex items-center gap-2">
                {selectedStaffDetails.status === 'PENDING' && (
                  <>
                    <Button
                      variant="primary"
                      size="sm"
                      icon={Check}
                      onClick={() => {
                        handleApproveStaff(selectedStaffDetails.userId || selectedStaffDetails.id);
                        setSelectedStaffDetails(null);
                      }}
                    >
                      Approve
                    </Button>
                    <Button
                      variant="danger"
                      size="sm"
                      icon={X}
                      onClick={() => {
                        const targetId = selectedStaffDetails.userId || selectedStaffDetails.id;
                        setSelectedStaffDetails(null);
                        handleOpenRejectStaff(targetId);
                      }}
                    >
                      Reject
                    </Button>
                  </>
                )}
                {(selectedStaffDetails.status === 'ACTIVE' || (!selectedStaffDetails.status && selectedStaffDetails.enabled)) && (
                  <Button
                    variant="outline"
                    size="sm"
                    icon={ShieldAlert}
                    onClick={() => {
                      handleSuspendStaff(selectedStaffDetails.userId || selectedStaffDetails.id);
                      setSelectedStaffDetails(null);
                    }}
                  >
                    Suspend Staff
                  </Button>
                )}
                {(selectedStaffDetails.status === 'SUSPENDED' || (!selectedStaffDetails.status && !selectedStaffDetails.enabled)) && (
                  <Button
                    variant="primary"
                    size="sm"
                    icon={RefreshCw}
                    onClick={() => {
                      handleReactivateStaff(selectedStaffDetails.userId || selectedStaffDetails.id);
                      setSelectedStaffDetails(null);
                    }}
                  >
                    Reactivate Staff
                  </Button>
                )}
              </div>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setSelectedStaffDetails(null)}
              >
                Close
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default AdminDashboard;

