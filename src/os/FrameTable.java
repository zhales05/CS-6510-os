package os;

import java.util.Arrays;

public class FrameTable {
    private static FrameTable instance;
    private boolean[] frameUsed;

    private FrameTable(int totalFrames) {
        frameUsed = new boolean[totalFrames];
    }

    public static FrameTable getInstance() {
        if (instance == null) {
            instance = new FrameTable(VirtualMemoryManager.getTotalFrames());
        }
        return instance;
    }

    public int allocateFreeFrame() {
        for (int i = 0; i < frameUsed.length; i++) {
            if (!frameUsed[i]) {
                frameUsed[i] = true;
                return i; // frame i allocated
            }
        }
        return -1; // No free frames available
    }

    public void freeFrame(int frameNumber) {
        frameUsed[frameNumber] = false;
    }

    public boolean isFrameFree(int frameNumber) {
        return !frameUsed[frameNumber];
    }

    public void reinitialize() {
        Arrays.fill(frameUsed, false);
    }

    public int getTotalFrames() {
        return frameUsed.length;
    }
}
