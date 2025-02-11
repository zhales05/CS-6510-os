package os;

import os.util.Logging;
import vm.hardware.Cpu;
import vm.hardware.Memory;
import os.util.ErrorDump;
import os.util.VerboseModeLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Scanner;

public class OperatingSystem implements Logging {
    private static final Memory memory = Memory.getInstance();
    private static final Cpu cpu = Cpu.getInstance();

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
                logError("No input provided");
                continue;
            }

            if (inputs[0].equals("redo")) {
                if (previousCommand == null) {
                    System.out.println("No previous command to redo");
                    logError("No previous command to redo");
                    continue;
                }

                log("Redo: " + Arrays.toString(previousCommand));
                inputs = previousCommand;
            }

            VerboseModeLogger.getInstance().setVerboseMode(isVerboseMode(inputs));

            switch (inputs[0]) {
                case "myvm":
                    prompt = "MYVM-> ";
                    break;
                case "vm":
                    prompt = "VM-> ";
                    break;
                case "errordump":
                    ErrorDump.getInstance().printLogs();
                    break;
                case "coredump":
                    System.out.println(memory.coreDump());
                    break;
                case "clearmem":
                    log("Clearing memory");
                    memory.clear();
                    break;
                case "help":
                    log("Need some help huh");
                    printHelp();
                    break;
                case "exit":
                    log("Exiting VM");
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
            logError("Error reading file: " + filePath + ": \n" + e.getMessage());
            return null;
        }
    }

    private boolean isVerboseMode(String[] inputs) {
        return inputs[inputs.length - 1].equals("-v");
    }

    private void printHelp() {
        final String FILE_PATH = "files/Engineering Glossary List.txt";
        try {
            String content = new String(Files.readAllBytes(Paths.get(FILE_PATH)));
            System.out.println(content);
        } catch (IOException e) {
            logError("Error reading file: " + FILE_PATH + ": \n" + e.getMessage());
        }
    }

}
