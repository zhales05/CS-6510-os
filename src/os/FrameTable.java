package os;

import os.util.Logging;


public class FrameTable implements Logging {
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
                return i;
            }
        }
        return -1; // No free frames
    }


    public void freeFrame(int frameNumber) {
        if (frameUsed[frameNumber]) {
            frameUsed[frameNumber] = false;
        }
    }


    public boolean isFrameFree(int frameNumber) {
        return !frameUsed[frameNumber];
    }

    public void reinitialize() {
        frameUsed = new boolean[VirtualMemoryManager.getTotalFrames()];
    }


    public int getTotalFrames() {
        return frameUsed.length;
    }
}
