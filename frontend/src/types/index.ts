// User related types
export interface User {
  userId: string;
  email: string;
  watchlist: string[];
}

export interface AuthRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  user: User;
}

// Financial data types
export interface FinancialData {
  ticker: string;
  revenue: number[];
  operating_expense: number[];
  operating_income: number[];
  operating_cash_flow: number[];
  net_profit: number[];
  capital_expenditure: number[];
  free_cash_flow: number[];
  eps: number[];
  total_debt: number[];
  ordinary_shares_number: number[];
  date_fetched: string;
}

// DCF calculation types
export interface DCFInput {
  ticker: string;
  discount_rate: number;
  growth_rate: number;
  terminal_growth_rate: number;
}

export interface DCFOutput {
  ticker: string;
  fair_value_per_share: number;
  current_price: number;
  valuation: 'Undervalued' | 'Overvalued' | 'Fair Value';
}

// API response types
export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  error?: string;
}

// Watchlist types
export interface WatchlistItem {
  ticker: string;
  fair_value_per_share?: number;
  current_price?: number;
  valuation?: 'Undervalued' | 'Overvalued' | 'Fair Value';
  last_updated?: string;
}

export interface WatchlistRequest {
  ticker: string;
}

// Chart data types
export interface ChartDataPoint {
  year: string;
  value: number;
}

export interface ChartData {
  revenue: ChartDataPoint[];
  net_income: ChartDataPoint[];
  free_cash_flow: ChartDataPoint[];
  eps: ChartDataPoint[];
}