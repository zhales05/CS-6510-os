package vm.hardware;

import util.Subject;

public class Clock extends Subject {
    private static Clock instance;
    private int time;

    private Clock() {
        time = 0;
    }

    public static synchronized Clock getInstance() {
        if (instance == null) {
            instance = new Clock();
        }
        return instance;
    }

    public int getTime() {
        return time;
    }

    public void tick() {
        tick(1);
    }

    public void tick(int ticks) {
        time += ticks;
        notifyObservers(getTime());
    }

}
