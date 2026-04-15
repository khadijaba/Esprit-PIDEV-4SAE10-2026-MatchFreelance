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

/** Compatibilité avec les composants qui utilisent un profil détaillé. */
export interface UserProfile {
  userId: number;
  email: string;
  fullName?: string | null;
  role: UserRole;
  createdAt?: string;
}

export type { User };
