package vm.hardware;

import os.ProcessControlBlock;
import os.util.Logging;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.LinkedList;

/**
 * SemaphoreManager manages semaphores for process synchronization.
 * It provides operations for creating, waiting on, and signaling semaphores.
 */
public class SemaphoreManager implements Logging {
    private static SemaphoreManager instance;
    
    private final Map<String, Semaphore> semaphoreMap = new HashMap<>();
    
    private SemaphoreManager() {
    }
    
    public static SemaphoreManager getInstance() {
        if (instance == null) {
            instance = new SemaphoreManager();
        }
        return instance;
    }
    
    /**
     * Initialize a semaphore with a given name and initial value
     * 
     * @param name The name of the semaphore
     * @param initialValue The initial value of the semaphore
     * @return true if the semaphore was successfully initialized, false otherwise
     */
    public boolean semInit(String name, int initialValue) {
        if (name == null || name.isEmpty()) {
            logError("Invalid semaphore name");
            return false;
        }
        
        if (initialValue < 0) {
            logError("Invalid initial value for semaphore: " + initialValue);
            return false;
        }
        
        if (semaphoreMap.containsKey(name)) {
            logError("Semaphore already exists: " + name);
            return false;
        }
        
        Semaphore semaphore = new Semaphore(name, initialValue);
        semaphoreMap.put(name, semaphore);
        log("Initialized semaphore: " + name + " with value: " + initialValue);
        return true;
    }
    
    /**
     * Wait (P) operation on a semaphore
     * 
     * @param name The name of the semaphore
     * @param pcb The process control block of the waiting process
     * @return true if the wait operation was successful, false otherwise
     */
    public boolean semWait(String name, ProcessControlBlock pcb) {
        if (name == null || name.isEmpty()) {
            logError("Invalid semaphore name");
            return false;
        }
        
        Semaphore semaphore = semaphoreMap.get(name);
        if (semaphore == null) {
            logError("Semaphore does not exist: " + name);
            return false;
        }
        
        return semaphore.wait(pcb);
    }
    
    /**
     * Signal (V) operation on a semaphore
     * 
     * @param name The name of the semaphore
     * @return The next process to run, or null if no process was waiting
     */
    public ProcessControlBlock semSignal(String name) {
        if (name == null || name.isEmpty()) {
            logError("Invalid semaphore name");
            return null;
        }
        
        Semaphore semaphore = semaphoreMap.get(name);
        if (semaphore == null) {
            logError("Semaphore does not exist: " + name);
            return null;
        }
        
        return semaphore.signal();
    }
    
    /**
     * Get a semaphore by name
     * 
     * @param name The name of the semaphore
     * @return The semaphore, or null if it doesn't exist
     */
    public Semaphore getSemaphore(String name) {
        return semaphoreMap.get(name);
    }
    
    /**
     * Remove a semaphore
     * 
     * @param name The name of the semaphore
     * @return true if the semaphore was successfully removed, false otherwise
     */
    public boolean semRemove(String name) {
        if (name == null || name.isEmpty()) {
            logError("Invalid semaphore name");
            return false;
        }
        
        if (!semaphoreMap.containsKey(name)) {
            logError("Semaphore does not exist: " + name);
            return false;
        }
        
        semaphoreMap.remove(name);
        log("Removed semaphore: " + name);
        return true;
    }
    
    public static class Semaphore implements Logging {
        private final String name;
        private int value;
        private final Queue<ProcessControlBlock> waitingQueue;
        
        public Semaphore(String name, int initialValue) {
            this.name = name;
            this.value = initialValue;
            this.waitingQueue = new LinkedList<>();
        }
        
        public String getName() {
            return name;
        }
        
        public int getValue() {
            return value;
        }
        
        /**
         * Wait (P) operation
         * 
         * @param pcb The process control block of the waiting process
         * @return true if the process can continue, false if it must block
         */
        public synchronized boolean wait(ProcessControlBlock pcb) {
            value--;
            log("Semaphore " + name + ": value decremented to " + value);
            
            if (value < 0) {
                // Process must block
                waitingQueue.add(pcb);
                log("Process " + pcb.getPid() + " blocked on semaphore " + name);
                return false;
            }
            
            return true;
        }
        
        public synchronized ProcessControlBlock signal() {
            value++;
            log("Semaphore " + name + ": value incremented to " + value);
            
            if (value <= 0) {
                ProcessControlBlock pcb = waitingQueue.poll();
                if (pcb != null) {
                    log("Process " + pcb.getPid() + " unblocked from semaphore " + name);
                    return pcb;
                }
            }
            
            return null;
        }
        
        public int getWaitingCount() {
            return waitingQueue.size();
        }
    }
} 