import { useState, useEffect, useCallback } from 'react';
import {
  Alert, Box, Button, CircularProgress, FormControl, IconButton,
  InputLabel, LinearProgress, MenuItem, Select, Snackbar,
  TextField, Tooltip, Typography,
} from '@mui/material';
import { DataGrid, type GridColDef } from '@mui/x-data-grid';
import PlayArrowIcon   from '@mui/icons-material/PlayArrow';
import PictureAsPdfIcon from '@mui/icons-material/PictureAsPdf';
import { billingApi, type Invoice } from '../api/client';

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
    catch (e: unknown) { setError(e instanceof Error ? e.message : String(e)); }
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
    } catch (e: unknown) { setError(e instanceof Error ? e.message : String(e)); }
    finally { setRunning(false); }
  };

  const downloadPdf = (id: string) => { window.open(billingApi.pdfUrl(id), '_blank'); };

  const columns: GridColDef<Invoice>[] = [
    { field: 'invoiceId',  headerName: 'Invoice ID',  flex: 1.2 },
    { field: 'contractId', headerName: 'Contract',    flex: 1, valueGetter: (_v, row) => row.contract?.contractId },
    { field: 'period',     headerName: 'Period',      flex: 0.7 },
    { field: 'totalKwh',   headerName: 'kWh',         flex: 0.6, valueFormatter: (v) => Number(v).toFixed(2) },
    { field: 'subtotal',   headerName: 'Subtotal €',  flex: 0.7, valueFormatter: (v) => Number(v).toFixed(2) },
    { field: 'tax',        headerName: 'Tax €',       flex: 0.6, valueFormatter: (v) => Number(v).toFixed(2) },
    { field: 'total',      headerName: 'Total €',     flex: 0.7, valueFormatter: (v) => Number(v).toFixed(2) },
    {
      field: 'generatedAt', headerName: 'Generated', flex: 1,
      valueFormatter: (v: string) => v?.substring(0, 19).replace('T', ' '),
    },
    {
      field: 'actions', headerName: 'PDF', sortable: false, filterable: false, width: 70,
      renderCell: ({ row }) => (
        <Tooltip title="Download PDF">
          <IconButton size="small" color="primary" onClick={() => downloadPdf(row.invoiceId)} aria-label="Download PDF">
            <PictureAsPdfIcon fontSize="small" />
          </IconButton>
        </Tooltip>
      ),
    },
  ];

  return (
    <Box>
      {/* Page header */}
      <Box sx={{ mb: 2 }}>
        <Typography variant="h4">Billing &amp; Invoices</Typography>
      </Box>

      {/* Run billing panel */}
      <Box sx={{ bgcolor: 'background.paper', borderRadius: 1, p: 2, mb: 2, boxShadow: 1 }}>
        <Typography variant="subtitle2" gutterBottom>Run Billing</Typography>
        <Box sx={{ display: 'flex', gap: 2, alignItems: 'center', flexWrap: 'wrap' }}>
          <TextField
            label="Period (YYYY-MM)"
            type="month"
            value={period}
            onChange={e => setPeriod(e.target.value)}
            size="small"
            InputLabelProps={{ shrink: true }}
            sx={{ width: 200 }}
          />
          <Button
            variant="contained"
            color="secondary"
            startIcon={running ? <CircularProgress size={16} sx={{ color: 'inherit' }} /> : <PlayArrowIcon />}
            onClick={runBilling}
            disabled={running}
          >
            {running ? 'Running…' : 'Generate Invoices'}
          </Button>
        </Box>
        {runResult && (
          <Typography variant="caption" sx={{ mt: 0.5, display: 'block' }}>
            Generated: {runResult.generated} invoice(s)
            {runResult.invoices.length > 0 && ` — IDs: ${runResult.invoices.join(', ')}`}
          </Typography>
        )}
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>{error}</Alert>}

      {/* Filter + list */}
      <Box sx={{ display: 'flex', gap: 2, mb: 2, alignItems: 'center', flexWrap: 'wrap' }}>
        <FormControl size="small" sx={{ minWidth: 200 }}>
          <InputLabel shrink>Filter Period</InputLabel>
          <Select
            label="Filter Period"
            displayEmpty
            value={filterPeriod}
            onChange={e => setFilterPeriod(e.target.value)}
            renderValue={v => v || '— all periods —'}
          >
            <MenuItem value="">— all periods —</MenuItem>
            {[...new Set(invoices.map(i => i.period))].sort().map(p => (
              <MenuItem key={p} value={p}>{p}</MenuItem>
            ))}
          </Select>
        </FormControl>
        <Button variant="outlined" size="small" onClick={loadInvoices}>Apply</Button>
      </Box>

      {loading
        ? <LinearProgress />
        : (
          <DataGrid
            rows={invoices}
            columns={columns}
            getRowId={r => r.invoiceId}
            autoHeight
            pageSizeOptions={[10, 25, 50]}
            initialState={{ pagination: { paginationModel: { pageSize: 10 } } }}
            disableRowSelectionOnClick
            slots={{ noRowsOverlay: () => <Box sx={{ p: 2, textAlign: 'center' }}>No invoices found.</Box> }}
            sx={{ bgcolor: 'background.paper' }}
          />
        )}

      <Snackbar open={!!success} autoHideDuration={3000} onClose={() => setSuccess('')}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}>
        <Alert severity="success" onClose={() => setSuccess('')}>{success}</Alert>
      </Snackbar>
    </Box>
  );
}

