import os.OperatingSystem;
import vm.MemoryManager;

public class Main {
    public static void main(String[] args) {
        OperatingSystem os = new OperatingSystem();
        // Set OS reference in memory manager
        MemoryManager.getInstance().setOperatingSystem(os);
        os.startShell();
    }
}