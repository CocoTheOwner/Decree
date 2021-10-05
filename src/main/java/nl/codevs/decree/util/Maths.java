package nl.codevs.decree.util;

/**
 * Math
 *
 * @author cyberpwn
 */
@SuppressWarnings("SpellCheckingInspection")
public class Maths {

    /**
     * Get true or false based on random percent
     *
     * @param d between 0 and 1
     * @return true if true
     */
    public static boolean r(Double d) {
        //noinspection ReplaceNullCheck
        if (d == null) {
            return Math.random() < 0.5;
        }

        return Math.random() < d;
    }

    /**
     * Get true or false, randomly
     */
    public static boolean r() {
        return Math.random() < 0.5;
    }

    /**
     * Get a random int from to (inclusive)
     *
     * @param f the lower bound
     * @param t the upper bound
     * @return the value
     */
    public static int irand(int f, int t) {
        return f + (int) (Math.random() * ((t - f) + 1));
    }

    /**
     * Get a random float from to (inclusive)
     *
     * @param f the lower bound
     * @param t the upper bound
     * @return the value
     */
    public static float frand(float f, float t) {
        return f + (float) (Math.random() * ((t - f) + 1));
    }

    /**
     * Get a random double from to (inclusive)
     *
     * @param f the lower bound
     * @param t the upper bound
     * @return the value
     */
    public static double drand(double f, double t) {
        return f + (Math.random() * ((t - f) + 1));
    }
}
