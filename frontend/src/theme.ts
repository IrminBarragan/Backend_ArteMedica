import { createTheme } from '@mui/material/styles';

// Colores corporativos de ArteMédica Hospital
// Azul Mediterráneo (primario): #264272
// Azul Medieval (secundario): #62b5e5
// Vivanco (acento/fondo): #eccfb4
const theme = createTheme({
  palette: {
    primary: {
      main: '#264272',
    },
    secondary: {
      main: '#62b5e5',
    },
    background: {
      default: '#eccfb4',
    },
  },
  typography: {
    fontFamily: [
      '-apple-system',
      'BlinkMacSystemFont',
      '"Segoe UI"',
      'Roboto',
      'Helvetica',
      'Arial',
      'sans-serif',
    ].join(','),
  },
});

export default theme;
