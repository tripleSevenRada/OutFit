package radim.outfit.core.timer;

public interface Timer {
    interface TimerCallback {
        boolean tick();
    }

    void start();
    void stop();
}
