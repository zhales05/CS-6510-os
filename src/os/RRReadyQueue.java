package os;

import java.util.LinkedList;
import java.util.Queue;

public class RRReadyQueue implements IReadyQueue{
    Queue<ProcessControlBlock> queue = new LinkedList<>();
    private final int quantum;

    public RRReadyQueue(int quantum) {
        this.quantum = quantum;
    }

    @Override
    public void addProcess(ProcessControlBlock pcb) {
        queue.add(pcb);
    }

    @Override
    public ProcessControlBlock getNextProcess() {
        return queue.poll();
    }

    @Override
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    @Override
    public int size() {
        return queue.size();
    }
}
