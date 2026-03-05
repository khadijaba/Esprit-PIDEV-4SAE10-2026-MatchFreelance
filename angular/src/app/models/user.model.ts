export type UserRole = 'ADMIN' | 'CLIENT' | 'FREELANCER';

export interface User {
  id: number;
  fullName?: string;
  email: string;
  role: UserRole;
  addressLine?: string;
  city?: string;
  lat?: number;
  lng?: number;
}

