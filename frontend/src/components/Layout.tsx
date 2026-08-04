import { AppBar, Box, Button, Toolbar, Typography } from '@mui/material';
import { Outlet } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';

// Espacio reservado para el menu de navegacion lateral, se agrega cuando existan las paginas reales.
export function Layout() {
  const { usuario, cerrarSesion } = useAuth();

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
      <AppBar position="static">
        <Toolbar sx={{ justifyContent: 'space-between' }}>
          <Typography variant="h6">ArteMédica Farmacia</Typography>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            {usuario && (
              <Typography variant="body2">
                {usuario.username} ({usuario.rol})
              </Typography>
            )}
            <Button color="inherit" onClick={cerrarSesion}>
              Cerrar sesión
            </Button>
          </Box>
        </Toolbar>
      </AppBar>
      <Box component="main" sx={{ flexGrow: 1, p: 3 }}>
        <Outlet />
      </Box>
    </Box>
  );
}
