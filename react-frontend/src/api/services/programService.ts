import { apiClient } from '../client';
import {
  ProgramInfo,
  ProgramCommands,
  ProgramSummary,
  ProgramResult,
  ExecuteProgramRequest,
  DebugStartRequest,
  ApiResponse,
} from '../../types/api';

export class ProgramService {
  // Get all programs information
  async getPrograms(): Promise<ApiResponse<ProgramInfo[]>> {
    return apiClient.get<ProgramInfo[]>('/program/information');
  }

  // Get all functions information
  async getFunctions(): Promise<ApiResponse<ProgramInfo[]>> {
    return apiClient.get<ProgramInfo[]>('/function/information');
  }

  // Get program commands for specific expansion level
  async getProgramCommands(expansionLevel: number): Promise<ApiResponse<ProgramCommands>> {
    return apiClient.get<ProgramCommands>('/program/commands', { expansionLevel });
  }

  // Get program summary for specific expansion level
  async getProgramSummary(expansionLevel: number): Promise<ApiResponse<ProgramSummary>> {
    return apiClient.get<ProgramSummary>('/program/summary', { expansionLevel });
  }

  // Get maximum expansion level
  async getMaxLevel(): Promise<ApiResponse<number>> {
    return apiClient.get<number>('/program/level');
  }

  // Get input variable names
  async getInputVariables(): Promise<ApiResponse<string[]>> {
    return apiClient.get<string[]>('/program/input');
  }

  // Set current program
  async setProgram(programName: string): Promise<ApiResponse<void>> {
    return apiClient.post<void>('/program/set', null, { programName });
  }

  // Execute program
  async executeProgram(request: ExecuteProgramRequest): Promise<ApiResponse<ProgramResult>> {
    return apiClient.post<ProgramResult>('/program/execute', request);
  }

  // Check if program can run
  async checkRunnability(expansionLevel: number, architecture: string): Promise<ApiResponse<string>> {
    return apiClient.get<string>('/program/runnability', { expansionLevel, architecture });
  }

  // Get highlight options (variables and labels)
  async getHighlightOptions(expansionLevel: number): Promise<ApiResponse<string[]>> {
    return apiClient.get<string[]>('/program/highlight', { expansionLevel });
  }

  // Get function dependency chain
  async getFunctionChain(name: string): Promise<ApiResponse<string[]>> {
    return apiClient.get<string[]>('/dependencies', { name });
  }

  // Debug operations
  async startDebug(request: DebugStartRequest): Promise<ApiResponse<ProgramResult>> {
    return apiClient.post<ProgramResult>('/program/debug/start', request);
  }

  async stepDebug(): Promise<ApiResponse<ProgramResult>> {
    return apiClient.post<ProgramResult>('/program/debug/step');
  }

  async continueDebug(): Promise<ApiResponse<ProgramResult>> {
    return apiClient.post<ProgramResult>('/program/debug/continue');
  }

  async stopDebug(): Promise<ApiResponse<number>> {
    return apiClient.post<number>('/program/debug/stop');
  }

  // Get command history
  async getCommandHistory(expansionLevel: number, index: number): Promise<ApiResponse<string[]>> {
    return apiClient.get<string[]>('/command/history', { expansionLevel, index });
  }
}

export const programService = new ProgramService();
