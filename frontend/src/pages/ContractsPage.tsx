import { useState, useEffect, useCallback } from 'react';
import { contractsApi, metersApi, Contract, Meter, ImportResult } from '../api/client';

const empty = (): Contract => ({
  contractId: '', meter: { meterId: '' }, customerId: '', fullName: '', nif: '', email: '',
  contractType: 'FIXED', startDate: '', endDate: '', billingCycle: 'MONTHLY',
  flatMonthlyFeeEur: undefined, includedKwh: undefined, overagePricePerKwhEur: undefined,
  fixedPricePerKwhEur: undefined, taxRate: 0.21, iban: '',
});

export default function ContractsPage() {
  const [contracts, setContracts] = useState<Contract[]>([]);
  const [meters, setMeters]       = useState<Meter[]>([]);
  const [loading, setLoading]     = useState(false);
  const [error, setError]         = useState('');
  const [success, setSuccess]     = useState('');
  const [editing, setEditing]     = useState<Contract | null>(null);
  const [isNew, setIsNew]         = useState(false);
  const [filterMeter, setFilterMeter] = useState('');
  const [csvFile, setCsvFile]     = useState<File | null>(null);
  const [importRes, setImportRes] = useState<ImportResult | null>(null);

  const load = useCallback(async () => {
    setLoading(true); setError('');
    try {
      const [c, m] = await Promise.all([
        contractsApi.list(filterMeter || undefined),
        metersApi.list(),
      ]);
      setContracts(c); setMeters(m);
    } catch (e: any) { setError(e.message); }
    finally { setLoading(false); }
  }, [filterMeter]);

  useEffect(() => { load(); }, [load]);

  const openCreate = () => { setEditing(empty()); setIsNew(true); setSuccess(''); setError(''); };
  const openEdit   = (c: Contract) => { setEditing({ ...c, meter: { meterId: c.meter?.meterId ?? '' } }); setIsNew(false); setSuccess(''); setError(''); };
  const closeForm  = () => { setEditing(null); };

  const save = async () => {
    if (!editing) return;
    try {
      // strip empty optional fields per contractType
      const payload: Contract = { ...editing };
      if (payload.contractType === 'FIXED') {
        payload.flatMonthlyFeeEur = undefined;
        payload.includedKwh = undefined;
        payload.overagePricePerKwhEur = undefined;
      } else {
        payload.fixedPricePerKwhEur = undefined;
      }
      if (payload.endDate === '') payload.endDate = undefined;
      if (isNew) await contractsApi.create(payload);
      else       await contractsApi.update(editing.contractId, payload);
      setSuccess(isNew ? 'Contract created.' : 'Contract updated.');
      closeForm(); load();
    } catch (e: any) { setError(e.message); }
  };

  const remove = async (id: string) => {
    if (!confirm(`Delete contract ${id}?`)) return;
    try { await contractsApi.delete(id); setSuccess('Contract deleted.'); load(); }
    catch (e: any) { setError(e.message); }
  };

  const handleImport = async () => {
    if (!csvFile) return;
    try {
      const res = await contractsApi.importCsv(csvFile);
      setImportRes(res); setSuccess(`Imported: ${res.inserted} inserted, ${res.skipped} skipped.`);
      load();
    } catch (e: any) { setError(e.message); }
  };

  const set = (field: keyof Contract, val: any) =>
    setEditing(e => e ? { ...e, [field]: val } : e);

  const isFlat = editing?.contractType === 'FLAT';

  return (
    <div>
      <div className="page-header">
        <h2>Contracts</h2>
        <button className="btn-primary" onClick={openCreate}>+ New Contract</button>
      </div>

      {error   && <div className="alert alert-error"   style={{ marginBottom: '.5rem' }}>{error}</div>}
      {success && <div className="alert alert-success" style={{ marginBottom: '.5rem' }}>{success}</div>}

      {/* CSV Import */}
      <div className="import-panel">
        <h4>CSV Import</h4>
        <div className="import-row">
          <input type="file" accept=".csv" onChange={e => setCsvFile(e.target.files?.[0] ?? null)} />
          <button className="btn-secondary" onClick={handleImport} disabled={!csvFile}>Import</button>
        </div>
        {importRes && (
          <div className="import-result">
            inserted={importRes.inserted} skipped={importRes.skipped}
            {importRes.errors.length > 0 && <> | errors: {importRes.errors.join(', ')}</>}
          </div>
        )}
      </div>

      {/* Filters */}
      <div className="filters">
        <label>Filter by Meter ID
          <select value={filterMeter} onChange={e => setFilterMeter(e.target.value)}>
            <option value="">— all —</option>
            {meters.map(m => <option key={m.meterId} value={m.meterId}>{m.meterId}</option>)}
          </select>
        </label>
      </div>

      {loading ? <p>Loading…</p> : (
        <table className="data-table">
          <thead>
            <tr>
              <th>Contract ID</th><th>Meter</th><th>Customer</th><th>Full Name</th>
              <th>Type</th><th>Start</th><th>End</th><th>Tax</th><th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {contracts.map(c => (
              <tr key={c.contractId}>
                <td>{c.contractId}</td>
                <td>{c.meter?.meterId}</td>
                <td>{c.customerId}</td>
                <td>{c.fullName}</td>
                <td>{c.contractType}</td>
                <td>{c.startDate}</td>
                <td>{c.endDate}</td>
                <td>{c.taxRate}</td>
                <td>
                  <div className="actions">
                    <button className="btn-secondary" onClick={() => openEdit(c)}>Edit</button>
                    <button className="btn-danger"    onClick={() => remove(c.contractId)}>Delete</button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {editing && (
        <div className="form-overlay">
          <div className="form-card">
            <h3>{isNew ? 'Create Contract' : `Edit Contract ${editing.contractId}`}</h3>
            <div className="form-grid">
              <label>Contract ID *
                <input value={editing.contractId} disabled={!isNew}
                  onChange={e => set('contractId', e.target.value)} />
              </label>
              <label>Meter ID *
                <select value={editing.meter.meterId} onChange={e => set('meter', { meterId: e.target.value })}>
                  <option value="">— select —</option>
                  {meters.map(m => <option key={m.meterId} value={m.meterId}>{m.meterId}</option>)}
                </select>
              </label>
              <label>Customer ID *
                <input value={editing.customerId} onChange={e => set('customerId', e.target.value)} />
              </label>
              <label>Full Name *
                <input value={editing.fullName} onChange={e => set('fullName', e.target.value)} />
              </label>
              <label>NIF
                <input value={editing.nif} onChange={e => set('nif', e.target.value)} />
              </label>
              <label>Email
                <input value={editing.email} onChange={e => set('email', e.target.value)} />
              </label>
              <label>Contract Type *
                <select value={editing.contractType} onChange={e => set('contractType', e.target.value)}>
                  <option value="FIXED">FIXED</option>
                  <option value="FLAT">FLAT</option>
                </select>
              </label>
              <label>Billing Cycle
                <select value={editing.billingCycle} onChange={e => set('billingCycle', e.target.value)}>
                  <option value="MONTHLY">MONTHLY</option>
                </select>
              </label>
              <label>Start Date *
                <input type="date" value={editing.startDate} onChange={e => set('startDate', e.target.value)} />
              </label>
              <label>End Date
                <input type="date" value={editing.endDate ?? ''} onChange={e => set('endDate', e.target.value)} />
              </label>
              <label>Tax Rate * (e.g. 0.21)
                <input type="number" step="0.01" value={editing.taxRate} onChange={e => set('taxRate', parseFloat(e.target.value))} />
              </label>
              <label>IBAN
                <input value={editing.iban ?? ''} onChange={e => set('iban', e.target.value)} />
              </label>

              {/* FIXED fields */}
              {!isFlat && (
                <label>Fixed Price/kWh € *
                  <input type="number" step="0.0001" value={editing.fixedPricePerKwhEur ?? ''}
                    onChange={e => set('fixedPricePerKwhEur', parseFloat(e.target.value))} />
                </label>
              )}

              {/* FLAT fields */}
              {isFlat && (<>
                <label>Monthly Fee € *
                  <input type="number" step="0.01" value={editing.flatMonthlyFeeEur ?? ''}
                    onChange={e => set('flatMonthlyFeeEur', parseFloat(e.target.value))} />
                </label>
                <label>Included kWh *
                  <input type="number" step="1" value={editing.includedKwh ?? ''}
                    onChange={e => set('includedKwh', parseFloat(e.target.value))} />
                </label>
                <label>Overage Price/kWh € *
                  <input type="number" step="0.0001" value={editing.overagePricePerKwhEur ?? ''}
                    onChange={e => set('overagePricePerKwhEur', parseFloat(e.target.value))} />
                </label>
              </>)}
            </div>
            <div className="form-actions">
              <button className="btn-secondary" onClick={closeForm}>Cancel</button>
              <button className="btn-primary"   onClick={save}>Save</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
