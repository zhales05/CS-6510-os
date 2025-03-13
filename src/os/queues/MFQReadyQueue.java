package os.queues;

import os.ProcessControlBlock;

public class MFQReadyQueue implements IReadyQueue{
    private final int quantum1;
    private final int quantum2;
    private int quantumCounter;

    private final RRReadyQueue rrReadyQueue1;
    private final RRReadyQueue rrReadyQueue2;
    private final FCFSReadyQueue fcfsReadyQueue;

    public MFQReadyQueue(int quantum1, int quantum2) {
        this.quantum1 = quantum1;
        this.quantum2 = quantum2;

        rrReadyQueue1 = new RRReadyQueue(quantum1);
        rrReadyQueue2 = new RRReadyQueue(quantum2);
        fcfsReadyQueue = new FCFSReadyQueue();

        quantumCounter = 0;
    }

    @Override
    public void addProcess(ProcessControlBlock pcb) {
    //evaluate what queue to add process to

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

    @Override
    public boolean incrementQuantumCounter() {
        //TODO change between queues here
        return false;
    }

    @Override
    public int getQuantum() {
        return 0;
    }

    @Override
    public int getQuantumCounter() {
        return 0;
    }

    @Override
    public void resetQuantumCounter() {
        //counter will go to zero
    }

    @Override
    public QueueIds getQueueId() {
        //return 1,2,3 based on the queue
        return QueueIds.MFQ_QUEUE_1;
    }
}
