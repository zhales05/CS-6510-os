package util;

import java.util.ArrayList;
import java.util.List;

public class ErrorDump {
    private static ErrorDump instance;

    private List<String> logs = new ArrayList<>();

    private ErrorDump() {
    }

    public static ErrorDump getInstance() {
        return instance;
    }

   public void logError(String error) {
        logs.add(error);
    }

    public void printLogs() {
        System.out.println("=== ERROR DUMP ===");
        int counter = 0;
        for (String log : logs) {
            System.out.println(counter++ + " " + log);
        }
    }
}
