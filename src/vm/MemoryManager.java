package vm;

import os.OperatingSystem;
import os.ProcessControlBlock;
import os.util.Logging;
import vm.hardware.Memory;

import java.util.*;

/**
 * Memory Manager class that handles virtual memory operations
 * including page fault handling and page replacement with ESCA
 */
public class MemoryManager implements Logging {
    private static MemoryManager instance;
    private final Memory memory = Memory.getInstance();
    private OperatingSystem os;
    
    // Configuration parameters
    private int pageSize = 256; // Default page size in bytes
    private int totalPageFrames = 16; // Default number of page frames in physical memory
    
    // Physical memory frames tracking
    private final Map<Integer, Integer> frameToProcess = new HashMap<>(); // Maps frame number to process ID
    private final Map<Integer, Integer> frameToPage = new HashMap<>(); // Maps frame number to page number
    private final Set<Integer> freeFrames = new HashSet<>(); // Tracks free frames
    
    // Page fault statistics
    private int totalPageFaults = 0;
    private final Map<Integer, Integer> pageFaultsPerProcess = new HashMap<>();
    
    private MemoryManager() {
        initializeFrames();
    }
    
    public static MemoryManager getInstance() {
        if (instance == null) {
            instance = new MemoryManager();
        }
        return instance;
    }
    
    /**
     * Initialize physical memory frames
     */
    private void initializeFrames() {
        freeFrames.clear();
        for (int i = 0; i < totalPageFrames; i++) {
            freeFrames.add(i);
        }
        log("Initialized " + totalPageFrames + " physical memory frames with page size " + pageSize);
    }
    
    /**
     * Set the page size for virtual memory
     */
    public void setPageSize(int pageSize) {
        if (pageSize <= 0) {
            logError("Invalid page size: " + pageSize);
            return;
        }
        this.pageSize = pageSize;
        log("Page size set to " + pageSize + " bytes");
        
        // Reset memory frames when configuration changes
        initializeFrames();
    }
    
    /**
     * Set the number of page frames in physical memory
     */
    public void setPageFrameCount(int pageCount) {
        if (pageCount <= 0) {
            logError("Invalid page count: " + pageCount);
            return;
        }
        this.totalPageFrames = pageCount;
        log("Total page frames set to " + pageCount);
        
        // Reset memory frames when configuration changes
        initializeFrames();
    }
    
    /**
     * Set the operating system reference for process lookup
     */
    public void setOperatingSystem(OperatingSystem os) {
        this.os = os;
    }
    
    /**
     * Convert a logical address to physical address for a process
     */
    public int getPhysicalAddress(ProcessControlBlock pcb, int logicalAddress) {
        int pageNumber = logicalAddress / pageSize;
        int offset = logicalAddress % pageSize;
        
        log("Process " + pcb.getPid() + " accessing logical address " + logicalAddress + 
            " (Page: " + pageNumber + ", Offset: " + offset + ")");
        
        // Check if page is valid (already in memory)
        if (!pcb.getPageTable().isPageValid(pageNumber)) {
            // Page fault - need to handle it
            handlePageFault(pcb, pageNumber);
        }
        
        // Mark the page as referenced (for ESCA)
        pcb.getPageTable().markReferenced(pageNumber);
        
        // Get the frame number from page table
        int frameNumber = pcb.getPageTable().getEntry(pageNumber).getFrameNumber();
        
        // Calculate the physical address
        int physicalAddress = frameNumber * pageSize + offset;
        log("Mapped to physical address: " + physicalAddress + " (Frame: " + frameNumber + ", Offset: " + offset + ")");
        
        return physicalAddress;
    }
    
    /**
     * Handle a page fault by loading the requested page into memory
     */
    private void handlePageFault(ProcessControlBlock pcb, int pageNumber) {
        log("Page fault for process " + pcb.getPid() + ", page " + pageNumber);
        
        // Track page fault statistics
        totalPageFaults++;
        pageFaultsPerProcess.put(pcb.getPid(), 
            pageFaultsPerProcess.getOrDefault(pcb.getPid(), 0) + 1);
        
        // Find a free frame or select a victim frame using ESCA
        int frameNumber;
        if (!freeFrames.isEmpty()) {
            // Use a free frame if available
            frameNumber = freeFrames.iterator().next();
            freeFrames.remove(frameNumber);
            log("Allocated free frame " + frameNumber + " for process " + pcb.getPid() + ", page " + pageNumber);
        } else {
            // No free frames, need to replace a page
            frameNumber = selectVictimFrame();
            log("Selected victim frame " + frameNumber + " for replacement");
            
            // Get the process and page that currently own this frame
            int victimPid = frameToProcess.get(frameNumber);
            int victimPage = frameToPage.get(frameNumber);
            
            // Get the victim process's PCB
            log("Page " + victimPage + " of process " + victimPid + " selected for replacement");
            
            // Simulate updating the victim's page table to mark page as invalid
            ProcessControlBlock victimPcb = findProcessById(victimPid);
            if (victimPcb != null) {
                victimPcb.getPageTable().getEntry(victimPage).setValid(false);
            }
        }
        
        // Simulate loading the page into memory
        // In real implementation, you'd copy from disk/storage to memory
        log("Loading page " + pageNumber + " for process " + pcb.getPid() + " into frame " + frameNumber);
        
        // Update page table
        PageTable.PageTableEntry entry = pcb.getPageTable().getEntry(pageNumber);
        entry.setFrameNumber(frameNumber);
        entry.setValid(true);
        entry.setReferenced(true);
        
        // Update frame tracking
        frameToProcess.put(frameNumber, pcb.getPid());
        frameToPage.put(frameNumber, pageNumber);
    }
    
    /**
     * Find a process by its ID using the operating system
     */
    private ProcessControlBlock findProcessById(int pid) {
        if (os != null) {
            return os.findProcessById(pid);
        }
        return null;
    }
    
    /**
     * Select a victim frame using Enhanced Second-Chance Algorithm
     * This simplified version only considers valid and reference bits
     */
    private int selectVictimFrame() {
        // Implement simplified ESCA with only reference bit
        // We'll have 2 classes in order of preference for page replacement:
        // Class 1: Not referenced - Pages that haven't been referenced recently
        // Class 2: Referenced - Pages that have been referenced
        
        List<Integer> frames = new ArrayList<>(frameToProcess.keySet());
        
        // Loop until we find a victim
        int pointer = 0;
        while (true) {
            // First pass: Look for unreferenced pages
            for (int i = 0; i < frames.size(); i++) {
                int frame = frames.get((pointer + i) % frames.size());
                int pid = frameToProcess.get(frame);
                int page = frameToPage.get(frame);
                
                ProcessControlBlock pcb = findProcessById(pid);
                if (pcb == null) {
                    // If we can't find the PCB, just return this frame
                    return frame;
                }
                
                PageTable.PageTableEntry entry = pcb.getPageTable().getEntry(page);
                
                // Class 1: Not referenced
                if (!entry.isReferenced()) {
                    pointer = (pointer + i + 1) % frames.size();
                    return frame;
                }
            }
            
            // If we get here, all pages are referenced - reset reference bits and try again
            for (int i = 0; i < frames.size(); i++) {
                int frame = frames.get((pointer + i) % frames.size());
                int pid = frameToProcess.get(frame);
                int page = frameToPage.get(frame);
                
                ProcessControlBlock pcb = findProcessById(pid);
                if (pcb != null) {
                    // Reset reference bit to give pages a second chance
                    pcb.getPageTable().getEntry(page).setReferenced(false);
                }
            }
            
            // If we're still here, we'll just take the next frame in order
            int selectedFrame = frames.get(pointer);
            pointer = (pointer + 1) % frames.size();
            return selectedFrame;
        }
    }
    
    /**
     * Allocate memory for a process's pages
     */
    public void allocateMemory(ProcessControlBlock pcb) {
        log("Allocating memory for process " + pcb.getPid());
        
        // With demand paging, we don't pre-allocate memory, 
        // instead pages are loaded as they are accessed
    }
    
    /**
     * Free memory used by a process
     */
    public void freeMemory(ProcessControlBlock pcb) {
        log("Freeing memory for process " + pcb.getPid());
        
        // Find all frames used by this process and mark them as free
        List<Integer> framesToFree = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : frameToProcess.entrySet()) {
            if (entry.getValue() == pcb.getPid()) {
                framesToFree.add(entry.getKey());
            }
        }
        
        // Free the frames
        for (int frame : framesToFree) {
            frameToProcess.remove(frame);
            frameToPage.remove(frame);
            freeFrames.add(frame);
            log("Freed frame " + frame + " from process " + pcb.getPid());
        }
    }
    
    /**
     * Get page fault statistics
     */
    public int getTotalPageFaults() {
        return totalPageFaults;
    }
    
    public int getPageFaultsForProcess(int pid) {
        return pageFaultsPerProcess.getOrDefault(pid, 0);
    }
    
    /**
     * Get memory usage information for ps command
     */
    public String getMemoryUsageInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("Memory Configuration:\n");
        sb.append("Page Size: ").append(pageSize).append(" bytes\n");
        sb.append("Total Physical Frames: ").append(totalPageFrames).append("\n");
        sb.append("Free Frames: ").append(freeFrames.size()).append("\n");
        sb.append("Used Frames: ").append(totalPageFrames - freeFrames.size()).append("\n");
        sb.append("Page Faults: ").append(totalPageFaults).append("\n");
        
        return sb.toString();
    }
    
    /**
     * Get page table info for a process
     */
    public String getPageTableInfo(ProcessControlBlock pcb) {
        if (pcb == null) {
            return "Process not found";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("Page Table for Process ").append(pcb.getPid()).append(":\n");
        
        Map<Integer, PageTable.PageTableEntry> entries = pcb.getPageTable().getAllEntries();
        if (entries.isEmpty()) {
            sb.append("  No pages allocated yet\n");
        } else {
            sb.append(String.format("  %-5s %-8s %-8s %-8s\n", 
                "Page", "Frame", "Valid", "Ref"));
            
            for (Map.Entry<Integer, PageTable.PageTableEntry> entry : entries.entrySet()) {
                PageTable.PageTableEntry pte = entry.getValue();
                sb.append(String.format("  %-5d %-8d %-8s %-8s\n",
                    entry.getKey(), 
                    pte.getFrameNumber(),
                    pte.isValid() ? "Yes" : "No",
                    pte.isReferenced() ? "Yes" : "No"));
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Reset all memory management data
     */
    public void reset() {
        initializeFrames();
        frameToProcess.clear();
        frameToPage.clear();
        totalPageFaults = 0;
        pageFaultsPerProcess.clear();
        log("Memory manager reset");
    }
    
    /**
     * Get the current page size
     */
    public int getPageSize() {
        return pageSize;
    }
} 