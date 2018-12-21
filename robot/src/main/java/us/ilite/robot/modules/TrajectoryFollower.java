package us.ilite.robot.modules;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.DemandType;
import com.ctre.phoenix.motorcontrol.NeutralMode;
import control.DriveController;
import control.DriveMotionPlanner;
import control.DriveOutput;
import us.ilite.common.config.SystemSettings;
import us.ilite.common.lib.geometry.Pose2d;
import us.ilite.common.lib.util.ReflectingCSVWriter;
import us.ilite.common.lib.util.Units;
import us.ilite.robot.MikeyProfile;
import us.ilite.robot.SetpointInfo;
import us.ilite.robot.loops.Loop;

public class TrajectoryFollower extends Loop {

    // These must be OUR implementation
    private ReflectingCSVWriter<Pose2d> mOdometryWriter = new ReflectingCSVWriter<>( "/home/lvuser/ODOMETRY.csv", Pose2d.class);
    private ReflectingCSVWriter<DriveMotionPlanner> mTrajectoryWriter = new ReflectingCSVWriter<>("/home/lvuser/TRAJECTORY.csv", DriveMotionPlanner.class);
    private ReflectingCSVWriter<SetpointInfo> mVelWriter = new ReflectingCSVWriter<>("/home/lvuser/VEL.csv", SetpointInfo.class);
    private ReflectingCSVWriter<SetpointInfo> mAccelWriter = new ReflectingCSVWriter<>("/home/lvuser/ACCEL.csv", SetpointInfo.class);

    private DriveController mDriveController = new DriveController(new MikeyProfile(), SystemSettings.kControlLoopPeriod);
    private DriveOutput mCurrentDriveOutput = new DriveOutput();

    private final Drive mDrive;

    private double mLastTimeUpdated = 0.0;
    private final double kP = 0.2;

    private double lastLeftVelRads = 0.0;
    private double lastRightVelRads = 0.0;
    private double leftVelRads = 0.0;
    private double rightVelRads = 0.0;
    private double leftAccelRads = 0.0;
    private double rightAccelRads = 0.0;

    private boolean enabled = false;

    public TrajectoryFollower(Drive pDrive) {
        mDrive = pDrive;
    }

    @Override
    public void modeInit(double pNow) {

    }

    @Override
    public void periodicInput(double pNow) {

    }

    @Override
    public void update(double pNow) {
        // TODO Put this conversion in a separate class
        leftVelRads = Units.vel_ticks_to_rads(mDrive.getDriveHardware().getLeftVelInches() / SystemSettings.DRIVETRAIN_WHEEL_CIRCUMFERENCE * 2.0 * Math.PI);
        rightVelRads = Units.vel_ticks_to_rads(mDrive.getDriveHardware().getRightVelInches() / SystemSettings.DRIVETRAIN_WHEEL_CIRCUMFERENCE * 2.0 * Math.PI);

        leftAccelRads = (leftVelRads - lastLeftVelRads) / (pNow - mLastTimeUpdated);
        rightAccelRads = (rightVelRads - lastRightVelRads) / (pNow - mLastTimeUpdated);

        lastLeftVelRads = leftVelRads;
        lastRightVelRads = rightVelRads;

        // Invert heading later
        mCurrentDriveOutput = mDriveController.getOutput(pNow, mDrive.getDriveHardware().getLeftInches(), mDrive.getDriveHardware().getRightInches(), mDrive.getDriveHardware().getHeading());


        // Hopefully this will bring the robot to a full stop at the end of the path
        if(mDriveController.isDone() && enabled) {
            mDrive.zero();
            System.out.println("DONE");
        } else if (!mDriveController.isDone() && enabled){
            // Since we aren't trying to deal with Talon velocity control yet - correct for error manually
            // mCurrentDriveOutput = velocityFeedbackCorrection(mCurrentDriveOutput);
            DriveMessage driveMessage = new DriveMessage(
                radiansPerSecondToTicksPer100ms(mCurrentDriveOutput.left_velocity),
                radiansPerSecondToTicksPer100ms(mCurrentDriveOutput.right_velocity),
                ControlMode.Velocity);

            driveMessage.setNeutralMode(NeutralMode.Brake, NeutralMode.Brake);

            driveMessage.setDemand(
             DemandType.ArbitraryFeedForward,
             (mCurrentDriveOutput.left_feedforward_voltage / 12.0) + SystemSettings.kDriveVelocity_kD * (radiansPerSecondToTicksPer100ms(mCurrentDriveOutput.left_accel) / 1000.0) / 1023.0,
             (mCurrentDriveOutput.right_feedforward_voltage / 12.0) + SystemSettings.kDriveVelocity_kD * (radiansPerSecondToTicksPer100ms(mCurrentDriveOutput.right_accel) / 1000.0) / 1023.0);

            mDrive.setDriveMessage(driveMessage);
        }
        System.out.println(pNow - mLastTimeUpdated);

        writeToCsv(pNow);

        mLastTimeUpdated = pNow;
    }

    private static double rotationsToInches(double rotations) {
        return rotations * (SystemSettings.kDriveWheelDiameterInches * Math.PI);
    }

    private static double rpmToInchesPerSecond(double rpm) {
        return rotationsToInches(rpm) / 60;
    }

    private static double inchesToRotations(double inches) {
        return inches / (SystemSettings.kDriveWheelDiameterInches * Math.PI);
    }

    private static double inchesPerSecondToRpm(double inches_per_second) {
        return inchesToRotations(inches_per_second) * 60;
    }

    private static double radiansPerSecondToTicksPer100ms(double rad_s) {
        return rad_s / (Math.PI * 2.0) * 1024.0 / 10.0;
    }

    @Override
    public void shutdown(double pNow) {
        mOdometryWriter.flush();
        mTrajectoryWriter.flush();
        mVelWriter.flush();
        mAccelWriter.flush();
    }

    @Override
    public void checkModule(double pNow) {

    }

    private double getLeftVelError(DriveOutput pOutputToCorrect) {
        return pOutputToCorrect.left_velocity - leftVelRads;
    }

    private double getRightVelError(DriveOutput pOutputToCorrect) {
        return pOutputToCorrect.right_velocity - rightVelRads;
    }

    private DriveOutput velocityFeedbackCorrection(DriveOutput pOutputToCorrect) {
        DriveOutput correctedOutput = new DriveOutput();
        correctedOutput.left_accel = pOutputToCorrect.left_accel;
        correctedOutput.right_accel = pOutputToCorrect.right_accel;
        correctedOutput.left_velocity = pOutputToCorrect.left_velocity;
        correctedOutput.right_velocity = pOutputToCorrect.right_velocity;
        correctedOutput.left_feedforward_voltage = pOutputToCorrect.left_feedforward_voltage + kP * getLeftVelError(pOutputToCorrect);
        correctedOutput.right_feedforward_voltage = pOutputToCorrect.right_feedforward_voltage + kP * getRightVelError(pOutputToCorrect);

        return correctedOutput;
    }

    private void writeToCsv(double time) {
        Pose2d latestPose = getDriveController().getRobotStateEstimator().getRobotState().getLatestFieldToVehiclePose();

        mVelWriter.add(new SetpointInfo(time, leftVelRads, rightVelRads, mCurrentDriveOutput.left_velocity, mCurrentDriveOutput.right_velocity));
        mAccelWriter.add(new SetpointInfo(time, leftAccelRads, rightAccelRads, mCurrentDriveOutput.left_accel, mCurrentDriveOutput.right_accel));
        mOdometryWriter.add(latestPose);
        mTrajectoryWriter.add(getDriveController().getDriveMotionPlanner());
    }

    public DriveController getDriveController() {
        return mDriveController;
    }

    public void enable() {
        enabled = true;
    }

    public void disable() {
        enabled = false;
    }

    @Override
    public void loop(double pNow) {
        update(pNow);
    }
}