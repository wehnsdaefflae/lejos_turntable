import lejos.nxt.*;
import lejos.util.Delay;

public class HelloWorld {
    public static void main (String[] args) {
        //LCD.drawString("Progam 5", 0, 0);
        //Button.waitForAnyPress();
        //LCD.clear();

        TouchSensor touch = new TouchSensor(SensorPort.S2);
        LightSensor light = new LightSensor(SensorPort.S1);
        light.setFloodlight(true);

        Motor.A.setSpeed(100);
        float lightValue;
        boolean stripe = false;
        float averageLight = 0f;
        float degreeTurned = 0;
        float lightChangePercent;

        while (!touch.isPressed() && degreeTurned < 360) {
            Motor.A.rotate(1000, true);
            light.calibrateHigh();
            lightValue = light.getNormalizedLightValue();
            averageLight = (averageLight * 4f + lightValue) / 5f;
            lightChangePercent = lightValue / averageLight - 1f;
            if (stripe && lightChangePercent >= .1f) {
                stripe = false;
                degreeTurned += 4.5f;
            } else if (!stripe && lightChangePercent < .1f) {
                stripe = true;
                degreeTurned += 4.5f;
            }
            LCD.clearDisplay();
            LCD.drawInt((int) (100f * lightChangePercent), 0, 0);
            LCD.drawInt((int) degreeTurned, 0, 2);
            Delay.msDelay(100);
        }

    }
}
