package os.queues;

import os.ProcessControlBlock;
import os.ProcessStatus;

import java.util.LinkedList;
import java.util.Queue;

public class FCFSReadyQueue implements IReadyQueue {
    Queue<ProcessControlBlock> queue = new LinkedList<>();

    @Override
    public void addProcess(ProcessControlBlock pcb) {
        queue.add(pcb);
        pcb.setStatus(ProcessStatus.READY, getQueueId());
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

    @Override
    public void resetQuantumCounter() {
        //yawn
    }

    @Override
    public QueueId getQueueId() {
        return QueueId.FCFS_QUEUE;
    }
}
