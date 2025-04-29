package os.queues;

import os.ProcessControlBlock;
import os.util.ProcessStatus;
import os.util.Logging;
import vm.hardware.Clock;

import java.util.LinkedList;

public class IOQueue implements Logging {
    Clock clock = Clock.getInstance();
    private final LinkedList<IoProcess> ioQueue = new LinkedList<>();

    public void add(ProcessControlBlock pcb) {
        log("Adding process " + pcb.getPid() + " to IO queue");
        pcb.setStatus(ProcessStatus.WAITING, QueueId.IO_QUEUE);
        IoProcess ioProcess = new IoProcess(pcb);
        ioQueue.add(ioProcess);
    }

    public ProcessControlBlock poll() {
        return ioQueue.poll().getPcb();
    }

    public int size() {
        return ioQueue.size();
    }

    public boolean isEmpty() {
        return ioQueue.isEmpty();
    }

    public boolean isReadyToLeave() {
        if (!ioQueue.isEmpty()) {
            IoProcess ioProcess = ioQueue.peek();
            return ioProcess.getIoTime() <= clock.getTime();
        }

        return false;
    }

    static class IoProcess implements Logging {
        private final ProcessControlBlock pcb;
        private final int ioTime;

        public IoProcess(ProcessControlBlock pcb) {
            this.pcb = pcb;
            this.ioTime = getRandomTime() + Clock.getInstance().getTime();
            log("IO release time for process " + pcb.getPid() + " is " + ioTime);
        }

        public ProcessControlBlock getPcb() {
            return pcb;
        }

        public int getIoTime() {
            return ioTime;
        }

        private int getRandomTime() {
            return (int) (Math.random() * 10) + 1;
        }
    }
}



