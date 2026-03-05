/**
 * Modèle User – à adapter selon les champs exposés par le microservice User.
 */
export interface User {
  id: number;
  email?: string;
  username?: string;
  fullName?: string;
  firstName?: string;
  lastName?: string;
  role?: string;
  createdAt?: string;
  [key: string]: unknown;
}

export interface UserRequest {
  email?: string;
  username?: string;
  password?: string;
  firstName?: string;
  lastName?: string;
  role?: string;
  [key: string]: unknown;
}
