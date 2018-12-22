package us.ilite.robot;

import com.flybotix.hfr.util.log.ELevel;
import com.flybotix.hfr.util.log.ILog;
import com.flybotix.hfr.util.log.Logger;

import control.DriveController;
import edu.wpi.first.wpilibj.IterativeRobot;
import edu.wpi.first.wpilibj.Timer;
import us.ilite.common.config.SystemSettings;
import us.ilite.common.types.drive.EDriveData;
import us.ilite.lib.drivers.Clock;
import us.ilite.lib.util.SimpleNetworkTable;
import us.ilite.robot.commands.CommandQueue;
import us.ilite.robot.driverinput.DriverInput;
import us.ilite.robot.loops.LoopManager;
import us.ilite.robot.modules.Drive;
import us.ilite.robot.modules.ModuleList;

public class Robot extends IterativeRobot {
    
    private ILog mLogger = Logger.createLog(this.getClass());

    private CommandQueue mCommandQueue = new CommandQueue();

    // It sure would be convenient if we could reduce this to just a LoopManager...Will have to test timing of Codex first
    private LoopManager mLoopManager = new LoopManager(SystemSettings.kControlLoopPeriod);
    private ModuleList mRunningModules = new ModuleList();

    private Clock mClock = new Clock();
    private Data mData = new Data();

    // Module declarations here
    private DriveController mDriveController = new DriveController(new MikeyProfile(), SystemSettings.kControlLoopPeriod);
    private Drive mDrive = new Drive(mData, mDriveController, mClock);
    private DriverInput mDriverInput = new DriverInput(mDrive, mData);

    @Override
    public void robotInit() {
        Timer initTimer = new Timer();
        initTimer.start();
        Logger.setLevel(ELevel.DEBUG);
        mLogger.info("Starting Robot Initialization...");

        mRunningModules.setModules();

        initTimer.stop();
        mLogger.info("Robot initialization finished. Took: ", initTimer.get(), " seconds");
    }

    /**
     * This contains code run in ALL robot modes.
     * It's also important to note that this runs AFTER mode-specific code
     */
    @Override
    public void robotPeriodic() {
//        mLogger.info(this.toString());

        mClock.cycleEnded();
    }

    @Override
    public void autonomousInit() {
        mapNonModuleInputs();

        mRunningModules.setModules();
        mRunningModules.modeInit(mClock.getCurrentTime());
        mRunningModules.periodicInput(mClock.getCurrentTime());

        mLoopManager.start();
    }

    @Override
    public void autonomousPeriodic() {
        mapNonModuleInputs();

        mRunningModules.periodicInput(mClock.getCurrentTime());
        mCommandQueue.update(mClock.getCurrentTime());
        mRunningModules.update(mClock.getCurrentTime());
    }

    @Override
    public void teleopInit() {
        mapNonModuleInputs();

        mRunningModules.setModules(mDriverInput);
        mRunningModules.modeInit(mClock.getCurrentTime());
        mRunningModules.periodicInput(mClock.getCurrentTime());

        mLoopManager.setRunningLoops(mDrive);
        mLoopManager.start();
    }

    @Override
    public void teleopPeriodic() {
        mapNonModuleInputs();

        mRunningModules.periodicInput(mClock.getCurrentTime());
        mRunningModules.update(mClock.getCurrentTime());
    }

    @Override
    public void disabledInit() {
        mRunningModules.shutdown(mClock.getCurrentTime());
        mLoopManager.stop();
    }

    @Override
    public void disabledPeriodic() {

    }

    @Override
    public void testInit() {
        mRunningModules.setModules();
        mRunningModules.modeInit(mClock.getCurrentTime());
        mRunningModules.periodicInput(mClock.getCurrentTime());

        mLoopManager.start();
    }

    @Override
    public void testPeriodic() {
        mapNonModuleInputs();

        mRunningModules.periodicInput(mClock.getCurrentTime());
        mRunningModules.checkModule(mClock.getCurrentTime());
        mRunningModules.update(mClock.getCurrentTime());
    }

    public void mapNonModuleInputs() {

    }

    public String toString() {

        String mRobotMode = "Unknown";
        String mRobotEnabledDisabled = "Unknown";
        double mNow = Timer.getFPGATimestamp();

        if(this.isAutonomous()) {
            mRobotMode = "Autonomous";
        }
        if(this.isOperatorControl()) {
            mRobotMode = "Operator Control";
        }
        if(this.isTest()) {
            mRobotEnabledDisabled = "Test";
        }

        if(this.isEnabled()) {
            mRobotEnabledDisabled = "Enabled";
        }
        if(this.isDisabled()) {
            mRobotEnabledDisabled = "Disabled";
        }

        return String.format("State: %s\tMode: %s\tTime: %s", mRobotEnabledDisabled, mRobotMode, mNow);

    }

}
