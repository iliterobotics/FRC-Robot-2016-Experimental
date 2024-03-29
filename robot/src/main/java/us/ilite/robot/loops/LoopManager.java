package us.ilite.robot.loops;

import com.flybotix.hfr.util.log.ILog;
import com.flybotix.hfr.util.log.Logger;

import edu.wpi.first.wpilibj.Notifier;
import edu.wpi.first.wpilibj.Timer;
import us.ilite.lib.drivers.Clock;

/**
 * A class which uses the WPILIB Notifier mechanic to run our Modules on
 * a set time.  Tune loop period to the desired,
 * but monitor CPU usage.
 */
public class LoopManager implements Runnable{
    private ILog mLog = Logger.createLog(LoopManager.class);

    private final double kLoopPeriodSeconds;

    private final Notifier mWpiNotifier;
    private final Timer mLoopSafetyTimer;
    private final Clock mClock;

    private final LoopList mLoopList = new LoopList();

    private final Object mTaskLock = new Object();
    private boolean mIsRunning = false;

    public LoopManager(double pLoopPeriodSeconds) {
        mWpiNotifier = new Notifier(this);
        mLoopSafetyTimer = new Timer();
        mClock = new Clock();
        this.kLoopPeriodSeconds = pLoopPeriodSeconds;
    }

    public void setRunningLoops(Loop ... pLoops) {
        mLoopList.setLoops(pLoops);
    }

    public synchronized void start() {

        mLoopSafetyTimer.reset();
        mLoopSafetyTimer.start();

        if(!mIsRunning) {
            mLog.info("Starting control loop");
            synchronized(mTaskLock) {
                mLoopList.modeInit(mClock.getCurrentTime());
                mIsRunning = true;
            }
            mWpiNotifier.startPeriodic(kLoopPeriodSeconds);
        }

        mClock.cycleEnded();
        mLoopSafetyTimer.stop();
        checkTiming("Loop start exceeds specified loop period.");

    }

    public synchronized void stop() {

        mLoopSafetyTimer.reset();
        mLoopSafetyTimer.start();

        if(mIsRunning) {
            mLog.info("Stopping control loop");
            mWpiNotifier.stop();
            synchronized(mTaskLock) {
                mIsRunning = false;
                mLoopList.shutdown(mClock.getCurrentTime());
            }
        }

        mClock.cycleEnded();
        mLoopSafetyTimer.stop();
        checkTiming("Loop stop exceeds specified loop period.");

    }

    @Override
    public void run() {

        mLoopSafetyTimer.reset();
        mLoopSafetyTimer.start();

        synchronized(mTaskLock) {
            try {
                if(mIsRunning) {
                    mLoopList.periodicInput(mClock.getCurrentTime());
                    mLoopList.loop(mClock.getCurrentTime());
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

        checkTiming("Loop update exceeds specified loop period.");
        mClock.cycleEnded();
        mLoopSafetyTimer.stop();

    }

    /**
     * Prints an error message onscreen if our safety timer has measured a time greater than the specified period.
     * @param pMessage
     */
    private void checkTiming(String pMessage) {
        if( mLoopSafetyTimer.get() > kLoopPeriodSeconds) {
            mLog.error(pMessage);
        }
    }
}
