import { useState, useEffect, useCallback } from 'react';
import {
  Alert, Box, Button, CircularProgress, Dialog, DialogActions, DialogContent,
  DialogTitle, FormControl, IconButton, InputLabel, LinearProgress, MenuItem,
  Select, Snackbar, TextField, Tooltip, Typography,
} from '@mui/material';
import { DataGrid, type GridColDef } from '@mui/x-data-grid';
import AddIcon    from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import UploadIcon from '@mui/icons-material/Upload';
import { readingsApi, metersApi, type Reading, type Meter, type ImportResult } from '../api/client';

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
  const [saving, setSaving]       = useState(false);
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
    } catch (e: unknown) { setError(e instanceof Error ? e.message : String(e)); }
    finally { setLoading(false); }
  }, [filterMeter, filterFrom, filterTo]);

  useEffect(() => { load(); }, [load]);

  const remove = async (meterId: string, date: string, hour: number) => {
    if (!confirm(`Delete reading ${meterId}/${date}/${hour}?`)) return;
    try { await readingsApi.delete(meterId, date, hour); setSuccess('Reading deleted.'); load(); }
    catch (e: unknown) { setError(e instanceof Error ? e.message : String(e)); }
  };

  const openCreate = () => { setForm(emptyReading()); setCreating(true); setError(''); };

  const handleCreate = async () => {
    setSaving(true);
    try {
      await readingsApi.create(form);
      setSuccess('Reading created.'); setCreating(false); load();
    } catch (e: unknown) { setError(e instanceof Error ? e.message : String(e)); }
    finally { setSaving(false); }
  };

  const handleImport = async () => {
    if (!csvFile) return;
    try {
      const res = await readingsApi.importCsv(csvFile);
      setImportRes(res);
      setSuccess(`Imported: ${res.inserted} inserted, ${res.skipped} skipped.`);
      load();
    } catch (e: unknown) { setError(e instanceof Error ? e.message : String(e)); }
  };

  const setId = (field: keyof Reading['id'], val: string | number) =>
    setForm(f => ({ ...f, id: { ...f.id, [field]: val } }));

  const columns: GridColDef<Reading>[] = [
    { field: 'meterId', headerName: 'Meter ID', flex: 1,   valueGetter: (_v, row) => row.id.meterId },
    { field: 'date',    headerName: 'Date',     flex: 0.8, valueGetter: (_v, row) => row.id.date },
    { field: 'hour',    headerName: 'Hour',     flex: 0.5, valueGetter: (_v, row) => row.id.hour },
    { field: 'kwh',     headerName: 'kWh',      flex: 0.6 },
    { field: 'quality', headerName: 'Quality',  flex: 0.7 },
    {
      field: 'actions', headerName: 'Actions', sortable: false, filterable: false, width: 80,
      renderCell: ({ row }) => (
        <Tooltip title="Delete">
          <IconButton size="small" color="error"
            onClick={() => remove(row.id.meterId, row.id.date, row.id.hour)}
            aria-label="Delete reading">
            <DeleteIcon fontSize="small" />
          </IconButton>
        </Tooltip>
      ),
    },
  ];

  return (
    <Box>
      {/* Page header */}
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 2 }}>
        <Typography variant="h4">Readings</Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={openCreate}>New Reading</Button>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>{error}</Alert>}

      {/* CSV Import */}
      <Box sx={{ bgcolor: 'background.paper', borderRadius: 1, p: 2, mb: 2, boxShadow: 1 }}>
        <Typography variant="subtitle2" gutterBottom>CSV Import</Typography>
        <Box sx={{ display: 'flex', gap: 1, alignItems: 'center', flexWrap: 'wrap' }}>
          <Button variant="outlined" component="label" size="small" startIcon={<UploadIcon />}>
            {csvFile ? csvFile.name : 'Choose CSV'}
            <input type="file" accept=".csv" hidden onChange={e => setCsvFile(e.target.files?.[0] ?? null)} />
          </Button>
          <Button variant="outlined" size="small" onClick={handleImport} disabled={!csvFile}>Import</Button>
        </Box>
        {importRes && (
          <Typography variant="caption" sx={{ mt: 0.5, display: 'block' }}>
            inserted={importRes.inserted} skipped={importRes.skipped}
            {importRes.errors.length > 0 && ` | errors: ${importRes.errors.join(', ')}`}
          </Typography>
        )}
      </Box>

      {/* Filters */}
      <Box sx={{ display: 'flex', gap: 2, mb: 2, alignItems: 'center', flexWrap: 'wrap' }}>
        <FormControl size="small" sx={{ minWidth: 180 }}>
          <InputLabel>Meter ID</InputLabel>
          <Select label="Meter ID" value={filterMeter} onChange={e => setFilterMeter(e.target.value)}>
            <MenuItem value="">— all —</MenuItem>
            {meters.map(m => <MenuItem key={m.meterId} value={m.meterId}>{m.meterId}</MenuItem>)}
          </Select>
        </FormControl>
        <TextField label="From" type="date" size="small" value={filterFrom}
          onChange={e => setFilterFrom(e.target.value)} InputLabelProps={{ shrink: true }} />
        <TextField label="To" type="date" size="small" value={filterTo}
          onChange={e => setFilterTo(e.target.value)} InputLabelProps={{ shrink: true }} />
        <Button variant="outlined" size="small" onClick={load}>Apply</Button>
      </Box>

      {loading
        ? <LinearProgress />
        : (
          <DataGrid
            rows={readings}
            columns={columns}
            getRowId={r => `${r.id.meterId}-${r.id.date}-${r.id.hour}`}
            autoHeight
            pageSizeOptions={[10, 25, 50]}
            initialState={{ pagination: { paginationModel: { pageSize: 10 } } }}
            disableRowSelectionOnClick
            slots={{ noRowsOverlay: () => <Box sx={{ p: 2, textAlign: 'center' }}>No readings found.</Box> }}
            sx={{ bgcolor: 'background.paper' }}
          />
        )}

      {/* Create dialog */}
      <Dialog open={creating} onClose={() => setCreating(false)} fullWidth maxWidth="sm">
        <DialogTitle>New Reading</DialogTitle>
        <DialogContent sx={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 2, pt: '12px !important' }}>
          <FormControl size="small">
            <InputLabel>Meter ID *</InputLabel>
            <Select label="Meter ID *" value={form.id.meterId} onChange={e => setId('meterId', e.target.value)}>
              <MenuItem value="">— select —</MenuItem>
              {meters.map(m => <MenuItem key={m.meterId} value={m.meterId}>{m.meterId}</MenuItem>)}
            </Select>
          </FormControl>
          <TextField label="Date *" type="date" value={form.id.date}
            onChange={e => setId('date', e.target.value)} size="small" InputLabelProps={{ shrink: true }} />
          <TextField label="Hour (0–23) *" type="number" inputProps={{ min: 0, max: 23 }}
            value={form.id.hour} onChange={e => setId('hour', parseInt(e.target.value, 10))} size="small" />
          <TextField label="kWh *" type="number" inputProps={{ step: 0.001, min: 0 }}
            value={form.kwh} onChange={e => setForm(f => ({ ...f, kwh: parseFloat(e.target.value) }))} size="small" />
          <FormControl size="small">
            <InputLabel>Quality</InputLabel>
            <Select label="Quality" value={form.quality ?? ''}
              onChange={e => setForm(f => ({ ...f, quality: (e.target.value as Reading['quality']) || undefined }))}>
              <MenuItem value="">— none —</MenuItem>
              <MenuItem value="REAL">REAL</MenuItem>
              <MenuItem value="ESTIMATED">ESTIMATED</MenuItem>
            </Select>
          </FormControl>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCreating(false)} disabled={saving}>Cancel</Button>
          <Button variant="contained" onClick={handleCreate} disabled={saving}
            startIcon={saving ? <CircularProgress size={16} /> : undefined}>
            Save
          </Button>
        </DialogActions>
      </Dialog>

      <Snackbar open={!!success} autoHideDuration={3000} onClose={() => setSuccess('')}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}>
        <Alert severity="success" onClose={() => setSuccess('')}>{success}</Alert>
      </Snackbar>
    </Box>
  );
}

