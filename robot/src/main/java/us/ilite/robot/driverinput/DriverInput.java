package us.ilite.robot.driverinput;

import java.util.LinkedList;
import java.util.Queue;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.NeutralMode;

import us.ilite.common.config.DriveTeamInputMap;
import us.ilite.common.config.SystemSettings;
import us.ilite.common.lib.util.Util;
import us.ilite.common.types.input.EInputScale;
import us.ilite.robot.Data;
import us.ilite.robot.commands.ICommand;
import us.ilite.robot.modules.Drive;
import us.ilite.robot.modules.DriveMessage;
import us.ilite.robot.modules.Module;

public class DriverInput extends Module {

  protected final Drive driveTrain;
  private boolean scaleInputs;
  private boolean currentDriverToggle, lastDriverToggle, currentOperatorToggle, lastOperatorToggle;
  
  private Queue<ICommand> desiredCommandQueue;
  private boolean lastCanRunCommandQueue;
  private boolean canRunCommandQueue;
  
  
	private Data mData;
	
	public DriverInput(Drive pDrivetrain, Data pData)
	{
	    this.driveTrain = pDrivetrain;
		this.mData = pData;
		this.desiredCommandQueue = new LinkedList<>();
		scaleInputs = false;
	}
	
	@Override
	public void modeInit(double pNow) {
		// TODO Auto-generated method stub
		
		canRunCommandQueue = lastCanRunCommandQueue == false;
		
	}

	@Override
	public void periodicInput(double pNow) {

	}

	@Override
	public void update(double pNow) {
//		if(mData.driverinput.get(DriveTeamInputMap.DRIVE_SNAIL_MODE) > 0.5)
//		  scaleInputs = true;
//		else
//		  scaleInputs = false;
		if(!canRunCommandQueue) {
		  updateDriveTrain();
		}
		updateCommands();

	}
	
	private void updateCommands() {
		
		//canRunCommandQueue = is a button triggered?

		if(shouldInitializeCommandQueue()) {
			desiredCommandQueue.clear();
			//desiredCommandQueue.add(<command>);
		}
		lastCanRunCommandQueue = canRunCommandQueue;
	}
	
	
	
	private void updateDriveTrain() {
		double desiredLeftOutput, desiredRightOutput;
	  
		double rotate = mData.driverinput.get(DriveTeamInputMap.DRIVER_TURN_AXIS);
		rotate = EInputScale.EXPONENTIAL.map(rotate, 2);
		double throttle = -mData.driverinput.get(DriveTeamInputMap.DRIVER_THROTTLE_AXIS);
//		throttle = EInputScale.EXPONENTIAL.map(throttle, 2);
		
//		if(mElevatorModule.decelerateHeight())
//		{
//		  throttle = Utils.clamp(throttle, 0.5);
//		}
		if(mData.driverinput.get(DriveTeamInputMap.DRIVER_SUB_WARP_AXIS) > 0.5) {
	      throttle *= SystemSettings.SNAIL_MODE_THROTTLE_LIMITER;
	      rotate *= SystemSettings.SNAIL_MODE_ROTATE_LIMITER;
		}
		
		rotate = Util.limit(rotate, 0.7);
		    //		System.out.println("ENGINE THROTTLE " + throttle);
		desiredLeftOutput = throttle + rotate;
		desiredRightOutput = throttle - rotate;
		
		int leftScalar = desiredLeftOutput < 0 ? -1 : 1;
		int rightScalar = desiredRightOutput < 0 ? -1 : 1;
		desiredLeftOutput =  leftScalar * Math.min(Math.abs(desiredLeftOutput), 1);
		desiredRightOutput = rightScalar * Math.min(Math.abs(desiredRightOutput), 1);
		
//		if(Math.abs(desiredRightOutput) > 0.01 || Math.abs(desiredLeftOutput) > 0.01) {
//			System.out.println("LEFT: " + desiredLeftOutput +"\tRIGHT: " +  desiredRightOutput + "");
//		}
		
		driveTrain.setDriveMessage(new DriveMessage(desiredLeftOutput, desiredRightOutput, ControlMode.PercentOutput).setNeutralMode(NeutralMode.Brake));
		
	}
	
	@Override
	public void shutdown(double pNow) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void checkModule(double pNow) {

	}


	public boolean shouldInitializeCommandQueue() {
		return lastCanRunCommandQueue == false && canRunCommandQueue == true;
	}
	
	public boolean canRunCommandQueue() {
		return canRunCommandQueue;
	}
	
	public Queue<ICommand> getDesiredCommandQueue() {
		return desiredCommandQueue;
	}
	

}
