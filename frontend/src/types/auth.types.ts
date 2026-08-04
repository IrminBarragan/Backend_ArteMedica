export type Rol = 'ADMIN' | 'MEDICO' | 'FARMACEUTICO';

export interface LoginRequestDTO {
  username: string;
  password: string;
}

export interface LoginResponseDTO {
  token: string;
  tipo: string;
  username: string;
  rol: Rol;
  empleadoId: number;
  expiresIn: number;
}
