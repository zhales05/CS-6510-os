package os.util;

import os.queues.QueueId;
import vm.hardware.Clock;

public class ProcessExecutionBurst {
    private static final Clock clock = Clock.getInstance();

    private final int start;
    private Integer end;
    private final QueueId queueId;

    private boolean burstFinished = false;

    public ProcessExecutionBurst(QueueId queueId) {
        this.queueId = queueId;
        start = clock.getTime();
    }

    public void end() {
        end = clock.getTime();
    }

    public int getExecutionTime() {
        if (end == null) {
            throw new IllegalStateException("Process has not ended yet");
        }

        return end - start;
    }

    public QueueId getQueueId() {
        return queueId;
    }

    public int getStart() {
        return start;
    }

    public Integer getEnd() {
        return end;
    }

    public void setEnd() {
        this.end = clock.getTime();
    }

    public boolean isBurstFinished() {
        return burstFinished;
    }

    public void setBurstFinished(boolean burstFinished) {
        this.burstFinished = burstFinished;
    }
}
