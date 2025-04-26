package os.util;

public interface Logging {
    default void log(String message){
        VerboseModeLogger.getInstance().print(message);
    }

    default void logError(String message){
        ErrorDump.getInstance().logError(message);
    }
}
