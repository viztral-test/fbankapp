// Shapes of the backend JSON bodies, as documented in backend/API.md.

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  tokenType: string;
  expiresAt: string;
  username: string;
  displayName: string;
}

export interface Wallet {
  balance: number;
  currency: string;
  updatedAt: string;
}

export type TransactionType = 'TOP_UP' | 'OPENING_BALANCE';

export interface Transaction {
  reference: string;
  type: TransactionType;
  amount: number;
  balanceAfter: number;
  description: string;
  createdAt: string;
}

export interface AddMoneyRequest {
  amount: number;
  note?: string;
}

export interface AddMoneyResponse {
  transaction: Transaction;
  wallet: Wallet;
}

export interface TransactionPage {
  items: Transaction[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  fieldErrors?: Record<string, string>;
}
