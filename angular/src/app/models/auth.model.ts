import { Role } from './user.model';

export interface JwtResponse {
    token: string;
    email: string;
    role: Role;
    message: string;
}

export interface SignInRequest {
    email: string;
    password?: string; // Optional depending on how form is sent, but usually required
}

export interface SignUpRequest {
    firstName: string;
    lastName: string;
    address: string;
    email: string;
    password?: string;
    birthDate: string; // YYYY-MM-DD
    role: Role;
}

export interface EmailVerificationRequest {
    email: string;
    verificationCode: string;
}

export interface ResendVerificationRequest {
    email: string;
}
