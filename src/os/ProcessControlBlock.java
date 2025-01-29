package os;

public class ProcessControlBlock {
    private int pid;
    private ProcessStatus status;
    private int programSize;


    private int[] registers = new int[12];


    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public ProcessStatus getStatus() {
        return status;
    }

    public void setStatus(ProcessStatus status) {
        this.status = status;
    }

    public int getProgramSize() {
        return programSize;
    }

    public void setProgramSize(int programSize) {
        this.programSize = programSize;
    }

    public int getPc() {
        return registers[11];
    }

    public void setPc(int pc) {
        registers[11] = pc;
    }

    public void setRegisters(int[] registers) {
        System.arraycopy(registers, 0, this.registers, 0, registers.length);
    }

    public int[] getRegisters() {
        return registers;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    private int codeStart;
    private String filePath;


    public ProcessControlBlock(int pid) {
        this.pid = pid;
        this.status = ProcessStatus.NEW;
    }


    public int getCodeStart() {
        return codeStart;
    }

    public void setCodeStart(int codeStart) {
        this.codeStart = codeStart;
    }
}
