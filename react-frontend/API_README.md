# S-Emulator React Frontend API Layer

This document describes the API layer infrastructure for the S-Emulator React frontend.

## Architecture Overview

The API layer is organized into several key components:

- **Types** (`src/types/api.ts`): TypeScript interfaces for all API requests and responses
- **Client** (`src/api/client.ts`): Axios-based HTTP client with interceptors
- **Services** (`src/api/services/`): Service classes for different API categories
- **Hooks** (`src/hooks/useApi.ts`): React hooks for API state management
- **Contexts** (`src/contexts/AuthContext.tsx`): React context for authentication
- **Utils** (`src/utils/`): Validation, error handling, and constants

## Quick Start

### 1. Setup Authentication

```tsx
import { AuthProvider, useAuth } from './contexts/AuthContext';

function App() {
  return (
    <AuthProvider>
      <YourApp />
    </AuthProvider>
  );
}

function YourComponent() {
  const { userId, login, logout } = useAuth();
  
  const handleLogin = async (username: string) => {
    try {
      await userService.registerUser();
      login(username);
    } catch (error) {
      console.error('Login failed:', error);
    }
  };
}
```

### 2. Using API Services

```tsx
import { programService, userService, chatService } from './api';

// Load programs
const programs = await programService.getPrograms();

// Execute a program
const result = await programService.executeProgram({
  expansionLevel: 0,
  input: [1, 2, 3],
  architecture: 'I'
});

// Send chat message
await chatService.sendMessage({
  username: 'user123',
  content: 'Hello world!'
});
```

### 3. Using React Hooks

```tsx
import { useApi } from './hooks/useApi';
import { programService } from './api';

function ProgramExecution() {
  const { execute, data, loading, error } = useApi();
  
  const runProgram = async () => {
    try {
      await execute(() => programService.executeProgram({
        expansionLevel: 0,
        input: [1, 2, 3],
        architecture: 'I'
      }));
    } catch (err) {
      console.error('Execution failed:', err);
    }
  };
  
  return (
    <div>
      <button onClick={runProgram} disabled={loading}>
        {loading ? 'Running...' : 'Run Program'}
      </button>
      {error && <div>Error: {error.message}</div>}
      {data && <div>Result: {data.result}</div>}
    </div>
  );
}
```

## API Services

### ProgramService

Handles all program-related operations:

- `getPrograms()` - Get all programs
- `getFunctions()` - Get all functions
- `setProgram(name)` - Set current program
- `executeProgram(request)` - Execute program
- `startDebug(request)` - Start debug session
- `stepDebug()` - Step through debug
- `continueDebug()` - Continue debug execution
- `stopDebug()` - Stop debug session

### UserService

Handles user-related operations:

- `registerUser()` - Register current user
- `getAllUsers()` - Get all users
- `getUserStatistics(userId)` - Get user statistics
- `getCredits()` - Get user credits
- `addCredits(amount)` - Add credits to user

### ChatService

Handles chat operations:

- `sendMessage(request)` - Send chat message
- `getChatHistory()` - Get chat history

## Error Handling

The API layer includes comprehensive error handling:

```tsx
import { ErrorHandler } from './utils/errorHandler';

try {
  await programService.executeProgram(request);
} catch (error) {
  const message = ErrorHandler.handle(error);
  console.error(message);
}
```

## Validation

Input validation is provided for all API requests:

```tsx
import { validators } from './utils/validation';

try {
  const username = validators.username(inputUsername);
  const credits = validators.credits(inputCredits);
  const input = validators.inputArray(inputArray);
} catch (error) {
  console.error('Validation failed:', error.message);
}
```

## Configuration

API configuration can be set via environment variables:

```env
REACT_APP_API_BASE_URL=http://localhost:8080
```

## Type Safety

All API requests and responses are fully typed:

```tsx
import { ProgramResult, ExecuteProgramRequest } from './types/api';

const request: ExecuteProgramRequest = {
  expansionLevel: 0,
  input: [1, 2, 3],
  architecture: 'I'
};

const result: ProgramResult = await programService.executeProgram(request);
```

## Examples

See `src/examples/apiUsage.ts` for comprehensive usage examples including:
- User authentication flow
- Program execution
- Debug sessions
- Chat functionality
- User management
- React hook usage

## Server Compatibility

This API layer is designed to work with the S-Emulator Java server endpoints:

- All endpoints return JSON with `{message, code, success, data}` format
- Authentication via `X-User-Id` header
- Error responses include detailed error messages
- Supports all server operations: programs, users, chat, debugging
