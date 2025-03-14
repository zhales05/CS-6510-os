package os.queues;

import os.ProcessControlBlock;
import os.ProcessStatus;

public class MFQReadyQueue implements IReadyQueue {
    private static final int TOTAL_QUANTUMS = 5;
    private static final double TOTAL_BURST_PERCENTAGE = 0.8;

    private final MFQReadyQueue1 rrReadyQueue1;
    private final MFQReadyQueue2 rrReadyQueue2;
    private final MFQReadyQueue3 fcfsReadyQueue;

    private IReadyQueue currentQueue;
    private int currentQuantum = -1;

    public MFQReadyQueue(int quantum1, int quantum2) {
        rrReadyQueue1 = new MFQReadyQueue1(quantum1);
        rrReadyQueue2 = new MFQReadyQueue2(quantum2);
        fcfsReadyQueue = new MFQReadyQueue3();

        currentQueue = rrReadyQueue1;
    }

    @Override
    public void addProcess(ProcessControlBlock pcb) {
        QueueId lastReadyQueue = pcb.getLastReadyQueue();
        Double burstCompletionPercentage = pcb.getBurstCompletionPercentage();
        QueueId addedQueue = null;

        if (lastReadyQueue == null) {
            rrReadyQueue1.addProcess(pcb);
            addedQueue = QueueId.MFQ_QUEUE_1;
        } else if (lastReadyQueue == QueueId.MFQ_QUEUE_1) {
            if (burstCompletionPercentage != null && burstCompletionPercentage < TOTAL_BURST_PERCENTAGE) {
                rrReadyQueue2.addProcess(pcb);
                addedQueue = QueueId.MFQ_QUEUE_2;
            } else {
                rrReadyQueue1.addProcess(pcb);
                addedQueue = QueueId.MFQ_QUEUE_1;
            }
        } else if (lastReadyQueue == QueueId.MFQ_QUEUE_2) {
            if (burstCompletionPercentage != null && burstCompletionPercentage < TOTAL_BURST_PERCENTAGE) {
                fcfsReadyQueue.addProcess(pcb);
                addedQueue = QueueId.MFQ_QUEUE_3;
            } else if (burstCompletionPercentage != null && burstCompletionPercentage > TOTAL_BURST_PERCENTAGE) {
                rrReadyQueue1.addProcess(pcb);
                addedQueue = QueueId.MFQ_QUEUE_1;
            } else {
                rrReadyQueue2.addProcess(pcb);
                addedQueue = QueueId.MFQ_QUEUE_2;
            }
        }

        pcb.setStatus(ProcessStatus.READY, addedQueue);
    }

    @Override
    public ProcessControlBlock getNextProcess() {
        determineCurrentQueue();
        return currentQueue.getNextProcess();
    }

    private void determineCurrentQueue() {
        if (currentQuantum >= TOTAL_QUANTUMS) {
            incrementCurrentQueue();
            currentQuantum = currentQueue == fcfsReadyQueue ? TOTAL_QUANTUMS : 0;
        }
    }

    private void incrementCurrentQueue(){
        while(currentQueue.isEmpty()) {
            if (currentQueue == rrReadyQueue1) {
                currentQueue = rrReadyQueue2;
            } else if (currentQueue == rrReadyQueue2) {
                currentQueue = fcfsReadyQueue;
            } else {
                currentQueue = rrReadyQueue1;
            }
        }
    }

    private boolean setCurrentQueue(IReadyQueue queue) {
        if (!queue.isEmpty()) {
            currentQueue = queue;
            log("Current Queue is now: " + currentQueue.toString());
            return true;
        }

        if (queue == rrReadyQueue1) {
            currentQueue = rrReadyQueue1;
            log("Current Queue is now: " + currentQueue.toString());
            return true;
        }

        return false;
    }


    @Override
    public boolean isEmpty() {
        return rrReadyQueue1.isEmpty() && rrReadyQueue2.isEmpty() && fcfsReadyQueue.isEmpty();
    }

    @Override
    public int size() {
        return rrReadyQueue1.size() + rrReadyQueue2.size() + fcfsReadyQueue.size();
    }

    @Override
    public boolean incrementQuantumCounter() {
        return currentQueue.incrementQuantumCounter();
    }

    @Override
    public int getQuantum() {
        return currentQueue.getQuantum();
    }

    @Override
    public int getQuantumCounter() {
        return currentQueue.getQuantumCounter();
    }

    @Override
    public void resetQuantumCounter() {
        currentQuantum++;
        currentQueue.resetQuantumCounter();
    }

    @Override
    public QueueId getQueueId() {
        return currentQueue.getQueueId();
    }

    static class MFQReadyQueue1 extends RRReadyQueue {
        public MFQReadyQueue1(int quantum) {
            super(quantum);
        }

        @Override
        public QueueId getQueueId() {
            return QueueId.MFQ_QUEUE_1;
        }

        @Override
        public void addProcess(ProcessControlBlock pcb) {
            queue.add(pcb);
        }
    }

    static class MFQReadyQueue2 extends MFQReadyQueue1 {
        public MFQReadyQueue2(int quantum) {
            super(quantum);
        }

        @Override
        public QueueId getQueueId() {
            return QueueId.MFQ_QUEUE_2;
        }
    }

    static class MFQReadyQueue3 extends FCFSReadyQueue {
        @Override
        public QueueId getQueueId() {
            return QueueId.MFQ_QUEUE_3;
        }

        @Override
        public void addProcess(ProcessControlBlock pcb) {
            queue.add(pcb);
        }
    }
}
