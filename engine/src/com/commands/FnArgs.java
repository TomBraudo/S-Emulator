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

    // Function name -> raw instructions (definitions)
    private static final Map<String, List<SInstruction>> DEFINITIONS = new HashMap<>();
    // Function name -> compiled Program cache
    private static final Map<String, Program> PROGRAM_CACHE = new HashMap<>();
    // Guard set to detect recursive/self references during program build
    private static final java.util.Set<String> BUILDING = new java.util.HashSet<>();

    /**
     * Register functions into the internal registry. Existing entries are cleared.
     */
    public static void registerFunctions(SFunctions functions) {
        DEFINITIONS.clear();
        PROGRAM_CACHE.clear();
        BUILDING.clear();
        if (functions == null) return;
        for (SFunction f : functions.getSFunction()) {
            DEFINITIONS.put(f.getName(), f.getSInstructions().getSInstruction());
        }
        // Note: Programs are built lazily on first use via getProgramByName()
        // If you prefer eager build, iterate names and call getProgramByName(name) here.
    }

    /**
     * Retrieve a Program instance by function name using the registered function body.
     */
    public static Program getProgramByName(String functionName) {
        Program cached = PROGRAM_CACHE.get(functionName);
        if (cached != null) return cached;

        List<SInstruction> functionInstructions = DEFINITIONS.get(functionName);
        if (functionInstructions == null) {
            throw new IllegalArgumentException("Function not found: " + functionName);
        }

        // Guard against recursive/self references
        if (!BUILDING.add(functionName)) {
            throw new IllegalStateException("Recursive function reference detected: " + functionName);
        }
        try {
            Program program = Program.createProgram(functionName, functionInstructions);
            PROGRAM_CACHE.put(functionName, program);
            return program;
        } finally {
            BUILDING.remove(functionName);
        }
    }

    public static void registerProgram(String functionName, Program p) {
        PROGRAM_CACHE.put(functionName, p);
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
            List<Object> inner = new ArrayList<>();
            if (!t.peekIs(")")) {
                t.expect(",");
                inner = parseArgList(t);
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
        // Heuristic: variables like x1, z2, etc. Adjust as needed for your naming.
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
}
