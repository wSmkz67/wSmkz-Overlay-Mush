package br.smkzoverlay;

import java.util.Locale;
import java.util.UUID;

public class Utils {
    private static final String HEX = "0123456789abcdef";
    private static final int[] RGB = {0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
            0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF};
    private static final String[] NAMES = {"Preto", "Azul Escuro", "Verde Escuro", "Ciano Escuro", "Vermelho Escuro", "Roxo",
            "Laranja", "Cinza", "Cinza Escuro", "Azul", "Verde", "Ciano", "Vermelho", "Rosa", "Amarelo", "Branco"};

    /** NPCs/bots: nick fora do padrao, UUID versao 2 ou nome-hex de 8 caracteres (ex: 007617aa). */
    public static boolean isBot(String name, UUID id) {
        if (name == null || !name.matches("[A-Za-z0-9_]{1,16}")) return true;
        if (name.matches("[0-9a-f]{8}") && name.matches(".*[0-9].*") && name.matches(".*[a-f].*")) return true;
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

    public static String fmt(double v, boolean ratio) {
        return ratio ? String.format(Locale.US, "%.1f", v) : String.valueOf((long) v);
    }
}
