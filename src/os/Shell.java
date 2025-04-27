package os;

import os.util.ErrorDump;
import os.util.Logging;
import os.util.VerboseModeLogger;

import java.util.Arrays;
import java.util.Scanner;

public class Shell implements Logging {
    private final OperatingSystem os;

    Shell(OperatingSystem operatingSystem) {
        this.os = operatingSystem;
    }

    void startShell() {
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

            if (isVerboseMode(inputs)) {
                VerboseModeLogger.getInstance().setVerboseMode(true);
                //getting rid of the -v flag input - reminder in our program it can only be at the very end
                inputs = Arrays.copyOf(inputs, inputs.length - 1);
            } else {
                VerboseModeLogger.getInstance().setVerboseMode(false);
            }

            switch (inputs[0]) {
                case "execute":
                    log("Starting execute");
                    //add some error checks here for input
                    os.schedule(inputs);
                    os.loadForReady();
                    os.run();
                    break;
                case "load":
                    log("loading program");
                    os.schedule(inputs);
                    os.loadForReady();
                    break;
                case "setpagesize":
                    log("setting page size");
                    if (inputs.length < 2) {
                        logError("Usage: setpagesize <size>");
                        break;
                    }
                    os.setPageSize(Integer.parseInt(inputs[1]));
                    break;
                case "setpagenumber":
                    log("setting page number");
                    os.setPageNumber(Integer.parseInt(inputs[1]));
                    break;
                case "getpagesize":
                    log("get page size");
                    System.out.println(VirtualMemoryManager.getPageSize());
                    break;
                case "run":
                    log("running program");
                    os.run();
                    break;
                case "ps":
                    os.ps(inputs);
                    break;
                case "myvm":
                    prompt = "MYVM-> ";
                    break;
                case "vm":
                    prompt = "VM-> ";
                    break;
                case "osx":
                    if (inputs.length < 3) {
                        logError("Not enough inputs provided");
                        break;
                    }
                    //if working on windows machine, use false, otherwise use true
                    os.assembleFile(inputs[1], inputs[2], true);
                    break;
                case "errordump":
                    ErrorDump.getInstance().printLogs();
                    break;
                case "coredump":
                    os.coreDump(inputs);
                    break;
                case "setsched":
                    os.setSchedule(inputs);
                    break;
                case "test":
                   os.testStuff();
                    break;
                case "help":
                    log("Need some help huh");
                    os.printHelp();
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

    boolean isVerboseMode(String[] inputs) {
        return inputs[inputs.length - 1].equals("-v");
    }


}
