package nl.codevs.decree.util;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

public class Form {
    private static NumberFormat NF;
    private static DecimalFormat DF;

    @SuppressWarnings("unused")
    public static String getNumberSuffixThStRd(int day) {
        if (day >= 11 && day <= 13) {
            return Form.f(day) + "th";
        }
        switch (day % 10) {
            case 1 -> {
                return Form.f(day) + "st";
            }
            case 2 -> {
                return Form.f(day) + "nd";
            }
            case 3 -> {
                return Form.f(day) + "rd";
            }
            default -> {
                return Form.f(day) + "th";
            }
        }
    }

    private static void instantiate() {
        if (NF == null) {
            NF = NumberFormat.getInstance(Locale.US);
        }
    }

    /**
     * Capitalize the first letter
     *
     * @param s the string
     * @return the capitalized string
     */
    public static String capitalize(String s) {
        StringBuilder roll = new StringBuilder();
        boolean f = true;

        for (Character i : s.trim().toCharArray()) {
            if (f) {
                roll.append(Character.toUpperCase(i));
                f = false;
            } else {
                roll.append(i);
            }
        }

        return roll.toString();
    }

    /**
     * Format a number. Changes -10334 into -10,334
     *
     * @param i the number
     * @return the string representation of the number
     */
    public static String f(int i) {
        instantiate();
        return NF.format(i);
    }

    /**
     * Formats a double's decimals to a limit
     *
     * @param i the double
     * @param p the number of decimal places to use
     * @return the formatted string
     */
    public static String f(double i, int p) {
        String form = "#";

        if (p > 0) {
            form = form + "." + repeat("#", p);
        }

        DF = new DecimalFormat(form);

        return DF.format(i).replaceAll("\\Q,\\E", ".");
    }

    /**
     * Formats a float's decimals to a limit
     *
     * @param i the float
     * @param p the number of decimal places to use
     * @return the formatted string
     */
    public static String f(float i, int p) {
        String form = "#";

        if (p > 0) {
            form = form + "." + repeat("#", p);
        }

        DF = new DecimalFormat(form);

        return DF.format(i);
    }
    /**
     * Repeat a string
     *
     * @param s the string
     * @param n the amount of times to repeat
     * @return the repeated string
     */
    @SuppressWarnings("StringRepeatCanBeUsed")
    public static String repeat(String s, int n) {
        if (s == null) {
            return null;
        }

        final StringBuilder sb = new StringBuilder();

        for (int i = 0; i < n; i++) {
            sb.append(s);
        }

        return sb.toString();
    }
}
