package vm;

import java.io.*;
import java.util.*;

public class VirtualMachine {
    private static final int MEMORY_SIZE = 1024; // Example size of memory
    private byte[] memory = new byte[MEMORY_SIZE];
    private int[] registers = new int[16]; // Example register count
    private int pc = 0; // Program Counter
    private boolean running = false;

    public void load(String filename) throws IOException {
        // Read the .osx file into memory
        try (FileInputStream fis = new FileInputStream(filename)) {
            int address = 0;
            int data;
            while ((data = fis.read()) != -1) {
                if (address >= MEMORY_SIZE) {
                    throw new IOException("Program exceeds memory size!");
                }
                memory[address++] = (byte) data;
            }
            System.out.println("Program loaded successfully into memory.");
        }
    }

    public void run() {
        running = true;
        System.out.println("Starting execution...");
        while (running) {
            try {
                fetchDecodeExecute();
            } catch (Exception e) {
                System.err.println("Execution error: " + e.getMessage());
                running = false;
            }
        }
        System.out.println("Execution finished.");
    }

    private void fetchDecodeExecute() {
        // Fetch: Get the instruction at the current PC
        byte instruction = memory[pc++];

        // Decode and Execute
        switch (instruction) {
            case 0x10: // Example: ADD instruction
                int reg1 = memory[pc++] & 0xFF;
                int reg2 = memory[pc++] & 0xFF;
                int reg3 = memory[pc++] & 0xFF;
                registers[reg1] = registers[reg2] + registers[reg3];
                break;

            case 0x20: // Example: MOV instruction
                reg1 = memory[pc++] & 0xFF;
                int value = memory[pc++] & 0xFF;
                registers[reg1] = value;
                break;

            case 0x30: // Example: HALT instruction
                running = false;
                break;

            default:
                throw new IllegalArgumentException("Unknown instruction: " + instruction);
        }
    }

    public void coreDump(String filename) {
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write("Memory Dump:\n");
            for (int i = 0; i < MEMORY_SIZE; i++) {
                writer.write(String.format("%02X ", memory[i]));
                if ((i + 1) % 16 == 0) {
                    writer.write("\n");
                }
            }
            writer.write("\nRegisters:\n");
            for (int i = 0; i < registers.length; i++) {
                writer.write(String.format("R%d: %d\n", i, registers[i]));
            }
            System.out.println("Core dump saved to " + filename);
        } catch (IOException e) {
            System.err.println("Error during core dump: " + e.getMessage());
        }
    }

    public void errorDump(String filename) {
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write("Error log (if applicable):\n");
            // You can add logic to write error information here
            System.out.println("Error dump saved to " + filename);
        } catch (IOException e) {
            System.err.println("Error during error dump: " + e.getMessage());
        }
    }
}
