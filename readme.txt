# README

Project name: S-Emulator

Architecture at a glance
- Layering
  - Core engine: the interpreter, program model, command hierarchy, and expansion logic.
  - Console/UI: a thin command-line wrapper that builds/executes programs and prints results.
- Java and modules
  - Built for Java 21.

Core engine model
- Program
  - Owns the program name and the ordered list of commands.
  - Unpacks metadata on construction:
    - Computes label-to-index mapping (first definition wins).
    - Collects present variables and derives input variables (x1, x2, …) in first-seen order.
  - Provides execution and analysis:
    - execute(inputs): runs to EXIT or end, returns ProgramResult with cycles and final variables map.
    - expand(level): returns a new, expanded Program with fresh temporaries and labels where needed.
    - getMaxExpansionLevel(): the maximum depth required to fully lower all commands.
    - verifyLegal(): ensures all control-flow targets are known labels or EXIT.
- ProgramState
  - Captures the mutable runtime state:
    - variables: map from variable name to a Variable object (name + integer value).
    - currentCommandIndex: instruction pointer.
    - cyclesCount: cumulative cost.
    - done: termination flag used by EXIT jumps.
    - labelToIndex: the resolved label table used for branching.
- ProgramResult
  - Immutable value returned at the end with:
    - cycles: total cycles consumed.
    - variables: final name->value mapping. By convention, y is the primary output.
- Variable
  - Minimal holder of a name and an integer value; instances are stored in ProgramState.variables.
- Command hierarchy
  - All commands inherit a common base that standardizes:
    - Metadata: label identifier, display index, declared cycle cost, and an optional “creator” (the original command that produced an expanded step).
    - Behavior: execute(state) and reporting helpers for printing/diagnostics.
    - Introspection: getPresentVariables(), getTargetLabel(), getExpansionLevel().
    - Rewriting: expand(nextVarId, nextLabelId, realIndex) to produce a sequence that preserves semantics.
  - Primitive/basic commands execute as-is and have expansion level 0.
  - Higher-level commands provide an expansion that rewrites them into combinations of basic ones.

Command set and costs (project-specific)
- Arithmetic
  - Increase v            — cost 1; increments a variable by one.
  - Decrease v            — cost 1; decrements a variable by one.
  - Zero v                — cost 1; sets a variable to zero.
  - Assignment v1 <- v2   — cost 4; copies a value.
  - Neutral v             — cost 0; structural no-op, anchors labels and sequencing points.
- Jumps
  - Goto L                — cost 1; unconditional jump to label L.
  - JZero v -> L          — cost 2; jump to L if v == 0.
  - JNZero v -> L         — cost 2; jump to L if v != 0.
  - JEC v == c -> L       — cost 2; jump to L if v == c.
  - JEV v1 == v2 -> L     — cost 2; jump to L if v1 == v2.

Macro-expansion strategy (how high-level forms are lowered)
- Deterministic passes
  - Each command advertises its expansion level. Calling expand(level) applies the lowering passes uniformly, ensuring deterministic output.
  - Fresh resources
    - Fresh temporaries are named z1, z2, … and are allocated by scanning the current maximum and incrementing counters.
    - Fresh labels are named L1, L2, … similarly allocated to avoid collisions with user labels.

Labeling and verification model
- First-definition wins
  - When unpacking, the mapping from label to instruction index is created from the first occurrence of each label. Later duplicates are allowed for display but do not change jump resolution.
- Static validation
  - verifyLegal() runs after unpacking to ensure that every jump target exists or is EXIT, preventing runtime errors.

Input and output conventions
- Input variables
  - Any variable whose name starts with x is treated as an input. The execute call expects a list of integers that matches the first-seen order of these x-variables in the program.
  - The input doesn't have to include exactly the number of input variables present, missing inputs will initialize variables at 0, and extra will be ignored.
- Output
  - By convention, the computed result is found in variable y. The ProgramResult also returns the entire variable map for inspection.
- Non-negativity
  - Programs are typically written under non-negative semantics; A variable with value 0 that is decreased by 1 will remain 0.

Error handling and termination
- Termination happens when execution reaches EXIT or the instruction pointer moves past the last command.
- Illegal or missing label targets cause a validation error before execution.

Build and runtime requirements
- Java 21 is required.
- A run script is provided to compile and execute without manual setup.

How to run
- Run the included run.bat script.
- No additional steps are required; the script uses the final packaged JAR hierarchy.

Typical usage flow in the run script
1. Build the project.
2. Load a program from a valid XML file.
3. Execute with a list of integers corresponding to x-variables.
4. Display:
   - Program name
   - Input tuple
   - Output y
   - Cycles

Zip submission structure
- root/
  - run.sh or run.bat (mandatory; runs the program end-to-end)
  - README.md (this file)
  - src/… (source code)
  - any build files (e.g., pom.xml/gradle files if used)
  - examples/ (optional sample inputs and expected outputs)


- Name: Tom Braudo
- ID: 324182914
- Email(s): tombr2@mta.ac.il
- GitHub repository URL: https://github.com/TomBraudo/S-Emulator