// API Configuration
export const API_CONFIG = {
  BASE_URL: process.env.REACT_APP_API_BASE_URL || '',
  TIMEOUT: 30000,
  RETRY_ATTEMPTS: 3,
  RETRY_DELAY: 1000,
} as const;

// Architecture constants
export const ARCHITECTURES = {
  I: 'I',
  II: 'II', 
  III: 'III',
  IV: 'IV',
} as const;

export const ARCHITECTURE_COSTS = {
  [ARCHITECTURES.I]: 0,
  [ARCHITECTURES.II]: 1,
  [ARCHITECTURES.III]: 2,
  [ARCHITECTURES.IV]: 3,
} as const;

// UI Constants
export const UI_CONFIG = {
  CHAT_POLL_INTERVAL: 1000, // 1 second
  DEBOUNCE_DELAY: 300,
  ANIMATION_DURATION: 200,
} as const;

// Error Messages
export const ERROR_MESSAGES = {
  NETWORK_ERROR: 'Network error: Please check your connection',
  SERVER_ERROR: 'Server error: Please try again later',
  UNAUTHORIZED: 'Please log in to continue',
  INSUFFICIENT_CREDITS: 'Insufficient credits to perform this action',
  PROGRAM_NOT_LOADED: 'No program is currently loaded',
  DEBUG_NOT_ACTIVE: 'No debug session is currently active',
} as const;
