import { Box, Typography } from '@mui/material';

export function NoAutorizadoPage() {
  return (
    <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '100vh' }}>
      <Typography variant="h5">No tienes permiso para ver esta página.</Typography>
    </Box>
  );
}
