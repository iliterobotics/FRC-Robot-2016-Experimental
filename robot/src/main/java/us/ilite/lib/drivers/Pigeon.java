package us.ilite.lib.drivers;

import com.ctre.phoenix.sensors.PigeonIMU;
import us.ilite.common.lib.geometry.Rotation2d;

public class Pigeon extends IMU{

	private double[] ypr;
	private short[] xyz;
	private PigeonIMU mPigeon;

  //TODO - single value for now - could be VERY noisy
  // others to try: {0.75, 0.25}, {0.6, 0.4}, {0.5, 0.3, 0.2}
  private static final double[] kCollisionGains = {1.0};
	
	public Pigeon(PigeonIMU pPigeon, double pCollisionThreshold_DeltaG){
		super(kCollisionGains);
		ypr = new double[3];
		xyz = new short[3];
		this.mPigeon = pPigeon;
		setCollisionThreshold_DeltaG(pCollisionThreshold_DeltaG);
		//mAccelerationX = new FilteredAverage(kCollisionGains);
		//mAccelerationY = new FilteredAverage(kCollisionGains);
	}
	
	/**
	 * Pre-populates the filters & calculated values so it's done only once per cycle
	 * @param pTimestampNow
	 */
	protected void updateSensorCache(double pTimestampNow) {
    mPigeon.getYawPitchRoll(ypr);
    for(int i = 0 ; i < ypr.length; i++) {
      ypr[i] = Rotation2d.fromDegrees(ypr[i]).getDegrees();
    }

    double currentAccelX = getRawAccelX();
    double currentAccelY = getRawAccelY();
    
    mJerkX = (currentAccelX - mAccelerationX.getAverage()) / (pTimestampNow - mLastUpdate);
    mJerkY = (currentAccelY - mAccelerationY.getAverage()) / (pTimestampNow - mLastUpdate);
    
    mPigeon.getBiasedAccelerometer(xyz);
    mAccelerationX.addNumber(currentAccelX);
    mAccelerationY.addNumber(currentAccelY);
    mLastUpdate = pTimestampNow;
	}
	
	public Rotation2d getHeading() {
	  return Rotation2d.fromDegrees(mPigeon.getFusedHeading());
	}
	
	public double getYaw() {
    return ypr[0];
	}
	
	public double getPitch() {
    return ypr[1];
	}
	
	public double getRoll() {
    return ypr[2];
	}	
	
	public double getAccelX() {
	  return mAccelerationX.getAverage();
	}
	
	public double getAccelY() {
	  return mAccelerationY.getAverage();
	}
	
	public double getJerkX() {
	  return mJerkX;
	}
	
	public double getJerkY() {
	  return mJerkY;
	}
	
	public void zeroAll() {
		for(int i = 0; i < ypr.length; i++) {
			ypr[i] = 0;
		}
		mPigeon.setYaw(0d, 20);
		mPigeon.setFusedHeading(0d, 20); //TODO - figure out CAN timeout defaults
	}

	public double getRawAccelX() {
		return xyz[0];
	}

	public double getRawAccelY() {
		return xyz[1];
	}
}