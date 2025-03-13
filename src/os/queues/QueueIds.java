package os.queues;

public enum QueueIds {
    MFQ_QUEUE_1(1),
    MFQ_QUEUE_2(2),
    MFQ_QUEUE_3(3),
    RR_QUEUE(4),
    FCFS_QUEUE(5),
    IO_QUEUE(6),
    TERMINATED_QUEUE(7),
    JOB_QUEUE(8),
    RUNNING_QUEUE(9);

    private final int id;

    QueueIds(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
