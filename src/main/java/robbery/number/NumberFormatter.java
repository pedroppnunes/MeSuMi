package robbery.number;

import robbery.player.PlayerData;

import java.text.DecimalFormat;

public class NumberFormatter {

    public static long roundToNearest(long price) {
        if (price <= 0) return 0;

        long magnitude = (long) Math.pow(10, (int) Math.log10(price));
        long step = magnitude / 100;

        if (step < 1) step = 1;

        return ((price + step / 2) / step) * step;
    }


    public static double applyPrestigeIncrease(double currentPrice, PlayerData p) {
        return currentPrice * p.getPrestigeBoost();
    }


    public static String formatDoubleNumber(double num) {
        if (num < 1000) return formatDouble(num);
        if (num < 1_000_000) return formatDouble(num / 1_000) + "K";
        if (num < 1_000_000_000) return formatDouble(num / 1_000_000) + "M";
        if (num < 1_000_000_000_000L) return formatDouble(num / 1_000_000_000) + "B";
        return formatDouble(num / 1_000_000_000_000L) + "T";
    }

    public static String formatDouble(double num) {
        DecimalFormat df = new DecimalFormat("0.##");
        return df.format(num);
    }

    public static String formatLong(long num) {
        double value;
        String suffix;

        if (num < 1_000) {
            return String.valueOf(num);
        } else if (num < 1_000_000) {
            value = num / 1_000.0;
            suffix = "K";
        } else if (num < 1_000_000_000) {
            value = num / 1_000_000.0;
            suffix = "M";
        } else if (num < 1_000_000_000_000L) {
            value = num / 1_000_000_000.0;
            suffix = "B";
        } else if (num < 1_000_000_000_000_000L) {
            value = num / 1_000_000_000_000.0;
            suffix = "T";
        } else {
            value = num / 1_000_000_000_000_000.0;
            suffix = "Q";
        }

        String formatted = String.format("%.2f", value);
        if (formatted.endsWith(".00")) {
            formatted = formatted.substring(0, formatted.length() - 3);
        }

        return formatted + suffix;
    }


}
