import java.io.*;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import ir.*;
import ir.operand.*;
import ir.datatype.*;
import ir.IRInstruction.OpCode;

/*
1. First BasicBlockNode class is finished, and a CFG is made (which is a Graph of BasicBlockNode obects)
2. Then iterate at most n times, in each iteration pick a Def you have NOT seen
3. For that def, iterate over IR and find its uses (so check operands). Add to a map <Def, IRInstruction reachingUses[]>
4. Build array of criticals (find all branches, I/O, not labels)
5. Iterate over it, querying the map for the Defs that reach it (they are implied critical), and add to array
6. Everytime u iterate over criticalArr/Queue, remove and add to importantInstructions arr[]
7. For instructions not in importantInstructions arr[], do NOT IRPrinter.printInstruction() it to the args[1] output path
 */
public class Optimizer {


    /**
     * Execution begins by checking that two command line arguments are passed into the program.
     * Then, both arguments are validated to confirm they point to valid .ir files.
     * @param args command line arguments passed into the program
     */
    public static void main(String[] args) throws FileNotFoundException, IRException {

        // Validate that two command line arguments have been provided
        if (args.length != 2) {
            System.err.println("Error: two command line arguments must be passed in");
            System.err.println("Run with: java Optimizer <path/to/input.ir> <path/to/output.ir>");
            System.exit(1);
        }

        // Check that both files passed in are .ir files and that the input file actually exists
        String inputIRFilepath = args[0];
        String outputIRFilepath = args[1];

        if (!inputIRFilepath.endsWith(".ir") || !outputIRFilepath.endsWith(".ir")) {
            System.err.println("Error: both input and output files must be .ir type files");
            System.exit(1);
        }

        File inputIRFile = new File(inputIRFilepath);
        if (!inputIRFile.exists() || !inputIRFile.isFile()) {
            System.err.println("Error: input path does not point to an existing .ir file or is not a file");
            System.exit(1);
        }

        // Initialize an IRReader and parse the input IR file to return an IRProgram to be analyzed
        IRReader irReader = new IRReader();
        IRProgram irProgram = irReader.parseIRFile(inputIRFilepath);

        // Initialize an IRPrinter that prints to output IR file
        PrintStream outputStream = new PrintStream(outputIRFilepath);
        IRPrinter outputIRPrinter = new IRPrinter(outputStream);

        // Initialize an IRPrinter that prints to stdout
        IRPrinter stdoutIRPrinter = new IRPrinter(new PrintStream(System.out));
        stdoutIRPrinter.printProgram(irProgram);

        // Call constant folding algorithm
        constantFolding(irProgram);

        // Write the optimized IR to the output IR file
        outputIRPrinter.printProgram(irProgram);
        outputStream.flush();

        System.out.printf("The optimized IR program is in this file: %s\n", outputIRFilepath);
        stdoutIRPrinter.printProgram(irProgram);

    }

    /**
     * Takes in a parsed irProgram to perform constant folding.
     * Constant Folding works on a set of foldable instructions (ADD, SUB, MULT, DIV, AND, and OR). This function
     * iterates over the entire program, and modifies any foldable instructions with two constant operands into a
     * valid ASSIGN instruction, which puts the folded constant into the original variable.
     * @param irProgram parsed IRProgram returned by an IRReader
     */
    public static void constantFolding(IRProgram irProgram) {

        // These instructions can be constant folded.
        Set<OpCode> foldableInstructions = Set.of(OpCode.ADD, OpCode.SUB, OpCode.MULT, OpCode.DIV, OpCode.AND, OpCode.OR);

        // Iterate over each function in the IR program, and over each instruction in each function
        for (IRFunction function : irProgram.functions) {
            /*
            Initialize a new IRInstruction List to hold the instructions of each function whether or not they have been
            modified.
            An instruction is only modified when it has constant operands that can be folded. Once converted into a
            valid ASSIGN instruction, this new ASSIGN instruction is added to the IRInstruction list. Unmodified
            instructions are simply added to the IRInstruction list
             */
            List<IRInstruction> newInstructions = new ArrayList<>();
            for (IRInstruction instruction : function.instructions) {

                /*
                Check if an instruction can be folded. It must have the same OpCode as one in the foldableInstructions
                set, have exactly three operands, and its second and third operands must be constants (their runtime
                types must be IRConstantOperand)
                */
                if (foldableInstructions.contains(instruction.opCode)
                        && instruction.operands.length == 3
                        && instruction.operands[1] instanceof IRConstantOperand
                        && instruction.operands[2] instanceof IRConstantOperand) {

                    System.out.printf("Instruction %d operands in order: ", instruction.irLineNumber);
                    for (IROperand operand : instruction.operands) {
                        System.out.printf("%s, ", operand.toString());
                    }
                    System.out.println();

                    // Extract the second and third operands (compile time type IROperand)
                    IROperand operand1 = instruction.operands[1];
                    IROperand operand2 = instruction.operands[2];

                    // Initialize a new IRInstruction to replace the original instruction with a folded ASSIGN
                    IRInstruction assignInstruction = createConstantFoldedAssignInstruction(instruction,
                                (IRConstantOperand) operand1, (IRConstantOperand) operand2);

                    // Add the new ASSIGN instruction to the current function's newInstructions IRInstruction List
                    newInstructions.add(assignInstruction);
                } else {
                    // The current instruction is not foldable, so just add the original instruction to current IRInstruction List
                    newInstructions.add(instruction);
                }
            }

            // Reassign the current IRFunctions function's List<IRInstruction> instructions List. This modifies the IR in place before being printed to the desired output path
            function.instructions = newInstructions;
        }

    }

    /**
     * Guaranteed that both operands are instances of IRConstantOperand
     * @param originalInstruction original arithmetic instruction be constant folded
     * @param operand1 first constant operand of IRConstantOperand runtime type
     * @param operand2 second constant operand of IRConstantOperand runtime type
     * @return a new ASSIGN IRInstruction with the new folded operand and original variable operand
     */
    private static IRInstruction createConstantFoldedAssignInstruction(IRInstruction originalInstruction,
                                                                       IRConstantOperand operand1, IRConstantOperand operand2) {

        // Initialize a new operands array to hold two operands: variable, constant
        IROperand[] operands = new IROperand[2];
        // Initialize a new instruction with the ASSIGN OpCode, length two operands array (populated later), and the original instruction's line number
        IRInstruction assignInstruction = new IRInstruction(OpCode.ASSIGN, operands, originalInstruction.irLineNumber);

        // Invariant: both IRConstantOperand's operand1 and operand2 will have the same type (int or float), so check the first operand's type
        IRType typeOperand1 = operand1.type;

        // Initialize doubles to hold the numerical values (using doubles since this will need to be downcast to an int or float later
        double numericValueOperand1;
        double numericValueOperand2;

        // Parse the string value as a double from both operands
        numericValueOperand1 = Double.parseDouble(operand1.getValueString());
        numericValueOperand2 = Double.parseDouble(operand2.getValueString());

        // variable to hold the final calculated constant value (also a double to be downcast later)
        double resultingConstant = 0;

        // OpCode of the original instruction, which will be needed for the switch statement
        OpCode originalOpCode = originalInstruction.opCode;

        // switch statement to perform the correct arithmetic based on the original OpCode of the instruction being folded
        resultingConstant = switch (originalOpCode) {
            case ADD -> numericValueOperand1 + numericValueOperand2;
            case SUB -> numericValueOperand1 - numericValueOperand2;
            case MULT -> numericValueOperand1 * numericValueOperand2;
            case DIV -> numericValueOperand1 / numericValueOperand2;
            case AND -> (int) numericValueOperand1 & (int) numericValueOperand2;
            case OR -> (int) numericValueOperand1 | (int) numericValueOperand2;
            default -> resultingConstant;
        };

        // create a new IRConstantOperand object to hold the folded constant value and assign its type based on the corresponding type of both original operands (both are guaranteed to be the same type)
        IRConstantOperand foldedOperand;
        if (typeOperand1 instanceof IRIntType) {
            resultingConstant = (int) resultingConstant;
            foldedOperand = new IRConstantOperand(IRIntType.get(), String.valueOf((int) resultingConstant), assignInstruction);
        } else {
            resultingConstant = (float) resultingConstant;
            foldedOperand = new IRConstantOperand(IRFloatType.get(), String.valueOf((float) resultingConstant), assignInstruction);
        }

        // populate the new operands array for the ASSIGN instruction with the original variable and the new folded constant operand
        assignInstruction.operands[0] = originalInstruction.operands[0];
        assignInstruction.operands[1] = foldedOperand;

        // return the assign instruction so it can be added to its corresponding function's List<IRInstruction> newInstructions list.
        return assignInstruction;
    }
}