package os.queues;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public enum QueueId {
    MFQ_QUEUE_1(1),
    MFQ_QUEUE_2(2),
    MFQ_QUEUE_3(3),
    RR_QUEUE(4),
    FCFS_QUEUE(5),
    IO_QUEUE(6),
    TERMINATED_QUEUE(7),
    JOB_QUEUE(8),
    RUNNING_QUEUE(9),
    BLOCKED_QUEUE(10);

    private final int id;

    public static final EnumSet<QueueId> READY_QUEUES = EnumSet.of(MFQ_QUEUE_1, MFQ_QUEUE_2, MFQ_QUEUE_3, RR_QUEUE, FCFS_QUEUE);

    QueueId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
