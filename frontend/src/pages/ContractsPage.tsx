import { useState, useEffect, useCallback } from 'react';
import {
  Alert, Box, Button, CircularProgress, Dialog, DialogActions, DialogContent,
  DialogTitle, FormControl, IconButton, InputLabel, LinearProgress, MenuItem,
  Select, Snackbar, TextField, Tooltip, Typography,
} from '@mui/material';
import { DataGrid, type GridColDef } from '@mui/x-data-grid';
import AddIcon    from '@mui/icons-material/Add';
import EditIcon   from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import UploadIcon from '@mui/icons-material/Upload';
import { contractsApi, metersApi, type Contract, type Meter, type ImportResult } from '../api/client';

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
  const [saving, setSaving]       = useState(false);
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
    } catch (e: unknown) { setError(e instanceof Error ? e.message : String(e)); }
    finally { setLoading(false); }
  }, [filterMeter]);

  useEffect(() => { load(); }, [load]);

  const openCreate = () => { setEditing(empty()); setIsNew(true); setError(''); };
  const openEdit   = (c: Contract) => {
    setEditing({ ...c, meter: { meterId: c.meter?.meterId ?? '' } });
    setIsNew(false); setError('');
  };
  const closeForm = () => { setEditing(null); setSaving(false); };

  const save = async () => {
    if (!editing) return;
    setSaving(true);
    try {
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
    } catch (e: unknown) { setError(e instanceof Error ? e.message : String(e)); }
    finally { setSaving(false); }
  };

  const remove = async (id: string) => {
    if (!confirm(`Delete contract ${id}?`)) return;
    try { await contractsApi.delete(id); setSuccess('Contract deleted.'); load(); }
    catch (e: unknown) { setError(e instanceof Error ? e.message : String(e)); }
  };

  const handleImport = async () => {
    if (!csvFile) return;
    try {
      const res = await contractsApi.importCsv(csvFile);
      setImportRes(res);
      setSuccess(`Imported: ${res.inserted} inserted, ${res.skipped} skipped.`);
      load();
    } catch (e: unknown) { setError(e instanceof Error ? e.message : String(e)); }
  };

  const set = (field: keyof Contract, val: unknown) =>
    setEditing(e => e ? { ...e, [field]: val } : e);

  const isFlat = editing?.contractType === 'FLAT';

  const columns: GridColDef<Contract>[] = [
    { field: 'contractId',    headerName: 'Contract ID', flex: 1.2 },
    { field: 'meterId',       headerName: 'Meter',       flex: 0.8, valueGetter: (_v, row) => row.meter?.meterId },
    { field: 'customerId',    headerName: 'Customer',    flex: 0.8 },
    { field: 'fullName',      headerName: 'Full Name',   flex: 1.2 },
    { field: 'contractType',  headerName: 'Type',        flex: 0.6 },
    { field: 'startDate',     headerName: 'Start',       flex: 0.8 },
    { field: 'endDate',       headerName: 'End',         flex: 0.8 },
    { field: 'taxRate',       headerName: 'Tax',         flex: 0.6 },
    {
      field: 'actions', headerName: 'Actions', sortable: false, filterable: false, width: 110,
      renderCell: ({ row }) => (
        <Box sx={{ display: 'flex', gap: 0.5 }}>
          <Tooltip title="Edit"><IconButton size="small" onClick={() => openEdit(row)} aria-label="Edit contract"><EditIcon fontSize="small" /></IconButton></Tooltip>
          <Tooltip title="Delete"><IconButton size="small" color="error" onClick={() => remove(row.contractId)} aria-label="Delete contract"><DeleteIcon fontSize="small" /></IconButton></Tooltip>
        </Box>
      ),
    },
  ];

  return (
    <Box>
      {/* Page header */}
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 2 }}>
        <Typography variant="h4">Contracts</Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={openCreate}>New Contract</Button>
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

      {/* Filter by meter */}
      <Box sx={{ display: 'flex', gap: 2, mb: 2, alignItems: 'center', flexWrap: 'wrap' }}>
        <FormControl size="small" sx={{ minWidth: 200 }}>
          <InputLabel>Filter by Meter</InputLabel>
          <Select label="Filter by Meter" value={filterMeter} onChange={e => setFilterMeter(e.target.value)}>
            <MenuItem value="">— all —</MenuItem>
            {meters.map(m => <MenuItem key={m.meterId} value={m.meterId}>{m.meterId}</MenuItem>)}
          </Select>
        </FormControl>
      </Box>

      {loading
        ? <LinearProgress />
        : (
          <DataGrid
            rows={contracts}
            columns={columns}
            getRowId={r => r.contractId}
            autoHeight
            pageSizeOptions={[10, 25, 50]}
            initialState={{ pagination: { paginationModel: { pageSize: 10 } } }}
            disableRowSelectionOnClick
            slots={{ noRowsOverlay: () => <Box sx={{ p: 2, textAlign: 'center' }}>No contracts found.</Box> }}
            sx={{ bgcolor: 'background.paper' }}
          />
        )}

      {/* Create / Edit dialog */}
      <Dialog open={!!editing} onClose={closeForm} fullWidth maxWidth="sm">
        <DialogTitle>{isNew ? 'Create Contract' : `Edit Contract – ${editing?.contractId}`}</DialogTitle>
        <DialogContent sx={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 2, pt: '12px !important' }}>
          <TextField label="Contract ID *" value={editing?.contractId ?? ''} disabled={!isNew}
            onChange={e => set('contractId', e.target.value)} size="small" />
          <FormControl size="small">
            <InputLabel>Meter ID *</InputLabel>
            <Select label="Meter ID *" value={editing?.meter.meterId ?? ''}
              onChange={e => set('meter', { meterId: e.target.value })}>
              <MenuItem value="">— select —</MenuItem>
              {meters.map(m => <MenuItem key={m.meterId} value={m.meterId}>{m.meterId}</MenuItem>)}
            </Select>
          </FormControl>
          <TextField label="Customer ID *" value={editing?.customerId ?? ''}
            onChange={e => set('customerId', e.target.value)} size="small" />
          <TextField label="Full Name *" value={editing?.fullName ?? ''}
            onChange={e => set('fullName', e.target.value)} size="small" />
          <TextField label="NIF" value={editing?.nif ?? ''}
            onChange={e => set('nif', e.target.value)} size="small" />
          <TextField label="Email" value={editing?.email ?? ''}
            onChange={e => set('email', e.target.value)} size="small" />
          <FormControl size="small">
            <InputLabel>Contract Type *</InputLabel>
            <Select label="Contract Type *" value={editing?.contractType ?? 'FIXED'}
              onChange={e => set('contractType', e.target.value)}>
              <MenuItem value="FIXED">FIXED</MenuItem>
              <MenuItem value="FLAT">FLAT</MenuItem>
            </Select>
          </FormControl>
          <FormControl size="small">
            <InputLabel>Billing Cycle</InputLabel>
            <Select label="Billing Cycle" value={editing?.billingCycle ?? 'MONTHLY'}
              onChange={e => set('billingCycle', e.target.value)}>
              <MenuItem value="MONTHLY">MONTHLY</MenuItem>
            </Select>
          </FormControl>
          <TextField label="Start Date *" type="date" value={editing?.startDate ?? ''}
            onChange={e => set('startDate', e.target.value)} size="small" InputLabelProps={{ shrink: true }} />
          <TextField label="End Date" type="date" value={editing?.endDate ?? ''}
            onChange={e => set('endDate', e.target.value)} size="small" InputLabelProps={{ shrink: true }} />
          <TextField label="Tax Rate * (e.g. 0.21)" type="number" inputProps={{ step: 0.01 }}
            value={editing?.taxRate ?? ''} onChange={e => set('taxRate', parseFloat(e.target.value))} size="small" />
          <TextField label="IBAN" value={editing?.iban ?? ''}
            onChange={e => set('iban', e.target.value)} size="small" />

          {/* FIXED fields */}
          {!isFlat && (
            <TextField label="Fixed Price/kWh € *" type="number" inputProps={{ step: 0.0001 }}
              value={editing?.fixedPricePerKwhEur ?? ''}
              onChange={e => set('fixedPricePerKwhEur', parseFloat(e.target.value))} size="small" />
          )}

          {/* FLAT fields */}
          {isFlat && (<>
            <TextField label="Monthly Fee € *" type="number" inputProps={{ step: 0.01 }}
              value={editing?.flatMonthlyFeeEur ?? ''}
              onChange={e => set('flatMonthlyFeeEur', parseFloat(e.target.value))} size="small" />
            <TextField label="Included kWh *" type="number" inputProps={{ step: 1 }}
              value={editing?.includedKwh ?? ''}
              onChange={e => set('includedKwh', parseFloat(e.target.value))} size="small" />
            <TextField label="Overage Price/kWh € *" type="number" inputProps={{ step: 0.0001 }}
              value={editing?.overagePricePerKwhEur ?? ''}
              onChange={e => set('overagePricePerKwhEur', parseFloat(e.target.value))} size="small" />
          </>)}
        </DialogContent>
        <DialogActions>
          <Button onClick={closeForm} disabled={saving}>Cancel</Button>
          <Button variant="contained" onClick={save} disabled={saving}
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

