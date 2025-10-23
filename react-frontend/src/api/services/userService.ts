import { apiClient } from '../client';
import { UserInfo, Statistic, ApiResponse } from '../../types/api';

export class UserService {
  // Register user
  async registerUser(): Promise<ApiResponse<void>> {
    return apiClient.post<void>('/user/register');
  }

  // Get all users
  async getAllUsers(): Promise<ApiResponse<UserInfo[]>> {
    return apiClient.get<UserInfo[]>('/user/all');
  }

  // Get user statistics
  async getUserStatistics(userId: string): Promise<ApiResponse<Statistic[]>> {
    return apiClient.get<Statistic[]>('/user/statistics', { user: userId });
  }

  // Get user credits
  async getCredits(): Promise<ApiResponse<number>> {
    return apiClient.get<number>('/user/credits');
  }

  // Add credits to user
  async addCredits(credits: number): Promise<ApiResponse<void>> {
    return apiClient.postFormData<void>('/user/credits', { credits });
  }
}

export const userService = new UserService();
