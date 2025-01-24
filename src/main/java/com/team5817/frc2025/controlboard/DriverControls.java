package com.team5817.frc2025.controlboard;

import org.littletonrobotics.junction.Logger;

import com.team5817.frc2025.subsystems.LEDs;
import com.team5817.frc2025.subsystems.Superstructure;
import com.team5817.frc2025.subsystems.Superstructure.GoalState;
import com.team5817.frc2025.subsystems.Drive.Drive;

public class DriverControls {

	ControlBoard mControlBoard = ControlBoard.getInstance();

	Superstructure s;
	Drive mDrive;
	LEDs mLEDs = LEDs.getInstance();
	public DriverControls(){
		mDrive = Drive.getInstance();
		s = Superstructure.getInstance();
		s.setGoal(GoalState.STOW);
	}

	/* ONE CONTROLLER */

	public void oneControllerMode() {
			// mDrive.overrideHeading(true);
		if(driver.getStartButton())
			mDrive.zeroGyro();
		
			
	}
	boolean climbAllowed = false;
	boolean autoAlignAllowed = true;
	CustomXboxController driver = mControlBoard.driver;
	CustomXboxController codriver = mControlBoard.operator;
	/* TWO CONTROLLERS */
	GoalState preparedGoal = GoalState.L4;
	public void twoControllerMode() {
		if(!climbAllowed){
			// if(driver.\)sswaaaaaa
			if(driver.leftBumper.isBeingPressed()){
				s.setGoal(GoalState.GROUND_CORAL_INTAKE);
			}
			if(driver.leftTrigger.isBeingPressed()){
				if(autoAlignAllowed)
					mDrive.autoAlign();
				else
					mDrive.autoAlignFinishedOverrride();
				s.setGoal(preparedGoal);
			}
			if(driver.rightBumper.isBeingPressed())
				mControlBoard.setSwerveScalar(.5);
			else
				mControlBoard.setSwerveScalar(1);
			if(driver.aButton.isBeingPressed())
				// s.AlgaeSmartCleanRequest();
				s.setGoal(GoalState.L4);
			if(driver.bButton.isBeingPressed())
				s.setGoal(GoalState.GROUND_CORAL_INTAKE);
			
			if(driver.xButton.isBeingPressed())
				s.setGoal(GoalState.L3);
			if(driver.yButton.isBeingPressed())
				s.setGoal(GoalState.L2);
			
			if(driver.releasedAny(driver.leftBumper,driver.leftTrigger,driver.aButton))
				s.setGoal(GoalState.STOW);

		
		}else {
			if (driver.aButton.wasActivated())
				s.setGoal(GoalState.CLIMB_PREPARE);
			if(driver.aButton.wasReleased())
				s.setGoal(GoalState.CLIMB_PULL);	
			}



		if(codriver.getLeftTriggerAxis()==1)
			s.setGoal(GoalState.STOW);

		if(codriver.yButton.isBeingPressed())
			preparedGoal = GoalState.L4;
		if(codriver.bButton.isBeingPressed())
			preparedGoal = GoalState.L3;
		if(codriver.aButton.isBeingPressed())
			preparedGoal = GoalState.L2;
		if(codriver.xButton.isBeingPressed())
			preparedGoal = GoalState.L1;
		if(codriver.POV0.isBeingPressed())
			preparedGoal = GoalState.NET;
		if(codriver.POV180.isBeingPressed())
			preparedGoal = GoalState.PROCESS;
			
		Logger.recordOutput("preparedGoal", preparedGoal);

		if(codriver.getRightTriggerAxis()==1)
			climbAllowed = true;	
		if(codriver.leftCenterClick.isBeingPressed())
			climbAllowed = false;
		Logger.recordOutput("Climb Allowed", climbAllowed);	
		
		boolean input = codriver.leftBumper.wasActivated();
		autoAlignAllowed = input?!autoAlignAllowed:autoAlignAllowed;
	}


	
}
