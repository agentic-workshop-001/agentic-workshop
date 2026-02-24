import { useState, useEffect, useCallback } from 'react';
import { readingsApi, metersApi, Reading, Meter, ImportResult } from '../api/client';

const emptyReading = (): Reading => ({
  id: { meterId: '', date: '', hour: 0 },
  kwh: 0,
  quality: 'REAL',
});

export default function ReadingsPage() {
  const [readings, setReadings]   = useState<Reading[]>([]);
  const [meters, setMeters]       = useState<Meter[]>([]);
  const [loading, setLoading]     = useState(false);
  const [error, setError]         = useState('');
  const [success, setSuccess]     = useState('');
  const [creating, setCreating]   = useState(false);
  const [form, setForm]           = useState<Reading>(emptyReading());

  const [filterMeter, setFilterMeter] = useState('');
  const [filterFrom, setFilterFrom]   = useState('');
  const [filterTo, setFilterTo]       = useState('');

  const [csvFile, setCsvFile]     = useState<File | null>(null);
  const [importRes, setImportRes] = useState<ImportResult | null>(null);

  const load = useCallback(async () => {
    setLoading(true); setError('');
    try {
      const [r, m] = await Promise.all([
        readingsApi.list({
          meterId: filterMeter || undefined,
          from: filterFrom || undefined,
          to: filterTo || undefined,
        }),
        metersApi.list(),
      ]);
      setReadings(r); setMeters(m);
    } catch (e: any) { setError(e.message); }
    finally { setLoading(false); }
  }, [filterMeter, filterFrom, filterTo]);

  useEffect(() => { load(); }, [load]);

  const remove = async (meterId: string, date: string, hour: number) => {
    if (!confirm(`Delete reading ${meterId}/${date}/${hour}?`)) return;
    try { await readingsApi.delete(meterId, date, hour); setSuccess('Reading deleted.'); load(); }
    catch (e: any) { setError(e.message); }
  };

  const openCreate = () => { setForm(emptyReading()); setCreating(true); setSuccess(''); setError(''); };

  const handleCreate = async () => {
    try {
      await readingsApi.create(form);
      setSuccess('Reading created.'); setCreating(false); load();
    } catch (e: any) { setError(e.message); }
  };

  const handleImport = async () => {
    if (!csvFile) return;
    try {
      const res = await readingsApi.importCsv(csvFile);
      setImportRes(res); setSuccess(`Imported: ${res.inserted} inserted, ${res.skipped} skipped.`);
      load();
    } catch (e: any) { setError(e.message); }
  };

  const setId = (field: keyof Reading['id'], val: any) =>
    setForm(f => ({ ...f, id: { ...f.id, [field]: val } }));

  return (
    <div>
      <div className="page-header">
        <h2>Readings</h2>
        <button className="btn-primary" onClick={openCreate}>+ New Reading</button>
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
        <label>Meter ID
          <select value={filterMeter} onChange={e => setFilterMeter(e.target.value)}>
            <option value="">— all —</option>
            {meters.map(m => <option key={m.meterId} value={m.meterId}>{m.meterId}</option>)}
          </select>
        </label>
        <label>From
          <input type="date" value={filterFrom} onChange={e => setFilterFrom(e.target.value)} />
        </label>
        <label>To
          <input type="date" value={filterTo} onChange={e => setFilterTo(e.target.value)} />
        </label>
        <button className="btn-secondary" onClick={load}>Apply</button>
      </div>

      {loading ? <p>Loading…</p> : (
        <table className="data-table">
          <thead>
            <tr>
              <th>Meter ID</th><th>Date</th><th>Hour</th><th>kWh</th><th>Quality</th><th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {readings.map(r => (
              <tr key={`${r.id.meterId}-${r.id.date}-${r.id.hour}`}>
                <td>{r.id.meterId}</td>
                <td>{r.id.date}</td>
                <td>{r.id.hour}</td>
                <td>{r.kwh}</td>
                <td>{r.quality}</td>
                <td>
                  <button className="btn-danger" onClick={() => remove(r.id.meterId, r.id.date, r.id.hour)}>
                    Delete
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {creating && (
        <div className="form-overlay">
          <div className="form-card">
            <h3>New Reading</h3>
            <div className="form-grid">
              <label>Meter ID *
                <select value={form.id.meterId} onChange={e => setId('meterId', e.target.value)}>
                  <option value="">— select —</option>
                  {meters.map(m => <option key={m.meterId} value={m.meterId}>{m.meterId}</option>)}
                </select>
              </label>
              <label>Date *
                <input type="date" value={form.id.date} onChange={e => setId('date', e.target.value)} />
              </label>
              <label>Hour (0-23) *
                <input type="number" min={0} max={23} value={form.id.hour}
                  onChange={e => setId('hour', parseInt(e.target.value, 10))} />
              </label>
              <label>kWh *
                <input type="number" step="0.001" min={0} value={form.kwh}
                  onChange={e => setForm(f => ({ ...f, kwh: parseFloat(e.target.value) }))} />
              </label>
              <label>Quality
                <select value={form.quality ?? ''} onChange={e => setForm(f => ({ ...f, quality: (e.target.value as any) || undefined }))}>
                  <option value="">— none —</option>
                  <option value="REAL">REAL</option>
                  <option value="ESTIMATED">ESTIMATED</option>
                </select>
              </label>
            </div>
            <div className="form-actions">
              <button className="btn-secondary" onClick={() => setCreating(false)}>Cancel</button>
              <button className="btn-primary"   onClick={handleCreate}>Save</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
