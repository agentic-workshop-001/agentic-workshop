import { createTheme } from '@mui/material/styles';

// Naturgy Workshop theme tokens
const theme = createTheme({
  palette: {
    primary: {
      main: '#1a1a2e',      // dark/navy – headers, sidebar, primary actions
      light: '#2c2f6b',
      contrastText: '#ffffff',
    },
    secondary: {
      main: '#27ae60',      // green – CTA / success actions
      contrastText: '#ffffff',
    },
    error: {
      main: '#c0392b',
    },
    background: {
      default: '#f5f6fa',
      paper: '#ffffff',
    },
  },
  typography: {
    fontFamily: 'system-ui, sans-serif',
  },
  shape: {
    borderRadius: 6,
  },
  components: {
    MuiButton: {
      styleOverrides: {
        root: { textTransform: 'none' },
      },
    },
    MuiTableCell: {
      styleOverrides: {
        head: {
          backgroundColor: '#1a1a2e',
          color: '#ffffff',
          fontWeight: 700,
          fontSize: '0.75rem',
          textTransform: 'uppercase',
        },
      },
    },
  },
});

export default theme;
