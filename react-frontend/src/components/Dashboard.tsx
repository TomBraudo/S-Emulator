import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { userService } from '../api/services/userService';
import { programService } from '../api/services/programService';
import { UserInfo, Statistic, ProgramInfo } from '../types/api';

// Component interfaces
interface UserCardProps {
  user: UserInfo;
  isSelected: boolean;
  onClick: () => void;
}

interface StatisticCardProps {
  statistic: Statistic;
  onExecute: (variables: any[]) => void;
}

interface ProgramCardProps {
  program: ProgramInfo;
  isSelected: boolean;
  isHighlighted: boolean;
  onClick: () => void;
}

interface FunctionCardProps {
  function: ProgramInfo;
  isSelected: boolean;
  isHighlighted: boolean;
  onClick: () => void;
}

interface ChargeCreditsModalProps {
  isOpen: boolean;
  onClose: () => void;
  onCharge: (credits: number) => void;
}

interface VariablesViewerProps {
  isOpen: boolean;
  onClose: () => void;
  statistic: Statistic | null;
  onExecute: (variables: any[]) => void;
}

// User Card Component
const UserCard: React.FC<UserCardProps> = ({ user, isSelected, onClick }) => (
  <div
    className={`p-4 rounded-lg border-2 cursor-pointer transition-all duration-200 ${
      isSelected 
        ? 'border-blue-500 bg-blue-50 shadow-md' 
        : 'border-gray-200 bg-white hover:border-gray-300 hover:shadow-sm'
    }`}
    onClick={onClick}
  >
    <div className="space-y-2">
      <div className="flex justify-between">
        <span className="font-semibold text-gray-900">User:</span>
        <span className="text-gray-700">{user.name}</span>
      </div>
      <div className="flex justify-between">
        <span className="text-sm text-gray-600">Programs Uploaded:</span>
        <span className="text-sm text-gray-700">{user.programUploadedCount}</span>
      </div>
      <div className="flex justify-between">
        <span className="text-sm text-gray-600">Functions Uploaded:</span>
        <span className="text-sm text-gray-700">{user.functionUploadedCount}</span>
      </div>
      <div className="flex justify-between">
        <span className="text-sm text-gray-600">Credits:</span>
        <span className="text-sm text-gray-700">{user.credits}</span>
      </div>
      <div className="flex justify-between">
        <span className="text-sm text-gray-600">Credits Used:</span>
        <span className="text-sm text-gray-700">{user.creditsUsed}</span>
      </div>
      <div className="flex justify-between">
        <span className="text-sm text-gray-600">Runs:</span>
        <span className="text-sm text-gray-700">{user.runCount}</span>
      </div>
    </div>
  </div>
);

// Statistic Card Component
const StatisticCard: React.FC<StatisticCardProps> = ({ statistic, onExecute }) => (
  <div className="p-4 rounded-lg border border-gray-200 bg-white hover:shadow-sm transition-shadow duration-200">
    <div className="grid grid-cols-2 gap-2 text-sm">
      <div className="flex justify-between">
        <span className="font-semibold text-gray-900">Index:</span>
        <span className="text-gray-700">{statistic.index}</span>
      </div>
      <div className="flex justify-between">
        <span className="font-semibold text-gray-900">Run Type:</span>
        <span className="text-gray-700">{statistic.runType}</span>
      </div>
      <div className="flex justify-between col-span-2">
        <span className="font-semibold text-gray-900">Name:</span>
        <span className="text-gray-700 truncate ml-2">{statistic.programName}</span>
      </div>
      <div className="flex justify-between">
        <span className="font-semibold text-gray-900">Architecture:</span>
        <span className="text-gray-700">{statistic.architecture}</span>
      </div>
      <div className="flex justify-between">
        <span className="font-semibold text-gray-900">Level:</span>
        <span className="text-gray-700">{statistic.expansionLevel}</span>
      </div>
      <div className="flex justify-between">
        <span className="font-semibold text-gray-900">Result:</span>
        <span className="text-gray-700">{statistic.result}</span>
      </div>
      <div className="flex justify-between">
        <span className="font-semibold text-gray-900">Cycles:</span>
        <span className="text-gray-700">{statistic.cyclesCount}</span>
      </div>
    </div>
    <button
      onClick={() => onExecute(statistic.variableToValue)}
      className="mt-3 w-full bg-blue-500 hover:bg-blue-600 text-white px-3 py-1 rounded text-sm transition-colors duration-200"
    >
      View Variables & Execute
    </button>
  </div>
);

// Program Card Component
const ProgramCard: React.FC<ProgramCardProps> = ({ program, isSelected, isHighlighted, onClick }) => (
  <div
    className={`p-4 rounded-lg border-2 cursor-pointer transition-all duration-200 ${
      isSelected 
        ? 'border-blue-500 bg-blue-50 shadow-md' 
        : isHighlighted
        ? 'border-red-500 bg-red-50 shadow-md'
        : 'border-gray-200 bg-white hover:border-gray-300 hover:shadow-sm'
    }`}
    onClick={onClick}
  >
    <div className="space-y-2">
      <div className="flex justify-between">
        <span className="font-semibold text-gray-900">Program Name:</span>
        <span className="text-gray-700 truncate ml-2">{program.name}</span>
      </div>
      <div className="flex justify-between">
        <span className="text-sm text-gray-600">Uploader Username:</span>
        <span className="text-sm text-gray-700">{program.owner}</span>
      </div>
      <div className="flex justify-between">
        <span className="text-sm text-gray-600">Commands Count:</span>
        <span className="text-sm text-gray-700">{program.commandsCount}</span>
      </div>
      <div className="flex justify-between">
        <span className="text-sm text-gray-600">Max Expansion Level:</span>
        <span className="text-sm text-gray-700">{program.maxLevel}</span>
      </div>
      <div className="flex justify-between">
        <span className="text-sm text-gray-600">Ran Count:</span>
        <span className="text-sm text-gray-700">{program.ranCount}</span>
      </div>
      <div className="flex justify-between">
        <span className="text-sm text-gray-600">Average Credit Cost:</span>
        <span className="text-sm text-gray-700">{program.averageCost}</span>
      </div>
    </div>
  </div>
);

// Function Card Component
const FunctionCard: React.FC<FunctionCardProps> = ({ function: func, isSelected, isHighlighted, onClick }) => (
  <div
    className={`p-4 rounded-lg border-2 cursor-pointer transition-all duration-200 ${
      isSelected 
        ? 'border-blue-500 bg-blue-50 shadow-md' 
        : isHighlighted
        ? 'border-red-500 bg-red-50 shadow-md'
        : 'border-gray-200 bg-white hover:border-gray-300 hover:shadow-sm'
    }`}
    onClick={onClick}
  >
    <div className="space-y-2">
      <div className="flex justify-between">
        <span className="font-semibold text-gray-900">Function Name:</span>
        <span className="text-gray-700 truncate ml-2">{func.name}</span>
      </div>
      <div className="flex justify-between">
        <span className="text-sm text-gray-600">Source Program:</span>
        <span className="text-sm text-gray-700">{func.sourceProgram || 'N/A'}</span>
      </div>
      <div className="flex justify-between">
        <span className="text-sm text-gray-600">Uploader Username:</span>
        <span className="text-sm text-gray-700">{func.owner}</span>
      </div>
      <div className="flex justify-between">
        <span className="text-sm text-gray-600">Commands Count:</span>
        <span className="text-sm text-gray-700">{func.commandsCount}</span>
      </div>
      <div className="flex justify-between">
        <span className="text-sm text-gray-600">Max Expansion Level:</span>
        <span className="text-sm text-gray-700">{func.maxLevel}</span>
      </div>
    </div>
  </div>
);

// Charge Credits Modal Component
const ChargeCreditsModal: React.FC<ChargeCreditsModalProps> = ({ isOpen, onClose, onCharge }) => {
  const [credits, setCredits] = useState('');
  const [error, setError] = useState('');

  const handleCharge = () => {
    const creditsNum = parseInt(credits);
    if (!credits || isNaN(creditsNum) || creditsNum <= 0) {
      setError('Please enter a valid number greater than 0');
      return;
    }
    onCharge(creditsNum);
    setCredits('');
    setError('');
  };

  const handleCancel = () => {
    setCredits('');
    setError('');
    onClose();
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg p-6 w-96 max-w-md mx-4">
        <h2 className="text-xl font-semibold text-gray-900 mb-4">Charge Credits</h2>
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Enter amount of credits
            </label>
            <input
              type="number"
              value={credits}
              onChange={(e) => setCredits(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="Enter amount"
            />
            {error && <p className="text-red-500 text-sm mt-1">{error}</p>}
          </div>
          <div className="flex space-x-3">
            <button
              onClick={handleCharge}
              className="flex-1 bg-blue-500 hover:bg-blue-600 text-white px-4 py-2 rounded-md transition-colors duration-200"
            >
              Charge
            </button>
            <button
              onClick={handleCancel}
              className="flex-1 bg-gray-300 hover:bg-gray-400 text-gray-700 px-4 py-2 rounded-md transition-colors duration-200"
            >
              Cancel
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

// Helper function to extract x variables (moved outside component for JSX access)
const extractXVariables = (variables: any[]): number[] => {
  const indexToValue = new Map<number, number>();
  let maxIndex = 0;
  
  // Extract x variables (x1, x2, x3, etc.) and map them to their indices
  for (const variable of variables) {
    const name = variable.variable;
    if (name && name.startsWith('x')) {
      try {
        const idx = parseInt(name.substring(1));
        maxIndex = Math.max(maxIndex, idx);
        indexToValue.set(idx, variable.value);
      } catch (e) {
        // Ignore invalid x variable names
      }
    }
  }
  
  // Create ordered array where position corresponds to x variable index
  const input: number[] = [];
  for (let i = 1; i <= maxIndex; i++) {
    input.push(indexToValue.get(i) || 0);
  }
  
  return input;
};

// Variables Viewer Component
const VariablesViewer: React.FC<VariablesViewerProps> = ({ isOpen, onClose, statistic, onExecute }) => {
  if (!isOpen || !statistic) return null;

  const variables = statistic.variableToValue || [];

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg w-full max-w-2xl max-h-[80vh] flex flex-col">
        {/* Header */}
        <div className="flex justify-between items-center p-6 border-b border-gray-200">
          <div>
            <h2 className="text-xl font-semibold text-gray-900">
              Variables - Run #{statistic.index}
            </h2>
            <p className="text-sm text-gray-600 mt-1">
              {statistic.programName} • {statistic.runType} • Level {statistic.expansionLevel}
            </p>
          </div>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 transition-colors duration-200"
          >
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto p-6">
          {variables.length === 0 ? (
            <div className="text-center py-8">
              <div className="text-gray-400 mb-2">
                <svg className="w-12 h-12 mx-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
              </div>
              <p className="text-gray-500">No variables recorded for this run</p>
            </div>
          ) : (
            <div className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {variables.map((variable, index) => {
                  const isXVariable = variable.variable.startsWith('x');
                  return (
                    <div
                      key={index}
                      className={`rounded-lg p-4 border-2 ${
                        isXVariable 
                          ? 'bg-blue-50 border-blue-200' 
                          : 'bg-gray-50 border-gray-200'
                      }`}
                    >
                      <div className="flex justify-between items-center">
                        <div className="flex items-center space-x-2">
                          <span className="font-mono text-sm font-semibold text-gray-900">
                            {variable.variable}
                          </span>
                          {isXVariable && (
                            <span className="px-2 py-1 text-xs font-semibold text-blue-700 bg-blue-100 rounded-full">
                              Input
                            </span>
                          )}
                        </div>
                        <span className={`text-lg font-bold ${
                          isXVariable ? 'text-blue-600' : 'text-gray-600'
                        }`}>
                          {variable.value}
                        </span>
                      </div>
                    </div>
                  );
                })}
              </div>
              
              {/* X Variables Input Array */}
              {(() => {
                const xVariables = variables.filter(v => v.variable.startsWith('x'));
                const inputArray = extractXVariables(variables);
                if (xVariables.length > 0) {
                  return (
                    <div className="mt-6 p-4 bg-green-50 rounded-lg border border-green-200">
                      <h3 className="font-semibold text-green-900 mb-3">Input Array for Execution</h3>
                      <div className="space-y-2">
                        <p className="text-sm text-green-700">
                          The following x variables will be used as input array:
                        </p>
                        <div className="flex flex-wrap gap-2">
                          {inputArray.map((value: number, index: number) => (
                            <div
                              key={index}
                              className="px-3 py-1 bg-green-100 text-green-800 rounded-md font-mono text-sm"
                            >
                              [{index + 1}]: {value}
                            </div>
                          ))}
                        </div>
                        <p className="text-xs text-green-600 mt-2">
                          Array length: {inputArray.length} • Missing indices filled with 0
                        </p>
                      </div>
                    </div>
                  );
                }
                return null;
              })()}

              {/* Summary */}
              <div className="mt-6 p-4 bg-blue-50 rounded-lg border border-blue-200">
                <h3 className="font-semibold text-blue-900 mb-2">Run Summary</h3>
                <div className="grid grid-cols-2 gap-4 text-sm">
                  <div>
                    <span className="text-blue-700">Architecture:</span>
                    <span className="ml-2 font-semibold text-blue-900">{statistic.architecture}</span>
                  </div>
                  <div>
                    <span className="text-blue-700">Result:</span>
                    <span className="ml-2 font-semibold text-blue-900">{statistic.result}</span>
                  </div>
                  <div>
                    <span className="text-blue-700">Cycles:</span>
                    <span className="ml-2 font-semibold text-blue-900">{statistic.cyclesCount}</span>
                  </div>
                  <div>
                    <span className="text-blue-700">Variables:</span>
                    <span className="ml-2 font-semibold text-blue-900">{variables.length}</span>
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="p-6 border-t border-gray-200 bg-gray-50 rounded-b-lg">
          <div className="flex justify-between items-center">
            <div className="text-sm text-gray-600">
              {variables.length > 0 ? `${variables.length} variables recorded` : 'No variables available'}
            </div>
            <div className="flex space-x-3">
              <button
                onClick={onClose}
                className="px-4 py-2 text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 transition-colors duration-200"
              >
                Close
              </button>
              {variables.length > 0 && (
                <button
                  onClick={() => onExecute(variables)}
                  className="px-4 py-2 bg-blue-500 hover:bg-blue-600 text-white rounded-md transition-colors duration-200"
                >
                  Execute with These Variables
                </button>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

const Dashboard: React.FC = () => {
  const navigate = useNavigate();
  const { userId, logout } = useAuth();
  
  // State management
  const [users, setUsers] = useState<UserInfo[]>([]);
  const [statistics, setStatistics] = useState<Statistic[]>([]);
  const [programs, setPrograms] = useState<ProgramInfo[]>([]);
  const [functions, setFunctions] = useState<ProgramInfo[]>([]);
  const [credits, setCredits] = useState<number>(0);
  const [selectedUserId, setSelectedUserId] = useState<string | null>(null);
  const [selectedProgram, setSelectedProgram] = useState<string | null>(null);
  const [selectedFunction, setSelectedFunction] = useState<string | null>(null);
  const [highlightedPrograms, setHighlightedPrograms] = useState<Set<string>>(new Set());
  const [highlightedFunctions, setHighlightedFunctions] = useState<Set<string>>(new Set());
  const [showChargeModal, setShowChargeModal] = useState(false);
  const [showVariablesViewer, setShowVariablesViewer] = useState(false);
  const [selectedStatistic, setSelectedStatistic] = useState<Statistic | null>(null);
  const [loading, setLoading] = useState(true);
  const [lastUpdated, setLastUpdated] = useState<Date>(new Date());

  // Load data functions
  const loadUsers = useCallback(async () => {
    try {
      const response = await userService.getAllUsers();
      if (response.success) {
        setUsers(response.data);
        // Set current user's credits
        const currentUser = response.data.find((user: UserInfo) => user.name === userId);
        if (currentUser) {
          setCredits(currentUser.credits);
        }
      }
    } catch (error: any) {
      console.error('Failed to load users:', error);
      const errorMessage = error.message || error.toString();
      console.error('Error details:', errorMessage);
    }
  }, [userId]);

  const loadStatistics = useCallback(async (userId: string) => {
    try {
      const response = await userService.getUserStatistics(userId);
      if (response.success) {
        setStatistics(response.data);
      }
    } catch (error: any) {
      console.error('Failed to load statistics:', error);
      const errorMessage = error.message || error.toString();
      console.error('Error details:', errorMessage);
    }
  }, []);

  const loadPrograms = useCallback(async () => {
    try {
      const response = await programService.getPrograms();
      if (response.success) {
        setPrograms(response.data);
      }
    } catch (error: any) {
      console.error('Failed to load programs:', error);
      const errorMessage = error.message || error.toString();
      console.error('Error details:', errorMessage);
    }
  }, []);

  const loadFunctions = useCallback(async () => {
    try {
      const response = await programService.getFunctions();
      if (response.success) {
        setFunctions(response.data);
      }
    } catch (error: any) {
      console.error('Failed to load functions:', error);
      const errorMessage = error.message || error.toString();
      console.error('Error details:', errorMessage);
    }
  }, []);

  // Load dependencies for highlighting
  const loadDependencies = useCallback(async (name: string, isProgram: boolean) => {
    try {
      const response = await programService.getFunctionChain(name);
      if (response.success) {
        if (isProgram) {
          setHighlightedFunctions(new Set(response.data));
          setHighlightedPrograms(new Set());
        } else {
          setHighlightedFunctions(new Set(response.data));
          // For functions, we might also need to highlight programs that use this function
          // This would require an additional API call if available
        }
      }
    } catch (error: any) {
      console.error('Failed to load dependencies:', error);
      const errorMessage = error.message || error.toString();
      console.error('Error details:', errorMessage);
    }
  }, []);

  // Initial load
  useEffect(() => {
    const loadAllData = async () => {
      setLoading(true);
      await Promise.all([
        loadUsers(),
        loadPrograms(),
        loadFunctions()
      ]);
      // Load current user's statistics by default
      if (userId) {
        setSelectedUserId(userId);
        await loadStatistics(userId);
      }
      setLastUpdated(new Date());
      setLoading(false);
    };
    loadAllData();
  }, [userId, loadUsers, loadPrograms, loadFunctions, loadStatistics]);

  // Polling effect - refresh data every second like the Java client
  useEffect(() => {
    const interval = setInterval(() => {
      // Only poll if not loading to avoid conflicts
      if (!loading) {
        loadUsers();
        loadPrograms();
        loadFunctions();
        setLastUpdated(new Date());
      }
    }, 1000); // Poll every second

    return () => clearInterval(interval);
  }, [loading, loadUsers, loadPrograms, loadFunctions]);

  // Event handlers
  const handleUserSelect = (user: UserInfo) => {
    setSelectedUserId(user.name);
    loadStatistics(user.name);
  };

  const handleUnselectUser = () => {
    if (userId) {
      setSelectedUserId(userId);
      loadStatistics(userId);
    }
  };

  const handleProgramSelect = (program: ProgramInfo) => {
    setSelectedProgram(program.name);
    setSelectedFunction(null);
    loadDependencies(program.name, true);
  };

  const handleFunctionSelect = (func: ProgramInfo) => {
    setSelectedFunction(func.name);
    setSelectedProgram(null);
    loadDependencies(func.name, false);
  };

  const handleStatisticExecute = (variables: any[]) => {
    // Show the variables viewer instead of directly executing
    setSelectedStatistic(statistics.find(stat => 
      stat.variableToValue === variables
    ) || null);
    setShowVariablesViewer(true);
  };


  const handleExecuteWithVariables = async (variables: any[]) => {
    if (!selectedStatistic) return;
    
    try {
      // Step 1: Set the current program on the server using API service
      const setProgramResponse = await programService.setProgram(selectedStatistic.programName);
      
      if (!setProgramResponse.success) {
        throw new Error(`Failed to set program: ${setProgramResponse.message}`);
      }
      
      // Step 2: Extract x variables from historical data
      const input = extractXVariables(variables);
      
      // Step 3: Parse architecture string to integer
      const architecture = parseArchitecture(selectedStatistic.architecture);
      
      // Step 4: Perform runnability check
      const runnabilityResponse = await programService.checkRunnability(
        selectedStatistic.expansionLevel,
        architectureCode(architecture)
      );
      
      if (!runnabilityResponse.success) {
        throw new Error(`Program cannot run: ${runnabilityResponse.message}`);
      }
      
      // Step 5: Execute with historical parameters
      const response = await programService.executeProgram({
        expansionLevel: selectedStatistic.expansionLevel,
        architecture: architectureCode(architecture),
        input: input
      });
      
      if (response.success) {
        const result = response.data;
        
        // Step 6: Handle different halt reasons
        if (result.haltReason === 'INSUFFICIENT_CREDITS') {
          // This is not a failure - show warning but continue with success flow
          alert('⚠️ Program execution halted due to insufficient credits. Partial results may be available.');
        } else if (result.haltReason === 'STOPPED_MANUALLY') {
          alert('Program execution was stopped manually.');
        }
        
        // Step 7: Update credits display
        await loadUsers();
        
        // Step 8: Refresh statistics if currently selected user is the logged-in user
        if (selectedUserId === userId && userId) {
          await loadStatistics(userId);
        }
        
        // Show success message with result details
        const haltReasonText = result.haltReason === 'INSUFFICIENT_CREDITS' ? ' (halted due to insufficient credits)' : '';
        alert(`Program executed successfully${haltReasonText}!\nResult: ${result.result}\nCycles: ${result.cycles}`);
      } else {
        alert(`Execution failed: ${response.message}`);
      }
    } catch (error: any) {
      console.error('Failed to execute with historical variables:', error);
      const errorMessage = error.message || error.toString();
      alert(`Failed to execute with historical variables: ${errorMessage}`);
    }
    
    setShowVariablesViewer(false);
    setSelectedStatistic(null);
  };

  const parseArchitecture = (arch: string): number => {
    if (!arch || arch.trim() === '') return 1;
    switch (arch.trim()) {
      case 'I': return 1;
      case 'II': return 2;
      case 'III': return 3;
      case 'IV': return 4;
      default: return 1;
    }
  };

  const architectureCode = (selected: number): string => {
    switch (selected) {
      case 1: return 'I';
      case 2: return 'II';
      case 3: return 'III';
      case 4: return 'IV';
      default: return 'I';
    }
  };

  const handleChargeCredits = async (creditsToAdd: number) => {
    try {
      const response = await userService.addCredits(creditsToAdd);
      if (response.success) {
        setCredits(prev => prev + creditsToAdd);
        setShowChargeModal(false);
        // Refresh users to get updated data
        await loadUsers();
      }
    } catch (error: any) {
      console.error('Failed to charge credits:', error);
      const errorMessage = error.message || error.toString();
      alert(`Failed to charge credits: ${errorMessage}`);
    }
  };

  const handleExecuteProgram = () => {
    if (!selectedProgram) {
      alert('Please select a program to execute');
      return;
    }
    navigate(`/execute/program/${encodeURIComponent(selectedProgram)}`);
  };

  const handleExecuteFunction = () => {
    if (!selectedFunction) {
      alert('Please select a function to execute');
      return;
    }
    navigate(`/execute/function/${encodeURIComponent(selectedFunction)}`);
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-500 mx-auto"></div>
          <p className="mt-4 text-gray-600">Loading dashboard...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white shadow-sm border-b border-gray-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center h-16">
            <div className="flex items-center">
              <h1 className="text-2xl font-bold text-gray-900">S-Emulator Dashboard</h1>
            </div>
            <div className="flex items-center space-x-4">
              <div className="flex items-center space-x-2">
                <span className="text-sm text-gray-700">Available Credits:</span>
                <span className="text-sm font-semibold text-gray-900">{credits}</span>
                <button
                  onClick={() => setShowChargeModal(true)}
                  className="bg-blue-500 hover:bg-blue-600 text-white px-3 py-1 rounded text-sm transition-colors duration-200"
                >
                  Charge Credits
                </button>
              </div>
              <div className="text-xs text-gray-500">
                Last updated: {lastUpdated.toLocaleTimeString()}
              </div>
              <span className="text-sm text-gray-700">Welcome, <span className="font-semibold">{userId}</span></span>
              <button
                onClick={logout}
                className="bg-red-500 hover:bg-red-600 text-white px-4 py-2 rounded-lg text-sm font-medium transition-colors duration-200"
              >
                Logout
              </button>
            </div>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
        <div className="px-4 py-6 sm:px-0">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Left Column */}
            <div className="space-y-6">
              {/* Users Section */}
              <div className="bg-white rounded-lg shadow p-6">
                <div className="flex justify-between items-center mb-4">
                  <h2 className="text-lg font-semibold text-gray-900">Users</h2>
                  <button
                    onClick={handleUnselectUser}
                    className="text-sm text-gray-500 hover:text-gray-700"
                  >
                    Unselect User
                  </button>
                </div>
                <div className="space-y-3 max-h-64 overflow-y-auto">
                  {users.map((user) => (
                    <UserCard
                      key={user.name}
                      user={user}
                      isSelected={selectedUserId === user.name}
                      onClick={() => handleUserSelect(user)}
                    />
                  ))}
                </div>
              </div>

              {/* Statistics Section */}
              <div className="bg-white rounded-lg shadow p-6">
                <h2 className="text-lg font-semibold text-gray-900 mb-4">Statistics</h2>
                <div className="space-y-3 max-h-64 overflow-y-auto">
                  {statistics.map((statistic) => (
                    <StatisticCard
                      key={statistic.index}
                      statistic={statistic}
                      onExecute={handleStatisticExecute}
                    />
                  ))}
                </div>
              </div>
            </div>

            {/* Right Column */}
            <div className="space-y-6">
              {/* Programs Section */}
              <div className="bg-white rounded-lg shadow p-6">
                <div className="flex justify-between items-center mb-4">
                  <h2 className="text-lg font-semibold text-gray-900">Programs</h2>
                  <button
                    onClick={handleExecuteProgram}
                    disabled={!selectedProgram}
                    className="px-4 py-2 bg-blue-500 hover:bg-blue-600 disabled:bg-gray-300 disabled:cursor-not-allowed text-white text-sm font-medium rounded-md transition-colors duration-200"
                  >
                    Execute Program
                  </button>
                </div>
                <div className="space-y-3 max-h-64 overflow-y-auto">
                  {programs.map((program) => (
                    <ProgramCard
                      key={program.name}
                      program={program}
                      isSelected={selectedProgram === program.name}
                      isHighlighted={highlightedPrograms.has(program.name)}
                      onClick={() => handleProgramSelect(program)}
                    />
                  ))}
                </div>
              </div>

              {/* Functions Section */}
              <div className="bg-white rounded-lg shadow p-6">
                <div className="flex justify-between items-center mb-4">
                  <h2 className="text-lg font-semibold text-gray-900">Functions</h2>
                  <button
                    onClick={handleExecuteFunction}
                    disabled={!selectedFunction}
                    className="px-4 py-2 bg-green-500 hover:bg-green-600 disabled:bg-gray-300 disabled:cursor-not-allowed text-white text-sm font-medium rounded-md transition-colors duration-200"
                  >
                    Execute Function
                  </button>
                </div>
                <div className="space-y-3 max-h-64 overflow-y-auto">
                  {functions.map((func) => (
                    <FunctionCard
                      key={func.name}
                      function={func}
                      isSelected={selectedFunction === func.name}
                      isHighlighted={highlightedFunctions.has(func.name)}
                      onClick={() => handleFunctionSelect(func)}
                    />
                  ))}
                </div>
              </div>
            </div>
          </div>
        </div>
      </main>

      {/* Charge Credits Modal */}
      <ChargeCreditsModal
        isOpen={showChargeModal}
        onClose={() => setShowChargeModal(false)}
        onCharge={handleChargeCredits}
      />

      {/* Variables Viewer Modal */}
      <VariablesViewer
        isOpen={showVariablesViewer}
        onClose={() => {
          setShowVariablesViewer(false);
          setSelectedStatistic(null);
        }}
        statistic={selectedStatistic}
        onExecute={handleExecuteWithVariables}
      />
    </div>
  );
};

export default Dashboard;
