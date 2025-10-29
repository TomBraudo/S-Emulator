// API Response wrapper
export interface ApiResponse<T = any> {
  message: string;
  code: number;
  success: boolean;
  data: T;
}

// Program and Function related types
export interface ProgramInfo {
  name: string;
  owner: string;
  commandsCount: number;
  maxLevel: number;
  ranCount: number;
  averageCost: number;
  sourceProgram: string | null; // only for functions; null for programs
  function: boolean; // true if helper function, false if program
}

export interface ProgramCommands {
  commands: string[];
  architectures: string[];
}

export interface ProgramSummary {
  architectureCommandsCount: number[];
}

export interface ProgramResult {
  cycles: number;
  sessionCycles: number;
  result: number;
  variableToValue: VariableToValue[];
  debugIndex: number;
  isDebug: boolean;
  haltReason: HaltReason;
}

export interface VariableToValue {
  variable: string;
  value: number;
}

export enum HaltReason {
  FINISHED = 'FINISHED',
  STOPPED_MANUALLY = 'STOPPED_MANUALLY',
  INSUFFICIENT_CREDITS = 'INSUFFICIENT_CREDITS'
}

// User related types
export interface UserInfo {
  name: string;
  programUploadedCount: number;
  functionUploadedCount: number;
  credits: number;
  creditsUsed: number;
  runCount: number;
}

export interface Statistic {
  index: number;
  programName: string;
  runType: RunType;
  expansionLevel: number;
  architecture: string;
  input: number[];
  result: number;
  cyclesCount: number;
  variableToValue: VariableToValue[];
}

export enum RunType {
  Program = 'Program',
  HelperFunction = 'HelperFunction'
}

// Chat related types
export interface ChatMessage {
  username: string;
  content: string;
  timestamp: number;
}

// Request types
export interface ExecuteProgramRequest {
  expansionLevel: number;
  input: number[];
  architecture: string;
}

export interface DebugStartRequest {
  expansionLevel: number;
  architecture: string;
  input: number[];
  breakpoints: number[];
}

export interface SendMessageRequest {
  username: string;
  content: string;
}

export interface AddCreditsRequest {
  credits: number;
}

// API Error type
export interface ApiError {
  message: string;
  code: number;
  details?: string;
}
