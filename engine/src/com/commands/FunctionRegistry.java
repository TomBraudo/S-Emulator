package com.commands;

import com.XMLHandlerV2.SFunction;
import com.XMLHandlerV2.SFunctions;
import com.XMLHandlerV2.SInstruction;
import com.program.Program;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
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
    private static final Map<String, java.util.Map.Entry<Double, Long>> AVERAGE_COST_BY_PROGRAM = new HashMap<>();

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
            // Phase B: mutate state
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
        } finally {
            write.unlock();
        }
    }

    // Backward-compatible overload (no program name)
    public static void registerFunctions(String userId, SFunctions functions) {
        registerFunctions(userId, functions, "UNKNOWN");
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
                return new Program(program);
            } finally {
                BUILDING.remove(name);
            }
        } finally {
            write.unlock();
        }
    }

    public static int getFunctionArity(String name) {
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
    public static void recordRunCost(String programName, long cyclesPlusOverhead) {
        var write = REGISTRY_LOCK.writeLock();
        write.lock();
        try {
            java.util.Map.Entry<Double, Long> cur = AVERAGE_COST_BY_PROGRAM.get(programName);
            if (cur == null) {
                double avg = roundTo1Decimal((double) cyclesPlusOverhead);
                AVERAGE_COST_BY_PROGRAM.put(programName, new java.util.AbstractMap.SimpleEntry<>(avg, 1L));
                return;
            }
            double prevAvg = cur.getKey();
            long prevCount = cur.getValue();
            long newCount = prevCount + 1L;
            double newAvg = (prevAvg * prevCount + cyclesPlusOverhead) / (double) newCount;
            newAvg = roundTo1Decimal(newAvg);
            AVERAGE_COST_BY_PROGRAM.put(programName, new java.util.AbstractMap.SimpleEntry<>(newAvg, newCount));
        } finally {
            write.unlock();
        }
    }

    public static OptionalDouble getAverageCost(String programName) {
        var read = REGISTRY_LOCK.readLock();
        read.lock();
        try {
            java.util.Map.Entry<Double, Long> cur = AVERAGE_COST_BY_PROGRAM.get(programName);
            if (cur == null) return OptionalDouble.empty();
            return OptionalDouble.of(cur.getKey());
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
}


