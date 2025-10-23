import axios, { AxiosInstance, AxiosResponse } from 'axios';
import { ApiResponse, ApiError } from '../types/api';

export class ApiClient {
  private client: AxiosInstance;
  private baseURL: string;
  private userId: string | null = null;

  constructor(baseURL: string = '') {
    this.baseURL = baseURL;
    this.client = axios.create({
      baseURL,
      timeout: 30000,
      headers: {
        'Content-Type': 'application/json',
      },
    });

    // Request interceptor to add X-User-Id header
    this.client.interceptors.request.use(
      (config) => {
        if (this.userId) {
          config.headers['X-User-Id'] = this.userId;
        }
        return config;
      },
      (error) => Promise.reject(error)
    );

    // Response interceptor for error handling
    this.client.interceptors.response.use(
      (response: AxiosResponse<ApiResponse>) => {
        return response;
      },
      (error) => {
        const apiError: ApiError = {
          message: this.extractMessageFromError(error),
          code: error.response?.status || 500,
          details: error.response?.data?.details || error.response?.data,
        };
        return Promise.reject(apiError);
      }
    );
  }

  setUserId(userId: string | null) {
    this.userId = userId;
  }

  getUserId(): string | null {
    return this.userId;
  }

  private extractMessageFromError(error: any): string {
    // Try to parse the error response body as an ApiResponse object and extract the message
    try {
      if (error.response?.data) {
        const errorData = error.response.data;
        
        // If it's already an ApiResponse object with a message
        if (errorData.message && typeof errorData.message === 'string') {
          return errorData.message;
        }
        
        // If it's a string response body, try to parse it as JSON
        if (typeof errorData === 'string' && errorData.trim().startsWith('{')) {
          const parsedError = JSON.parse(errorData);
          if (parsedError.message && typeof parsedError.message === 'string') {
            return parsedError.message;
          }
        }
        
        // If it's an object but not ApiResponse format, try to find a message field
        if (typeof errorData === 'object' && errorData !== null) {
          if (errorData.message) {
            return errorData.message;
          }
          if (errorData.error) {
            return errorData.error;
          }
        }
      }
    } catch (parseError) {
      // If parsing fails, fall back to other methods
    }
    
    // Fallback to axios error message or generic message
    return error.message || error.response?.statusText || 'Unknown error';
  }

  async get<T = any>(url: string, params?: Record<string, any>): Promise<ApiResponse<T>> {
    const response = await this.client.get<ApiResponse<T>>(url, { params });
    return response.data;
  }

  async post<T = any>(url: string, data?: any, params?: Record<string, any>): Promise<ApiResponse<T>> {
    const response = await this.client.post<ApiResponse<T>>(url, data, { params });
    return response.data;
  }

  async put<T = any>(url: string, data?: any, params?: Record<string, any>): Promise<ApiResponse<T>> {
    const response = await this.client.put<ApiResponse<T>>(url, data, { params });
    return response.data;
  }

  async delete<T = any>(url: string, params?: Record<string, any>): Promise<ApiResponse<T>> {
    const response = await this.client.delete<ApiResponse<T>>(url, { params });
    return response.data;
  }

  async postMultipart<T = any>(
    url: string,
    formData: FormData,
    params?: Record<string, any>
  ): Promise<ApiResponse<T>> {
    const response = await this.client.post<ApiResponse<T>>(url, formData, {
      params,
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  }

  async postFormData<T = any>(
    url: string,
    data: Record<string, any>,
    params?: Record<string, any>
  ): Promise<ApiResponse<T>> {
    const formData = new URLSearchParams();
    Object.entries(data).forEach(([key, value]) => {
      formData.append(key, String(value));
    });

    const response = await this.client.post<ApiResponse<T>>(url, formData, {
      params,
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
    });
    return response.data;
  }
}

// Singleton instance
export const apiClient = new ApiClient();
