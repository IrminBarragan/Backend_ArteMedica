import { apiClient } from './client';
import type { LoginRequestDTO, LoginResponseDTO } from '@/types/auth.types';

export async function login(credenciales: LoginRequestDTO): Promise<LoginResponseDTO> {
  const { data } = await apiClient.post<LoginResponseDTO>('/auth/login', credenciales);
  return data;
}
