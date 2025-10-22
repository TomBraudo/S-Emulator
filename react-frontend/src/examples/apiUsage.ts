// Example usage of the API layer
import { 
  programService, 
  userService, 
  chatService, 
  apiClient
} from '../api';
import { useApi } from '../hooks/useApi';
import { AuthProvider, useAuth } from '../contexts/AuthContext';
import { validators } from '../utils/validation';
import { ErrorHandler } from '../utils/errorHandler';

// Example: Using the API services directly
export class ApiUsageExamples {
  
  // Example: User authentication flow
  static async loginUser(username: string) {
    try {
      // Validate input
      const validatedUsername = validators.username(username);
      
      // Set user ID in API client
      apiClient.setUserId(validatedUsername);
      
      // Register user
      await userService.registerUser();
      
      console.log('User logged in successfully');
      return true;
    } catch (error) {
      console.error('Login failed:', ErrorHandler.handle(error as any));
      return false;
    }
  }

  // Example: Loading programs and functions
  static async loadProgramsAndFunctions() {
    try {
      const [programsResponse, functionsResponse] = await Promise.all([
        programService.getPrograms(),
        programService.getFunctions()
      ]);

      console.log('Programs:', programsResponse.data);
      console.log('Functions:', functionsResponse.data);
      
      return {
        programs: programsResponse.data,
        functions: functionsResponse.data
      };
    } catch (error) {
      console.error('Failed to load programs:', ErrorHandler.handle(error as any));
      throw error;
    }
  }

  // Example: Executing a program
  static async executeProgram(programName: string, input: number[], architecture: string) {
    try {
      // Validate inputs
      const validatedInput = validators.inputArray(input);
      const validatedArchitecture = validators.architecture(architecture);
      
      // Set the program
      await programService.setProgram(programName);
      
      // Check if program can run
      const runnabilityResponse = await programService.checkRunnability(0, validatedArchitecture);
      console.log('Runnability check:', runnabilityResponse.data);
      
      // Execute the program
      const executeRequest = {
        expansionLevel: 0,
        input: validatedInput,
        architecture: validatedArchitecture
      };
      
      const result = await programService.executeProgram(executeRequest);
      console.log('Execution result:', result.data);
      
      return result.data;
    } catch (error) {
      console.error('Execution failed:', ErrorHandler.handle(error as any));
      throw error;
    }
  }

  // Example: Debug session
  static async debugProgram(programName: string, input: number[], breakpoints: number[]) {
    try {
      // Validate inputs
      const validatedInput = validators.inputArray(input);
      const validatedBreakpoints = validators.breakpoints(breakpoints);
      
      // Set the program
      await programService.setProgram(programName);
      
      // Start debug session
      const debugRequest = {
        expansionLevel: 0,
        architecture: 'I',
        input: validatedInput,
        breakpoints: validatedBreakpoints
      };
      
      const startResult = await programService.startDebug(debugRequest);
      console.log('Debug started:', startResult.data);
      
      // Step through debug
      const stepResult = await programService.stepDebug();
      console.log('Debug step:', stepResult.data);
      
      // Continue debug
      const continueResult = await programService.continueDebug();
      console.log('Debug continue:', continueResult.data);
      
      // Stop debug
      const stopResult = await programService.stopDebug();
      console.log('Debug stopped, total cost:', stopResult.data);
      
      return {
        start: startResult.data,
        step: stepResult.data,
        continue: continueResult.data,
        stopCost: stopResult.data
      };
    } catch (error) {
      console.error('Debug failed:', ErrorHandler.handle(error as any));
      throw error;
    }
  }

  // Example: Chat functionality
  static async sendChatMessage(username: string, content: string) {
    try {
      const validatedUsername = validators.username(username);
      const validatedContent = validators.messageContent(content);
      
      await chatService.sendMessage({
        username: validatedUsername,
        content: validatedContent
      });
      
      console.log('Message sent successfully');
    } catch (error) {
      console.error('Failed to send message:', ErrorHandler.handle(error as any));
      throw error;
    }
  }

  // Example: User management
  static async manageUserCredits() {
    try {
      // Get current credits
      const creditsResponse = await userService.getCredits();
      console.log('Current credits:', creditsResponse.data);
      
      // Add credits
      const addCreditsResponse = await userService.addCredits(100);
      console.log('Credits added successfully');
      
      // Get updated credits
      const updatedCreditsResponse = await userService.getCredits();
      console.log('Updated credits:', updatedCreditsResponse.data);
      
      return updatedCreditsResponse.data;
    } catch (error) {
      console.error('Credit management failed:', ErrorHandler.handle(error as any));
      throw error;
    }
  }
}

// Example: Using React hooks
export function useProgramExecution() {
  const { execute, data, loading, error } = useApi();
  
  const executeProgram = async (programName: string, input: number[], architecture: string) => {
    return execute(() => ApiUsageExamples.executeProgram(programName, input, architecture));
  };
  
  return {
    executeProgram,
    result: data,
    loading,
    error
  };
}

export function useChat() {
  const { execute, data, loading, error } = useApi();
  
  const sendMessage = async (username: string, content: string) => {
    return execute(() => ApiUsageExamples.sendChatMessage(username, content));
  };
  
  const loadHistory = async () => {
    return execute(() => chatService.getChatHistory());
  };
  
  return {
    sendMessage,
    loadHistory,
    messages: data,
    loading,
    error
  };
}
