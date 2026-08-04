import type { Rol } from './auth.types';

export type TipoEmpleado = 'MEDICO' | 'FARMACEUTICO' | 'ADMIN';

export interface EmpleadoRequestDTO {
  nombres: string;
  apellidoPaterno: string;
  apellidoMaterno?: string;
  tipo: TipoEmpleado;
  especialidad?: string;
  cedulaProfesional?: string;
  telefonoGuardia?: string;
}

export interface EmpleadoResponseDTO {
  id: number;
  nombres: string;
  apellidoPaterno: string;
  apellidoMaterno: string | null;
  tipo: TipoEmpleado;
  especialidad: string | null;
  cedulaProfesional: string | null;
  telefonoGuardia: string | null;
  activo: boolean;
}

export interface UsuarioRequestDTO {
  username: string;
  password: string;
  empleadoId: number;
  rol: Rol;
}

export interface UsuarioResponseDTO {
  id: number;
  username: string;
  empleadoId: number;
  empleadoNombre: string;
  rol: Rol;
  activo: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface AreaRequestDTO {
  nombre: string;
  descripcion?: string;
}

export interface AreaResponseDTO {
  id: number;
  nombre: string;
  descripcion: string | null;
  activo: boolean;
}

export interface CategoriaMedicamentoRequestDTO {
  nombre: string;
  descripcion?: string;
}

export interface CategoriaMedicamentoResponseDTO {
  id: number;
  nombre: string;
  descripcion: string | null;
  activo: boolean;
}

export interface ProveedorRequestDTO {
  nombre: string;
  direccion?: string;
  telefono?: string;
  correo?: string;
}

export interface ProveedorResponseDTO {
  id: number;
  nombre: string;
  direccion: string | null;
  telefono: string | null;
  correo: string | null;
  activo: boolean;
}
