package com.commands;

import com.program.Program;

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

        /**
         * Compute the expansion depth of this call when used as an argument in a quotation.
         * Depth is 1 + max( depth(underlying program), depth(nested args) ).
         */
        public int expansionDepth(){
            Program nested = FnArgs.getProgramByName(name);
            int programDepth = nested.getMaxExpansionLevel();
            int nestedArgsDepth = ArgExpr.computeArgsDepth(args);
            return 1 + Math.max(programDepth, nestedArgsDepth);
        }
    }

    private ArgExpr() {
        // utility holder
    }

    /**
     * Compute the maximum expansion depth contributed by a list of mixed arguments
     * (variables or nested function calls) when placed inside a quotation context.
     */
    public static int computeArgsDepth(List<Object> args){
        if (args == null || args.isEmpty()) return 0;
        int max = 0;
        for (Object a : args){
            int d;
            if (a instanceof String){
                d = 0; // variables do not add depth
            } else {
                d = ((ArgCall) a).expansionDepth();
            }
            if (d > max) max = d;
        }
        return max;
    }
}
