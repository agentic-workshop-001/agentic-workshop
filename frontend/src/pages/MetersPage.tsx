import { useState, useEffect, useCallback } from 'react';
import { metersApi, Meter, ImportResult } from '../api/client';

const empty = (): Meter => ({ meterId: '', cups: '', address: '', postalCode: '', city: '' });

export default function MetersPage() {
  const [meters, setMeters]       = useState<Meter[]>([]);
  const [loading, setLoading]     = useState(false);
  const [error, setError]         = useState('');
  const [success, setSuccess]     = useState('');
  const [editing, setEditing]     = useState<Meter | null>(null);
  const [isNew, setIsNew]         = useState(false);
  const [csvFile, setCsvFile]     = useState<File | null>(null);
  const [importRes, setImportRes] = useState<ImportResult | null>(null);

  const load = useCallback(async () => {
    setLoading(true); setError('');
    try { setMeters(await metersApi.list()); }
    catch (e: any) { setError(e.message); }
    finally { setLoading(false); }
  }, []);

  useEffect(() => { load(); }, [load]);

  const openCreate = () => { setEditing(empty()); setIsNew(true); setSuccess(''); setError(''); };
  const openEdit   = (m: Meter) => { setEditing({ ...m }); setIsNew(false); setSuccess(''); setError(''); };
  const closeForm  = () => { setEditing(null); };

  const save = async () => {
    if (!editing) return;
    try {
      if (isNew) await metersApi.create(editing);
      else       await metersApi.update(editing.meterId, editing);
      setSuccess(isNew ? 'Meter created.' : 'Meter updated.');
      closeForm(); load();
    } catch (e: any) { setError(e.message); }
  };

  const remove = async (id: string) => {
    if (!confirm(`Delete meter ${id}?`)) return;
    try { await metersApi.delete(id); setSuccess('Meter deleted.'); load(); }
    catch (e: any) { setError(e.message); }
  };

  const handleImport = async () => {
    if (!csvFile) return;
    try {
      const res = await metersApi.importCsv(csvFile);
      setImportRes(res); setSuccess(`Imported: ${res.inserted} inserted, ${res.skipped} skipped.`);
      load();
    } catch (e: any) { setError(e.message); }
  };

  const set = (field: keyof Meter, val: string) =>
    setEditing(e => e ? { ...e, [field]: val } : e);

  return (
    <div>
      <div className="page-header">
        <h2>Meters</h2>
        <button className="btn-primary" onClick={openCreate}>+ New Meter</button>
      </div>

      {error   && <div className="alert alert-error"   style={{ marginBottom: '.5rem' }}>{error}</div>}
      {success && <div className="alert alert-success" style={{ marginBottom: '.5rem' }}>{success}</div>}

      {/* CSV Import */}
      <div className="import-panel">
        <h4>CSV Import</h4>
        <div className="import-row">
          <input type="file" accept=".csv" onChange={e => setCsvFile(e.target.files?.[0] ?? null)} />
          <button className="btn-secondary" onClick={handleImport} disabled={!csvFile}>Import</button>
          <a className="btn-secondary" style={{ textDecoration: 'none', padding: '.4rem .9rem' }}
            href={"data:text/csv;charset=utf-8," + encodeURIComponent("meterId,cups,address,postalCode,city\n")}
            download="meters-template.csv">Download template</a>
        </div>
        {importRes && (
          <div className="import-result">
            inserted={importRes.inserted} skipped={importRes.skipped}
            {importRes.errors.length > 0 && <> | errors: {importRes.errors.join(', ')}</>}
          </div>
        )}
      </div>

      {loading ? <p>Loading…</p> : (
        <table className="data-table">
          <thead>
            <tr>
              <th>Meter ID</th><th>CUPS</th><th>Address</th><th>Postal</th><th>City</th><th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {meters.map(m => (
              <tr key={m.meterId}>
                <td>{m.meterId}</td>
                <td>{m.cups}</td>
                <td>{m.address}</td>
                <td>{m.postalCode}</td>
                <td>{m.city}</td>
                <td>
                  <div className="actions">
                    <button className="btn-secondary" onClick={() => openEdit(m)}>Edit</button>
                    <button className="btn-danger"    onClick={() => remove(m.meterId)}>Delete</button>
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
            <h3>{isNew ? 'Create Meter' : `Edit Meter ${editing.meterId}`}</h3>
            <div className="form-grid">
              <label>Meter ID *
                <input value={editing.meterId} disabled={!isNew}
                  onChange={e => set('meterId', e.target.value)} />
              </label>
              <label>CUPS
                <input value={editing.cups ?? ''} onChange={e => set('cups', e.target.value)} />
              </label>
              <label className="full-width">Address *
                <input value={editing.address} onChange={e => set('address', e.target.value)} />
              </label>
              <label>Postal Code
                <input value={editing.postalCode} onChange={e => set('postalCode', e.target.value)} />
              </label>
              <label>City *
                <input value={editing.city} onChange={e => set('city', e.target.value)} />
              </label>
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
