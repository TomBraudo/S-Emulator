package com.commands;

import com.XMLHandlerV2.SFunctions;
import com.program.Program;
import com.program.ProgramState;
import com.api.ProgramResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    // Registry state moved to FunctionRegistry; this class focuses on parsing/evaluation and delegates lookups/registrations.

    // ---- Registration APIs (additive; no clearing) ----
    public static void registerFunctions(String userId, SFunctions functions, String programName) {
        FunctionRegistry.registerFunctions(userId, functions, programName);
    }

    /**
     * Pre-validate a program name for uniqueness without mutating state.
     * Throws if the program name is owned by another user.
     */
    public static void assertProgramNameAvailable(String userId, String programName) {
        FunctionRegistry.assertProgramNameAvailable(userId, programName);
    }

    /**
     * Retrieve a Program instance by function name using the registered function body.
     */
    public static Program getProgramByName(String functionName) {
        return FunctionRegistry.getProgramByName(functionName);
    }

    /**
     * Register a compiled Program as a function implementation for the given user.
     * If the function name is owned by another user, throws.
     */
    public static void registerProgramAsFunction(String userId, String functionName, Program p) {
        FunctionRegistry.registerProgramAsFunction(userId, functionName, p);
    }


    /**
     * Register a compiled Program as a user program (not a function), enforcing global name uniqueness.
     */
    public static void registerProgram(String userId, Program p) {
        FunctionRegistry.registerProgram(userId, p);
    }

    /**
     * Calculate the arity of a function by finding the highest x-variable index.
     * For example, if the function uses x1, x5, x11, the arity is 11.
     */
    // Arity calculation moved to FunctionRegistry

    public static void clearFunctions(){ }

    /**
     * Extract the index from an x-variable (e.g., "x1" -> 1, "x11" -> 11)
     * Returns 0 if not an x-variable.
     */
    // Regex helpers moved to FunctionRegistry

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
    public static List<java.util.Map.Entry<String, String>> getFunctionEntries() {
        return FunctionRegistry.getFunctionEntries();
    }

    public static List<java.util.Map.Entry<String, String>> getFunctionEntries(String userId) {
        return FunctionRegistry.getFunctionEntries(userId);
    }

    public static List<java.util.Map.Entry<String, String>> getProgramEntries(String userId) {
        return FunctionRegistry.getProgramEntries(userId);
    }

    public static List<java.util.Map.Entry<String, String>> getProgramEntries() {
        return FunctionRegistry.getProgramEntries();
    }

    public static List<String> getFunctionCommands(String functionName){
        List<String> commands = new ArrayList<>();
        for(BaseCommand c : getProgramByName(functionName).getCommands()){
            commands.add(c.toString());
        }

        return commands;
    }
}
