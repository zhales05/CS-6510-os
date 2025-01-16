import hardware.Memory;
import util.ErrorDump;
import util.VerboseModeLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Scanner;

public class VirtualMachine {
    public void startShell() {
        final String PROMPT = "MYVM-> ";
        Scanner scanner = new Scanner(System.in);
        ErrorDump errorDump = ErrorDump.getInstance();
        VerboseModeLogger logger = VerboseModeLogger.getInstance();
        String[] previousCommand = null;
        boolean rerunMode = false;

        while (true) {
            System.out.print(PROMPT);
            String userInput = scanner.nextLine();
            String[] inputs = Arrays.stream(userInput.split(" "))
                    .map(String::toLowerCase)
                    .toArray(String[]::new);

            if(inputs.length == 0) {
                errorDump.logError("No input provided");
            }

            if(inputs[0].equals("rerun")) {
                if(previousCommand == null || previousCommand.length == 0) {
                    System.out.println("No previous command to rerun");
                    errorDump.logError("No previous command to rerun");
                    continue;
                }
                inputs = previousCommand;
            }

            //TODO: need to handle verbose mode when there is only two inputs "clear -v"
            logger.setVerboseMode(inputs.length > 2 && inputs[2].equals("-v"));

            switch (inputs[0]) {
                case "load":
                    logger.print("Starting load");
                    Memory.getInstance().load(readProgram(inputs[1]));
                    break;
                case "run":
                case "errordump":
                    errorDump.printLogs();
                    break;
                case "coredump":
                    System.out.println(Memory.getInstance().coreDump());
                    break;
                case "clear":
                    logger.print("Clearing memory");
                    Memory.getInstance().clear();
                    break;
                case "stop":
                    logger.print("Stopping VM");
                    scanner.close();
                    return;
                default:
                    System.out.println("Unknown input -  please try again.");
                    break;
            }

            if(!rerunMode) {
                previousCommand = inputs;
            }
        }
    }

    private byte[] readProgram(String filePath) {
        try {
            return Files.readAllBytes(Paths.get(filePath));
        } catch (IOException e) {
            ErrorDump.getInstance().logError("Error reading file: " + filePath);
            return null;
        }
    }

}
