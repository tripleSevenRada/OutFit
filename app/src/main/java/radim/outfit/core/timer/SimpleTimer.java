package radim.outfit.core.timer;

import android.os.Handler;

//https://gabesechansoftware.com/timers-in-android/
public class SimpleTimer implements Timer {
    private long mTimeOut;
    private TimerCallback mCallback;
    private final Handler mHandler;
    private Runnable mPostRunnable;
    private boolean mRunning;

    public SimpleTimer(long ms, TimerCallback callback) {
        mTimeOut = ms;
        mCallback = callback;
        mHandler = new Handler();
        mPostRunnable = new Runnable() {
            @Override
            public void run() {
                boolean again = mCallback.tick();
                if (again) {
                    synchronized (mHandler) {
                        if (mRunning) {
                            mHandler.postDelayed(mPostRunnable, mTimeOut);
                        }
                    }
                }
            }
        };
    }

    public void start() {
        synchronized (mHandler) {
            mRunning = true;
            mHandler.postDelayed(mPostRunnable, mTimeOut);
        }
    }

    public void stop() {
        synchronized (mHandler) {
            mRunning = false;
            mHandler.removeCallbacks(mPostRunnable);
        }
    }
}
