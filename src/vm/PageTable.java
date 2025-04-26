package vm;

import java.util.HashMap;
import java.util.Map;

/**
 * Page table for managing virtual to physical memory mapping
 */
public class PageTable {
    // Page table entry structure
    public static class PageTableEntry {
        private int frameNumber; // Physical frame number
        private boolean valid;   // Valid/invalid bit
        private boolean referenced; // Referenced bit for ESCA
        
        public PageTableEntry() {
            this.frameNumber = -1;
            this.valid = false;
            this.referenced = false;
        }
        
        public int getFrameNumber() {
            return frameNumber;
        }
        
        public void setFrameNumber(int frameNumber) {
            this.frameNumber = frameNumber;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public void setValid(boolean valid) {
            this.valid = valid;
        }
        
        public boolean isReferenced() {
            return referenced;
        }
        
        public void setReferenced(boolean referenced) {
            this.referenced = referenced;
        }
        
        @Override
        public String toString() {
            return String.format("Frame: %d, Valid: %s, Ref: %s", 
                frameNumber, valid, referenced);
        }
    }
    
    private Map<Integer, PageTableEntry> entries;
    
    public PageTable() {
        this.entries = new HashMap<>();
    }
    
    /**
     * Get a page table entry for a specific page number
     */
    public PageTableEntry getEntry(int pageNumber) {
        if (!entries.containsKey(pageNumber)) {
            entries.put(pageNumber, new PageTableEntry());
        }
        return entries.get(pageNumber);
    }
    
    /**
     * Set a page table entry for a specific page number
     */
    public void setEntry(int pageNumber, PageTableEntry entry) {
        entries.put(pageNumber, entry);
    }
    
    /**
     * Check if a page is valid (loaded in physical memory)
     */
    public boolean isPageValid(int pageNumber) {
        PageTableEntry entry = getEntry(pageNumber);
        return entry.isValid();
    }
    
    /**
     * Mark a page as referenced (used in ESCA)
     */
    public void markReferenced(int pageNumber) {
        PageTableEntry entry = getEntry(pageNumber);
        entry.setReferenced(true);
    }
    
    /**
     * Get all entries in the page table
     */
    public Map<Integer, PageTableEntry> getAllEntries() {
        return entries;
    }
} 