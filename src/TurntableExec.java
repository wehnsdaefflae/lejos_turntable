import lejos.nxt.*;
import lejos.util.Delay;

import static java.lang.Math.abs;


class Turntable {
    private int speed;
    private float tolerance;
    private int smoothing;
    private float stepSize;

    private float averageLight = -1f;
    private float tableToMotor = -1f;

    private boolean cancel = false;

    private LightSensor light = new LightSensor(SensorPort.S1);
    private TouchSensor touch = new TouchSensor(SensorPort.S2);

    Turntable(int speed, int noStripes, float tolerance, int smoothing) {
        this.speed = speed;
        this.tolerance = tolerance;
        this.smoothing = smoothing;
        this.stepSize = 360f / (2f * (float) noStripes);
    }

    private static float SMOOTH(float oldValue, float newValue, int factor) {
        if (oldValue < 0f) return newValue;
        return (oldValue * (float) (factor - 1) + newValue) / (float) factor;
    }

    private void waitTillTouch() {
        boolean wasPressed = false;
        boolean isPressed = false;
        while (!wasPressed || isPressed) {
            Delay.msDelay(100);
            wasPressed = isPressed;
            isPressed = this.touch.isPressed();
        }
    }

    private void shoot(){
        Delay.msDelay(500);
        Sound.beepSequenceUp();
        Sound.buzz();
    }

    void start(int stepDegree, int noSteps) {
        LCD.clearDisplay();
        LCD.drawString("No. stripes: " + Integer.toString((int) (180 / this.stepSize)), 0, 0);
        LCD.drawString("Str. angle: " + Float.toString(this.stepSize), 0, 1);
        LCD.drawString("Target: " + Integer.toString(stepDegree * noSteps), 0, 2);
        LCD.drawString("Press touch...", 0, 6);
        this.waitTillTouch();

        for (int i = 0; i < noSteps; i++) {
            this.light.setFloodlight(true);
            Delay.msDelay(100);
            this.turn(stepDegree, i);
            this.light.setFloodlight(false);
            if (this.cancel) break;
            Delay.msDelay(100);
            this.shoot();
        }

        // LCD info

    }

    private float getLightPercentChange() {
        float lightValue = this.light.getNormalizedLightValue();
        this.averageLight = Turntable.SMOOTH(this.averageLight, lightValue, this.smoothing);
        return lightValue / this.averageLight - 1f;
    }


    private void turn(int degree) {
        this.turn(degree, 0);
    }

    private void turn(int degree, int noTurn) {
        int lastCount = 0;
        float degreeTurned = 0f;

        Motor.A.resetTachoCount();
        Motor.A.setSpeed(this.speed);

        Boolean stripe = null;

        for (int thisCount = Motor.A.getTachoCount(); degreeTurned < degree || lastCount < 1; thisCount = Motor.A.getTachoCount()) {
            if (this.touch.isPressed()) {
                this.cancel = true;
                break;
            }

            Motor.A.rotate(1000, true);

            float lightChangePercent = this.getLightPercentChange();
            if (this.tolerance < abs(lightChangePercent)) {
                if (stripe == null) {
                    stripe = lightChangePercent < 0f;
                    lastCount = thisCount;

                } else if ((stripe && 0f < lightChangePercent) || (!stripe && lightChangePercent < 0f)){
                    stripe = !stripe;

                    float thisRatio = (thisCount - lastCount) / this.stepSize;
                    this.tableToMotor = Turntable.SMOOTH(this.tableToMotor, thisRatio, this.smoothing);
                    lastCount = thisCount;
                }
            }

            degreeTurned = thisCount / this.tableToMotor;

            if (degree - degreeTurned < 5) {
                Motor.A.setSpeed(this.speed / 2);
            }

            LCD.clearDisplay();
            LCD.drawString("Light: " + Float.toString(100f * lightChangePercent), 0, 0);
            LCD.drawString("Deg.: " + Integer.toString((int) degreeTurned) + "/" + Integer.toString(degree), 0, 1);
            LCD.drawString("Turn: " + Integer.toString(noTurn), 0, 2);
            LCD.drawString("Transl.: " + Float.toString(this.tableToMotor), 0, 3);
            LCD.drawString("Step s.: " + Float.toString(this.stepSize), 0, 4);
            Delay.msDelay(100);
        }

        Motor.A.stop();
    }

}

public class TurntableExec {

    public static void main (String[] args) {
        Turntable turntable = new Turntable(300, 37, .05f, 5);
        turntable.start(45, 8);
    }
}
