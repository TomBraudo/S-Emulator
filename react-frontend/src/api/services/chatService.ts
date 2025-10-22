import { apiClient } from '../client';
import { ChatMessage, SendMessageRequest, ApiResponse } from '../../types/api';

export class ChatService {
  // Send message
  async sendMessage(request: SendMessageRequest): Promise<ApiResponse<void>> {
    return apiClient.post<void>('/chat/send', request);
  }

  // Get chat history
  async getChatHistory(): Promise<ApiResponse<ChatMessage[]>> {
    return apiClient.get<ChatMessage[]>('/chat/history');
  }
}

export const chatService = new ChatService();
