package ua.leskivproduction.fifteenth.utils;

public class Lerper {
    public static float lerp(float start, float end, float percent) {
        return start*(1-percent)+end*percent;
    }
}
