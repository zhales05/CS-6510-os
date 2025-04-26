package os;

public class PageTableEntry {
    int frameNumber;
    boolean valid;
    boolean referenceBit;
    boolean dirtyBit;

    public boolean isReferenceBit() {
        return referenceBit;
    }

    public void setReferenceBit(boolean referenceBit) {
        this.referenceBit = referenceBit;
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

    public boolean isDirtyBit() {
        return dirtyBit;
    }

    public void setDirtyBit(boolean dirtyBit) {
        this.dirtyBit = dirtyBit;
    }

}
