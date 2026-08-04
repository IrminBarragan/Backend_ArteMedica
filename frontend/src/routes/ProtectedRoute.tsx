import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';
import type { Rol } from '@/types/auth.types';

interface ProtectedRouteProps {
  rolesPermitidos?: Rol[];
}

export function ProtectedRoute({ rolesPermitidos }: ProtectedRouteProps) {
  const { isAuthenticated, usuario } = useAuth();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (rolesPermitidos && (!usuario || !rolesPermitidos.includes(usuario.rol))) {
    return <Navigate to="/no-autorizado" replace />;
  }

  return <Outlet />;
}
