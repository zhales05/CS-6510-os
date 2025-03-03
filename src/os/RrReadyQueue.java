package os;

import java.util.LinkedList;
import java.util.Queue;

public class RrReadyQueue implements IReadyQueue{
    Queue<ProcessControlBlock> queue = new LinkedList<>();

    @Override
    public void addProcess(ProcessControlBlock pcb) {

    }

    @Override
    public ProcessControlBlock getNextProcess() {
        return null;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public int size() {
        return 0;
    }
}
