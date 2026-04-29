export type UserRole = 'ADMIN' | 'FREELANCER' | 'CLIENT';

/** Utilisateur retourné par GET /api/users (liste). */
export interface User {
  id: number;
  email: string;
  fullName: string | null;
  role: UserRole;
  createdAt?: string;
}

/** Profil complet retourné par GET /api/users/me/profile. */
export interface UserProfile {
  userId: number;
  email: string;
  fullName: string;
  role: UserRole;
  createdAt?: string;
}

export interface AuthResponse {
  token: string;
  userId: number;
  email: string;
  fullName: string | null;
  role: UserRole;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  fullName?: string;
  role: UserRole;
}
