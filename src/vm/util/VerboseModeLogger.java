package vm.util;

public class VerboseModeLogger {
    private boolean verboseMode = false;

    private static VerboseModeLogger instance;

    private VerboseModeLogger() {
    }

    public static VerboseModeLogger getInstance() {
        if (instance == null) {
            instance = new VerboseModeLogger();
        }

        return instance;
    }

    public void setVerboseMode(boolean verboseMode) {
        this.verboseMode = verboseMode;
    }

    public void print(String message) {
        if (verboseMode) {
            System.out.println(message);
        }
    }

    public void printError() {
        print("Program encountered error, for more information please call errordump.");
    }
}
