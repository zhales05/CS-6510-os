package os;

import os.util.Logging;

public interface IReadyQueue extends Logging {

    void addProcess(ProcessControlBlock pcb);  // Add a process to the queue

    ProcessControlBlock getNextProcess();   // Get the next process to execute

    boolean isEmpty();          // Check if the queue is empty

    int size();

    /**
     * Increment the quantum counter
     *
     * @return true if the quantum counter has reached the quantum limit
     */
    boolean incrementQuantumCounter();

    int getQuantum();

    int getQuantumCounter();
}
