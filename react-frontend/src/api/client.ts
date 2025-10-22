import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios';
import { ApiResponse, ApiError } from '../types/api';

export class ApiClient {
  private client: AxiosInstance;
  private baseURL: string;
  private userId: string | null = null;

  constructor(baseURL: string = 'http://localhost:8080') {
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
          message: error.response?.data?.message || error.message || 'Unknown error',
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
}

// Singleton instance
export const apiClient = new ApiClient();
