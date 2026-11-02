package br.smkzoverlay;

import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

public class Utils {
    private static final String HEX = "0123456789abcdef";
    private static final int[] RGB = {0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
            0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF};
    private static final String[] NAMES = {"Preto", "Azul Escuro", "Verde Escuro", "Ciano Escuro", "Vermelho Escuro", "Roxo",
            "Laranja", "Cinza", "Cinza Escuro", "Azul", "Verde", "Ciano", "Vermelho", "Rosa", "Amarelo", "Branco"};
    private static final Pattern NICK = Pattern.compile("[A-Za-z0-9_]{1,16}");
    private static final Pattern HEX8 = Pattern.compile("[0-9a-f]{8}");

    /** Cores de time do Bed Wars, na ordem de exibicao: vermelho, azul, verde, amarelo, ciano, branco, rosa, cinza. */
    public static final String TEAM_ORDER = "c9aebfd7";

    /** NPCs/bots: nick fora do padrao, UUID versao 2 ou nome-hex de 8 caracteres (ex: 007617aa). */
    public static boolean isBot(String name, UUID id) {
        if (name == null || !NICK.matcher(name).matches()) return true;
        if (HEX8.matcher(name).matches()) {
            boolean digit = false, letter = false;
            for (int i = 0; i < name.length(); i++) { if (Character.isDigit(name.charAt(i))) digit = true; else letter = true; }
            if (digit && letter) return true;
        }
        return id != null && id.version() == 2;
    }

    /** Ultimo codigo de COR (0-9a-f) da string; ignora negrito/italico e trata r como branco. */
    public static char colorChar(String s) {
        char out = 'f';
        if (s == null) return out;
        for (int i = 0; i < s.length() - 1; i++) {
            if (s.charAt(i) == '\u00a7') {
                char c = Character.toLowerCase(s.charAt(i + 1));
                if (HEX.indexOf(c) >= 0) out = c;
                else if (c == 'r') out = 'f';
            }
        }
        return out;
    }

    public static String lastColor(String s) { return "\u00a7" + colorChar(s); }

    public static int rgbOf(char c) { int i = HEX.indexOf(Character.toLowerCase(c)); return RGB[i < 0 ? 15 : i]; }
    public static String colorName(char c) { int i = HEX.indexOf(Character.toLowerCase(c)); return NAMES[i < 0 ? 15 : i]; }

    /** Cor de time mais proxima de uma cor RGB (armadura tingida); 0 se nao parece nenhuma. */
    public static char teamCharFromRgb(int rgb) {
        int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
        long bestD = Long.MAX_VALUE;
        char best = 0;
        for (int i = 0; i < TEAM_ORDER.length(); i++) {
            char ch = TEAM_ORDER.charAt(i);
            int c = rgbOf(ch);
            long dr = ((c >> 16) & 255) - r, dg = ((c >> 8) & 255) - g, db = (c & 255) - b;
            long d = dr * dr + dg * dg + db * db;
            if (d < bestD) { bestD = d; best = ch; }
        }
        return bestD <= 190L * 190L ? best : 0;
    }

    public static String fmt(double v, boolean ratio) {
        return ratio ? String.format(Locale.US, "%.1f", v) : String.valueOf((long) v);
    }
}
