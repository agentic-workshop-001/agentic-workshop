import { useState, useEffect, useCallback } from 'react';
import {
  Alert, Box, Button, CircularProgress, Dialog, DialogActions, DialogContent,
  DialogTitle, IconButton, LinearProgress, Snackbar, TextField, Tooltip,
  Typography,
} from '@mui/material';
import { DataGrid, type GridColDef } from '@mui/x-data-grid';
import AddIcon    from '@mui/icons-material/Add';
import EditIcon   from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import UploadIcon from '@mui/icons-material/Upload';
import { metersApi, type Meter, type ImportResult } from '../api/client';

const empty = (): Meter => ({ meterId: '', cups: '', address: '', postalCode: '', city: '' });

export default function MetersPage() {
  const [meters, setMeters]       = useState<Meter[]>([]);
  const [loading, setLoading]     = useState(false);
  const [error, setError]         = useState('');
  const [success, setSuccess]     = useState('');
  const [editing, setEditing]     = useState<Meter | null>(null);
  const [isNew, setIsNew]         = useState(false);
  const [saving, setSaving]       = useState(false);
  const [csvFile, setCsvFile]     = useState<File | null>(null);
  const [importRes, setImportRes] = useState<ImportResult | null>(null);

  const load = useCallback(async () => {
    setLoading(true); setError('');
    try { setMeters(await metersApi.list()); }
    catch (e: unknown) { setError(e instanceof Error ? e.message : String(e)); }
    finally { setLoading(false); }
  }, []);

  useEffect(() => { load(); }, [load]);

  const openCreate = () => { setEditing(empty()); setIsNew(true); setError(''); };
  const openEdit   = (m: Meter) => { setEditing({ ...m }); setIsNew(false); setError(''); };
  const closeForm  = () => { setEditing(null); setSaving(false); };

  const save = async () => {
    if (!editing) return;
    setSaving(true);
    try {
      if (isNew) await metersApi.create(editing);
      else       await metersApi.update(editing.meterId, editing);
      setSuccess(isNew ? 'Meter created.' : 'Meter updated.');
      closeForm(); load();
    } catch (e: unknown) { setError(e instanceof Error ? e.message : String(e)); }
    finally { setSaving(false); }
  };

  const remove = async (id: string) => {
    if (!confirm(`Delete meter ${id}?`)) return;
    try { await metersApi.delete(id); setSuccess('Meter deleted.'); load(); }
    catch (e: unknown) { setError(e instanceof Error ? e.message : String(e)); }
  };

  const handleImport = async () => {
    if (!csvFile) return;
    try {
      const res = await metersApi.importCsv(csvFile);
      setImportRes(res);
      setSuccess(`Imported: ${res.inserted} inserted, ${res.skipped} skipped.`);
      load();
    } catch (e: unknown) { setError(e instanceof Error ? e.message : String(e)); }
  };

  const set = (field: keyof Meter, val: string) =>
    setEditing(e => e ? { ...e, [field]: val } : e);

  const columns: GridColDef<Meter>[] = [
    { field: 'meterId',    headerName: 'Meter ID',    flex: 1 },
    { field: 'cups',       headerName: 'CUPS',        flex: 1 },
    { field: 'address',    headerName: 'Address',     flex: 2 },
    { field: 'postalCode', headerName: 'Postal Code', flex: 1 },
    { field: 'city',       headerName: 'City',        flex: 1 },
    {
      field: 'actions', headerName: 'Actions', sortable: false, filterable: false, width: 110,
      renderCell: ({ row }) => (
        <Box sx={{ display: 'flex', gap: 0.5 }}>
          <Tooltip title="Edit"><IconButton size="small" onClick={() => openEdit(row)} aria-label="Edit meter"><EditIcon fontSize="small" /></IconButton></Tooltip>
          <Tooltip title="Delete"><IconButton size="small" color="error" onClick={() => remove(row.meterId)} aria-label="Delete meter"><DeleteIcon fontSize="small" /></IconButton></Tooltip>
        </Box>
      ),
    },
  ];

  return (
    <Box>
      {/* Page header */}
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 2 }}>
        <Typography variant="h4">Meters</Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={openCreate}>New Meter</Button>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>{error}</Alert>}

      {/* CSV Import panel */}
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

      {/* Data table */}
      {loading
        ? <LinearProgress />
        : (
          <DataGrid
            rows={meters}
            columns={columns}
            getRowId={r => r.meterId}
            autoHeight
            pageSizeOptions={[10, 25, 50]}
            initialState={{ pagination: { paginationModel: { pageSize: 10 } } }}
            disableRowSelectionOnClick
            slots={{ noRowsOverlay: () => <Box sx={{ p: 2, textAlign: 'center' }}>No meters found.</Box> }}
            sx={{ bgcolor: 'background.paper' }}
          />
        )}

      {/* Create / Edit dialog */}
      <Dialog open={!!editing} onClose={closeForm} fullWidth maxWidth="sm">
        <DialogTitle>{isNew ? 'Create Meter' : `Edit Meter – ${editing?.meterId}`}</DialogTitle>
        <DialogContent sx={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 2, pt: '12px !important' }}>
          <TextField label="Meter ID *" value={editing?.meterId ?? ''} disabled={!isNew}
            onChange={e => set('meterId', e.target.value)} size="small" />
          <TextField label="CUPS" value={editing?.cups ?? ''}
            onChange={e => set('cups', e.target.value)} size="small" />
          <TextField label="Address *" value={editing?.address ?? ''}
            onChange={e => set('address', e.target.value)} size="small" sx={{ gridColumn: '1/-1' }} />
          <TextField label="Postal Code" value={editing?.postalCode ?? ''}
            onChange={e => set('postalCode', e.target.value)} size="small" />
          <TextField label="City *" value={editing?.city ?? ''}
            onChange={e => set('city', e.target.value)} size="small" />
        </DialogContent>
        <DialogActions>
          <Button onClick={closeForm} disabled={saving}>Cancel</Button>
          <Button variant="contained" onClick={save} disabled={saving}
            startIcon={saving ? <CircularProgress size={16} /> : undefined}>
            Save
          </Button>
        </DialogActions>
      </Dialog>

      {/* Success snackbar */}
      <Snackbar open={!!success} autoHideDuration={3000} onClose={() => setSuccess('')}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}>
        <Alert severity="success" onClose={() => setSuccess('')}>{success}</Alert>
      </Snackbar>
    </Box>
  );
}

