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

// Financial data types - BigDecimal values come as strings from backend
export interface FinancialData {
  ticker: string;
  revenue: string[];
  operating_expense: string[];
  operating_income: string[];
  operating_cash_flow: string[];
  net_profit: string[];
  capital_expenditure: string[];
  free_cash_flow: string[];
  eps: string[];
  total_debt: string[];
  ordinary_shares_number: string[];
  date_fetched: string;
}

// DCF calculation types - BigDecimal values as strings
export interface DCFInput {
  ticker: string;
  discountRate: string;
  growthRate: string;
  terminalGrowthRate: string;
}

export interface DCFOutput {
  ticker: string;
  fairValuePerShare: string;
  currentPrice: string;
  valuation: 'Undervalued' | 'Overvalued' | 'Fair Value';
  upsideDownsidePercentage?: string;
  terminalValue?: string;
  presentValueOfCashFlows?: string;
  enterpriseValue?: string;
  equityValue?: string;
  sharesOutstanding?: string;
}

// API response types
export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  error?: string;
}

// Watchlist types - BigDecimal values as strings
export interface WatchlistItem {
  ticker: string;
  fair_value_per_share?: string;
  current_price?: string;
  valuation?: 'Undervalued' | 'Overvalued' | 'Fair Value';
  last_updated?: string;
}

export interface WatchlistRequest {
  ticker: string;
}

// Chart data types - values as numbers for display
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