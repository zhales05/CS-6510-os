package os.queues;

import os.ProcessControlBlock;
import os.ProcessStatus;

import java.util.LinkedList;
import java.util.Queue;

public class RRReadyQueue implements IReadyQueue{
    Queue<ProcessControlBlock> queue = new LinkedList<>();
    private final int quantum;
    private int quantumCounter = 0;

    public RRReadyQueue(int quantum) {
        this.quantum = quantum;
    }

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
        quantumCounter++;
        return quantumCounter == quantum;
    }

    @Override
    public int getQuantum() {
        return quantum;
    }

    @Override
    public int getQuantumCounter() {
        return quantumCounter;
    }

    @Override
    public void resetQuantumCounter() {
        quantumCounter = 0;
    }

    @Override
    public QueueId getQueueId() {
        return QueueId.RR_QUEUE;
    }

    @Override
    public String toString() {
        return "RRReadyQueue " + quantum;
    }

}
