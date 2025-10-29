// Validation utilities for API requests

export class ValidationError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'ValidationError';
  }
}

export const validators = {
  // Username validation
  username: (username: string): string => {
    if (!username || username.trim().length === 0) {
      throw new ValidationError('Username is required');
    }
    if (username.length > 50) {
      throw new ValidationError('Username must be less than 50 characters');
    }
    return username.trim();
  },

  // Credits validation
  credits: (credits: number): number => {
    if (typeof credits !== 'number' || isNaN(credits)) {
      throw new ValidationError('Credits must be a valid number');
    }
    if (credits < 0) {
      throw new ValidationError('Credits cannot be negative');
    }
    if (credits > 1000000) {
      throw new ValidationError('Credits cannot exceed 1,000,000');
    }
    return Math.floor(credits);
  },

  // Expansion level validation
  expansionLevel: (level: number): number => {
    if (typeof level !== 'number' || isNaN(level)) {
      throw new ValidationError('Expansion level must be a valid number');
    }
    if (level < 0) {
      throw new ValidationError('Expansion level cannot be negative');
    }
    if (level > 10) {
      throw new ValidationError('Expansion level cannot exceed 10');
    }
    return Math.floor(level);
  },

  // Architecture validation
  architecture: (arch: string): string => {
    const validArchitectures = ['I', 'II', 'III', 'IV'];
    if (!validArchitectures.includes(arch)) {
      throw new ValidationError(`Architecture must be one of: ${validArchitectures.join(', ')}`);
    }
    return arch;
  },

  // Input array validation
  inputArray: (input: number[]): number[] => {
    if (!Array.isArray(input)) {
      throw new ValidationError('Input must be an array');
    }
    if (input.length > 100) {
      throw new ValidationError('Input array cannot exceed 100 elements');
    }
    for (let i = 0; i < input.length; i++) {
      if (typeof input[i] !== 'number' || isNaN(input[i])) {
        throw new ValidationError(`Input[${i}] must be a valid number`);
      }
    }
    return input;
  },

  // Breakpoints validation
  breakpoints: (breakpoints: number[]): number[] => {
    if (!Array.isArray(breakpoints)) {
      throw new ValidationError('Breakpoints must be an array');
    }
    if (breakpoints.length > 50) {
      throw new ValidationError('Breakpoints array cannot exceed 50 elements');
    }
    for (let i = 0; i < breakpoints.length; i++) {
      if (typeof breakpoints[i] !== 'number' || isNaN(breakpoints[i])) {
        throw new ValidationError(`Breakpoint[${i}] must be a valid number`);
      }
      if (breakpoints[i] < 0) {
        throw new ValidationError(`Breakpoint[${i}] cannot be negative`);
      }
    }
    return breakpoints;
  },

  // Message content validation
  messageContent: (content: string): string => {
    if (!content || content.trim().length === 0) {
      throw new ValidationError('Message content is required');
    }
    if (content.length > 1000) {
      throw new ValidationError('Message content cannot exceed 1000 characters');
    }
    return content.trim();
  },

  // Program name validation
  programName: (name: string): string => {
    if (!name || name.trim().length === 0) {
      throw new ValidationError('Program name is required');
    }
    if (name.length > 100) {
      throw new ValidationError('Program name cannot exceed 100 characters');
    }
    if (!/^[a-zA-Z0-9_-]+$/.test(name)) {
      throw new ValidationError('Program name can only contain letters, numbers, underscores, and hyphens');
    }
    return name.trim();
  },
};
