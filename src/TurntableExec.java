import lejos.nxt.*;
import lejos.util.Delay;

import static java.lang.Math.abs;
import static java.lang.Math.floor;


class Turntable {
    private int speed;
    private float tolerance;
    private int smoothing;
    private float stepSize;

    private Boolean stripe;
    private float averageLight = -1f;
    private float tableToMotor = -1f;

    private boolean cancel = false;

    private LightSensor light = new LightSensor(SensorPort.S1);
    private TouchSensor touch = new TouchSensor(SensorPort.S2);

    public Turntable(int speed, int noStripes, float tolerance, int smoothing) {
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

    public void start(int stepDegree, int noSteps) {
        LCD.clearDisplay();
        LCD.drawString("No. stripes: " + Integer.toString((int) (180 / this.stepSize)), 0, 0);
        LCD.drawString("Str. angle: " + Float.toString(this.stepSize), 0, 1);
        LCD.drawString("Target: " + Integer.toString(stepDegree * noSteps), 0, 2);
        LCD.drawString("Press touch...", 0, 6);
        this.waitTillTouch();

        for (int i = 0; i < noSteps; i++) {
            this.light.setFloodlight(true);
            Delay.msDelay(100);
            this.turn(stepDegree);
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

    public void turn(int degree) {
        int fastDegrees = (int) (floor(((float) degree / this.stepSize)) * this.stepSize);
        int slowDegrees = degree - fastDegrees;

        this.moveFast(fastDegrees);
        if (this.cancel) return;
        // Motor.A.flt(true);
        Motor.A.stop();

        this.moveSlow(slowDegrees);
        if (this.cancel) return;
        Motor.A.stop();
    }

    private void moveFast(int degree) {
        float degreeTurned = 0f;
        int lastTachoCount, thisTachoCount = 0;

        int noStripes = 0;

        Motor.A.resetTachoCount();
        Motor.A.setSpeed(this.speed);

        while (degreeTurned < degree) {
            if (this.touch.isPressed()) {
                this.cancel = true;
                break;
            }

            Motor.A.rotate(1000, true);

            float lightChangePercent = this.getLightPercentChange();
            if (this.tolerance < abs(lightChangePercent)) {
                if (this.stripe == null) {
                    this.stripe = lightChangePercent < 0f;
                }
                if ((this.stripe && 0f < lightChangePercent) || (!this.stripe && lightChangePercent < 0f)){
                    if (!this.stripe) noStripes++;
                    this.stripe = !this.stripe;
                    degreeTurned += this.stepSize;

                    lastTachoCount = thisTachoCount;
                    thisTachoCount = Motor.A.getTachoCount();
                    float thisRatio = (thisTachoCount - lastTachoCount) / this.stepSize;
                    this.tableToMotor = Turntable.SMOOTH(this.tableToMotor, thisRatio, this.smoothing);
                }
            }
            LCD.clearDisplay();
            LCD.drawString("Light: " + Float.toString(100f * lightChangePercent), 0, 0);
            LCD.drawString("Turned: " + Integer.toString((int) degreeTurned) + "/" + Integer.toString(degree), 0, 1);
            LCD.drawString("Transl.: " + Float.toString(this.tableToMotor), 0, 3);
            LCD.drawString("Step s.: " + Float.toString(this.stepSize), 0, 4);
            LCD.drawString("Stripes: " + Integer.toString(noStripes), 0, 6);
            Delay.msDelay(100);
        }
    }

    private void moveSlow(int degree) {
        this.moveSlow(degree, this.tableToMotor);
    }

    public void moveSlow(int degree, float tableToMotorRatio) {
        float degreeTurned = 0f;
        int lastTachoCount, thisTachoCount = 0;

        Motor.A.resetTachoCount();
        Motor.A.setSpeed(this.speed / 2);

        Motor.A.rotate((int) (degree * tableToMotorRatio), true);
        Delay.msDelay(100);
        while (Motor.A.isMoving()) {
            if (this.touch.isPressed()) {
                this.cancel = true;
                break;
            }
            float lightChangePercent = this.getLightPercentChange();
            if (this.tolerance < abs(lightChangePercent)) {
                if (this.stripe == null) {
                    this.stripe = lightChangePercent < 0f;
                }
                if ((this.stripe && 0f < lightChangePercent) || (!this.stripe && lightChangePercent < 0f)){
                    this.stripe = !this.stripe;
                }
            }

            lastTachoCount = thisTachoCount;
            thisTachoCount = Motor.A.getTachoCount();
            degreeTurned += (thisTachoCount - lastTachoCount) / tableToMotorRatio;
            LCD.clearDisplay();
            LCD.drawString("Turned: " + Integer.toString((int) degreeTurned) + "/" + Integer.toString(degree), 0, 1);
            LCD.drawString("Transl.: " + Float.toString(tableToMotorRatio), 0, 3);
            Delay.msDelay(100);
        }
    }

}

public class TurntableExec {

    public static void main (String[] args) {
        Turntable turntable = new Turntable(300, 37, .05f, 5);
        turntable.start(36, 10);
    }
}
