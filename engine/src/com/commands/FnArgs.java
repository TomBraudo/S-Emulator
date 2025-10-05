package com.commands;

import com.XMLHandlerV2.SFunction;
import com.XMLHandlerV2.SFunctions;
import com.XMLHandlerV2.SInstruction;
import com.program.Program;
import com.program.ProgramState;
import com.api.ProgramResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.AbstractMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Handles function arguments:
 * - Registers functions and keeps a lookup registry.
 * - Parses functionArguments strings into a mixed list of elements:
 *     - String (variable name), or
 *     - ArgExpr.ArgCall (nested function invocation).
 * - Provides Program lookup by function name for nested evaluation.
 *
 * Examples:
 *   "x1,x2"                   -> [ "x1", "x2" ]
 *   "(Successor,x1)"          -> [ ArgCall("Successor", ["x1"]) ]
 *   "(CONST7,(Successor,x1))" -> [ ArgCall("CONST7", []), ArgCall("Successor", ["x1"]) ]
 *   "CONST7"                  -> [ ArgCall("CONST7", []) ]
 */
public final class FnArgs {

    private FnArgs() {}

    // ---- Concurrency guard over entire registry ----
    private static final ReentrantReadWriteLock REGISTRY_LOCK = new ReentrantReadWriteLock();

    // ---- Function registry (by user) ----
    // userId -> (functionName -> raw SInstruction list)
    private static final Map<String, Map<String, List<SInstruction>>> DEFINITIONS_BY_USER = new HashMap<>();
    // userId -> (functionName -> compiled Program cache)
    private static final Map<String, Map<String, Program>> FUNCTION_PROGRAM_CACHE_BY_USER = new HashMap<>();
    // functionName -> owner userId (enforces global uniqueness across users)
    private static final Map<String, String> FUNCTION_OWNER_BY_NAME = new HashMap<>();
    // Function name -> arity (global, since names are unique globally)
    private static final Map<String, Integer> FUNCTION_ARITY = new HashMap<>();
    // Guard set to detect recursive/self references during program build (global by name)
    private static final java.util.Set<String> BUILDING = new java.util.HashSet<>();

    // ---- Program registry (non-function programs, by user) ----
    // userId -> (programName -> Program)
    private static final Map<String, Map<String, Program>> PROGRAMS_BY_USER = new HashMap<>();
    // programName -> owner userId (global uniqueness)
    private static final Map<String, String> PROGRAM_OWNER_BY_NAME = new HashMap<>();

    // ---- Registration APIs (additive; no clearing) ----
    public static void registerFunctions(String userId, SFunctions functions) {
        if (functions == null) return;
        var write = REGISTRY_LOCK.writeLock();
        write.lock();
        try {
            Map<String, List<SInstruction>> defs = DEFINITIONS_BY_USER.computeIfAbsent(userId, k -> new HashMap<>());
            Map<String, Program> cache = FUNCTION_PROGRAM_CACHE_BY_USER.computeIfAbsent(userId, k -> new HashMap<>());
            for (SFunction f : functions.getSFunction()) {
                String name = f.getName();
                String owner = FUNCTION_OWNER_BY_NAME.get(name);
                if (owner != null && !owner.equals(userId)) {
                    throw new IllegalStateException("Function name already registered by another user: " + name);
                }
                List<SInstruction> instructions = f.getSInstructions().getSInstruction();
                defs.put(name, instructions);                  // upsert for this user
                cache.remove(name);                            // invalidate cache for this user
                FUNCTION_OWNER_BY_NAME.put(name, userId);      // claim ownership (idempotent for same user)

                // Calculate and store the arity (max x-variable index)
                int arity = calculateArity(instructions);
                FUNCTION_ARITY.put(name, arity);
            }
        } finally {
            write.unlock();
        }
    }

    /**
     * Retrieve a Program instance by function name using the registered function body.
     */
    public static Program getProgramByName(String functionName) {
        var read = REGISTRY_LOCK.readLock();
        read.lock();
        try {
            String owner = FUNCTION_OWNER_BY_NAME.get(functionName);
            if (owner == null) {
                throw new IllegalArgumentException("Function not found: " + functionName);
            }
            Map<String, Program> cache = FUNCTION_PROGRAM_CACHE_BY_USER.getOrDefault(owner, Map.of());
            Program cached = cache.get(functionName);
            if (cached != null) return cached;
        } finally {
            read.unlock();
        }

        // Build lazily outside of read lock, then install under write lock
        List<SInstruction> functionInstructions;
        String owner;
        var write = REGISTRY_LOCK.writeLock();
        write.lock();
        try {
            owner = FUNCTION_OWNER_BY_NAME.get(functionName);
            if (owner == null) throw new IllegalArgumentException("Function not found: " + functionName);
            Map<String, List<SInstruction>> defs = DEFINITIONS_BY_USER.get(owner);
            if (defs == null || (functionInstructions = defs.get(functionName)) == null) {
                throw new IllegalArgumentException("Function not found: " + functionName);
            }

            if (!BUILDING.add(functionName)) {
                throw new IllegalStateException("Recursive function reference detected: " + functionName);
            }
            try {
                Program program = Program.createProgram(functionName, functionInstructions);
                FUNCTION_PROGRAM_CACHE_BY_USER.computeIfAbsent(owner, k -> new HashMap<>()).put(functionName, program);
                return program;
            } finally {
                BUILDING.remove(functionName);
            }
        } finally {
            write.unlock();
        }
    }

    /**
     * Register a compiled Program as a function implementation for the given user.
     * If the function name is owned by another user, throws.
     */
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

    /**
     * Backwards-compat shim: if called without user, treat as GLOBAL owner.
     * Prefer registerProgramAsFunction(userId, name, program).
     */
    @Deprecated
    public static void registerProgram(String functionName, Program p) {
        registerProgramAsFunction("GLOBAL", functionName, p);
    }

    /**
     * Register a compiled Program as a user program (not a function), enforcing global name uniqueness.
     */
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

    /**
     * Calculate the arity of a function by finding the highest x-variable index.
     * For example, if the function uses x1, x5, x11, the arity is 11.
     */
    private static int calculateArity(List<SInstruction> instructions) {
        int maxXIndex = 0;
        
        for (SInstruction instruction : instructions) {
            // Check the main variable
            String variable = instruction.getSVariable();
            if (variable != null) {
                maxXIndex = Math.max(maxXIndex, extractXIndex(variable));
            }
            
            // Check arguments for variable references
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

    public static void clearFunctions(){
        var write = REGISTRY_LOCK.writeLock();
        write.lock();
        try {
            DEFINITIONS_BY_USER.clear();
            FUNCTION_PROGRAM_CACHE_BY_USER.clear();
            FUNCTION_OWNER_BY_NAME.clear();
            FUNCTION_ARITY.clear();
            BUILDING.clear();
        } finally {
            write.unlock();
        }
    }

    /**
     * Extract the index from an x-variable (e.g., "x1" -> 1, "x11" -> 11)
     * Returns 0 if not an x-variable.
     */
    private static int extractXIndex(String variable) {
        if (variable != null && variable.startsWith("x") && variable.length() > 1) {
            try {
                return Integer.parseInt(variable.substring(1));
            } catch (NumberFormatException e) {
                // Not a valid x-variable format
            }
        }
        return 0;
    }

    /**
     * Find the maximum x-variable index in a string that might contain function arguments.
     * For example, "(g, x1, x5)" would return 5.
     */
    private static int findMaxXIndexInString(String str) {
        int maxIndex = 0;
        // Simple regex to find x-variables in the string
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\bx(\\d+)\\b");
        java.util.regex.Matcher matcher = pattern.matcher(str);
        
        while (matcher.find()) {
            try {
                int index = Integer.parseInt(matcher.group(1));
                maxIndex = Math.max(maxIndex, index);
            } catch (NumberFormatException e) {
                // Skip invalid numbers
            }
        }
        
        return maxIndex;
    }

    /**
     * Get the arity (expected number of input arguments) for a function.
     */
    public static int getFunctionArity(String functionName) {
        Integer arity = FUNCTION_ARITY.get(functionName);
        if (arity == null) {
            throw new IllegalArgumentException("Function not found: " + functionName);
        }
        return arity;
    }

    /**
     * Parse a functionArguments attribute into a list whose elements are either:
     * - String (variable name), or
     * - ArgExpr.ArgCall (nested function invocation)
     */
    public static List<Object> parse(String raw) {
        if (raw == null) return List.of();
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) return List.of();

        TokenStream t = new TokenStream(trimmed);

        List<Object> result;
        if (t.peekIs("(")) {
            // Bracketed top-level list: (elem,elem,...)
            t.consume("(");
            result = parseArgList(t);
            t.expect(")");
        } else {
            // Unbracketed CSV list: elem,elem,...
            result = parseArgList(t);
        }
        t.expectEnd();
        return result;
    }

    /**
     * Context-aware parsing that considers the expected number of arguments.
     * This is the main parsing method that should be used for QUOTE and JUMP_EQUAL_FUNCTION.
     * 
     * @param raw The raw argument string from XML
     * @param expectedArgCount The number of arguments the target function expects
     * @return List of parsed arguments (String variables or ArgExpr.ArgCall function calls)
     */
    public static List<Object> parseWithArity(String raw, int expectedArgCount) {
        if (raw == null) return List.of();
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) return List.of();

        TokenStream t = new TokenStream(trimmed);
        
        List<Object> result;
        if (expectedArgCount == 1 && t.peekIs("(")) {
            // For single argument, try parsing as one element first
            // This handles "(Successor,x1)" as a single function call
            result = List.of(parseElem(t));
        } else if (t.peekIs("(")) {
            // For multiple arguments, parse as list
            // This handles "(Const7,(Successor,x1))" as two separate elements
            t.consume("(");
            result = parseArgList(t);
            t.expect(")");
        } else {
            // No parentheses, parse as CSV
            result = parseArgList(t);
        }
        t.expectEnd();
        return result;
    }

    // ---------------- Evaluation / Rendering / Mapping utilities ----------------

    /**
     * Recursively evaluate a mixed list of arguments (String variable or ArgCall) to integer values
     * in the context of the given ProgramState.
     */
    public static List<Integer> evaluateArgs(ProgramState state, List<Object> args) {
        List<Integer> out = new ArrayList<>(args.size());
        for (Object a : args) {
            out.add(evalArg(state, a));
        }
        return out;
    }

    private static int evalArg(ProgramState state, Object arg) {
        if (arg instanceof String varName) {
            return state.variables.get(varName).getValue();
        }
        ArgExpr.ArgCall call = (ArgExpr.ArgCall) arg;
        List<Integer> inner = new ArrayList<>(call.args().size());
        for (Object sub : call.args()) {
            inner.add(evalArg(state, sub));
        }
        Program nested = getProgramByName(call.name());
        ProgramResult r = nested.execute(inner);
        return r.getResult();
    }

    /**
     * Render each argument to a human-readable string (for toString).
     */
    public static List<String> renderArgList(List<Object> args) {
        List<String> out = new ArrayList<>(args.size());
        for (Object a : args) out.add(renderArg(a));
        return out;
    }

    private static String renderArg(Object a) {
        if (a instanceof String v) return v;
        ArgExpr.ArgCall c = (ArgExpr.ArgCall) a;
        if (c.args().isEmpty()) return c.name();
        StringBuilder sb = new StringBuilder();
        sb.append('(').append(c.name());
        for (Object sub : c.args()) {
            sb.append(',').append(renderArg(sub));
        }
        sb.append(')');
        return sb.toString();
    }

    /**
     * Collect all variable names referenced inside a mixed args list, left-to-right.
     */
    public static List<String> collectVariables(List<Object> exprs) {
        List<String> out = new ArrayList<>();
        collect(exprs, out);
        return out;
    }

    private static void collect(List<Object> exprs, List<String> acc) {
        for (Object e : exprs) {
            if (e instanceof String v) {
                acc.add(v);
            } else {
                ArgExpr.ArgCall c = (ArgExpr.ArgCall) e;
                collect(c.args(), acc);
            }
        }
    }

    /**
     * Replace variable names in a mixed args tree using values popped from 'mapped' in order.
     * Nested calls are preserved; only Strings are replaced.
     */
    public static List<Object> replaceVarsInArgs(List<Object> original, java.util.Deque<String> mapped) {
        List<Object> out = new ArrayList<>(original.size());
        for (Object o : original) {
            if (o instanceof String) {
                out.add(mapped.removeFirst());
            } else {
                ArgExpr.ArgCall c = (ArgExpr.ArgCall) o;
                List<Object> replacedInner = replaceVarsInArgs(c.args(), mapped);
                out.add(new ArgExpr.ArgCall(c.name(), replacedInner));
            }
        }
        return out;
    }

    // ---------------- Parsing helpers ----------------

    // Returns a list where each element is String (variable) or ArgExpr.ArgCall (nested call)
    private static List<Object> parseArgList(TokenStream t) {
        List<Object> args = new ArrayList<>();
        if (t.peekIs(")")) return args; // empty list
        args.add(parseElem(t));
        while (t.peekIs(",")) {
            t.consume(",");
            args.add(parseElem(t));
        }
        return args;
    }

    // Parse a single element: variable name or function call
    private static Object parseElem(TokenStream t) {
        if (t.peekIs("(")) {
            // Function call in bracketed form: (Name, arg1, arg2, ...)
            t.consume("(");
            String name = t.readIdent();
            
            // Get the arity of this function to parse exactly the right number of arguments
            int arity = getFunctionArity(name);
            List<Object> inner = new ArrayList<>();
            
            // Parse exactly 'arity' number of arguments
            for (int i = 0; i < arity; i++) {
                t.expect(","); // Expect comma before each argument
                inner.add(parseElem(t)); // Recursive parsing for each argument
            }
            
            t.expect(")");
            return new ArgExpr.ArgCall(name, inner);
        } else {
            // Identifier: either variable or a 0-arg function call (e.g., CONST7)
            String ident = t.readIdent();
            if (looksLikeVariable(ident)) {
                return ident; // variable name
            } else {
                return new ArgExpr.ArgCall(ident, List.of()); // 0-arg function call
            }
        }
    }

    private static boolean looksLikeVariable(String ident) {
        // Heuristic: variables like x1, z2, etc. Both input (x) and work (z) variables.
        return ident.matches("[xz][A-Za-z0-9_]*\\d+");
    }

    // ---------------- Tokenizer ----------------

    private static final class TokenStream {
        private final String s;
        private int i = 0;

        TokenStream(String s) {
            this.s = s;
        }

        boolean peekIs(String token) {
            skipWs();
            return s.startsWith(token, i);
        }

        void consume(String token) {
            skipWs();
            if (!s.startsWith(token, i)) {
                throw error("Expected '" + token + "'");
            }
            i += token.length();
        }

        void expect(String token) {
            consume(token);
        }

        void expectEnd() {
            skipWs();
            if (i != s.length()) {
                throw error("Unexpected trailing input: '" + s.substring(i) + "'");
            }
        }

        String readIdent() {
            skipWs();
            int start = i;
            while (i < s.length()) {
                char c = s.charAt(i);
                if (Character.isLetterOrDigit(c) || c == '_' ) {
                    i++;
                } else {
                    break;
                }
            }
            if (start == i) {
                throw error("Identifier expected");
            }
            return s.substring(start, i);
        }

        void skipWs() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        }

        private IllegalArgumentException error(String msg) {
            return new IllegalArgumentException(msg + " at pos " + i);
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

    // -------- Tuple-returning listings (userId, name) --------
    public static List<Map.Entry<String, String>> getFunctionEntries() {
        var read = REGISTRY_LOCK.readLock();
        read.lock();
        try {
            List<Map.Entry<String, String>> out = new ArrayList<>(FUNCTION_OWNER_BY_NAME.size());
            for (Map.Entry<String, String> e : FUNCTION_OWNER_BY_NAME.entrySet()) {
                // e: functionName -> ownerUserId; return (userId, functionName)
                out.add(new AbstractMap.SimpleEntry<>(e.getValue(), e.getKey()));
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
                out.add(new AbstractMap.SimpleEntry<>(userId, name));
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
                out.add(new AbstractMap.SimpleEntry<>(userId, name));
            }
            return out;
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
                // e: programName -> ownerUserId; return (userId, programName)
                out.add(new AbstractMap.SimpleEntry<>(e.getValue(), e.getKey()));
            }
            return out;
        } finally {
            read.unlock();
        }
    }

    public static List<String> getFunctionCommands(String functionName){
        List<String> commands = new ArrayList<>();
        for(BaseCommand c : getProgramByName(functionName).getCommands()){
            commands.add(c.toString());
        }

        return commands;
    }
}
