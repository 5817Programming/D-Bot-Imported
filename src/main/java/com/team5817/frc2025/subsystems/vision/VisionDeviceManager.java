package com.team5817.frc2025.subsystems.vision;

import com.team5817.frc2025.Constants;
import com.team5817.frc2025.RobotState;
import com.team5817.frc2025.Constants.PoseEstimatorConstants;
import com.team5817.frc2025.RobotState.VisionUpdate;
import com.team5817.frc2025.loops.ILooper;
import com.team5817.frc2025.loops.Loop;
import com.team5817.lib.drivers.Subsystem;
import com.team254.lib.geometry.Pose2d;
import com.team254.lib.geometry.Rotation2d;
import com.team254.lib.geometry.Translation2d;
import com.team254.lib.geometry.Translation3d;
import com.team254.lib.util.MovingAverage;

import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.DriverStation.Alliance;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import java.util.List;

import org.littletonrobotics.junction.Logger;

/**
 * Manages vision devices and processes vision data.
 */
public class VisionDeviceManager extends Subsystem {

	/**
	 * Singleton instance of VisionDeviceManager.
	 */
	public static VisionDeviceManager mInstance;

	/**
	 * Returns the singleton instance of VisionDeviceManager.
	 * @return the singleton instance.
	 */
	public static VisionDeviceManager getInstance() {
		if (mInstance == null) {
			mInstance = new VisionDeviceManager();
		}
		return mInstance;
	}

	private VisionDevice mRightCamera;
	private VisionDevice mLeftCamera;
	private VisionDevice mUpCamera;

	private RobotState mRobotState;

	private List<VisionDevice> mAllCameras;

	private static double timestampOffset = 0.1;

	private MovingAverage mHeadingAvg = new MovingAverage(100);
	private double mMovingAvgRead = 0.0;

	private static boolean disable_vision = false;
	double timeOfLastUpdate = Double.MIN_VALUE;

	/**
	 * Constructor for VisionDeviceManager.
	 */
	public VisionDeviceManager() {
		mRightCamera = new VisionDevice("limelight-right");
		mLeftCamera = new VisionDevice("limelight-left");
		mUpCamera = new VisionDevice("limelight-up");

		mAllCameras = List.of(mRightCamera, mLeftCamera, mUpCamera);
		mRobotState = RobotState.getInstance();
	}

	/**
	 * Registers the enabled loops.
	 * @param enabledLooper the enabled looper.
	 */
	@Override
	public void registerEnabledLoops(ILooper enabledLooper) {
		enabledLooper.register(new Loop() {
			@Override
			public void onStart(double timestamp) {
			}

			@Override
			public void onLoop(double timestamp) {

			}

		});
	}

	/**
	 * Reads periodic inputs from vision devices.
	 */
	@Override
	public void readPeriodicInputs() {
		if ( Constants.mode == Constants.Mode.SIM) {
			RobotState.getInstance().addVisionUpdate(new VisionUpdate(1, Timer.getTimestamp(), 1.0,
					RobotState.getInstance().getPoseFromOdom(Timer.getTimestamp()).getTranslation()));
		} else {

			for (VisionDevice device : mAllCameras) {
				device.update(Timer.getTimestamp());
				if (!device.getVisionUpdate().isEmpty()) {
					VisionUpdate update = device.getVisionUpdate().get();
					RobotState.getInstance().addVisionUpdate(update);
					if (update.getTimestamp() > timeOfLastUpdate)
						timeOfLastUpdate = update.getTimestamp();
				}
			}
		}
	}

	/**
	 * Writes periodic outputs.
	 */
	@Override
	public void writePeriodicOutputs() {
	}

	/**
	 * Outputs telemetry data to the dashboard.
	 */
	@Override
	public void outputTelemetry() {
		Logger.recordOutput("Elastic/Time Since Last Update", Timer.getTimestamp() - timeOfLastUpdate);
		for (VisionDevice device : mAllCameras) {
			device.outputTelemetry();
		}
	}

	/**
	 * Returns the best vision device based on the vision update.
	 * @return the best vision device.
	 */
	public VisionDevice getBestDevice() {
		double bestTa = 0;
		VisionDevice bestDevice = null;
		for(VisionDevice device : mAllCameras){
			if(device.getVisionUpdate().isEmpty()){
				continue;
			}
			if(device.getVisionUpdate().get().getTa() > bestTa){
				bestTa = device.getVisionUpdate().get().getTa();
				bestDevice = device;
			}
		}
		return bestDevice;
		
	}

	/**
	 * Verifies epipolar geometry between two sets of points.
	 * @param pointsCam1 points from the first camera.
	 * @param pointsCam2 points from the second camera.
	 * @return true if the points satisfy the epipolar constraint, false otherwise.
	 */
	public boolean epipolarVerification(List<Translation2d> pointsCam1, List<Translation2d> pointsCam2) {

		if (pointsCam1.size() != pointsCam2.size()) {
			throw new IllegalArgumentException("Point lists must have the same size.");
		}
		double threshold = 1e-6; // Tolerance for numerical errors
		for (int i = 0; i < pointsCam1.size(); i++) {
			Translation3d x1 = toHomogeneous(pointsCam1.get(i));
			Translation3d x2 = toHomogeneous(pointsCam2.get(i));
			// Compute Fx1 = F * x1
			Translation3d Fx1 = multiplyMatrixVector(Constants.fundamentalMatrix, x1);
			// Compute x2 • (Fx1)
			double result = x2.dot(Fx1);
			// Check if result is close to zero
			if (Math.abs(result) > threshold) {
				return false;
			}
		}
		return true;

	}

	/**
	 * Applies a translational filter to the vision data.
	 * @param domTargetToCamera the pose of the dominant target to the camera.
	 * @param subDevice the pose of the subordinate device. HI MIKEY HI MIKEY HI MKIEY HI HUBERT HI HUBERT
	 * @return true if the error is within the threshold, false otherwise.
	 */
	public boolean translationalFilter(Pose3d domTargetToCamera, Pose3d subDevice) {
		Transform3d expectedDelta = PoseEstimatorConstants.kDomVisionDevice.kRobotToCamera
				.plus(PoseEstimatorConstants.kSubVisionDevice.kRobotToCamera.inverse());
		Transform3d delta;
		delta = new Transform3d(domTargetToCamera, subDevice);
		Transform3d error = delta.plus(expectedDelta.inverse());
		return (error.getTranslation().getNorm() > 0.1 || error.getRotation().getAngle() > 0.5);// TODO Find threshold
																								// (meters and radians)

	}



	/**
	 * Returns the moving average object.
	 * @return the moving average object.
	 */
	public MovingAverage getMovingAverage() {
		return mHeadingAvg;
	}

	/**
	 * Returns the left vision device.
	 * @return the left vision device.
	 */
	public VisionDevice getLeftVision() {
		return mRightCamera;
	}


	/**
	 * Returns the right vision device.
	 * @return the right vision device.
	 */
	public VisionDevice getRightVision() {
		return mLeftCamera;
	}

	/**
	 * Returns the timestamp offset.
	 * @return the timestamp offset.
	 */
	public static double getTimestampOffset() {
		return timestampOffset;
	}

	/**
	 * Checks if vision is disabled.
	 * @return true if vision is disabled, false otherwise.
	 */
	public static boolean visionDisabled() {
		return disable_vision;
	}

	/**
	 * Sets the vision disabled state.
	 * @param disable the new state of vision disabled.
	 */
	public static void setDisableVision(boolean disable) {
		disable_vision = disable;
	}

	/**
	 * Checks if the system is fully connected.
	 * @return true if fully connected, false otherwise.
	 */
	public boolean fullyConnected() {
		return false;//TODO
	}

	/**
	 * Verifies epipolar geometry between two sets of points using a fundamental matrix.
	 * @param pointsCam1 points from the first camera.
	 * @param pointsCam2 points from the second camera.
	 * @param fundamentalMatrix the fundamental matrix.
	 * @return true if the points satisfy the epipolar constraint, false otherwise.
	 */
	public static boolean verifyEpipolarGeometry(List<Translation2d> pointsCam1,
			List<Translation2d> pointsCam2,
			double[][] fundamentalMatrix) {
		if (pointsCam1.size() != pointsCam2.size()) {
			throw new IllegalArgumentException("Point lists must have the same size.");
		}

		double threshold = 1e-6; // Tolerance for numerical errors
		for (int i = 0; i < pointsCam1.size(); i++) {
			Translation3d x1 = toHomogeneous(pointsCam1.get(i));
			Translation3d x2 = toHomogeneous(pointsCam2.get(i));

			// Compute Fx1 = F * x1
			Translation3d Fx1 = multiplyMatrixVector(fundamentalMatrix, x1);

			// Compute x2 • (Fx1)
			double result = x2.dot(Fx1);

			// Check if result is close to zero
			if (Math.abs(result) > threshold) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Converts a Translation2D to homogeneous coordinates (Translation3D).
	 * @param point the 2D point.
	 * @return the 3D point in homogeneous coordinates.
	 */
	private static Translation3d toHomogeneous(Translation2d point) {
		return new Translation3d(point.x(), point.y(), 1.0);
	}

	/**
	 * Multiplies a 3x3 matrix with a 3D vector.
	 * @param matrix the 3x3 matrix.
	 * @param vector the 3D vector./
	 * @return the resulting 3D vector.
	 */
	private static Translation3d multiplyMatrixVector(double[][] matrix, Translation3d vector) {
		double x = matrix[0][0] * vector.x() + matrix[0][1] * vector.y() + matrix[0][2] * vector.z();
		double y = matrix[1][0] * vector.x() + matrix[1][1] * vector.y() + matrix[1][2] * vector.z();
		double z = matrix[2][0] * vector.x() + matrix[2][1] * vector.y() + matrix[2][2] * vector.z();
		return new Translation3d(x, y, z);
	}

}