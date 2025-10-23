import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { programService } from '../api/services/programService';
import { userService } from '../api/services/userService';

const ExecutionPage: React.FC = () => {
  const { programName, functionName } = useParams<{
    programName?: string;
    functionName?: string;
  }>();
  const navigate = useNavigate();
  const { userId } = useAuth();
  const [credits, setCredits] = useState<number>(0);
  const [loading, setLoading] = useState<boolean>(true);
  const [executionMode, setExecutionMode] = useState<'execute' | 'debug'>('execute');
  const [selectedArchitecture, setSelectedArchitecture] = useState<number>(1);
  const [expansionLevel, setExpansionLevel] = useState<number>(0);

  // Load initial data
  useEffect(() => {
    const loadInitialData = async () => {
      try {
        if (userId) {
          const response = await userService.getCredits();
          if (response.success) {
            setCredits(response.data);
          }
        }
      } catch (error: any) {
        console.error('Failed to load credits:', error);
      } finally {
        setLoading(false);
      }
    };

    loadInitialData();
  }, [userId]);

  const handleExecuteProgram = async () => {
    if (!programName) {
      alert('No program selected for execution');
      return;
    }

    try {
      // Set the program on the server
      const setProgramResponse = await programService.setProgram(programName);
      if (!setProgramResponse.success) {
        throw new Error(`Failed to set program: ${setProgramResponse.message}`);
      }

      // Check runnability
      const runnabilityResponse = await programService.checkRunnability(
        expansionLevel,
        architectureCode(selectedArchitecture)
      );

      if (!runnabilityResponse.success) {
        throw new Error(`Program cannot run: ${runnabilityResponse.message}`);
      }

      // For now, just show a placeholder message
      alert(`Execute Program: ${programName}\nArchitecture: ${architectureCode(selectedArchitecture)}\nExpansion Level: ${expansionLevel}\n\nExecution interface will be implemented here.`);
    } catch (error: any) {
      console.error('Failed to execute program:', error);
      const errorMessage = error.message || error.toString();
      alert(`Failed to execute program: ${errorMessage}`);
    }
  };

  const handleExecuteFunction = async () => {
    if (!functionName) {
      alert('No function selected for execution');
      return;
    }

    try {
      // Set the function on the server (functions are also programs)
      const setProgramResponse = await programService.setProgram(functionName);
      if (!setProgramResponse.success) {
        throw new Error(`Failed to set function: ${setProgramResponse.message}`);
      }

      // Check runnability
      const runnabilityResponse = await programService.checkRunnability(
        expansionLevel,
        architectureCode(selectedArchitecture)
      );

      if (!runnabilityResponse.success) {
        throw new Error(`Function cannot run: ${runnabilityResponse.message}`);
      }

      // For now, just show a placeholder message
      alert(`Execute Function: ${functionName}\nArchitecture: ${architectureCode(selectedArchitecture)}\nExpansion Level: ${expansionLevel}\n\nExecution interface will be implemented here.`);
    } catch (error: any) {
      console.error('Failed to execute function:', error);
      const errorMessage = error.message || error.toString();
      alert(`Failed to execute function: ${errorMessage}`);
    }
  };

  const architectureCode = (arch: number): string => {
    switch (arch) {
      case 1: return 'I';
      case 2: return 'II';
      case 3: return 'III';
      case 4: return 'IV';
      default: return 'I';
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-500 mx-auto mb-4"></div>
          <p className="text-gray-600">Loading execution page...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <div className="bg-white shadow-sm border-b border-gray-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center h-16">
            <div className="flex items-center space-x-4">
              <h1 className="text-2xl font-bold text-gray-900">
                S-Emulator - Execution
              </h1>
              <div className="text-sm text-gray-600">
                {programName ? `Program: ${programName}` : `Function: ${functionName}`}
              </div>
            </div>
            
            <div className="flex items-center space-x-4">
              <div className="text-sm text-gray-600">
                Credits: <span className="font-semibold text-blue-600">{credits}</span>
              </div>
              <button
                onClick={() => navigate('/dashboard')}
                className="px-4 py-2 bg-gray-600 hover:bg-gray-700 text-white rounded-md transition-colors duration-200"
              >
                Back to Dashboard
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Main Content */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          
          {/* Left Panel - Program/Function Info */}
          <div className="lg:col-span-1">
            <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
              <h2 className="text-lg font-semibold text-gray-900 mb-4">
                {programName ? 'Program Information' : 'Function Information'}
              </h2>
              
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Name
                  </label>
                  <div className="text-sm text-gray-900 bg-gray-50 px-3 py-2 rounded-md">
                    {programName || functionName}
                  </div>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Type
                  </label>
                  <div className="text-sm text-gray-900 bg-gray-50 px-3 py-2 rounded-md">
                    {programName ? 'Program' : 'Helper Function'}
                  </div>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Expansion Level
                  </label>
                  <select
                    value={expansionLevel}
                    onChange={(e) => setExpansionLevel(parseInt(e.target.value))}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                  >
                    <option value={0}>Level 0</option>
                    <option value={1}>Level 1</option>
                    <option value={2}>Level 2</option>
                    <option value={3}>Level 3</option>
                    <option value={4}>Level 4</option>
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Architecture
                  </label>
                  <div className="grid grid-cols-4 gap-2">
                    {[1, 2, 3, 4].map((arch) => (
                      <button
                        key={arch}
                        onClick={() => setSelectedArchitecture(arch)}
                        className={`px-3 py-2 text-sm font-medium rounded-md transition-colors duration-200 ${
                          selectedArchitecture === arch
                            ? 'bg-blue-500 text-white'
                            : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                        }`}
                      >
                        {architectureCode(arch)}
                      </button>
                    ))}
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Right Panel - Execution Controls */}
          <div className="lg:col-span-2">
            <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
              <h2 className="text-lg font-semibold text-gray-900 mb-6">
                Execution Controls
              </h2>

              {/* Mode Selection */}
              <div className="mb-6">
                <label className="block text-sm font-medium text-gray-700 mb-3">
                  Execution Mode
                </label>
                <div className="flex space-x-1 bg-gray-100 p-1 rounded-lg w-fit">
                  <button
                    onClick={() => setExecutionMode('execute')}
                    className={`px-4 py-2 text-sm font-medium rounded-md transition-colors duration-200 ${
                      executionMode === 'execute'
                        ? 'bg-white text-blue-600 shadow-sm'
                        : 'text-gray-600 hover:text-gray-900'
                    }`}
                  >
                    Execute
                  </button>
                  <button
                    onClick={() => setExecutionMode('debug')}
                    className={`px-4 py-2 text-sm font-medium rounded-md transition-colors duration-200 ${
                      executionMode === 'debug'
                        ? 'bg-white text-blue-600 shadow-sm'
                        : 'text-gray-600 hover:text-gray-900'
                    }`}
                  >
                    Debug
                  </button>
                </div>
              </div>

              {/* Action Buttons */}
              <div className="space-y-4">
                {executionMode === 'execute' ? (
                  <div className="text-center">
                    <button
                      onClick={programName ? handleExecuteProgram : handleExecuteFunction}
                      className="px-8 py-3 bg-blue-500 hover:bg-blue-600 text-white font-medium rounded-lg transition-colors duration-200 shadow-sm"
                    >
                      {programName ? 'Execute Program' : 'Execute Function'}
                    </button>
                  </div>
                ) : (
                  <div className="text-center">
                    <button
                      onClick={programName ? handleExecuteProgram : handleExecuteFunction}
                      className="px-8 py-3 bg-green-500 hover:bg-green-600 text-white font-medium rounded-lg transition-colors duration-200 shadow-sm"
                    >
                      {programName ? 'Start Debugging Program' : 'Start Debugging Function'}
                    </button>
                  </div>
                )}
              </div>

              {/* Execution Interface Placeholder */}
              <div className="mt-8 p-6 bg-gray-50 rounded-lg border-2 border-dashed border-gray-300">
                <div className="text-center">
                  <div className="text-gray-400 mb-4">
                    <svg className="w-16 h-16 mx-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
                    </svg>
                  </div>
                  <h3 className="text-lg font-medium text-gray-900 mb-2">
                    Execution Interface
                  </h3>
                  <p className="text-gray-600 mb-4">
                    The execution interface will be implemented here. This will include:
                  </p>
                  <ul className="text-sm text-gray-600 text-left space-y-1 max-w-md mx-auto">
                    <li>• Program/Function command display</li>
                    <li>• Input variable configuration</li>
                    <li>• Execution progress tracking</li>
                    <li>• Variable state visualization</li>
                    <li>• Debug controls (step over, continue, etc.)</li>
                    <li>• Results and output display</li>
                  </ul>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ExecutionPage;
