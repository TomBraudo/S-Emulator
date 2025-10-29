package com.program;

import com.XMLHandlerV2.SFunction;
import com.XMLHandlerV2.SFunctions;
import com.XMLHandlerV2.SInstruction;
import com.XMLHandlerV2.SProgram;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Central registry for functions and user programs.
 * - Enforces global name uniqueness (across users) per category (function/program)
 * - Two-phase registration for functions (pre-validate, then insert)
 * - Lazy program build for functions on first resolution
 * - Thread-safe via a single RW lock across all maps
 */
public final class FunctionRegistry {

    private FunctionRegistry() {}

    // ---- Concurrency guard ----
    private static final ReentrantReadWriteLock REGISTRY_LOCK = new ReentrantReadWriteLock();

    // ---- Function registry (by user) ----
    private static final Map<String, Map<String, List<SInstruction>>> DEFINITIONS_BY_USER = new HashMap<>(); // userId -> (functionName -> raw SInstruction list)
    private static final Map<String, Map<String, Program>> FUNCTION_PROGRAM_CACHE_BY_USER = new HashMap<>(); // userId -> (functionName -> compiled Program)
    private static final Map<String, String> FUNCTION_OWNER_BY_NAME = new HashMap<>(); // functionName -> owner userId
    private static final Map<String, Integer> FUNCTION_ARITY = new HashMap<>(); // functionName -> arity
    private static final java.util.Set<String> BUILDING = new java.util.HashSet<>(); // guards recursive references (by function name)
    private static final Map<String, String> FUNCTION_SOURCE_PROGRAM_BY_NAME = new HashMap<>(); // functionName -> SProgram name

    // ---- Program registry (by user) ----
    private static final Map<String, Map<String, Program>> PROGRAMS_BY_USER = new HashMap<>(); // userId -> (programName -> Program)
    private static final Map<String, String> PROGRAM_OWNER_BY_NAME = new HashMap<>(); // programName -> owner userId

    // ---- Global per-program average cost registry ----
    // Key: program name; Value: (averageCostRoundedTo1Decimal, runCount)
    private static final Map<String, java.util.Map.Entry<Double, Integer>> AVERAGE_COST_BY_PROGRAM = new HashMap<>();

    // ---- Function usage tracking ----
    // Key: program/function name; Value: list of function names it uses
    private static final Map<String, List<String>> FUNCTIONS_USED_BY = new HashMap<>();
    // Key: function name; Value: list of program/function names that use it
    private static final Map<String, List<String>> PROGRAMS_USING_FUNCTION = new HashMap<>();

    // ---- Transactional build context (thread-local) ----
    private static final ThreadLocal<TxContext> TX = new ThreadLocal<>();

    private static final class TxContext {
        final String userId;
        final String programName;
        final Map<String, List<SInstruction>> tempFunctionDefsByName = new HashMap<>();
        final Map<String, Program> tempCompiledFunctionsByName = new HashMap<>();
        final Map<String, Integer> tempFunctionArityByName = new HashMap<>();
        Program tempProgram;

        TxContext(String userId, String programName) {
            this.userId = userId;
            this.programName = programName;
        }
    }

    // ---- Public registration APIs ----
    public static void registerFunctions(String userId, SFunctions functions, String programName) {
        if (functions == null) return;
        var write = REGISTRY_LOCK.writeLock();
        write.lock();
        try {
            // Phase A: pre-validate all names atomically
            for (SFunction f : functions.getSFunction()) {
                String name = f.getName();
                String owner = FUNCTION_OWNER_BY_NAME.get(name);
                if (owner != null && !owner.equals(userId)) {
                    throw new IllegalStateException("Function name already registered by another user: " + name);
                }
            }
            // Phase B: register metadata and definitions
            Map<String, List<SInstruction>> defs = DEFINITIONS_BY_USER.computeIfAbsent(userId, k -> new HashMap<>());
            Map<String, Program> cache = FUNCTION_PROGRAM_CACHE_BY_USER.computeIfAbsent(userId, k -> new HashMap<>());
            for (SFunction f : functions.getSFunction()) {
                String name = f.getName();
                List<SInstruction> instructions = f.getSInstructions().getSInstruction();
                defs.put(name, instructions);
                cache.remove(name); // invalidate cache for this user
                FUNCTION_OWNER_BY_NAME.put(name, userId);
                int arity = calculateArity(instructions);
                FUNCTION_ARITY.put(name, arity);
                FUNCTION_SOURCE_PROGRAM_BY_NAME.put(name, programName);
            }
            
            // Phase C: eagerly compile all functions and track dependencies
            for (SFunction f : functions.getSFunction()) {
                String name = f.getName();
                
                // Skip if already compiled (might have been compiled as dependency of another function)
                if (cache.containsKey(name)) {
                    continue;
                }
                
                List<SInstruction> instructions = defs.get(name);
                
                if (!BUILDING.add(name)) {
                    throw new IllegalStateException("Recursive function reference detected: " + name);
                }
                try {
                    Program program = Program.createProgram(name, instructions);
                    cache.put(name, program);
                    
                    // Track function usage for this function
                    List<String> usedFunctions = extractFunctionNamesFromProgram(program);
                    updateUsageTracking(name, usedFunctions);
                } finally {
                    BUILDING.remove(name);
                }
            }
        } finally {
            write.unlock();
        }
    }

    // Backward-compatible overload (no program name)
    public static void registerFunctions(String userId, SFunctions functions) {
        registerFunctions(userId, functions, "UNKNOWN");
    }

    /**
     * Transactionally register a full SProgram (program + its functions).
     * If any step fails, nothing is committed to the global registries.
     */
    public static void registerProgramBundle(String userId, SProgram sp) {
        if (sp == null) throw new IllegalArgumentException("SProgram is null");

        // Pre-validate program name uniqueness under read lock first (fast-fail)
        assertProgramNameAvailable(userId, sp.getName());

        // Prepare transaction context
        TxContext ctx = new TxContext(userId, sp.getName());
        TX.set(ctx);
        try {
            // Phase A: pre-validate function names uniqueness (no mutations yet)
            SFunctions sFunctions = sp.getSFunctions();
            if (sFunctions != null) {
                var read = REGISTRY_LOCK.readLock();
                read.lock();
                try {
                    for (SFunction f : sFunctions.getSFunction()) {
                        String name = f.getName();
                        String owner = FUNCTION_OWNER_BY_NAME.get(name);
                        if (owner != null && !owner.equals(userId)) {
                            throw new IllegalStateException("Function name already registered by another user: " + name);
                        }
                    }
                } finally {
                    read.unlock();
                }

                // Stage temp function defs and arities
                for (SFunction f : sFunctions.getSFunction()) {
                    String name = f.getName();
                    List<SInstruction> instructions = f.getSInstructions().getSInstruction();
                    ctx.tempFunctionDefsByName.put(name, instructions);
                    int arity = calculateArity(instructions);
                    ctx.tempFunctionArityByName.put(name, arity);
                }

                // Eagerly compile functions into temp cache (using TX-aware lookups)
                for (SFunction f : sFunctions.getSFunction()) {
                    String name = f.getName();
                    if (ctx.tempCompiledFunctionsByName.containsKey(name)) continue;
                    List<SInstruction> instructions = ctx.tempFunctionDefsByName.get(name);
                    if (!BUILDING.add(name)) {
                        throw new IllegalStateException("Recursive function reference detected: " + name);
                    }
                    try {
                        Program program = Program.createProgram(name, instructions);
                        ctx.tempCompiledFunctionsByName.put(name, program);
                        List<String> usedFunctions = extractFunctionNamesFromProgram(program);
                        // track in global tracker map only after commit; keep local for now if needed
                    } finally {
                        BUILDING.remove(name);
                    }
                }
            }

            // Build the main program into temp
            ctx.tempProgram = Program.createProgram(sp.getName(), sp.getSInstructions().getSInstruction());

            // Commit: under write lock, insert everything atomically
            var write = REGISTRY_LOCK.writeLock();
            write.lock();
            try {
                // Re-assert program name availability just before commit
                String existing = PROGRAM_OWNER_BY_NAME.get(sp.getName());
                if (existing != null && !existing.equals(userId)) {
                    throw new IllegalStateException("Program name already registered by another user: " + sp.getName());
                }

                // Commit functions
                if (!ctx.tempFunctionDefsByName.isEmpty()) {
                    Map<String, List<SInstruction>> defs = DEFINITIONS_BY_USER.computeIfAbsent(userId, k -> new HashMap<>());
                    Map<String, Program> cache = FUNCTION_PROGRAM_CACHE_BY_USER.computeIfAbsent(userId, k -> new HashMap<>());
                    for (Map.Entry<String, List<SInstruction>> e : ctx.tempFunctionDefsByName.entrySet()) {
                        String fname = e.getKey();
                        defs.put(fname, e.getValue());
                        cache.remove(fname);
                        FUNCTION_OWNER_BY_NAME.put(fname, userId);
                        FUNCTION_ARITY.put(fname, ctx.tempFunctionArityByName.getOrDefault(fname, 0));
                        FUNCTION_SOURCE_PROGRAM_BY_NAME.put(fname, sp.getName());
                    }
                    // Put compiled cache
                    cache.putAll(ctx.tempCompiledFunctionsByName);
                }

                // Commit program
                PROGRAM_OWNER_BY_NAME.put(sp.getName(), userId);
                PROGRAMS_BY_USER.computeIfAbsent(userId, k -> new HashMap<>()).put(sp.getName(), ctx.tempProgram);

                // Update usage tracking for committed items
                if (!ctx.tempCompiledFunctionsByName.isEmpty()) {
                    for (Map.Entry<String, Program> e : ctx.tempCompiledFunctionsByName.entrySet()) {
                        List<String> used = extractFunctionNamesFromProgram(e.getValue());
                        updateUsageTracking(e.getKey(), used);
                    }
                }
                List<String> programUsed = extractFunctionNamesFromProgram(ctx.tempProgram);
                updateUsageTracking(sp.getName(), programUsed);

            } finally {
                write.unlock();
            }
        } finally {
            TX.remove();
        }
    }

    public static void registerProgramAsFunction(String userId, String functionName, Program p) {
        var write = REGISTRY_LOCK.writeLock();
        write.lock();
        try {
            String owner = FUNCTION_OWNER_BY_NAME.get(functionName);
            if (owner != null && !owner.equals(userId)) {
                throw new IllegalStateException("Function name already registered by another user: " + functionName);
            }
            FUNCTION_OWNER_BY_NAME.put(functionName, userId);
            FUNCTION_PROGRAM_CACHE_BY_USER.computeIfAbsent(userId, k -> new HashMap<>()).put(functionName, p);
        } finally {
            write.unlock();
        }
    }

    public static void registerProgram(String userId, Program p) {
        String name = p.getName();
        var write = REGISTRY_LOCK.writeLock();
        write.lock();
        try {
            String owner = PROGRAM_OWNER_BY_NAME.get(name);
            if (owner != null && !owner.equals(userId)) {
                throw new IllegalStateException("Program name already registered by another user: " + name);
            }
            PROGRAM_OWNER_BY_NAME.put(name, userId);
            PROGRAMS_BY_USER.computeIfAbsent(userId, k -> new HashMap<>()).put(name, p);
            
            // Track function usage
            List<String> usedFunctions = extractFunctionNamesFromProgram(p);
            updateUsageTracking(name, usedFunctions);
        } finally {
            write.unlock();
        }
    }

    public static void assertProgramNameAvailable(String userId, String programName) {
        var read = REGISTRY_LOCK.readLock();
        read.lock();
        try {
            String owner = PROGRAM_OWNER_BY_NAME.get(programName);
            if (owner != null && !owner.equals(userId)) {
                throw new IllegalStateException("Program name already registered by another user: " + programName);
            }
        } finally {
            read.unlock();
        }
    }

    // ---- Lookup APIs ----
    public static Program getProgramByName(String name) {
        // Check TX context first (during transactional build)
        TxContext tx = TX.get();
        if (tx != null) {
            // main program in TX
            if (name.equals(tx.programName) && tx.tempProgram != null) {
                return new Program(tx.tempProgram);
            }
            // compiled functions in TX
            Program compiled = tx.tempCompiledFunctionsByName.get(name);
            if (compiled != null) {
                return new Program(compiled);
            }
            // If definition exists in TX but not compiled yet, compile on-demand within TX
            List<SInstruction> txDef = tx.tempFunctionDefsByName.get(name);
            if (txDef != null) {
                if (!BUILDING.add(name)) {
                    throw new IllegalStateException("Recursive function reference detected: " + name);
                }
                try {
                    Program program = Program.createProgram(name, txDef);
                    tx.tempCompiledFunctionsByName.put(name, program);
                    return new Program(program);
                } finally {
                    BUILDING.remove(name);
                }
            }
        }
        // Fast path under read lock: check program registry first, then function cache
        var read = REGISTRY_LOCK.readLock();
        read.lock();
        try {
            // Is it a user program?
            String programOwner = PROGRAM_OWNER_BY_NAME.get(name);
            if (programOwner != null) {
                Map<String, Program> progs = PROGRAMS_BY_USER.getOrDefault(programOwner, Map.of());
                Program p = progs.get(name);
                if (p == null) {
                    throw new IllegalArgumentException("Program not found: " + name);
                }
                return new Program(p);
            }
            // Else, treat as function: try cached compiled program
            String funcOwner = FUNCTION_OWNER_BY_NAME.get(name);
            if (funcOwner == null) {
                throw new IllegalArgumentException("Function or program not found: " + name);
            }
            Map<String, Program> cache = FUNCTION_PROGRAM_CACHE_BY_USER.getOrDefault(funcOwner, Map.of());
            Program cached = cache.get(name);
            if (cached != null) return new Program(cached);
        } finally {
            read.unlock();
        }

        // If it's a function and not cached, lazily build under write lock
        // NOTE: Functions should normally be eagerly compiled during registerFunctions.
        // This lazy path is mainly for edge cases and transitive dependencies during compilation.
        var write = REGISTRY_LOCK.writeLock();
        write.lock();
        try {
            // Re-check function path under write lock
            String funcOwner = FUNCTION_OWNER_BY_NAME.get(name);
            if (funcOwner == null) {
                throw new IllegalArgumentException("Function or program not found: " + name);
            }
            Map<String, List<SInstruction>> defs = DEFINITIONS_BY_USER.get(funcOwner);
            List<SInstruction> instructions = defs == null ? null : defs.get(name);
            if (instructions == null) throw new IllegalArgumentException("Function not found: " + name);

            if (!BUILDING.add(name)) {
                throw new IllegalStateException("Recursive function reference detected: " + name);
            }
            try {
                Program program = Program.createProgram(name, instructions);
                FUNCTION_PROGRAM_CACHE_BY_USER.computeIfAbsent(funcOwner, k -> new HashMap<>()).put(name, program);
                
                // Track function usage for this function
                List<String> usedFunctions = extractFunctionNamesFromProgram(program);
                updateUsageTracking(name, usedFunctions);
                
                return new Program(program);
            } finally {
                BUILDING.remove(name);
            }
        } finally {
            write.unlock();
        }
    }

    public static int getFunctionArity(String name) {
        // TX override
        TxContext tx = TX.get();
        if (tx != null) {
            if (name.equals(tx.programName) && tx.tempProgram != null) {
                return calculateProgramArity(tx.tempProgram);
            }
            Integer arity = tx.tempFunctionArityByName.get(name);
            if (arity != null) return arity;
        }
        // If name belongs to a user program, derive arity from its input variables (max x-index)
        var read = REGISTRY_LOCK.readLock();
        read.lock();
        try {
            String programOwner = PROGRAM_OWNER_BY_NAME.get(name);
            if (programOwner != null) {
                Map<String, Program> progs = PROGRAMS_BY_USER.getOrDefault(programOwner, Map.of());
                Program p = progs.get(name);
                if (p == null) {
                    throw new IllegalArgumentException("Program not found: " + name);
                }
                return calculateProgramArity(p);
            }
            Integer arity = FUNCTION_ARITY.get(name);
            if (arity == null) throw new IllegalArgumentException("Function not found: " + name);
            return arity;
        } finally {
            read.unlock();
        }
    }

    public static List<String> getFunctionNames() {
        var read = REGISTRY_LOCK.readLock();
        read.lock();
        try {
            return new ArrayList<>(FUNCTION_OWNER_BY_NAME.keySet());
        } finally {
            read.unlock();
        }
    }

    public static List<String> getFunctionNames(String userId) {
        var read = REGISTRY_LOCK.readLock();
        read.lock();
        try {
            Map<String, List<SInstruction>> defs = DEFINITIONS_BY_USER.get(userId);
            if (defs == null) return List.of();
            return new ArrayList<>(defs.keySet());
        } finally {
            read.unlock();
        }
    }

    public static List<String> getProgramNames() {
        var read = REGISTRY_LOCK.readLock();
        read.lock();
        try {
            return new ArrayList<>(PROGRAM_OWNER_BY_NAME.keySet());
        }
        finally {
            read.unlock();
        }
    }

    public static List<String> getProgramNames(String userId) {
        var read = REGISTRY_LOCK.readLock();
        read.lock();
        try {
            Map<String, Program> progs = PROGRAMS_BY_USER.get(userId);
            if (progs == null) return List.of();
            return new ArrayList<>(progs.keySet());
        } finally {
            read.unlock();
        }
    }

    public static List<Map.Entry<String, String>> getFunctionEntries() {
        var read = REGISTRY_LOCK.readLock();
        read.lock();
        try {
            List<Map.Entry<String, String>> out = new ArrayList<>(FUNCTION_OWNER_BY_NAME.size());
            for (Map.Entry<String, String> e : FUNCTION_OWNER_BY_NAME.entrySet()) {
                out.add(new java.util.AbstractMap.SimpleEntry<>(e.getValue(), e.getKey()));
            }
            return out;
        } finally {
            read.unlock();
        }
    }

    public static List<Map.Entry<String, String>> getFunctionEntries(String userId) {
        var read = REGISTRY_LOCK.readLock();
        read.lock();
        try {
            Map<String, List<SInstruction>> defs = DEFINITIONS_BY_USER.get(userId);
            if (defs == null) return List.of();
            List<Map.Entry<String, String>> out = new ArrayList<>(defs.size());
            for (String name : defs.keySet()) {
                out.add(new java.util.AbstractMap.SimpleEntry<>(userId, name));
            }
            return out;
        } finally {
            read.unlock();
        }
    }

    public static List<Map.Entry<String, String>> getProgramEntries(String userId) {
        var read = REGISTRY_LOCK.readLock();
        read.lock();
        try {
            Map<String, Program> progs = PROGRAMS_BY_USER.get(userId);
            if (progs == null) return List.of();
            List<Map.Entry<String, String>> out = new ArrayList<>(progs.size());
            for (String name : progs.keySet()) {
                out.add(new java.util.AbstractMap.SimpleEntry<>(userId, name));
            }
            return out;
        } finally {
            read.unlock();
        }
    }

    public static int getProgramUploadedCount(String userId) {
        var read = REGISTRY_LOCK.readLock();
        read.lock();
        try {
            int count = 0;
            for (Map.Entry<String, String> e : PROGRAM_OWNER_BY_NAME.entrySet()){
                if (userId.equals(e.getValue())) count++;
            }
            return count;
        } finally {
            read.unlock();
        }
    }
    public static int getFunctionUploadedCount(String userId) {
        var read = REGISTRY_LOCK.readLock();
        read.lock();
        try {
            int count = 0;
            for (Map.Entry<String, String> e : FUNCTION_OWNER_BY_NAME.entrySet()){
                if (userId.equals(e.getValue())) count++;
            }
            return count;
        } finally {
            read.unlock();
        }
    }

    public static List<Map.Entry<String, String>> getProgramEntries() {
        var read = REGISTRY_LOCK.readLock();
        read.lock();
        try {
            List<Map.Entry<String, String>> out = new ArrayList<>(PROGRAM_OWNER_BY_NAME.size());
            for (Map.Entry<String, String> e : PROGRAM_OWNER_BY_NAME.entrySet()) {
                out.add(new java.util.AbstractMap.SimpleEntry<>(e.getValue(), e.getKey()));
            }
            return out;
        } finally {
            read.unlock();
        }
    }

    public static String getFunctionSourceProgram(String functionName){
        var read = REGISTRY_LOCK.readLock();
        read.lock();
        try {
            return FUNCTION_SOURCE_PROGRAM_BY_NAME.get(functionName);
        } finally {
            read.unlock();
        }
    }

    // ---- Average cost APIs ----
    public static void recordRunCost(String programName, long cycles) {
        var write = REGISTRY_LOCK.writeLock();
        write.lock();
        try {
            java.util.Map.Entry<Double, Integer> cur = AVERAGE_COST_BY_PROGRAM.get(programName);
            if (cur == null) {
                double avg = roundTo1Decimal((double) cycles);
                AVERAGE_COST_BY_PROGRAM.put(programName, new java.util.AbstractMap.SimpleEntry<>(avg, 1));
                return;
            }
            double prevAvg = cur.getKey();
            int prevCount = cur.getValue();
            int newCount = prevCount + 1;
            double newAvg = (prevAvg * prevCount + cycles) / (double) newCount;
            newAvg = roundTo1Decimal(newAvg);
            AVERAGE_COST_BY_PROGRAM.put(programName, new java.util.AbstractMap.SimpleEntry<>(newAvg, newCount));
        } finally {
            write.unlock();
        }
    }

    public static double getAverageCost(String programName) {
        var read = REGISTRY_LOCK.readLock();
        read.lock();
        try {
            java.util.Map.Entry<Double, Integer> cur = AVERAGE_COST_BY_PROGRAM.get(programName);
            if (cur == null) return 0.0;
            return cur.getKey();
        } finally {
            read.unlock();
        }
    }

    private static double roundTo1Decimal(double value) {
        return Math.round(value * 10.0d) / 10.0d;
    }

    // ---- Utilities ----
    private static int calculateArity(List<SInstruction> instructions) {
        int maxXIndex = 0;
        for (SInstruction instruction : instructions) {
            String variable = instruction.getSVariable();
            if (variable != null) {
                maxXIndex = Math.max(maxXIndex, extractXIndex(variable));
            }
            if (instruction.getSInstructionArguments() != null) {
                for (com.XMLHandlerV2.SInstructionArgument arg : instruction.getSInstructionArguments().getSInstructionArgument()) {
                    String value = arg.getValue();
                    if (value != null) {
                        maxXIndex = Math.max(maxXIndex, findMaxXIndexInString(value));
                    }
                }
            }
        }
        return maxXIndex;
    }

    private static int extractXIndex(String variable) {
        if (variable != null && variable.startsWith("x") && variable.length() > 1) {
            try {
                return Integer.parseInt(variable.substring(1));
            } catch (NumberFormatException ignored) {}
        }
        return 0;
    }

    private static int calculateProgramArity(Program program) {
        int maxXIndex = 0;
        for (String v : program.getPresentVariables()) {
            maxXIndex = Math.max(maxXIndex, extractXIndex(v));
        }
        return maxXIndex;
    }

    private static int findMaxXIndexInString(String str) {
        int maxIndex = 0;
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\bx(\\d+)\\b");
        java.util.regex.Matcher matcher = pattern.matcher(str);
        while (matcher.find()) {
            try {
                int index = Integer.parseInt(matcher.group(1));
                maxIndex = Math.max(maxIndex, index);
            } catch (NumberFormatException ignored) {}
        }
        return maxIndex;
    }

    public static String getOwnerByName(String name) {
        var read = REGISTRY_LOCK.readLock();
        read.lock();
        try {
            String owner = PROGRAM_OWNER_BY_NAME.get(name);
            if (owner != null) return owner;
            return FUNCTION_OWNER_BY_NAME.get(name);
        } finally {
            read.unlock();
        }
    }

    public static int getRunCountByName(String name) {
        var read = REGISTRY_LOCK.readLock();
        read.lock();
        try {
            java.util.Map.Entry<Double, Integer> entry = AVERAGE_COST_BY_PROGRAM.get(name);
            return entry != null ? entry.getValue() : 0;
        } finally {
            read.unlock();
        }
    }

    public static boolean isFunction(String name) {
        return FUNCTION_OWNER_BY_NAME.containsKey(name);
    }

    // ---- Function usage tracking methods ----
    
    /**
     * Extracts all function names called by a Program (recursively from all nested commands)
     */
    private static List<String> extractFunctionNamesFromProgram(Program program) {
        List<String> functionNames = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        
        for (com.commands.BaseCommand cmd : program.getCommands()) {
            for (String funcName : cmd.getCalledFunctionNames()) {
                if (!seen.contains(funcName)) {
                    seen.add(funcName);
                    functionNames.add(funcName);
                }
            }
        }
        return functionNames;
    }

    /**
     * Updates the usage tracking maps for a program/function
     */
    private static void updateUsageTracking(String programName, List<String> usedFunctions) {
        // Store which functions this program uses
        FUNCTIONS_USED_BY.put(programName, new ArrayList<>(usedFunctions));
        
        // Update reverse mapping: for each function, add this program to its users
        for (String funcName : usedFunctions) {
            PROGRAMS_USING_FUNCTION.computeIfAbsent(funcName, k -> new ArrayList<>()).add(programName);
        }
    }

    /**
     * Get list of PROGRAM names that use a specific function (excludes other functions)
     */
    public static List<String> getProgramsUsing(String functionName) {
        var read = REGISTRY_LOCK.readLock();
        read.lock();
        try {
            List<String> allUsers = PROGRAMS_USING_FUNCTION.get(functionName);
            if (allUsers == null || allUsers.isEmpty()) {
                return List.of();
            }
            // Filter to only return programs (not functions)
            List<String> programs = new ArrayList<>();
            for (String name : allUsers) {
                if (PROGRAM_OWNER_BY_NAME.containsKey(name)) {
                    programs.add(name);
                }
            }
            return programs;
        } finally {
            read.unlock();
        }
    }

    /**
     * Recursively get all functions in the dependency chain (transitive closure)
     * @param programName the program or function name to analyze
     * @return List of all function names used directly or indirectly (no duplicates)
     */
    public static List<String> getAllFunctionsInChain(String programName) {
        var read = REGISTRY_LOCK.readLock();
        read.lock();
        try {
            List<String> allFunctions = new ArrayList<>();
            java.util.Set<String> visited = new java.util.HashSet<>();
            collectFunctionsRecursive(programName, allFunctions, visited);
            return allFunctions;
        } finally {
            read.unlock();
        }
    }

    /**
     * Helper method for recursive dependency collection (assumes no cycles)
     */
    private static void collectFunctionsRecursive(String name, List<String> result, java.util.Set<String> visited) {
        // Get direct dependencies
        List<String> directDeps = FUNCTIONS_USED_BY.get(name);
        if (directDeps == null || directDeps.isEmpty()) {
            return;
        }

        // Process each dependency
        for (String funcName : directDeps) {
            // Add to result if not already present
            if (!visited.contains(funcName)) {
                visited.add(funcName);
                result.add(funcName);
                // Recursively process this function's dependencies
                collectFunctionsRecursive(funcName, result, visited);
            }
        }
    }

}
