package vm.hardware;

import os.util.Logging;
import java.util.HashMap;
import java.util.Map;

public class SharedMemoryManager implements Logging {
    private static SharedMemoryManager instance;
    
    public static final int SHM_READ = 1;
    public static final int SHM_WRITE = 2;
    public static final int SHM_READ_WRITE = 3;
    
    private final Map<String, SharedMemorySegment> sharedMemoryMap = new HashMap<>();
    
    private SharedMemoryManager() {
    }
    
    public static SharedMemoryManager getInstance() {
        if (instance == null) {
            instance = new SharedMemoryManager();
        }
        return instance;
    }
    
    public int shmOpen(String name, int mode, int size) {
        if (name == null || name.isEmpty()) {
            logError("Invalid shared memory name");
            return -1;
        }
        
        if (sharedMemoryMap.containsKey(name)) {
            SharedMemorySegment segment = sharedMemoryMap.get(name);
            log("Accessing existing shared memory segment: " + name);
            return segment.getAddress();
        }
        
        if (size <= 0) {
            logError("Invalid shared memory size: " + size);
            return -1;
        }
        
        if (mode != SHM_READ && mode != SHM_WRITE && mode != SHM_READ_WRITE) {
            logError("Invalid shared memory mode: " + mode);
            return -1;
        }
        
        try {
            SharedMemorySegment segment = new SharedMemorySegment(name, mode, size);
            sharedMemoryMap.put(name, segment);
            log("Created new shared memory segment: " + name + " with size: " + size);
            return segment.getAddress();
        } catch (Exception e) {
            logError("Failed to create shared memory segment: " + e.getMessage());
            return -1;
        }
    }
    
    public boolean shmUnlink(String name) {
        if (name == null || name.isEmpty()) {
            logError("Invalid shared memory name");
            return false;
        }
        
        if (!sharedMemoryMap.containsKey(name)) {
            logError("Shared memory segment does not exist: " + name);
            return false;
        }
        
        sharedMemoryMap.remove(name);
        log("Removed shared memory segment: " + name);
        return true;
    }
    
    public SharedMemorySegment getSegment(String name) {
        return sharedMemoryMap.get(name);
    }
    
    public SharedMemorySegment getSegmentByAddress(int address) {
        for (SharedMemorySegment segment : sharedMemoryMap.values()) {
            if (segment.getAddress() == address) {
                return segment;
            }
        }
        return null;
    }
    
    public static class SharedMemorySegment {
        private final String name;
        private final int mode;
        private final int size;
        private final byte[] data;
        private final int address;
        
        private static int nextAddress = 0x10000000;
        
        public SharedMemorySegment(String name, int mode, int size) {
            this.name = name;
            this.mode = mode;
            this.size = size;
            this.data = new byte[size];
            this.address = nextAddress;
            nextAddress += size;
        }
        
        public String getName() {
            return name;
        }
        
        public int getMode() {
            return mode;
        }
        
        public int getSize() {
            return size;
        }
        
        public byte[] getData() {
            return data;
        }
        
        public int getAddress() {
            return address;
        }
        
        public byte readByte(int offset) {
            if (offset < 0 || offset >= size) {
                throw new IndexOutOfBoundsException("Offset out of bounds: " + offset);
            }
            
            if ((mode & SHM_READ) == 0) {
                throw new IllegalStateException("Segment not opened for reading");
            }
            
            return data[offset];
        }
        
        public void writeByte(int offset, byte value) {
            if (offset < 0 || offset >= size) {
                throw new IndexOutOfBoundsException("Offset out of bounds: " + offset);
            }
            
            if ((mode & SHM_WRITE) == 0) {
                throw new IllegalStateException("Segment not opened for writing");
            }
            
            data[offset] = value;
        }
    }
}