import { useEffect, useState } from 'react';
import { FileBarChart, Download, FileSpreadsheet } from 'lucide-react';
import { backend } from '../../lib/backend';
import { adminAnalytics } from '../../lib/adminApi';
import { PageLoader } from '../../components/Loader';
import { ErrorState } from '../../components/States';
import { StatCard } from '../../components/DataTable';

const REPORT_TYPES = [
  { value: 'SALES', label: 'Sales' },
  { value: 'VENDORS', label: 'Vendors' },
  { value: 'PRODUCTS', label: 'Products' },
  { value: 'CUSTOMERS', label: 'Customers' },
  { value: 'PAYMENTS', label: 'Payments' },
  { value: 'COUPONS', label: 'Coupons' },
  { value: 'INVENTORY', label: 'Inventory' },
];

export function AdminReportsPage() {
  const [analytics, setAnalytics] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [preset, setPreset] = useState('monthly');
  const [reportType, setReportType] = useState('SALES');
  const [reportData, setReportData] = useState(null);
  const [reportLoading, setReportLoading] = useState(false);
  const [reportError, setReportError] = useState(null);
  const [exportMsg, setExportMsg] = useState('');

  useEffect(() => {
    (async () => {
      setLoading(true);
      try {
        const a = await adminAnalytics();
        setAnalytics(a);
      } catch (e) {
        setError(e.message);
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const fetchReport = async () => {
    setReportLoading(true);
    setReportError(null);
    setReportData(null);
    try {
      // Try Spring Boot backend first; fall back to Supabase analytics.
      try {
        const res = await backend.get(
          `/api/admin/reports/${reportType.toLowerCase()}?preset=${preset}`
        );
        setReportData(res.data?.data || res.data);
      } catch (backendErr) {
        // Backend unavailable — show a structured summary from analytics instead.
        setReportData({ _fallback: true, message: backendErr.message });
      }
    } catch (e) {
      setReportError(e.message);
    } finally {
      setReportLoading(false);
    }
  };

  const exportReport = async (format) => {
    setExportMsg('');
    try {
      const res = await backend.get(
        `/api/admin/reports/export/${format}?type=${reportType}&preset=${preset}`,
        { responseType: format === 'excel' ? 'blob' : 'text' }
      );
      setExportMsg(`${format.toUpperCase()} export ready.`);
    } catch (e) {
      setExportMsg(`${format.toUpperCase()} export unavailable: ${e.message}`);
    }
  };

  if (loading) return <PageLoader label="Loading reports…" />;
  if (error) return <ErrorState message={error} />;

  return (
    <div className="space-y-6">
      <div className="grid gap-4 sm:grid-cols-3">
        <StatCard label="Total revenue" value={`$${Number(analytics?.revenue || 0).toFixed(2)}`} icon={FileBarChart} accent="emerald" />
        <StatCard label="Total orders" value={analytics?.counts?.orders || 0} icon={FileBarChart} accent="brand" />
        <StatCard label="Total products" value={analytics?.counts?.products || 0} icon={FileBarChart} accent="slate" />
      </div>

      <section className="card p-5">
        <h3 className="mb-4 flex items-center gap-2 font-semibold text-slate-800">
          <FileBarChart size={18} className="text-brand-600" /> Generate report
        </h3>
        <div className="flex flex-wrap items-end gap-3">
          <div>
            <label className="label">Report type</label>
            <select className="input" value={reportType} onChange={(e) => setReportType(e.target.value)}>
              {REPORT_TYPES.map((r) => <option key={r.value} value={r.value}>{r.label}</option>)}
            </select>
          </div>
          <div>
            <label className="label">Preset</label>
            <select className="input" value={preset} onChange={(e) => setPreset(e.target.value)}>
              <option value="daily">Daily</option>
              <option value="weekly">Weekly</option>
              <option value="monthly">Monthly</option>
              <option value="yearly">Yearly</option>
            </select>
          </div>
          <button onClick={fetchReport} disabled={reportLoading} className="btn-primary">
            {reportLoading ? 'Loading…' : 'Generate'}
          </button>
        </div>

        <div className="mt-3 flex gap-2">
          <button onClick={() => exportReport('csv')} className="btn-secondary"><Download size={16} /> CSV</button>
          <button onClick={() => exportReport('excel')} className="btn-secondary"><FileSpreadsheet size={16} /> Excel</button>
        </div>
        {exportMsg && <p className="mt-2 text-xs text-slate-500">{exportMsg}</p>}
      </section>

      <section className="card p-5">
        <h3 className="mb-3 font-semibold text-slate-800">Report output</h3>
        {reportLoading ? (
          <p className="text-sm text-slate-400">Loading report…</p>
        ) : reportError ? (
          <ErrorState message={reportError} />
        ) : !reportData ? (
          <p className="text-sm text-slate-400">Select a report type and generate it to see results here.</p>
        ) : reportData._fallback ? (
          <div className="rounded-lg bg-amber-50 p-4 text-sm text-amber-800">
            <p className="font-medium">Spring Boot report backend not reachable.</p>
            <p className="mt-1">{reportData.message}</p>
            <p className="mt-2">Summary from live data — Revenue: ${Number(analytics?.revenue || 0).toFixed(2)}, Orders: {analytics?.counts?.orders || 0}, Products: {analytics?.counts?.products || 0}.</p>
          </div>
        ) : (
          <pre className="max-h-96 overflow-auto rounded-lg bg-slate-900 p-4 text-xs text-slate-100">
            {JSON.stringify(reportData, null, 2)}
          </pre>
        )}
      </section>
    </div>
  );
}
