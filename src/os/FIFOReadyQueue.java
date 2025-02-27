package os;

import java.util.LinkedList;
import java.util.Queue;

public class FIFOReadyQueue implements IReadyQueue {
    Queue<ProcessControlBlock> queue = new LinkedList<>();

    @Override
    public void addProcess(ProcessControlBlock pcb) {
        pcb.setStatus(ProcessStatus.READY);
        log("Adding process " + pcb.getPid() + " to ready queue");
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
