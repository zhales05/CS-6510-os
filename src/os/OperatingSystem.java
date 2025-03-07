package os;

import os.queues.FCFSReadyQueue;
import os.queues.MFQReadyQueue;
import os.queues.RRReadyQueue;
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

/**
 * The OperatingSystem class is a facade between the hardware and the os software
 * Think like an interface that allows them to talk to each other
 */
public class OperatingSystem implements Logging {
    private static final Memory memory = Memory.getInstance();
    private static final Cpu cpu = Cpu.getInstance();
    private static final Clock clock = Clock.getInstance();
    //this will eventually be instantiated based on the input from the user
    //this scheduler will eventually need to be able to update the scheduling algorithm
    private final Scheduler scheduler = new Scheduler(this);

    public void startShell() {
        clock.addObserver(scheduler);
        new Shell(this).startShell();
    }

    //Making the express decision that the quantum values come after the scheduling algorithm
     void setSchedule(String[] inputs) {
        //TODO: add error checking for inputs
        //TODO: make enums/variables for the scheduling algorithms
        switch (inputs[1]) {
            case "fcfs":
                scheduler.setReadyQueue(new FCFSReadyQueue());
                break;
            case "rr":
                scheduler.setReadyQueue(new RRReadyQueue(Integer.parseInt(inputs[2])));
                break;
            case "mfq":
                scheduler.setReadyQueue(new MFQReadyQueue(Integer.parseInt(inputs[2]), Integer.parseInt(inputs[3])));
                break;
            default:
                logError("Unknown scheduling algorithm");
                break;
        }
    }

     boolean assembleFile(String filePath, String loaderAddress, boolean mac){
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

     boolean isVerboseMode(String[] inputs) {
        return inputs[inputs.length - 1].equals("-v");
    }

     void schedule(String[] inputs) {
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

            ProcessControlBlock pcb = new ProcessControlBlock(scheduler.getNewPid(), inputs[i], Integer.parseInt(inputs[i + 1]), clock.getTime());
            scheduler.addToJobQueue(pcb);
        }
        scheduler.processJobs();
    }



     void printHelp() {
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

      void coreDump(String[] inputs) {
        if (inputs.length == 1) {
            System.out.println(memory.coreDump());
            return;
        }

        System.out.println(memory.coreDump(scheduler.getProcess(inputs[1])));
    }

    public void terminateProcess(ProcessControlBlock pcb) {
        scheduler.addToTerminatedQueue(pcb);
    }



}
