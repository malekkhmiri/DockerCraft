// shared/models/auth-response.model.ts
export interface AuthResponse {
  token: string;
  username: string;
  email: string;
  role: string;
  userType: string;
  planType: string;
  userId?: number;
  isStudentVerified?: boolean; // Reçu du backend
}
