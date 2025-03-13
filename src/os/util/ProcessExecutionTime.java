package os.util;

import os.queues.QueueIds;
import vm.hardware.Clock;

public class ProcessExecutionTime {
    private static final Clock clock = Clock.getInstance();

    private final int start;
    private Integer end;
    private final QueueIds queueId;

    public ProcessExecutionTime(QueueIds queueId) {
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

    public QueueIds getQueueId() {
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
}
