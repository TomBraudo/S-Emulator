import { ApiError } from '../types/api';

export class ErrorHandler {
  static handle(error: ApiError): string {
    // Handle specific error codes
    switch (error.code) {
      case 400:
        return `Bad Request: ${error.message}`;
      case 401:
        return 'Unauthorized: Please log in again';
      case 403:
        return 'Forbidden: You do not have permission to perform this action';
      case 404:
        return 'Not Found: The requested resource was not found';
      case 500:
        return 'Server Error: Something went wrong on the server';
      default:
        return error.message || 'An unexpected error occurred';
    }
  }

  static isNetworkError(error: ApiError): boolean {
    return error.code === 0 || !error.code;
  }

  static isServerError(error: ApiError): boolean {
    return error.code >= 500;
  }

  static isClientError(error: ApiError): boolean {
    return error.code >= 400 && error.code < 500;
  }
}
