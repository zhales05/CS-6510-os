package vm;

import vm.hardware.Cpu;
import vm.hardware.Memory;
import vm.util.ErrorDump;
import vm.util.VerboseModeLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Scanner;

public class VirtualMachine {
    static VerboseModeLogger logger = VerboseModeLogger.getInstance();
    static ErrorDump errorDump = ErrorDump.getInstance();

    public void startShell() {
        String prompt = "VM-> ";
        Scanner scanner = new Scanner(System.in);
        String[] previousCommand = null;
        boolean rerunMode = false;

        while (true) {
            System.out.print(prompt);
            String userInput = scanner.nextLine();
            String[] inputs = Arrays.stream(userInput.split(" "))
                    .map(String::toLowerCase)
                    .toArray(String[]::new);

            if (inputs.length == 0) {
                errorDump.logError("No input provided");
                continue;
            }

            if (inputs[0].equals("redo")) {
                if (previousCommand == null) {
                    System.out.println("No previous command to redo");
                    errorDump.logError("No previous command to redo");
                    continue;
                }

                logger.print("Redo: " + Arrays.toString(previousCommand));
                inputs = previousCommand;
            }

            logger.setVerboseMode(isVerboseMode(inputs));

            switch (inputs[0]) {
                case "load":
                    logger.print("Starting load");
                    Memory.getInstance().load(readProgram(inputs[1]));
                    break;
                case "run":
                    logger.print("Starting run");
                    Cpu.getInstance().run();
                    break;
                case "myvm":
                    prompt = "MYVM-> ";
                    break;
                case "vm":
                    prompt = "VM-> ";
                    break;
                case "errordump":
                    errorDump.printLogs();
                    break;
                case "coredump":
                    System.out.println(Memory.getInstance().coreDump());
                    break;
                case "clearmem":
                    logger.print("Clearing memory");
                    Memory.getInstance().clear();
                    break;
                case "help":
                    logger.print("Need some help huh");
                    printHelp();
                    break;
                case "exit":
                    logger.print("Exiting VM");
                    scanner.close();
                    return;
                default:
                    System.out.println("Unknown input -  please try again.");
                    break;
            }

            if (!rerunMode) {
                previousCommand = inputs;
            }
        }
    }


    private byte[] readProgram(String filePath) {
        try {
            return Files.readAllBytes(Paths.get(filePath));
        } catch (IOException e) {
            errorDump.logError("Error reading file: " + filePath + ": \n" + e.getMessage());
            return null;
        }
    }

    private boolean isVerboseMode(String[] inputs) {
        return inputs[inputs.length - 1].equals("-v");
    }

    public static void printHelp() {
        final String FILE_PATH = "files/Engineering Glossary List.txt";
        try {
            String content = new String(Files.readAllBytes(Paths.get(FILE_PATH)));
            System.out.println(content);
        } catch (IOException e) {
            errorDump.logError("Error reading file: " + FILE_PATH + ": \n" + e.getMessage());
        }
    }

}
