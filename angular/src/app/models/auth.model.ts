export type UserRole = 'CLIENT' | 'FREELANCER' | 'ADMIN';

export interface User {
  id: number;
  email: string;
  name: string;
  role: UserRole;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  id: number;
  email: string;
  name: string;
  role: UserRole;
}
