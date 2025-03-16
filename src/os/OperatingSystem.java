package os;

import os.queues.FCFSReadyQueue;
import os.queues.MFQReadyQueue;
import os.queues.RRReadyQueue;
import os.util.Logging;
import os.util.VerboseModeLogger;
import vm.hardware.Clock;
import vm.hardware.Cpu;
import vm.hardware.Memory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

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

    void assembleFile(String filePath, String loaderAddress, boolean mac) {
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
        } catch (Exception e) {
            logError("Error running osx: " + e.getMessage());
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

    ProcessControlBlock loadIntoMemory(ProcessControlBlock pcb) {
        //pcb doesn't exist or is terminated, let's load it into memory
        if (pcb == null || pcb.getFilePath() == null) {
            return null;
        }

        byte[] program = readProgram(pcb.getFilePath());
        if (program == null) {
            logError("Process doesn't exist");
            return null;
        }
        pcb = memory.load(program, pcb);
        return pcb;
    }

    public void runProcess(ProcessControlBlock pcb) {
        //if null ready queue is empty just return.
        if (pcb != null) {
            cpu.run(pcb, this);
        }
    }

    boolean isVerboseMode(String[] inputs) {
        return inputs[inputs.length - 1].equals("-v");
    }

    void schedule(String[] inputs) {
        if (inputs.length < 3) {
            logError("Not enough inputs provided");
            return;
        }
        scheduler.clearCurrentProcesses();
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

    void transitionProcess(ProcessControlBlock next) {
        cpu.transition(next);
    }

    public void addToIOQueue(ProcessControlBlock pcb) {
        scheduler.addToIOQueue(pcb);
    }

    public void stopProcess() {
        cpu.stopProcess();
    }

    public void charts() {
        scheduler.makeChart();
    }

    public void testStuff() {
        String one = "execute files/cases/s-cpu-1.osx 1 files/cases/s-cpu-2.osx 1 files/cases/s-cpu-3.osx 1";
        String two = "execute files/cases/s-io-1.osx 1 files/cases/s-io-2.osx 1 files/cases/s-io-3.osx 1";
        String three = "execute files/cases/m-cpu-1.osx 1 files/cases/m-cpu-2.osx 1 files/cases/m-cpu-3.osx 1";
        String four = "execute files/cases/m-io-1.osx 1 files/cases/m-io-2.osx 1 files/cases/m-io-3.osx 1";
        String five = "execute files/cases/l-cpu-1.osx 1 files/cases/l-cpu-2.osx 1 files/cases/l-cpu-3.osx 1";
        String six = "execute files/cases/l-io-1.osx 1 files/cases/l-io-2.osx 1 files/cases/l-io-3.osx 1";

       // VerboseModeLogger.getInstance().setVerboseMode(true);
        String[] inputs1 = one.split(" ");
        String[] inputs2 = two.split(" ");
        String[] inputs3 = three.split(" ");
        String[] inputs4 = four.split(" ");
        String[] inputs5 = five.split(" ");
        String[] inputs6 = six.split(" ");

        //Quantum pairs
        Set<int[]> quantumPairs = new HashSet<>();
        quantumPairs.add(new int[]{1, 2});
        quantumPairs.add(new int[]{1, 5});
        quantumPairs.add(new int[]{2, 4});
        quantumPairs.add(new int[]{2, 8});
        quantumPairs.add(new int[]{4, 12});
        quantumPairs.add(new int[]{4, 16});
        quantumPairs.add(new int[]{8, 32});
        quantumPairs.add(new int[]{10, 40});
        quantumPairs.add(new int[]{15, 60});
        quantumPairs.add(new int[]{20, 80});
        quantumPairs.add(new int[]{20, 30});
        quantumPairs.add(new int[]{22, 32});


        for(int[] qp : quantumPairs) {
            scheduler.setReadyQueue(new MFQReadyQueue(qp[0], qp[1]));
            schedule(inputs1);
            schedule(inputs2);
            schedule(inputs3);
            schedule(inputs4);
            schedule(inputs5);
            schedule(inputs6);
        }
      //  charts();
    }
}
