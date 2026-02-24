import { useState } from 'react';
import { NavLink, Routes, Route, Navigate } from 'react-router-dom';
import {
  AppBar, Box, Drawer, List, ListItem, ListItemButton,
  ListItemText, Toolbar, Typography, useTheme,
} from '@mui/material';
import BoltIcon from '@mui/icons-material/Bolt';
import MetersPage    from './pages/MetersPage';
import ContractsPage from './pages/ContractsPage';
import ReadingsPage  from './pages/ReadingsPage';
import BillingPage   from './pages/BillingPage';

const DRAWER_WIDTH = 220;

const navItems = [
  { label: 'Meters',            to: '/meters'    },
  { label: 'Contracts',         to: '/contracts' },
  { label: 'Readings',          to: '/readings'  },
  { label: 'Billing / Invoices', to: '/billing'  },
];

export default function App() {
  const theme = useTheme();
  // dummy state to trigger re-render on route change (NavLink handles active style)
  const [, setTick] = useState(0);

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh' }}>
      {/* Permanent left drawer */}
      <Drawer
        variant="permanent"
        sx={{
          width: DRAWER_WIDTH,
          flexShrink: 0,
          '& .MuiDrawer-paper': {
            width: DRAWER_WIDTH,
            boxSizing: 'border-box',
            bgcolor: 'primary.main',
            color: 'primary.contrastText',
          },
        }}
      >
        <Toolbar sx={{ gap: 1 }}>
          <BoltIcon sx={{ color: 'secondary.main' }} />
          <Typography variant="subtitle1" noWrap fontWeight={700}>
            Naturgy Workshop
          </Typography>
        </Toolbar>
        <List disablePadding>
          {navItems.map(item => (
            <ListItem key={item.to} disablePadding>
              <ListItemButton
                component={NavLink}
                to={item.to}
                onClick={() => setTick(t => t + 1)}
                sx={{
                  color: 'rgba(255,255,255,0.75)',
                  '&.active, &:hover': {
                    bgcolor: 'primary.light',
                    color: '#fff',
                  },
                }}
              >
                <ListItemText primary={item.label} primaryTypographyProps={{ fontSize: '0.875rem' }} />
              </ListItemButton>
            </ListItem>
          ))}
        </List>
      </Drawer>

      {/* Top AppBar + main content */}
      <Box sx={{ display: 'flex', flexDirection: 'column', flex: 1, overflow: 'hidden' }}>
        <AppBar
          position="static"
          elevation={1}
          sx={{ bgcolor: 'background.paper', color: 'text.primary', borderBottom: `1px solid ${theme.palette.divider}` }}
        >
          <Toolbar variant="dense">
            <Typography variant="h6" fontWeight={600}>
              Naturgy – Energy Management
            </Typography>
          </Toolbar>
        </AppBar>

        <Box component="main" sx={{ flex: 1, p: 3, overflowX: 'auto', bgcolor: 'background.default' }}>
          <Routes>
            <Route path="/"          element={<Navigate to="/meters" replace />} />
            <Route path="/meters"    element={<MetersPage />} />
            <Route path="/contracts" element={<ContractsPage />} />
            <Route path="/readings"  element={<ReadingsPage />} />
            <Route path="/billing"   element={<BillingPage />} />
          </Routes>
        </Box>
      </Box>
    </Box>
  );
}

