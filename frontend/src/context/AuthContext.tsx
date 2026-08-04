import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';
import { login } from '@/api/auth.api';
import type { LoginRequestDTO, Rol } from '@/types/auth.types';

interface UsuarioAutenticado {
  username: string;
  rol: Rol;
  empleadoId: number;
}

interface AuthContextValue {
  usuario: UsuarioAutenticado | null;
  token: string | null;
  isAuthenticated: boolean;
  iniciarSesion: (credenciales: LoginRequestDTO) => Promise<void>;
  cerrarSesion: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const navigate = useNavigate();
  const [usuario, setUsuario] = useState<UsuarioAutenticado | null>(null);
  const [token, setToken] = useState<string | null>(null);

  useEffect(() => {
    const storedToken = localStorage.getItem('token');
    const storedUsuario = localStorage.getItem('usuario');
    if (storedToken && storedUsuario) {
      setToken(storedToken);
      setUsuario(JSON.parse(storedUsuario) as UsuarioAutenticado);
    }
  }, []);

  async function iniciarSesion(credenciales: LoginRequestDTO) {
    const response = await login(credenciales);
    const usuarioAutenticado: UsuarioAutenticado = {
      username: response.username,
      rol: response.rol,
      empleadoId: response.empleadoId,
    };
    localStorage.setItem('token', response.token);
    localStorage.setItem('usuario', JSON.stringify(usuarioAutenticado));
    setToken(response.token);
    setUsuario(usuarioAutenticado);
  }

  function cerrarSesion() {
    localStorage.removeItem('token');
    localStorage.removeItem('usuario');
    setToken(null);
    setUsuario(null);
    navigate('/login');
  }

  const value: AuthContextValue = {
    usuario,
    token,
    isAuthenticated: token !== null,
    iniciarSesion,
    cerrarSesion,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth debe usarse dentro de un AuthProvider');
  }
  return context;
}
