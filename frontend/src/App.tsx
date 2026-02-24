import { NavLink, Routes, Route, Navigate } from 'react-router-dom';
import MetersPage    from './pages/MetersPage';
import ContractsPage from './pages/ContractsPage';
import ReadingsPage  from './pages/ReadingsPage';
import BillingPage   from './pages/BillingPage';

export default function App() {
  return (
    <div className="app-shell">
      <nav className="sidebar">
        <h1>⚡ Naturgy Workshop</h1>
        <NavLink to="/meters"    className={({ isActive }) => isActive ? 'active' : ''}>Meters</NavLink>
        <NavLink to="/contracts" className={({ isActive }) => isActive ? 'active' : ''}>Contracts</NavLink>
        <NavLink to="/readings"  className={({ isActive }) => isActive ? 'active' : ''}>Readings</NavLink>
        <NavLink to="/billing"   className={({ isActive }) => isActive ? 'active' : ''}>Billing / Invoices</NavLink>
      </nav>
      <main className="main-content">
        <Routes>
          <Route path="/"          element={<Navigate to="/meters" replace />} />
          <Route path="/meters"    element={<MetersPage />} />
          <Route path="/contracts" element={<ContractsPage />} />
          <Route path="/readings"  element={<ReadingsPage />} />
          <Route path="/billing"   element={<BillingPage />} />
        </Routes>
      </main>
    </div>
  );
}
