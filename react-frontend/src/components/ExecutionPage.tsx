import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { programService } from '../api/services/programService';
import { userService } from '../api/services/userService';
import { ProgramCommands, ProgramSummary, ProgramResult, VariableToValue } from '../types/api';

const ExecutionPage: React.FC = () => {
  const { programName, functionName } = useParams<{
    programName?: string;
    functionName?: string;
  }>();
  const navigate = useNavigate();
  const { userId } = useAuth();
  const [credits, setCredits] = useState<number>(0);
  const [loading, setLoading] = useState<boolean>(true);
  const [selectedArchitecture, setSelectedArchitecture] = useState<number>(1);
  const [expansionLevel, setExpansionLevel] = useState<number>(0);
  const [maxExpansionLevel, setMaxExpansionLevel] = useState<number>(0);
  const [programCommands, setProgramCommands] = useState<ProgramCommands | null>(null);
  const [programSummary, setProgramSummary] = useState<ProgramSummary | null>(null);
  const [inputVariables, setInputVariables] = useState<string[]>([]);
  const [inputValues, setInputValues] = useState<Map<string, number>>(new Map());
  const [customInputVariables, setCustomInputVariables] = useState<string[]>([]);
  const [customInputValues, setCustomInputValues] = useState<Map<string, number>>(new Map());
  const [executionResult, setExecutionResult] = useState<ProgramResult | null>(null);
  const [isExecuting, setIsExecuting] = useState<boolean>(false);
  const [showInputForm, setShowInputForm] = useState<boolean>(false);

  // Load initial data
  useEffect(() => {
    const loadInitialData = async () => {
      try {
        if (userId) {
          // Load credits
          const creditsResponse = await userService.getCredits();
          if (creditsResponse.success) {
            setCredits(creditsResponse.data);
          }

          // Set the program/function
          const programToSet = programName || functionName;
          if (programToSet) {
            const setProgramResponse = await programService.setProgram(programToSet);
            if (!setProgramResponse.success) {
              throw new Error(`Failed to set program: ${setProgramResponse.message}`);
            }
          }

          // Load max expansion level
          const maxLevelResponse = await programService.getMaxLevel();
          if (maxLevelResponse.success) {
            setMaxExpansionLevel(maxLevelResponse.data);
          }

          // Load initial data for expansion level 0
          await loadProgramData(0);
        }
      } catch (error: any) {
        console.error('Failed to load initial data:', error);
        alert(`Failed to load execution page: ${error.message}`);
      } finally {
        setLoading(false);
      }
    };

    loadInitialData();
  }, [userId, programName, functionName]);

  // Load program data for specific expansion level
  const loadProgramData = async (level: number) => {
    try {
      // Load program commands
      const commandsResponse = await programService.getProgramCommands(level);
      if (commandsResponse.success) {
        setProgramCommands(commandsResponse.data);
      }

      // Load program summary
      const summaryResponse = await programService.getProgramSummary(level);
      if (summaryResponse.success) {
        setProgramSummary(summaryResponse.data);
      }

      // Load input variables
      const inputResponse = await programService.getInputVariables();
      if (inputResponse.success) {
        setInputVariables(inputResponse.data);
      }
    } catch (error: any) {
      console.error('Failed to load program data:', error);
    }
  };

  // Handle expansion level change
  const handleExpansionLevelChange = async (level: number) => {
    setExpansionLevel(level);
    await loadProgramData(level);
  };

  // Handle architecture change
  const handleArchitectureChange = (arch: number) => {
    setSelectedArchitecture(arch);
  };

  // Start execution process
  const handleStartExecution = async () => {
    try {
      // Check runnability first
      const runnabilityResponse = await programService.checkRunnability(
        expansionLevel,
        architectureCode(selectedArchitecture)
      );

      if (!runnabilityResponse.success) {
        throw new Error(`Program cannot run: ${runnabilityResponse.message}`);
      }

      // Reset custom variables when opening form
      setCustomInputVariables([]);
      setCustomInputValues(new Map());
      setInputValues(new Map());

      // Show input form
      setShowInputForm(true);
    } catch (error: any) {
      console.error('Failed to start execution:', error);
      alert(`Failed to start execution: ${error.message}`);
    }
  };

  // Execute program with input values
  const handleExecuteWithInput = async (requiredInputMap: Map<string, number>, customInputMap: Map<string, number>) => {
    setIsExecuting(true);
    setShowInputForm(false);
    
    try {
      // Combine required and custom inputs, padding missing indices with zeroes
      const allInputs = new Map<string, number>();
      
      // Add required inputs
      requiredInputMap.forEach((value, key) => {
        allInputs.set(key, value);
      });
      
      // Add custom inputs
      customInputMap.forEach((value, key) => {
        allInputs.set(key, value);
      });
      
      // Convert to array, padding missing indices with zeroes
      const inputArray = createPaddedInputArray(allInputs);
      
      const executeResponse = await programService.executeProgram({
        expansionLevel,
        architecture: architectureCode(selectedArchitecture),
        input: inputArray
      });

      if (executeResponse.success) {
        setExecutionResult(executeResponse.data);
        
        // Update credits
        const newCredits = credits - (executeResponse.data.cycles + architectureToCost(selectedArchitecture));
        setCredits(newCredits);
        
        // Show warning if insufficient credits
        if (executeResponse.data.haltReason === 'INSUFFICIENT_CREDITS') {
          alert('Program execution halted due to insufficient credits, and did not execute completely');
        }
      } else {
        throw new Error(executeResponse.message);
      }
    } catch (error: any) {
      console.error('Failed to execute program:', error);
      alert(`Failed to execute program: ${error.message}`);
    } finally {
      setIsExecuting(false);
    }
  };

  // Cancel input form
  const handleCancelInput = () => {
    setShowInputForm(false);
  };

  // Create padded input array from variable map
  const createPaddedInputArray = (inputMap: Map<string, number>): number[] => {
    const maxIndex = Math.max(
      ...Array.from(inputMap.keys()).map(key => {
        const match = key.match(/^x(\d+)$/);
        return match ? parseInt(match[1]) : 0;
      }),
      -1
    );
    
    const result: number[] = [];
    for (let i = 1; i <= maxIndex; i++) {
      const xKey = `x${i}`;
      const xValue = inputMap.get(xKey);
      
      // Add x value if exists, otherwise 0
      result.push(xValue !== undefined ? xValue : 0);
    }
    
    return result;
  };

  // Add custom input variable
  const addCustomInputVariable = () => {
    const allVariables = [...customInputVariables, ...inputVariables];
    const maxIndex = Math.max(
      ...allVariables.map(key => {
        const match = key.match(/^x(\d+)$/);
        return match ? parseInt(match[1]) : 0;
      }),
      0
    );
    
    const nextIndex = maxIndex + 1;
    const newVariable = `x${nextIndex}`;
    
    setCustomInputVariables(prev => [...prev, newVariable]);
    setCustomInputValues(prev => {
      const newMap = new Map(prev);
      newMap.set(newVariable, 0);
      return newMap;
    });
  };

  // Remove any input variable (required or custom)
  const removeInputVariable = (variable: string) => {
    // Check if it's a required variable
    if (inputVariables.includes(variable)) {
      // Remove from required variables
      setInputVariables(prev => prev.filter(v => v !== variable));
      setInputValues(prev => {
        const newMap = new Map(prev);
        newMap.delete(variable);
        return newMap;
      });
    } else {
      // Remove from custom variables
      setCustomInputVariables(prev => prev.filter(v => v !== variable));
      setCustomInputValues(prev => {
        const newMap = new Map(prev);
        newMap.delete(variable);
        return newMap;
      });
    }
  };

  // Update custom input value
  const updateCustomInputValue = (variable: string, value: number) => {
    setCustomInputValues(prev => {
      const newMap = new Map(prev);
      newMap.set(variable, value);
      return newMap;
    });
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

  const architectureToCost = (arch: number): number => {
    switch (arch) {
      case 1: return 5;
      case 2: return 100;
      case 3: return 500;
      case 4: return 1000;
      default: return 0;
    }
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
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
          
          {/* Left Panel - Program Commands and Summary */}
          <div className="space-y-6">
            {/* Program Commands */}
            <div className="bg-white rounded-lg shadow-sm border border-gray-200">
              <div className="p-4 border-b border-gray-200">
                <div className="flex items-center justify-between">
                  <h2 className="text-lg font-semibold text-gray-900">Program Commands</h2>
                  <div className="flex items-center space-x-4">
                <div>
                      <label className="block text-xs text-gray-500 mb-1">Expansion Level</label>
                  <select
                    value={expansionLevel}
                        onChange={(e) => handleExpansionLevelChange(parseInt(e.target.value))}
                        className="px-3 py-1 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                      >
                        {Array.from({ length: maxExpansionLevel + 1 }, (_, i) => (
                          <option key={i} value={i}>Level {i}</option>
                        ))}
                  </select>
                </div>
                <div>
                      <label className="block text-xs text-gray-500 mb-1">Architecture</label>
                      <div className="flex space-x-1">
                    {[1, 2, 3, 4].map((arch) => (
                      <button
                        key={arch}
                            onClick={() => handleArchitectureChange(arch)}
                            className={`px-2 py-1 text-xs font-medium rounded transition-colors duration-200 ${
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

              <div className="p-4 max-h-96 overflow-y-auto">
                {programCommands ? (
                  <div className="space-y-2">
                    {programCommands.commands.map((command, index) => {
                      const commandArch = parseArchitecture(programCommands.architectures[index]);
                      const isHighlighted = commandArch <= selectedArchitecture;
                      
                      return (
                        <div
                          key={index}
                          className={`p-2 rounded text-sm font-mono ${
                            isHighlighted
                              ? 'bg-blue-50 border-l-4 border-blue-500'
                              : 'bg-gray-50 border-l-4 border-gray-200'
                          }`}
                        >
                          <div className="flex items-center justify-between">
                            <span className="text-gray-600 text-xs w-8">{index}</span>
                            <span className="flex-1 ml-2">{command}</span>
                            <span className="text-xs text-gray-500 ml-2">
                              {programCommands.architectures[index]}
                            </span>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                ) : (
                  <div className="text-center text-gray-500 py-8">
                    <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-500 mx-auto mb-2"></div>
                    Loading commands...
                  </div>
                )}
              </div>
            </div>

            {/* Program Summary */}
            {programSummary && (
              <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-4">
                <h3 className="text-md font-semibold text-gray-900 mb-3">Program Summary</h3>
                <div className="grid grid-cols-4 gap-4 text-sm">
                  <div className="text-center">
                    <div className="font-medium text-gray-900">I</div>
                    <div className="text-gray-600">{programSummary.architectureCommandsCount[0] || 0}</div>
                  </div>
                  <div className="text-center">
                    <div className="font-medium text-gray-900">II</div>
                    <div className="text-gray-600">{programSummary.architectureCommandsCount[1] || 0}</div>
                  </div>
                  <div className="text-center">
                    <div className="font-medium text-gray-900">III</div>
                    <div className="text-gray-600">{programSummary.architectureCommandsCount[2] || 0}</div>
                  </div>
                  <div className="text-center">
                    <div className="font-medium text-gray-900">IV</div>
                    <div className="text-gray-600">{programSummary.architectureCommandsCount[3] || 0}</div>
                  </div>
                </div>
              </div>
            )}
          </div>

          {/* Right Panel - Execution Controls and Results */}
          <div className="space-y-6">
            {/* Execution Controls */}
            <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
              <h2 className="text-lg font-semibold text-gray-900 mb-4">Execution Controls</h2>
              
              <div className="text-center">
                  <button
                  onClick={handleStartExecution}
                  disabled={isExecuting}
                  className="px-8 py-3 bg-blue-500 hover:bg-blue-600 disabled:bg-gray-400 text-white font-medium rounded-lg transition-colors duration-200 shadow-sm"
                >
                  {isExecuting ? 'Executing...' : 'Start Execution'}
                  </button>
              </div>
            </div>

            {/* Input Variables Display */}
            {inputVariables.length > 0 && (
              <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-4">
                <h3 className="text-md font-semibold text-gray-900 mb-3">Input Variables</h3>
                <div className="text-sm text-gray-600">
                  Required variables: {inputVariables.join(', ')}
                </div>
              </div>
            )}

            {/* Execution Results */}
            {executionResult && (
              <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
                <h3 className="text-lg font-semibold text-gray-900 mb-4">Execution Results</h3>
                
              <div className="space-y-4">
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <div className="text-sm font-medium text-gray-700">Cycles</div>
                      <div className="text-lg font-semibold text-blue-600">{executionResult.cycles}</div>
                    </div>
                    <div>
                      <div className="text-sm font-medium text-gray-700">Result</div>
                      <div className="text-lg font-semibold text-green-600">{executionResult.result}</div>
                    </div>
                  </div>

                  {executionResult.variableToValue && executionResult.variableToValue.length > 0 && (
                    <div>
                      <div className="text-sm font-medium text-gray-700 mb-2">Variables</div>
                      <div className="space-y-1">
                        {executionResult.variableToValue.map((variable, index) => (
                          <div key={index} className="flex justify-between text-sm">
                            <span className="font-mono">{variable.variable}</span>
                            <span className="font-semibold">{variable.value}</span>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}

                  {executionResult.haltReason && executionResult.haltReason !== 'FINISHED' && (
                    <div className="p-3 bg-yellow-50 border border-yellow-200 rounded-md">
                      <div className="text-sm text-yellow-800">
                        <strong>Halt Reason:</strong> {executionResult.haltReason.replace('_', ' ').toLowerCase()}
                      </div>
                    </div>
                  )}
                </div>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Input Form Modal */}
      {showInputForm && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg shadow-xl max-w-lg w-full mx-4 max-h-[90vh] overflow-y-auto">
            <div className="p-6">
              <h3 className="text-lg font-semibold text-gray-900 mb-4">Input Variables</h3>
              
              <div className="mb-6">
                <div className="text-sm text-gray-600 mb-4">
                  Required variables: {inputVariables.length > 0 ? inputVariables.join(', ') : 'None'}
                </div>
                
                {/* Input Array Preview */}
                <div className="mb-4 p-3 bg-gray-50 rounded-md">
                  <div className="text-sm font-medium text-gray-700 mb-2">Input Array Preview:</div>
                  <div className="text-xs font-mono text-gray-600">
                    [{createPaddedInputArray(new Map([...Array.from(inputValues.entries()), ...Array.from(customInputValues.entries())])).join(', ')}]
                  </div>
                </div>
                
                {/* All Input Variables */}
                <div className="space-y-3">
                  <div className="flex items-center justify-between">
                    <h4 className="text-md font-medium text-gray-800">Input Variables</h4>
                    <button
                      onClick={addCustomInputVariable}
                      className="px-3 py-1 bg-green-500 hover:bg-green-600 text-white text-sm rounded-md transition-colors duration-200 flex items-center space-x-1"
                    >
                      <span>+</span>
                      <span>Add Variable</span>
                    </button>
                  </div>
                  
                  {inputVariables.length === 0 && customInputVariables.length === 0 && (
                    <div className="text-sm text-gray-500 italic text-center py-4">
                      No variables added. Click "Add Variable" to add more.
                    </div>
                  )}
                  
                  {/* Required Variables */}
                  {inputVariables.map((variable) => (
                    <div key={variable} className="flex items-center space-x-2">
                      <div className="flex-1">
                        <label className="block text-sm font-medium text-gray-700 mb-1">
                          {variable} <span className="text-xs text-gray-500">(required)</span>
                        </label>
                        <input
                          type="number"
                          value={inputValues.get(variable) || ''}
                          onChange={(e) => {
                            const newValues = new Map(inputValues);
                            newValues.set(variable, parseInt(e.target.value) || 0);
                            setInputValues(newValues);
                          }}
                          className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                          placeholder="Enter value"
                        />
                      </div>
                      <button
                        onClick={() => removeInputVariable(variable)}
                        className="px-2 py-2 bg-red-500 hover:bg-red-600 text-white text-sm rounded-md transition-colors duration-200 flex items-center justify-center w-8 h-8"
                        title="Remove variable"
                      >
                        −
                      </button>
                    </div>
                  ))}
                  
                  {/* Custom Variables */}
                  {customInputVariables.map((variable) => (
                    <div key={variable} className="flex items-center space-x-2">
                      <div className="flex-1">
                        <label className="block text-sm font-medium text-gray-700 mb-1">
                          {variable}
                        </label>
                        <input
                          type="number"
                          value={customInputValues.get(variable) || ''}
                          onChange={(e) => updateCustomInputValue(variable, parseInt(e.target.value) || 0)}
                          className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                          placeholder="Enter value"
                        />
                      </div>
                      <button
                        onClick={() => removeInputVariable(variable)}
                        className="px-2 py-2 bg-red-500 hover:bg-red-600 text-white text-sm rounded-md transition-colors duration-200 flex items-center justify-center w-8 h-8"
                        title="Remove variable"
                      >
                        −
                      </button>
                    </div>
                  ))}
                </div>
              </div>
              
              <div className="flex space-x-3">
                <button
                  onClick={handleCancelInput}
                  className="flex-1 px-4 py-2 bg-gray-300 hover:bg-gray-400 text-gray-700 rounded-md transition-colors duration-200"
                >
                  Cancel
                </button>
                <button
                  onClick={() => handleExecuteWithInput(inputValues, customInputValues)}
                  disabled={isExecuting}
                  className="flex-1 px-4 py-2 bg-blue-500 hover:bg-blue-600 disabled:bg-gray-400 text-white rounded-md transition-colors duration-200"
                >
                  {isExecuting ? 'Executing...' : 'Execute'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default ExecutionPage;
