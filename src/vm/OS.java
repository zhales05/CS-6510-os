package vm;

import java.io.*;
import java.util.*;

public class OS {
    private static VirtualMachine vm = new VirtualMachine();

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("Welcome to the osX Virtual Machine Shell");

            while (true) {
                System.out.print("MYVM> ");
                String input = scanner.nextLine();
                String[] command = input.split(" ");

                if (command.length < 2) {
                    System.out.println("Invalid command. Use one of: load, run, coredump, errordump");
                    continue;
                }

                switch (command[0]) {
                    case "load":
                        handleLoadCommand(command);
                        break;
                    case "run":
                        handleRunCommand(command);
                        break;
                    case "coredump":
                        handleCoreDumpCommand(command);
                        break;
                    case "errordump":
                        handleErrorDumpCommand(command);
                        break;
                    case "exit":
                        System.out.println("Exiting MYVM. Goodbye!");
                        return;
                    default:
                        System.out.println("Unknown command: " + command[0]);
                }
            }
        }
    }

    private static void handleLoadCommand(String[] command) {
        if (command.length != 3 || !command[1].equals("-v")) {
            System.out.println("Usage: load -v <filename>");
        } else {
            try {
                vm.load(command[2]);
                System.out.println("Program loaded successfully.");
            } catch (IOException e) {
                System.out.println("Error loading program: " + e.getMessage());
            }
        }
    }

    private static void handleRunCommand(String[] command) {
        if (command.length != 3 || !command[1].equals("-v")) {
            System.out.println("Usage: run -v <filename>");
        } else {
            try {
                vm.run();
                System.out.println("Program executed successfully.");
            } catch (Exception e) {
                System.out.println("Error during execution: " + e.getMessage());
            }
        }
    }

    private static void handleCoreDumpCommand(String[] command) {
        if (command.length != 3 || !command[1].equals("-v")) {
            System.out.println("Usage: coredump -v <filename>");
        } else {
            vm.coreDump(command[2]);
        }
    }

    private static void handleErrorDumpCommand(String[] command) {
        if (command.length != 3 || !command[1].equals("-v")) {
            System.out.println("Usage: errordump -v <filename>");
        } else {
            vm.errorDump(command[2]);
        }
    }
}
