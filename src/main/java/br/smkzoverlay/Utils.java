package br.smkzoverlay;

import java.util.Locale;
import java.util.UUID;

public class Utils {
    /** NPCs/bots: nick fora do padr\u00e3o ou UUID vers\u00e3o 2. */
    public static boolean isBot(String name, UUID id) {
        if (name == null || !name.matches("[A-Za-z0-9_]{1,16}")) return true;
        return id != null && id.version() == 2;
    }

    public static String lastColor(String s) {
        String out = "\u00a7f";
        if (s == null) return out;
        for (int i = 0; i < s.length() - 1; i++) {
            if (s.charAt(i) == '\u00a7') {
                char c = s.charAt(i + 1);
                out = c == 'r' ? "\u00a7f" : "\u00a7" + c;
            }
        }
        return out;
    }

    public static String fmt(double v, boolean ratio) {
        return ratio ? String.format(Locale.US, "%.1f", v) : String.valueOf((long) v);
    }
}
