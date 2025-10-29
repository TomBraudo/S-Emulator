import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { apiClient } from '../api';

interface AuthContextType {
  userId: string | null;
  isAuthenticated: boolean;
  login: (userId: string) => void;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [userId, setUserId] = useState<string | null>(null);

  useEffect(() => {
    // Set the user ID in the API client whenever it changes
    apiClient.setUserId(userId);
  }, [userId]);

  const login = (newUserId: string) => {
    setUserId(newUserId);
  };

  const logout = () => {
    setUserId(null);
  };

  const value: AuthContextType = {
    userId,
    isAuthenticated: !!userId,
    login,
    logout,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
