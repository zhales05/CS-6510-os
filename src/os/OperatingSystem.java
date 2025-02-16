package os;

import os.util.Logging;
import vm.hardware.Clock;
import vm.hardware.Cpu;
import vm.hardware.Memory;
import os.util.ErrorDump;
import os.util.VerboseModeLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class OperatingSystem implements Logging {
    private static final Memory memory = Memory.getInstance();
    private static final Cpu cpu = Cpu.getInstance();
    private static final Clock clock = Clock.getInstance();
    private final Scheduler scheduler = new Scheduler(this);

    public void startShell() {
        String prompt = "VM-> ";
        Scanner scanner = new Scanner(System.in);
        String[] previousCommand = null;
        boolean rerunMode = false;
        clock.addObserver(scheduler);

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
            }

            switch (inputs[0]) {
                case "execute":
                    log("Starting execute");
                    //add some error checks here for input
                    schedule(inputs);
                    break;
                case "myvm":
                    prompt = "MYVM-> ";
                    break;
                case "vm":
                    prompt = "VM-> ";
                    break;
                case "osx":
                    //if working on windows machine, use false, otherwise use true
                    assembleFile(inputs[1], inputs[2], true);
                    break;
                case "errordump":
                    ErrorDump.getInstance().printLogs();
                    break;
                case "coredump":
                    if (inputs.length == 1) {
                        System.out.println(memory.coreDump());
                        break;
                    }

                    System.out.println(memory.coreDump(scheduler.getProcess(inputs[1])));
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

    private boolean assembleFile(String filePath, String loaderAddress, boolean mac){
        final String macPath = "files/osx_mac";
        final String windowsPath = "files/osx.exe";

        String path = mac ? macPath : windowsPath;
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(path, filePath, loaderAddress);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            int exitCode = process.waitFor();
            log("Assembled with code: " + exitCode);
            return exitCode == 0;
        } catch (Exception e) {
            logError("Error running osx: " + e.getMessage());
        }

        return false;
    }



    private byte[] readProgram(String filePath) {
        try {
            return Files.readAllBytes(Paths.get(filePath));
        } catch (IOException e) {
            logError("Error reading file: " + ": " + e.getMessage());
            return null;
        }
    }

    ProcessControlBlock prepareForReadyQueue(ProcessControlBlock pcb) {
        //pcb doesn't exist or is terminated, let's load it into memory
        if (pcb == null || pcb.getFilePath() == null) {
            logError("Process doesn't exist");
            return null;
        }

        byte[] program = readProgram(pcb.getFilePath());
        if (program == null) {
            return null;
        }
        pcb = memory.load(program, pcb);
        return pcb;
    }

    void removeProcess(ProcessControlBlock pcb) {
        Memory.getInstance().clear(pcb);
    }

    public ProcessControlBlock runProcess(ProcessControlBlock pcb) {
        //if null ready queue is empty so just return.
        if (pcb != null) {
            cpu.run(pcb, this);
        }

        return pcb;
    }


    private void schedule(String[] inputs) {
        if (inputs.length < 3) {
            logError("Not enough inputs provided");
            return;
        }
        //grabbing filename and clock starting time
        for (int i = 1; i < inputs.length; i += 2) {
            if (i + 1 >= inputs.length) {
                logError("Not enough inputs provided, you likely forgot to add the starting clock time");
                return;
            }

            ProcessControlBlock pcb = new ProcessControlBlock(scheduler.getNewPid(), inputs[i], Integer.parseInt(inputs[i + 1]));
            scheduler.addToJobQueue(pcb);
        }
        scheduler.processJobs();
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
            logError("Error reading file: " + ": " + e.getMessage());
        }
    }

    public ProcessControlBlock startChildProcess(ProcessControlBlock parent) {
        return scheduler.startChildProcess(parent);
    }

    public void terminateProcess(ProcessControlBlock pcb) {
        scheduler.addToTerminatedQueue(pcb);
    }

}
