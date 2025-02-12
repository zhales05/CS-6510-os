package vm.hardware;

public class Clock {
    private static Clock instance;
    private int time;

    private Clock() {
        time = 0;
    }

    public static synchronized Clock getInstance() {
        if(instance == null) {
            instance = new Clock();
        }
        return instance;
    }

    public int getTime() {
        return time;
    }

    public void tick() {
         time++;
    }

    public void tick(int ticks) {
        time += ticks;
    }

}
