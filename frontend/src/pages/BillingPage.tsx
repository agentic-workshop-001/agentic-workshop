import { useState, useEffect, useCallback } from 'react';
import { billingApi, Invoice } from '../api/client';

export default function BillingPage() {
  const [period, setPeriod]         = useState('');
  const [running, setRunning]       = useState(false);
  const [runResult, setRunResult]   = useState<{ generated: number; invoices: string[] } | null>(null);
  const [invoices, setInvoices]     = useState<Invoice[]>([]);
  const [filterPeriod, setFilterPeriod] = useState('');
  const [loading, setLoading]       = useState(false);
  const [error, setError]           = useState('');
  const [success, setSuccess]       = useState('');

  const loadInvoices = useCallback(async () => {
    setLoading(true); setError('');
    try { setInvoices(await billingApi.listInvoices(filterPeriod || undefined)); }
    catch (e: any) { setError(e.message); }
    finally { setLoading(false); }
  }, [filterPeriod]);

  useEffect(() => { loadInvoices(); }, [loadInvoices]);

  const runBilling = async () => {
    if (!period) { setError('Enter a period (YYYY-MM).'); return; }
    setRunning(true); setError(''); setSuccess(''); setRunResult(null);
    try {
      const res = await billingApi.run(period);
      setRunResult({ generated: res.generated, invoices: res.invoices });
      setSuccess(`Billing run complete: ${res.generated} invoice(s) generated.`);
      loadInvoices();
    } catch (e: any) { setError(e.message); }
    finally { setRunning(false); }
  };

  const downloadPdf = (id: string) => {
    window.open(billingApi.pdfUrl(id), '_blank');
  };

  return (
    <div>
      <div className="page-header">
        <h2>Billing &amp; Invoices</h2>
      </div>

      {/* Run billing */}
      <div className="import-panel">
        <h4>Run Billing</h4>
        <div className="import-row">
          <input
            type="month"
            value={period}
            onChange={e => setPeriod(e.target.value)}
            placeholder="YYYY-MM"
            style={{ padding: '.35rem .5rem', border: '1px solid #ccc', borderRadius: 4, fontSize: '.875rem' }}
          />
          <button className="btn-success" onClick={runBilling} disabled={running}>
            {running ? 'Running…' : '▶ Generate Invoices'}
          </button>
        </div>
        {runResult && (
          <div className="import-result">
            Generated: {runResult.generated} invoice(s)
            {runResult.invoices.length > 0 && <> — IDs: {runResult.invoices.join(', ')}</>}
          </div>
        )}
      </div>

      {error   && <div className="alert alert-error"   style={{ marginBottom: '.5rem' }}>{error}</div>}
      {success && <div className="alert alert-success" style={{ marginBottom: '.5rem' }}>{success}</div>}

      {/* Filter + list */}
      <div className="filters">
        <label>Filter period
          <input
            type="month"
            value={filterPeriod}
            onChange={e => setFilterPeriod(e.target.value)}
            style={{ padding: '.35rem .5rem', border: '1px solid #ccc', borderRadius: 4, fontSize: '.875rem' }}
          />
        </label>
        <button className="btn-secondary" onClick={loadInvoices}>Apply</button>
      </div>

      {loading ? <p>Loading…</p> : (
        <table className="data-table">
          <thead>
            <tr>
              <th>Invoice ID</th><th>Contract</th><th>Period</th>
              <th>kWh</th><th>Subtotal €</th><th>Tax €</th><th>Total €</th>
              <th>Generated</th><th>PDF</th>
            </tr>
          </thead>
          <tbody>
            {invoices.map(inv => (
              <tr key={inv.invoiceId}>
                <td>{inv.invoiceId}</td>
                <td>{inv.contract?.contractId}</td>
                <td>{inv.period}</td>
                <td>{Number(inv.totalKwh).toFixed(2)}</td>
                <td>{Number(inv.subtotal).toFixed(2)}</td>
                <td>{Number(inv.tax).toFixed(2)}</td>
                <td><strong>{Number(inv.total).toFixed(2)}</strong></td>
                <td style={{ fontSize: '.75rem' }}>{inv.generatedAt?.substring(0, 19).replace('T', ' ')}</td>
                <td>
                  <button className="btn-primary" onClick={() => downloadPdf(inv.invoiceId)}>
                    ⬇ PDF
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
