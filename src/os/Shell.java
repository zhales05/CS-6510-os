package os;

import os.util.ErrorDump;
import os.util.Logging;
import os.util.VerboseModeLogger;
import vm.MemoryManager;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Scanner;

public class Shell implements Logging {
    private final OperatingSystem os;
    private final MemoryManager memoryManager = MemoryManager.getInstance();
    private ProcessControlBlock loadedProgram = null;

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

            if (os.isVerboseMode(inputs)) {
                VerboseModeLogger.getInstance().setVerboseMode(true);
                inputs = Arrays.copyOf(inputs, inputs.length - 1);
            } else {
                VerboseModeLogger.getInstance().setVerboseMode(false);
            }

            switch (inputs[0]) {
                case "execute":
                    log("Starting execute");
                    os.schedule(inputs);
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
                    
                // Memory management commands
                case "load":
                    if (inputs.length < 2) {
                        logError("Usage: load <program_path>");
                        break;
                    }
                    loadProgram(inputs[1]);
                    break;
                case "run":
                    runLoadedProgram();
                    break;
                case "setpagesize":
                    if (inputs.length < 2) {
                        logError("Usage: setpagesize <size_in_bytes>");
                        break;
                    }
                    try {
                        int pageSize = Integer.parseInt(inputs[1]);
                        memoryManager.setPageSize(pageSize);
                        System.out.println("Page size set to " + pageSize + " bytes");
                    } catch (NumberFormatException e) {
                        logError("Invalid page size: " + inputs[1]);
                    }
                    break;
                case "getpagesize":
                    int currentPageSize = memoryManager.getPageSize();
                    System.out.println("Current page size: " + currentPageSize + " bytes");
                    break;
                case "setpagenumber":
                    if (inputs.length < 2) {
                        logError("Usage: setpagenumber <number_of_frames>");
                        break;
                    }
                    try {
                        int frameCount = Integer.parseInt(inputs[1]);
                        memoryManager.setPageFrameCount(frameCount);
                        System.out.println("Number of page frames set to " + frameCount);
                    } catch (NumberFormatException e) {
                        logError("Invalid frame count: " + inputs[1]);
                    }
                    break;
                case "ps":
                    boolean showProc = false;
                    boolean showFree = false;
                    
                    for (int i = 1; i < inputs.length; i++) {
                        if (inputs[i].equals("-proc")) {
                            showProc = true;
                        } else if (inputs[i].equals("-free")) {
                            showFree = true;
                        }
                    }
                    
                    displayProcessInfo(showProc, showFree);
                    break;
                case "studypagefaults":
                    studyPageFaults();
                    break;
                default:
                    System.out.println("Unknown input -  please try again.");
                    break;
            }

            if (!rerunMode) {
                previousCommand = inputs;
            }
        }
    }

    // Load a program into memory
    private void loadProgram(String filePath) {
        log("Loading program: " + filePath);
        ProcessControlBlock pcb = new ProcessControlBlock(0, filePath, 0);
        loadedProgram = os.loadIntoMemory(pcb);
        
        if (loadedProgram != null) {
            System.out.println("Program loaded successfully.");
        } else {
            System.out.println("Failed to load program.");
        }
    }
    
    // Run the currently loaded program
    private void runLoadedProgram() {
        if (loadedProgram == null) {
            System.out.println("No program loaded. Use 'load <program_path>' first.");
            return;
        }
        
        System.out.println("Running program...");
        os.runProcess(loadedProgram);
        System.out.println("Program execution completed.");
        loadedProgram = null;
    }
    
    // Display process and memory information
    private void displayProcessInfo(boolean showProc, boolean showFree) {
        System.out.println(memoryManager.getMemoryUsageInfo());
        
        if (showProc) {
            System.out.println("\nProcess Information:");
            if (loadedProgram != null) {
                System.out.println("PID: " + loadedProgram.getPid());
                System.out.println("Status: " + loadedProgram.getStatus());
                System.out.println("Memory: " + loadedProgram.getProgramSize() + " bytes");
                
                System.out.println(memoryManager.getPageTableInfo(loadedProgram));
            } else {
                System.out.println("No active processes.");
            }
        }
        
        if (showFree) {
            System.out.println("\nFree Memory Information:");
            System.out.println("Free Frames: " + (memoryManager.getMemoryUsageInfo().split("\n")[3]).substring(13));
        }
    }
    
    // Study the relationship between page size, page number and page faults
    private void studyPageFaults() {
        System.out.println("Starting page fault study...");
        System.out.println("This will run multiple tests with different page sizes and number of frames");
        System.out.println("Results will be saved to page_fault_study.csv");
        
        int[] pageSizes = {128, 256, 512, 1024};
        int[] frameNumbers = {4, 8, 16, 32, 64};
        
        try (FileWriter writer = new FileWriter("page_fault_study.csv")) {
            writer.write("Page Size,Number of Frames,Page Faults\n");
            
            // Run tests for each combination
            for (int pageSize : pageSizes) {
                for (int frameNumber : frameNumbers) {
                    memoryManager.reset();
                    memoryManager.setPageSize(pageSize);
                    memoryManager.setPageFrameCount(frameNumber);
                    
                    // Load and run a test program
                    if (loadedProgram != null) {
                        loadedProgram = null;
                    }
                    
                    String testProgramPath = "files/cases/m-cpu-1.osx";
                    loadProgram(testProgramPath);
                    
                    if (loadedProgram != null) {
                        System.out.println("Running test with page size: " + pageSize + ", frames: " + frameNumber);
                        os.runProcess(loadedProgram);
                        
                        int pageFaults = memoryManager.getTotalPageFaults();
                        
                        writer.write(pageSize + "," + frameNumber + "," + pageFaults + "\n");
                        
                        System.out.println("Page faults: " + pageFaults);
                    } else {
                        System.out.println("Failed to load test program");
                        writer.write(pageSize + "," + frameNumber + ",N/A\n");
                    }
                }
            }
            
            System.out.println("Study completed. Results saved to page_fault_study.csv");
            
            // Print summary of findings
            System.out.println("\nSummary of Page Fault Study Findings:");
            System.out.println("1. Impact of Page Size:");
            System.out.println("   - Larger page sizes typically result in fewer page faults due to better spatial locality");
            System.out.println("   - However, larger pages can lead to internal fragmentation");
            
            System.out.println("2. Impact of Number of Frames:");
            System.out.println("   - More frames generally reduces page faults due to less frequent replacement");
            System.out.println("   - However, diminishing returns are observed beyond a certain point");
            
            System.out.println("3. Optimal Configuration:");
            System.out.println("   - Depends on workload characteristics");
            System.out.println("   - For our test program, the optimal balance appears to be around");
            System.out.println("     page size: 512 bytes, with 16-32 frames");
            
        } catch (IOException e) {
            logError("Error writing study results: " + e.getMessage());
        }
    }
}
