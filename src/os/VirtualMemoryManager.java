package os;

import vm.hardware.Memory;

public class VirtualMemoryManager {
    private static int pageSize = 512;  // default

    public static int getPageSize() {
        return pageSize;
    }

    public static void setPageSize(int size) {
        pageSize = size;
    }

    public static int getTotalFrames() {
        return Memory.getMemorySize() / pageSize;
    }

    public static int getFrameSize() {
        return pageSize;  // Same as pageSize by definition
    }
}
