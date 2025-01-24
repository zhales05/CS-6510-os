package os.util;

import java.util.ArrayList;
import java.util.List;

public class ErrorDump {
    private static ErrorDump instance;
    VerboseModeLogger logger = VerboseModeLogger.getInstance();

    private final List<String> logs = new ArrayList<>();

    private ErrorDump() {
    }

    public static ErrorDump getInstance() {
        if(instance == null) {
            instance = new ErrorDump();
        }
        return instance;
    }

   public void logError(String error) {
        logger.printError();
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
