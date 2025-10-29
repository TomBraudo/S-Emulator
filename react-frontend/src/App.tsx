import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './contexts/AuthContext';
import LoginPage from './components/LoginPage';
import Dashboard from './components/Dashboard';
import ExecutionPage from './components/ExecutionPage';

function App() {
  return (
    <AuthProvider>
      <Router>
        <div className="App">
          <Routes>
            {/* Login Route */}
            <Route path="/login" element={<LoginPage />} />
            
            {/* Dashboard Route */}
            <Route path="/dashboard" element={<Dashboard />} />
            
            {/* Execution Routes */}
            <Route path="/execute/program/:programName" element={<ExecutionPage />} />
            <Route path="/execute/function/:functionName" element={<ExecutionPage />} />
            
            {/* Default redirect to login */}
            <Route path="/" element={<Navigate to="/login" replace />} />
            
            {/* Catch all - redirect to login */}
            <Route path="*" element={<Navigate to="/login" replace />} />
          </Routes>
        </div>
      </Router>
    </AuthProvider>
  );
}

export default App;