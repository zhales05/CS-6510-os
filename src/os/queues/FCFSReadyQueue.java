package os.queues;

import os.ProcessControlBlock;

import java.util.LinkedList;
import java.util.Queue;

public class FCFSReadyQueue implements IReadyQueue {
    Queue<ProcessControlBlock> queue = new LinkedList<>();

    @Override
    public void addProcess(ProcessControlBlock pcb) {
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

    @Override
    public boolean incrementQuantumCounter() {
        //no action needed fifo baby
        return false;
    }

    @Override
    public int getQuantum() {
        //FIFO no quantum
        return -1;
    }

    @Override
    public int getQuantumCounter() {
        return -1;
    }
}
