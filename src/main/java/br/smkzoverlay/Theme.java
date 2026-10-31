package br.smkzoverlay;

import net.minecraft.client.gui.FontRenderer;

/** Cores do tema (principal e secundaria), lidas do config e editaveis no ThemeGui. */
public class Theme {
    public static final int DEFAULT_A1 = 0xFF8B5CF6, DEFAULT_A2 = 0xFF22D3EE;
    public static final int DARK = 0xFF0A0A14;

    public static int a1() { return 0xFF000000 | SmkzOverlay.I.cfg.accent; }
    public static int a2() { return 0xFF000000 | SmkzOverlay.I.cfg.accent2; }

    /** Mistura duas cores (t = 0 devolve c1, t = 1 devolve c2). */
    public static int mix(int c1, int c2, float t) {
        int r = (int) (((c1 >> 16) & 255) + ((((c2 >> 16) & 255) - ((c1 >> 16) & 255)) * t));
        int g = (int) (((c1 >> 8) & 255) + ((((c2 >> 8) & 255) - ((c1 >> 8) & 255)) * t));
        int b = (int) ((c1 & 255) + (((c2 & 255) - (c1 & 255)) * t));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    public static int alpha(int rgb, int a) { return (a << 24) | (rgb & 0xFFFFFF); }

    /** Cor do tema escurecida (para fundos). */
    public static int dark(int rgb, float t) { return mix(rgb, DARK, t); }

    /** Desenha o titulo "SmkzOverlay" nas duas cores do tema; devolve a largura. */
    public static int title(FontRenderer fr, int x, int y) {
        String a = "\u00a7lSmkz", b = "\u00a7lOverlay";
        fr.drawStringWithShadow(a, x, y, a2());
        int w = fr.getStringWidth(a);
        fr.drawStringWithShadow(b, x + w, y, a1());
        return w + fr.getStringWidth(b);
    }
}
