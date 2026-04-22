/**
 * Modèle User – à adapter selon les champs exposés par le microservice User.
 */
export interface User {
  id: number;
  userId?: number;
  email?: string;
  username?: string;
  fullName?: string;
  /** Renseignés par le microservice User (JSON) */
  firstName?: string;
  lastName?: string;
  role?: string;
  createdAt?: string;
}

export interface UserRequest {
  email?: string;
  username?: string;
  password?: string;
  firstName?: string;
  lastName?: string;
  role?: string;
}
