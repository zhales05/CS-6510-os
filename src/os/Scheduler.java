package os;

import java.util.LinkedList;

public class Scheduler {
    private final LinkedList<String> jobQueue = new LinkedList<>();
    private final LinkedList<ProcessControlBlock> readyQueue = new LinkedList<>();

    private static Scheduler instance;

    private Scheduler() {
    }

    public static Scheduler getInstance() {
        if (instance == null) {
            instance = new Scheduler();
        }

        return instance;
    }

    public void addJob(String fileName) {
        jobQueue.add(fileName);
    }
}
