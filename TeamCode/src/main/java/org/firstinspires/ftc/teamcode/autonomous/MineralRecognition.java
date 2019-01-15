package org.firstinspires.ftc.teamcode.autonomous;

// import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import android.util.Log;
import android.widget.HorizontalScrollView;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.vuforia.CameraDevice;

import java.util.List;
import org.firstinspires.ftc.robotcore.external.ClassFactory;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer.CameraDirection;
import org.firstinspires.ftc.robotcore.external.tfod.TFObjectDetector;
import org.firstinspires.ftc.robotcore.external.tfod.Recognition;
import org.firstinspires.ftc.teamcode.hardware.hardwareutils.HardwareManager;

@Autonomous
// @Disabled
public class MineralRecognition extends LinearOpMode {
    private HardwareManager hardware;
    private AutoCommands commands;
    private String loggingName = "MineralRecognition";
    //TODO: replace all those -1s with an internal enum or constant
    private final int TARGET_NOT_DETECTED = -1;
    /**
     * {@link #vuforia} is the variable we will use to store our instance of the Vuforia
     * localization engine.
     */
    private VuforiaLocalizer vuforia;

    /**
     * {@link #tfod} is the variable we will use to store our instance of the Tensor Flow Object
     * Detection engine.
     */
    private TFObjectDetector tfod;

    @Override
    public void runOpMode() {
        initVuforia();
        hardware = new HardwareManager(hardwareMap);
        commands = new AutoCommands(hardware, telemetry);
        if (ClassFactory.getInstance().canCreateTFObjectDetector()) {
            initTfod();
        } else {
            telemetry.addData("Sorry!", "This device is not compatible with TFOD");
        }
        //turns on flash
        CameraDevice.getInstance().setFlashTorchMode(true);
        telemetry.addData(">", "Press Play to start tracking");
        telemetry.update();
        waitForStart();

        if (opModeIsActive()) {
            /** Activate Tensor Flow Object Detection. */
            if (tfod != null) {
                tfod.activate();
            }

            while (opModeIsActive()) {
                if (tfod != null) {
                    // getUpdatedRecognitions() will return null if no new information is available since
                    // the last time that call was made.
                    List<Recognition> updatedRecognitions = tfod.getUpdatedRecognitions();
                    if (updatedRecognitions != null) {
                        telemetry.addData("# Object Detected", updatedRecognitions.size());
                        if (updatedRecognitions.size() != 0) {
                            int goldMineralX = TARGET_NOT_DETECTED;
                            int silverMineral1X = TARGET_NOT_DETECTED;
                            int silverMineral2X = TARGET_NOT_DETECTED;
                            Recognition imageCenterCalc = updatedRecognitions.get(0);
                            int centerX = imageCenterCalc.getImageWidth() / 2;
                            int goldMineralCenterX = TARGET_NOT_DETECTED;
                            for (Recognition recognition : updatedRecognitions) {
                                if (recognition.getLabel().equals(MineralConstants.LABEL_GOLD_MINERAL)) {
                                    goldMineralCenterX = (int) (recognition.getLeft() + recognition.getRight())/2;

                                }
//                            if (goldMineralX != -1 && silverMineral1X != -1 && silverMineral2X != -1) {
//                              if (goldMineralX < silverMineral1X && goldMineralX < silverMineral2X) {
//                                telemetry.addData("Gold Mineral Position", "Left");
//                              } else if (goldMineralX > silverMineral1X && goldMineralX > silverMineral2X) {
//                                telemetry.addData("Gold Mineral Position", "Right");
//                              } else {
//                                telemetry.addData("Gold Mineral Position", "Center");
//                              }
//                            }
                            }
                            if (goldMineralX != TARGET_NOT_DETECTED && silverMineral1X != TARGET_NOT_DETECTED && silverMineral2X != TARGET_NOT_DETECTED) {
                                if (goldMineralX < silverMineral1X && goldMineralX < silverMineral2X) {
                                    telemetry.addData("Gold Mineral Position", "Left");
                                } else if (goldMineralX > silverMineral1X && goldMineralX > silverMineral2X) {
                                    telemetry.addData("Gold Mineral Position", "Right");
                                } else {
                                    telemetry.addData("Gold Mineral Position", "Center");
                                }
                            }
                            int error = centerX - goldMineralCenterX;
                            //if gold isn't detected, stop moving
                            if(goldMineralCenterX == TARGET_NOT_DETECTED)
                            {
                                pidLoop(0);
                                Log.i(loggingName, "Gold Mineral X not detected");
                                telemetry.addData("Gold Mineral X", "not detected");
                            }
                            else {
                                telemetry.addData("Center", centerX);
                                telemetry.addData("Error", error);
                                telemetry.addData("Gold Mineral X", goldMineralCenterX);
                            }
                            pidLoop(error);
                            telemetry.update();

                        }
                    }
                }
            }
        }
        if (tfod != null) {
            tfod.shutdown();
        }
    }

    /**
     * Initialize the Vuforia localization engine.
     */
    private void initVuforia() {
        /*
         * Configure Vuforia by creating a Parameter object, and passing it to the Vuforia engine.
         */
        VuforiaLocalizer.Parameters parameters = new VuforiaLocalizer.Parameters();

        parameters.vuforiaLicenseKey = MineralConstants.VUFORIA_KEY;
        parameters.cameraDirection = CameraDirection.BACK;

        //  Instantiate the Vuforia engine
        vuforia = ClassFactory.getInstance().createVuforia(parameters);

        // Loading trackables is not necessary for the Tensor Flow Object Detection engine.
    }

    /**s
     * Initialize the Tensor Flow Object Detection engine.
     */
    private void initTfod() {
        int tfodMonitorViewId = hardwareMap.appContext.getResources().getIdentifier(
            "tfodMonitorViewId", "id", hardwareMap.appContext.getPackageName());
        TFObjectDetector.Parameters tfodParameters = new TFObjectDetector.Parameters(tfodMonitorViewId);
        tfod = ClassFactory.getInstance().createTFObjectDetector(tfodParameters, vuforia);
        tfod.loadModelFromAsset(MineralConstants.TFOD_MODEL_ASSET, MineralConstants.LABEL_GOLD_MINERAL, MineralConstants.LABEL_SILVER_MINERAL);
    }

    //may need to scale this down so it stops losing track
    private void pidLoop(int error) {
        double kp = -0.002;
        double sideShiftPower = error * kp;
        telemetry.addData("Power", sideShiftPower);
        telemetry.update();
        Log.i(loggingName, "Power for pid is " + sideShiftPower);
        Log.i(loggingName, "Error for pid is " + error);
        commands.HorizontalMove(sideShiftPower);
    }
}