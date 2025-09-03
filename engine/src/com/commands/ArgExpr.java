package com.commands;

import java.util.List;

/**
 * Function-arguments model for QUOTE/JUMP_EQUAL_FUNCTION.
 *
 * Each argument is either:
 * - String: a variable name present in ProgramState, or
 * - ArgExpr.ArgCall: a nested function call whose own arguments are again
 *   either String (variable) or ArgCall (nested call).
 */
public final class ArgExpr {

    /**
     * A function call with zero or more nested arguments.
     * Example encodings:
     *   "CONST7"                  -> new ArgCall("CONST7", List.of())
     *   "(Successor,x1)"          -> new ArgCall("Successor", List.of("x1"))
     *   "(F,(G,x1),x2)"           -> new ArgCall("F", List.of(new ArgCall("G", List.of("x1")), "x2"))
     *
     * The args list contains elements that are either:
     *   - String (variable name), or
     *   - ArgExpr.ArgCall (another nested call).
     */
    public static final class ArgCall {
        private final String name;
        private final List<Object> args; // elements: String or ArgCall

        public ArgCall(String name, List<Object> args) {
            this.name = name;
            this.args = args;
        }

        public String name() {
            return name;
        }

        public List<Object> args() {
            return args;
        }
    }

    private ArgExpr() {
        // utility holder
    }
}
