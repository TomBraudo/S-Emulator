package com.commands;

import com.api.Api;
import com.api.ProgramResult;
import com.program.Program;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        // Build q(x1): y <- x1; INCREASE y  => returns x1 + 1
        Assignment assignYFromX1 = new Assignment("y", "x1", BaseCommand.NO_LABEL, 0, null);
        Increase incY = new Increase("y", BaseCommand.NO_LABEL, 1, null);
        Program q = new Program("q", List.of(assignYFromX1, incY));

        Assignment assignYFromX12 = new Assignment("y", "x1", BaseCommand.NO_LABEL, 0, null);
        JumpZero jz = new JumpZero("x2", BaseCommand.EXIT_LABEL, "L1", 1, null);
        Decrease decX2 = new Decrease("x2", BaseCommand.NO_LABEL, 2, null);
        Increase incY2 = new Increase("y", BaseCommand.NO_LABEL, 3, null);
        GotoLabel gotoL1 = new GotoLabel("L1", BaseCommand.NO_LABEL, 4, null);
        Program addition = new Program("addition", List.of(assignYFromX12, jz, decX2, incY2, gotoL1));


        // Register q so nested ArgCall("q", ...) can be resolved at runtime
        FnArgs.registerProgram("q", q);
        FnArgs.registerProgram("addition", addition);

        // Build nested args for outer call: (q, (q, y))
        // This is a single argument: ArgCall "q" with one argument "y"
        ArgExpr.ArgCall inner = new ArgExpr.ArgCall("q", List.of("y"));
        List<Object> quotedArgs = new ArrayList<>();
        quotedArgs.add(inner);

        List<Object> inputForAddition = new ArrayList<>();
        inputForAddition.add("y");
        inputForAddition.add("x1");

        // Build a program that performs: y <- (q, (q, y))
        Quotation quotation = new Quotation("y", q, quotedArgs, BaseCommand.NO_LABEL, 0, null);
        Quotation q2 = new Quotation("y", addition, inputForAddition, BaseCommand.NO_LABEL, 1, null);
        Program test = new Program("test", List.of(quotation,q2));
        System.out.println(test.expand(1));
        // Execute: starting y assumed 0, so (q, (q, y)) => (q, (q, 0)) = (q, 1) = 2
    }
}
