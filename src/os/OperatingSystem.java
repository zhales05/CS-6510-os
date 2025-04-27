package os;

import os.queues.FCFSReadyQueue;
import os.queues.MFQReadyQueue;
import os.queues.RRReadyQueue;
import os.util.Logging;
import vm.hardware.Clock;
import vm.hardware.Cpu;
import vm.hardware.Memory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The OperatingSystem class is a facade between the hardware and the os software
 * Think like an interface that allows them to talk to each other
 */
public class OperatingSystem implements Logging {
    private static final Memory memory = Memory.getInstance();
    private static final Cpu cpu = Cpu.getInstance();
    private static final Clock clock = Clock.getInstance();

    public AtomicInteger counter = new AtomicInteger(0);
    public int[] sharedArray = new int[10];
    private int in = 0;
    private int out = 0;
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

    public void writeToSharedMemory(int value) {
        counter.incrementAndGet();
        sharedArray[getIn()] = value;
        incrementIn();
    }

    public int readFromSharedMemory() {
        counter.decrementAndGet();
        int currOut = getOut();
        incrementOut();
        return sharedArray[currOut];
    }

    public void unlinkFromSharedMemory() {
        counter.set(0);
        sharedArray = new int[10];
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

    void setPageSize(int pageSize) {
        VirtualMemoryManager.setPageSize(pageSize);
        FrameTable.getInstance().reinitialize();
    }

    public void runProcess(ProcessControlBlock pcb) {
        //if null ready queue is empty just return.
        if (pcb != null) {
            cpu.run(pcb, this);
        }
    }

    void schedule(String[] inputs) {
        if (inputs.length < 3) {
            logError("Not enough inputs provided");
            return;
        }
        scheduler.clearCurrentProcesses();
        List<ProcessControlBlock> toAdd = new ArrayList<>();
        boolean sharedAccess = false;
        //grabbing filename and clock starting time
        for (int i = 1; i < inputs.length; i += 2) {
            if('|' == inputs[i].charAt(0)) {
               log("Shared Memory");
               sharedAccess = true;
               break;
            }

            if (i + 1 >= inputs.length) {
                logError("Not enough inputs provided, you likely forgot to add the starting clock time");
                return;
            }

            ProcessControlBlock pcb = new ProcessControlBlock(scheduler.getNewPid(), inputs[i], Integer.parseInt(inputs[i + 1]));
            toAdd.add(pcb);
            scheduler.addToJobQueue(pcb);
        }
        // set shared access if needed
        // can only be found retroactively based on the pipe so this is the best way.
        if(sharedAccess) {
            for(ProcessControlBlock pcb : toAdd) {
                pcb.setShareDataAccess(sharedAccess);
            }
        }
    }

    void loadJobs(){
        scheduler.loadJobs();
    }

    void moveToReadyAndRun(String[] inputs) {
        scheduler.run(inputs);
        run();
    }

    void moveToReadyAndRun() {
        scheduler.processJobsForReadyQueue();
    }

    void run(){
        scheduler.runThroughReadyQueue();
        scheduler.systemGanttChart();
    }

    void ps(String[] inputs) {
        if(inputs.length == 1) {
            printProcesses(true, true);
        }
    }

    private void printProcesses(boolean showProcesses, boolean showFreeFrames) {
        if (showProcesses) {
            System.out.println("Processes:");
            for (ProcessControlBlock pcb : scheduler.getCurrentProcesses()) {
                System.out.println("PID: " + pcb.getPid() +
                        " | Status: " + pcb.getStatus() +
                        " | Frames: " + pcb.getPageTable().getUsedFrames());
            }
        }

        if (showFreeFrames) {
            System.out.println("\nFree Frames:");
            for (int i = 0; i < FrameTable.getInstance().getTotalFrames(); i++) {
                if (FrameTable.getInstance().isFrameFree(i)) {
                    System.out.print(i + " ");
                }
            }
            System.out.println();
        }
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
            System.out.println(memory.fullCoreDump());
            return;
        }

        System.out.println(memory.coreDump(scheduler.getProcess(inputs[1])));
    }

    public void terminateProcess(ProcessControlBlock pcb) {
        scheduler.addToTerminatedQueue(pcb);
        memory.clear(pcb);  // Your method already exists!

    }

    void transitionProcess() {
        cpu.transition();
    }

    public void addToIOQueue(ProcessControlBlock pcb) {
        scheduler.addToIOQueue(pcb);
    }

    public void stopProcess() {
        cpu.stopProcess();
    }

    public void setPageNumber(int pageNumber) {
        memory.setPageNumber(pageNumber);
    }

    public void testStuff() {
        String one = "execute files/cases/s-cpu-1.osx 1 files/cases/s-cpu-2.osx 1 files/cases/s-cpu-3.osx 1";
        String two = "execute files/cases/s-io-1.osx 1 files/cases/s-io-2.osx 1 files/cases/s-io-3.osx 1";
        String three = "execute files/cases/m-cpu-1.osx 1 files/cases/m-cpu-2.osx 1 files/cases/m-cpu-3.osx 1";
        String four = "execute files/cases/m-io-1.osx 1 files/cases/m-io-2.osx 1 files/cases/m-io-3.osx 1";
        String five = "execute files/cases/l-cpu-1.osx 1 files/cases/l-cpu-2.osx 1 files/cases/l-cpu-3.osx 1";
        String six = "execute files/cases/l-io-1.osx 1 files/cases/l-io-2.osx 1 files/cases/l-io-3.osx 1";

        String[] inputs1 = one.split(" ");
        String[] inputs2 = two.split(" ");
        String[] inputs3 = three.split(" ");
        String[] inputs4 = four.split(" ");
        String[] inputs5 = five.split(" ");
        String[] inputs6 = six.split(" ");

        // 🔹 Quantum pairs optimized for clean surface plots
        Set<String> quantumPairs = getQuantumPairs();

        // ✅ Iterate over unique quantum pairs and execute tests
        for (String qp : quantumPairs) {
            String[] split = qp.split(",");
            int q1 = Integer.parseInt(split[0]);
            int q2 = Integer.parseInt(split[1]);

            System.out.println("Testing with Quantum1: " + q1 + ", Quantum2: " + q2);
            scheduler.setReadyQueue(new MFQReadyQueue(q1, q2));

            schedule(inputs1);
            schedule(inputs2);
            schedule(inputs3);
            schedule(inputs4);
            schedule(inputs5);
            schedule(inputs6);
        }
    }

    private static Set<String> getQuantumPairs() {
        Set<String> quantumPairs = new HashSet<>();

        // ✅ Key Baseline Points (Small & Large)
        quantumPairs.add("2,4");   // Small
        quantumPairs.add("5,10");  // Small
        quantumPairs.add("10,20"); // Medium
        quantumPairs.add("15,30"); // Medium
        quantumPairs.add("30,60"); // Large
        quantumPairs.add("50,100");// Large

        // ✅ Capture Key 1:2, 1:3, 2:5, 3:5 Ratios
        quantumPairs.add("6,12");  // 1:2
        quantumPairs.add("8,16");  // 1:2
        quantumPairs.add("10,30"); // 1:3
        quantumPairs.add("20,60"); // 1:3
        quantumPairs.add("40,100");// 2:5
        quantumPairs.add("50,150");// 1:3
        quantumPairs.add("60,120");// 1:2
        quantumPairs.add("80,160");// 1:2
        quantumPairs.add("120,180");// 2:3
        quantumPairs.add("150,250");// 3:5

        // ✅ Nonlinear Jumps for Interesting Surface Patterns
        quantumPairs.add("5,12");   // 1:2.4
        quantumPairs.add("10,24");  // 1:2.4
        quantumPairs.add("15,36");  // 1:2.4
        quantumPairs.add("35,75");  // 1:2.14

        // ✅ Testing High Quantums for Batch Jobs
        quantumPairs.add("100,200");
        quantumPairs.add("125,250");
        quantumPairs.add("150,300");

        // ✅ Include some "extreme" outliers for context
        quantumPairs.add("3,15");  // 1:5
        quantumPairs.add("50,200"); // 1:4
        return quantumPairs;
    }


    public int getIn() {
        return in;
    }

    public void incrementIn() {
        this.in = (in + 1) % 10;
    }

    public int getOut() {
        return out;
    }

    public void incrementOut() {
        this.out = (out + 1) % 10;
    }
}
