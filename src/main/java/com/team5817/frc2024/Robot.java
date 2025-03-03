// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package com.team5817.frc2024;

import org.littletonrobotics.junction.LogFileUtil;
import org.littletonrobotics.junction.LoggedRobot;

import org.littletonrobotics.junction.Logger;
import org.littletonrobotics.junction.networktables.NT4Publisher;
import org.littletonrobotics.junction.wpilog.WPILOGReader;
import org.littletonrobotics.junction.wpilog.WPILOGWriter;

import com.team254.lib.swerve.ChassisSpeeds;
import com.team5817.BuildConstants;
import com.team5817.frc2024.controlboard.ControlBoard;
import com.team5817.frc2024.controlboard.DriverControls;
import com.team5817.frc2024.loops.Looper;
import com.team5817.frc2024.subsystems.Drive;
import com.team5817.frc2024.subsystems.Superstructure;
import com.team5817.frc2024.subsystems.vision.VisionDeviceManager;
import com.team5817.lib.Util;

import edu.wpi.first.wpilibj.DataLogManager;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.DriverStation.Alliance;
import edu.wpi.first.wpilibj.PowerDistribution;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.PowerDistribution.ModuleType;

public class Robot extends LoggedRobot {

    SubsystemManager mSubsystemManager;
    Superstructure s = Superstructure.getInstance();
    Drive drive = Drive.getInstance();
    VisionDeviceManager mVision = VisionDeviceManager.getInstance();
    DriverControls controls = new DriverControls();
    ControlBoard controlBoard = ControlBoard.getInstance();
    // public LoggedDashboardChooser<AutoBase> autoChooser = new LoggedDashboardChooser<>("AutoChooser");
    private final Looper mEnabledLooper = new Looper();
  
  // HashMap<String,AutoBase> autos = new HashMap<String,AutoBase>();
    @SuppressWarnings("resource")
    @Override
    public void robotInit() {
      DriverStation.silenceJoystickConnectionWarning(true);
      // autos.put("Middle 4", new M6());
  
  
      DriverStation.startDataLog(DataLogManager.getLog());
  
      RobotState.getInstance().resetKalman();
      // for(HashMap.Entry<String, AutoBase> entry : autos.entrySet()) {
        // String N = entry.getKey();
        // AutoBase A = entry.getValue();
        // autoChooser.addOption(N, A);
      // }
  
      Logger.recordMetadata("ProjectName", "MyProject"); // Set a metadata value
    Logger.recordMetadata("GitSHA", BuildConstants.GIT_SHA);

if (isReal()) {
    Logger.addDataReceiver(new WPILOGWriter()); // Log to a USB stick ("/U/logs")
    Logger.addDataReceiver(new NT4Publisher()); // Publish data to NetworkTables
    new PowerDistribution(1, ModuleType.kRev); // Enables power distribution logging
} else {
    if(Constants.mode == Constants.Mode.REPLAY) {
      setUseTiming(false); // Run as fast as possible
      String logPath = LogFileUtil.findReplayLog(); // Pull the replay log from AdvantageScope (or prompt the user)
      Logger.setReplaySource(new WPILOGReader(logPath)); // Read replay log
      Logger.addDataReceiver(new WPILOGWriter(LogFileUtil.addPathSuffix(logPath, "_sim"))); // Save outputs to a new log
      setUseTiming(false);
    }else{
      //physics
    }
}

Logger.start(); // Start logging! No more data receivers, replay sources, or metadata values may be added.
      drive.resetModulesToAbsolute();
      mSubsystemManager = SubsystemManager.getInstance();
  
      mSubsystemManager.setSubsystems(
          Drive.getInstance(),
          Superstructure.getInstance(),
          VisionDeviceManager.getInstance()
          );
          mSubsystemManager.registerEnabledLoops(mEnabledLooper);
          mEnabledLooper.start();
      }
  
    @Override
    public void robotPeriodic() {
      // auto = autoChooser.get();
      
      mEnabledLooper.outputToSmartDashboard();
          mSubsystemManager.outputLoopTimes();
      SubsystemManager.getInstance().outputTelemetry();
      
      
    }
  
  
boolean disableGyroReset = false;
    @Override
    public void autonomousInit() {
      disableGyroReset = true;
      drive = Drive.getInstance();
      drive.resetModulesToAbsolute();
      // Superstructure.getInstance().setState(Superstructure.AUTO);
      // autoExecuter.setAuto(auto);
      // autoExecuter.start();
    }
  
    /** This function is called periodically during autonomous. */
    @Override
    public void autonomousPeriodic() { 
    }
  
    /** This function is called once when teleop is enabled. */  
    @Override
    public void teleopInit() {
      
      // swerve.fieldzeroSwerve();
      drive.resetModulesToAbsolute();
      drive.feedTeleopSetpoint(new ChassisSpeeds(0,0,0));
    }
  
    /** This function is called periodically during operator control. */
    @Override
    public void teleopPeriodic() {
      controls.twoControllerMode();
      controlBoard.update();
      drive.feedTeleopSetpoint(ChassisSpeeds.fromFieldRelativeSpeeds(
        controlBoard.getSwerveTranslation().x(),
        controlBoard.getSwerveTranslation().y(),
        controlBoard.getSwerveRotation(),
        Util.robotToFieldRelative(drive.getHeading(), DriverStation.getAlliance().get().equals(Alliance.Red))
      ));
    }
  
    /** This function is called once when the robot is disabled. */
  
    @Override
    public void disabledInit() {
      mSubsystemManager.stop();
      // Superstructure.getInstance().clearQueues();
      // autoExecuter.stop();
      // autoExecuter = new AutoExecuter();
    }
  
    /** This function is called periodically when disabled. */
    @Override
    public void disabledPeriodic() {
      RobotState.getInstance().outputTelemetry();
      if(!disableGyroReset)
      drive.zeroGyro(mVision.getMovingAverageRead());
    }
  
    /** This function is called once when test mode is enabled. */
    @Override
    public void testInit() {
    }
  
    /** This function is called periodically during test mode. */
    @Override
    public void testPeriodic() {
    }
  }
 