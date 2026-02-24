// API client – all calls go through Vite proxy /api → http://localhost:8080

const BASE = '/api';

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    ...options,
  });
  if (!res.ok) {
    let msg = `HTTP ${res.status}`;
    try {
      const err = await res.json();
      msg = err.message || err.error || JSON.stringify(err);
    } catch { /* ignore */ }
    throw new Error(msg);
  }
  if (res.status === 204) return undefined as T;
  return res.json();
}

// ── Types ────────────────────────────────────────────────────────────────────

export interface Meter {
  meterId: string;
  cups?: string;
  address: string;
  postalCode: string;
  city: string;
}

export interface Contract {
  contractId: string;
  meter: { meterId: string };
  customerId: string;
  fullName: string;
  nif: string;
  email: string;
  contractType: 'FLAT' | 'FIXED';
  startDate: string;
  endDate?: string;
  billingCycle: 'MONTHLY';
  flatMonthlyFeeEur?: number;
  includedKwh?: number;
  overagePricePerKwhEur?: number;
  fixedPricePerKwhEur?: number;
  taxRate: number;
  iban?: string;
}

export interface Reading {
  id: { meterId: string; date: string; hour: number };
  kwh: number;
  quality?: 'REAL' | 'ESTIMATED';
}

export interface Invoice {
  invoiceId: string;
  contract: { contractId: string; fullName?: string };
  period: string;
  totalKwh: number;
  subtotal: number;
  tax: number;
  total: number;
  generatedAt: string;
}

export interface ImportResult {
  inserted: number;
  skipped: number;
  errors: string[];
}

// ── Meters ───────────────────────────────────────────────────────────────────

export const metersApi = {
  list: () => request<Meter[]>('/meters'),
  get: (id: string) => request<Meter>(`/meters/${id}`),
  create: (m: Meter) => request<Meter>('/meters', { method: 'POST', body: JSON.stringify(m) }),
  update: (id: string, m: Meter) => request<Meter>(`/meters/${id}`, { method: 'PUT', body: JSON.stringify(m) }),
  delete: (id: string) => request<void>(`/meters/${id}`, { method: 'DELETE' }),
  importCsv: (file: File) => {
    const fd = new FormData(); fd.append('file', file);
    return request<ImportResult>('/meters/import', { method: 'POST', headers: {}, body: fd });
  },
};

// ── Contracts ────────────────────────────────────────────────────────────────

export const contractsApi = {
  list: (meterId?: string) => request<Contract[]>(`/contracts${meterId ? `?meterId=${meterId}` : ''}`),
  get: (id: string) => request<Contract>(`/contracts/${id}`),
  create: (c: Contract) => request<Contract>('/contracts', { method: 'POST', body: JSON.stringify(c) }),
  update: (id: string, c: Contract) => request<Contract>(`/contracts/${id}`, { method: 'PUT', body: JSON.stringify(c) }),
  delete: (id: string) => request<void>(`/contracts/${id}`, { method: 'DELETE' }),
  importCsv: (file: File) => {
    const fd = new FormData(); fd.append('file', file);
    return request<ImportResult>('/contracts/import', { method: 'POST', headers: {}, body: fd });
  },
};

// ── Readings ─────────────────────────────────────────────────────────────────

export const readingsApi = {
  list: (params?: { meterId?: string; from?: string; to?: string }) => {
    const q = new URLSearchParams();
    if (params?.meterId) q.set('meterId', params.meterId);
    if (params?.from) q.set('from', params.from);
    if (params?.to) q.set('to', params.to);
    const qs = q.toString();
    return request<Reading[]>(`/readings${qs ? `?${qs}` : ''}`);
  },
  create: (r: Reading) => request<Reading>('/readings', { method: 'POST', body: JSON.stringify(r) }),
  delete: (meterId: string, date: string, hour: number) =>
    request<void>(`/readings/${meterId}/${date}/${hour}`, { method: 'DELETE' }),
  importCsv: (file: File) => {
    const fd = new FormData(); fd.append('file', file);
    return request<ImportResult>('/readings/import', { method: 'POST', headers: {}, body: fd });
  },
};

// ── Billing / Invoices ───────────────────────────────────────────────────────

export const billingApi = {
  run: (period: string) =>
    request<{ period: string; generated: number; invoices: string[] }>(`/billing/run?period=${period}`, { method: 'POST' }),
  listInvoices: (period?: string) =>
    request<Invoice[]>(`/invoices${period ? `?period=${period}` : ''}`),
  getInvoice: (id: string) => request<Invoice>(`/invoices/${id}`),
  pdfUrl: (id: string) => `${BASE}/invoices/${id}/pdf`,
};
