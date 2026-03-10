import ir.*;
import ir.datatype.IRArrayType;
import ir.datatype.IRIntType;
import ir.datatype.IRType;
import ir.operand.IRConstantOperand;
import ir.operand.IROperand;
import ir.operand.IRVariableOperand;

import java.io.PrintStream;
import java.util.*;

public class Demo {
    public static void main(String[] args) throws Exception {
        // Parse the IR file
        IRReader irReader = new IRReader();
        // The first command line argument should be the path to the IR file
        IRProgram program = irReader.parseIRFile(args[0]);

        // Print the IR to another file
        // The optimized IR is written to the second command line argument, which is the path for the optimized output
        IRPrinter filePrinter = new IRPrinter(new PrintStream(args[1]));
        filePrinter.printProgram(program);

        // Create an IR printer that prints to stdout
        IRPrinter irPrinter = new IRPrinter(new PrintStream(System.out));

        // Print all instructions that stores a constant to an array
        System.out.println("Instructions that stores a constant to an array:");
        // The List<IRFunction> field is from the IRProgram being analyzed. We iterate over this to get each function's instructions
        for (IRFunction function : program.functions) {
            // Loop over the List<IRInstruction> field in IRFunction objects
            for (IRInstruction instruction : function.instructions) {
                // check if the opCode field in the IRInstruction object is the ARRAY_STORE enum
                // TODO: in real optimizer do validation checks
                if (instruction.opCode == IRInstruction.OpCode.ARRAY_STORE) {
                    // TODO: IRConstantOperand has no subclasses, can just validate class
                    if (instruction.operands[0] instanceof IRConstantOperand) {
                        // TODO: redundant .format call - not optimal
                        System.out.print(String.format("Line %d:", instruction.irLineNumber));
                        // prints to wherever I specified the path
                        irPrinter.printInstruction(instruction);
                        // stdout test
                        System.out.println("hello");
                    }
                }
            }
        }
        System.out.println();

        // Print the name of all int scalars and int arrays with a size of 1
        System.out.println("Int scalars and 1-sized arrays:");
        // Iterate over the IR's functions through its List<IRFunction> field in the IRProgram object we parsed
        for (IRFunction function : program.functions) {
            List<String> vars = new ArrayList<>();
            // Iterate over the List<IRVariableOperand> field in IRProgram
            for (IRVariableOperand v : function.variables) {
                IRType type = v.type;
                System.out.printf("IRVariableOperand's IRType: %s", v.type.toString());
                // For each unique data type, only one IRType object will be created
                // so that IRType objects can be compared using '=='
                /*
                FIXME: important note here to understand how the singleton instance is created by the parser.
                       IRReader line 226 calls parseVariableList() to create all IRVariableOperand objects with the IRType properly initialized

                 */
                if (type == IRIntType.get() || type == IRArrayType.get(IRIntType.get(), 1))
                    vars.add(v.getName());
            }
            if (!vars.isEmpty())
                System.out.println(function.name + ": " + String.join(", ", vars));
        }
        System.out.println();

        // FIXME: Useful for DCE
        // Print all variables that are declared but not used (including unused parameters)
        System.out.println("Unused variables/parameters:");
        for (IRFunction function : program.functions) {
            // IROperand objects are not shared between instructions/parameter list/variable list
            // They should be compared using their names
            Set<String> vars = new HashSet<>();
            // Parameters are not included in the variable list
            for (IRVariableOperand v : function.parameters)
                vars.add(v.getName());
            for (IRVariableOperand v : function.variables)
                vars.add(v.getName());
            for (IRInstruction instruction : function.instructions)
                for (IROperand operand : instruction.operands)
                    if (operand instanceof IRVariableOperand) {
                        IRVariableOperand variableOperand = (IRVariableOperand) operand;
                        vars.remove(variableOperand.getName());
                    }
            if (!vars.isEmpty())
                System.out.println(function.name + ": " + String.join(", ", vars));
        }
        System.out.println();
    }
}
