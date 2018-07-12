import lejos.nxt.*;
import lejos.util.Delay;

import static java.lang.Math.floor;

public class Turntable {

    private static float smooth(float oldValue, float newValue, int factor) {
        return (oldValue * (float) (factor - 1) + newValue) / (float) factor;
    }

    private static void turn(int degree) {
        TouchSensor touch = new TouchSensor(SensorPort.S2);
        LightSensor light = new LightSensor(SensorPort.S1);
        light.setFloodlight(true);

        Motor.A.setSpeed(300);
        int noStripes = 37;
        float tolerance = .05f;
        float stepSize = 360f / (2f * (float) noStripes);

        // degree must be above stepSize

        int smoothing = 5;
        float lightValue;
        boolean stripe = false;
        float averageLight = 0f;
        float lightChangePercent;
        float degreeTurned = 0;
        int thisTachoCount = 0, lastTachoCount = 0;
        float tableToMotor = -1f;

        int fastDegrees = (int) (floor(((float) degree / stepSize)) * stepSize);
        int slowDegrees = degree - fastDegrees;

        LCD.clearDisplay();
        LCD.drawString("No. stripes: " + Integer.toString(noStripes), 0, 0);
        LCD.drawString("Str. angle: " + Float.toString(stepSize), 0, 1);
        LCD.drawString("Target: " + Integer.toString(degree), 0, 2);
        LCD.drawString("Fast: " + Integer.toString(fastDegrees), 0, 3);
        LCD.drawString("Slow: " + Integer.toString(slowDegrees), 0, 4);
        LCD.drawString("Press button...", 0, 6);

        Button.waitForAnyPress();

        while (degreeTurned < fastDegrees) {
            if (touch.isPressed()) {
                return;
            }
            Motor.A.rotate(1000, true);
            light.calibrateHigh();
            lightValue = light.getNormalizedLightValue();
            // lightValue = light.getLightValue();
            averageLight = Turntable.smooth(averageLight, lightValue, smoothing);
            lightChangePercent = lightValue / averageLight - 1f;
            if (stripe && lightChangePercent >= tolerance) {
                stripe = false;
                degreeTurned += stepSize;

                thisTachoCount = Motor.A.getTachoCount();
                float thisRatio = (thisTachoCount - lastTachoCount) / stepSize;
                if (tableToMotor < 0f) {
                    tableToMotor = thisRatio;
                } else {
                    tableToMotor = Turntable.smooth(tableToMotor, thisRatio, smoothing);
                }
                lastTachoCount = thisTachoCount;


            } else if (!stripe && lightChangePercent < -tolerance) {
                stripe = true;
                degreeTurned += stepSize;

                thisTachoCount = Motor.A.getTachoCount();
                float thisRatio = (thisTachoCount - lastTachoCount) / stepSize;
                if (tableToMotor < 0f) {
                    tableToMotor = thisRatio;
                } else {
                    tableToMotor = Turntable.smooth(tableToMotor, thisRatio, smoothing);
                }
                lastTachoCount = thisTachoCount;

            }
            LCD.clearDisplay();
            LCD.drawString("Light: " + Float.toString(100f * lightChangePercent), 0, 0);
            LCD.drawString("Turned: " + Integer.toString((int) degreeTurned) + "/" + Integer.toString(degree), 0, 1);
            LCD.drawString("Transl.: " + Float.toString(tableToMotor), 0, 3);

            LCD.drawString("Dist.: " + Integer.toString(thisTachoCount - lastTachoCount), 0, 5);
            LCD.drawString("Step s.: " + Float.toString(stepSize), 0, 6);
            Delay.msDelay(100);
        }

        Motor.A.setSpeed(100);
        Motor.A.rotate((int) (slowDegrees * tableToMotor));

        Motor.A.stop();
    }

    public static void main (String[] args) {
        Turntable.turn(4);

    }
}
