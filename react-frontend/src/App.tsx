import React from 'react';
import { AuthProvider } from './contexts/AuthContext';
import './App.css';

function App() {
  return (
    <AuthProvider>
      <div className="App">
        <header className="App-header">
          <h1>S-Emulator React Frontend</h1>
          <p>API Layer Infrastructure Ready</p>
          <p>Check src/examples/apiUsage.ts for usage examples</p>
        </header>
      </div>
    </AuthProvider>
  );
}

export default App;
