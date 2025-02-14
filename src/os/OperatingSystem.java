package os;

import os.util.Logging;
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
    private final Map<String, ProcessControlBlock> activePcbs = new HashMap<>();
    Scheduler scheduler = new Scheduler(this);

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
                    try {
                        ProcessBuilder processBuilder = new ProcessBuilder("./files/osx_mac", inputs[1], inputs[2]);
                        processBuilder.redirectErrorStream(true);
                        Process process = processBuilder.start();

                        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                        String line;
                        while ((line = reader.readLine()) != null) {
                            System.out.println(line);
                        }

                        int exitCode = process.waitFor();
                        System.out.println("Exited with code: " + exitCode);
                    } catch (Exception e) {
                        logError("Error running osx: " + e.getMessage());
                    }
                    break;
                case "errordump":
                    ErrorDump.getInstance().printLogs();
                    break;
                case "coredump":
                    if(inputs.length == 1){
                        System.out.println(memory.coreDump());
                        break;
                    }

                    System.out.println(memory.coreDump(activePcbs.get(inputs[1])));
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
            logError("Error reading file: " + ": " + e.getMessage());
            return null;
        }
    }

    ProcessControlBlock prepareForReadyQueue(String filePath) {
        ProcessControlBlock pcb = activePcbs.get(filePath);

        //pcb doesn't exist or is terminated, let's load it into memory
        if (pcb == null) {
            byte[] program = readProgram(filePath);
            if (program == null) {
                return null;
            }
            pcb = memory.load(program);
            pcb.setFilePath(filePath);
        }

        return pcb;
    }

    void removeProcess(ProcessControlBlock pcb) {
        Memory.getInstance().clear(pcb);
        activePcbs.remove(pcb.getFilePath());
    }

    public ProcessControlBlock runProcess(ProcessControlBlock pcb) {
        //if null ready queue is empty so just return.
        if (pcb != null) {
            activePcbs.putIfAbsent(pcb.getFilePath(), pcb);
            cpu.run(pcb, this);
        }

        return pcb;
    }


    private void schedule(String[] inputs) {
        if(inputs.length < 3){
            logError("Not enough inputs provided");
            return;
        }

        for (int i = 1; i < inputs.length - 1; i++) {
            //ignoring clock for now
            if (i % 2 == 1) {
                scheduler.addJob(inputs[i]);
            }
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

    public Integer startChildProcess() {
       return scheduler.startChildProcess();
    }

    public void terminateProcess(ProcessControlBlock pcb) {
        scheduler.addToTerminatedQueue(pcb);
    }

}
