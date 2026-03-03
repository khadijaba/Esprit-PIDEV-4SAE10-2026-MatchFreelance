import { User } from './user.model';

export type UserRole = 'FREELANCER' | 'PROJECT_OWNER' | 'ADMIN';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  username: string;
  firstName?: string;
  lastName?: string;
  role: UserRole;
}

export interface AuthResponse {
  token: string;
  user: User;
  role?: string;
}
