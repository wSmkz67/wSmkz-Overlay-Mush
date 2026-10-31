package br.smkzoverlay;

import java.io.IOException;
import net.minecraft.client.gui.*;

/** Seletor de cores: temas prontos + sliders RGB para a cor principal e a secundaria. */
public class ThemeGui extends GuiScreen {
    private static final int W = 320, H = 218;
    private static final int[][] PRESETS = {
        {0x8B5CF6, 0x22D3EE}, {0x3B82F6, 0x22D3EE}, {0xEF4444, 0xF59E0B}, {0x22C55E, 0xA3E635},
        {0xEC4899, 0x8B5CF6}, {0xF97316, 0xFACC15}, {0x06B6D4, 0xE5E7EB}, {0x64748B, 0xE2E8F0}};
    private static final String[] NAMES = {"Roxo", "Azul", "Vermelho", "Verde", "Rosa", "Laranja", "Ciano", "Cinza"};
    private static final String[] CH = {"R", "G", "B"};
    private final GuiScreen prev;
    private int px, py, drag = -1;
    private SmkzButton bReset, bBack;

    public ThemeGui(GuiScreen prev) { this.prev = prev; }

    private void layout() { px = width / 2 - W / 2; py = Math.max(2, (height - H) / 2); }

    // geometria dos 6 sliders (0-2 = cor principal R,G,B; 3-5 = secundaria)
    private int sx() { return px + 34; }
    private int sw() { return W - 34 - 56; }
    private int sy(int i) { return (i < 3 ? py + 82 : py + 130) + (i % 3) * 11; }

    private int rgb(int i) { return (i < 3 ? SmkzOverlay.I.cfg.accent : SmkzOverlay.I.cfg.accent2) & 0xFFFFFF; }
    private int shift(int i) { return 16 - 8 * (i % 3); }
    private int val(int i) { return (rgb(i) >> shift(i)) & 255; }

    private void setVal(int i, int v) {
        int c = (rgb(i) & ~(255 << shift(i))) | (v << shift(i));
        if (i < 3) SmkzOverlay.I.cfg.accent = 0xFF000000 | c; else SmkzOverlay.I.cfg.accent2 = 0xFF000000 | c;
    }

    private void applySlider(int mx) {
        if (drag < 0) return;
        int v = (mx - sx()) * 255 / Math.max(1, sw());
        setVal(drag, Math.max(0, Math.min(255, v)));
    }

    private int presetX(int i) { return px + (W - (8 * 28 + 7 * 6)) / 2 + i * 34; }

    @Override
    public void initGui() {
        layout();
        buttonList.clear();
        int by = py + H - 24;
        bReset = new SmkzButton(0, px + 10, by, 140, 20, "Restaurar padr\u00e3o");
        bBack = new SmkzButton(1, px + W - 150, by, 140, 20, "Voltar");
        buttonList.add(bReset);
        buttonList.add(bBack);
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        layout();
        bReset.xPosition = px + 10; bReset.yPosition = py + H - 24;
        bBack.xPosition = px + W - 150; bBack.yPosition = py + H - 24;
        int a1 = Theme.a1(), a2 = Theme.a2();
        drawGradientRect(0, 0, width, height, 0xC0080810, Theme.alpha(Theme.dark(a1, 0.85f), 0xD0));
        drawRect(px - 1, py - 1, px + W + 1, py + H + 1, a1);
        drawRect(px, py, px + W, py + H, 0xEC12121F);
        drawCenteredString(fontRendererObj, "\u00a7lCores do SmkzOverlay", px + W / 2, py + 8, 0xFFFFFF);

        // temas prontos
        drawString(fontRendererObj, "Temas prontos", px + 10, py + 24, 0xFF9CA3AF);
        String hover = null;
        for (int i = 0; i < PRESETS.length; i++) {
            int x = presetX(i), y = py + 36;
            boolean cur = (SmkzOverlay.I.cfg.accent & 0xFFFFFF) == PRESETS[i][0] && (SmkzOverlay.I.cfg.accent2 & 0xFFFFFF) == PRESETS[i][1];
            boolean hov = mx >= x && mx < x + 28 && my >= y && my < y + 16;
            if (hov) hover = NAMES[i];
            drawRect(x - 1, y - 1, x + 29, y + 17, cur ? 0xFFFFFFFF : hov ? 0xFF9CA3AF : 0xFF000000);
            drawRect(x, y, x + 14, y + 16, 0xFF000000 | PRESETS[i][0]);
            drawRect(x + 14, y, x + 28, y + 16, 0xFF000000 | PRESETS[i][1]);
        }
        if (hover != null) drawCenteredString(fontRendererObj, hover, px + W / 2, py + 55, 0xFFD1D5DB);

        // sliders
        drawString(fontRendererObj, "Cor principal", px + 10, py + 70, a1);
        drawRect(px + W - 30, py + 68, px + W - 10, py + 80, 0xFFFFFFFF);
        drawRect(px + W - 29, py + 69, px + W - 11, py + 79, a1);
        drawString(fontRendererObj, "Cor secund\u00e1ria", px + 10, py + 118, a2);
        drawRect(px + W - 30, py + 116, px + W - 10, py + 128, 0xFFFFFFFF);
        drawRect(px + W - 29, py + 117, px + W - 11, py + 127, a2);
        for (int i = 0; i < 6; i++) {
            int x = sx(), y = sy(i), w = sw(), v = val(i);
            drawString(fontRendererObj, CH[i % 3], px + 18, y, 0xFFB0B0C0);
            drawRect(x, y, x + w, y + 7, 0xFF1E1E2C);
            int chCol = 0xFF000000 | (v << shift(i));
            drawRect(x, y, x + w * v / 255, y + 7, chCol);
            int kx = x + w * v / 255;
            drawRect(kx - 1, y - 1, kx + 2, y + 8, 0xFFFFFFFF);
            drawString(fontRendererObj, String.valueOf(v), px + W - 46, y, 0xFFD1D5DB);
        }

        // previa ao vivo
        int pyv = py + 166;
        drawRect(px + 9, pyv - 1, px + W - 9, pyv + 23, a1);
        drawGradientRect(px + 10, pyv, px + W - 10, pyv + 22, Theme.dark(a1, 0.35f), Theme.dark(a1, 0.75f));
        drawRect(px + 10, pyv + 20, px + W - 10, pyv + 22, a2);
        Theme.title(fontRendererObj, px + 16, pyv + 6);
        int bw = fontRendererObj.getStringWidth("Bed Wars") + 10;
        drawRect(px + W - 16 - bw, pyv + 4, px + W - 16, pyv + 16, a2);
        drawRect(px + W - 15 - bw, pyv + 5, px + W - 17, pyv + 15, Theme.dark(a1, 0.8f));
        drawCenteredString(fontRendererObj, "Bed Wars", px + W - 16 - bw / 2, pyv + 6, a2);

        super.drawScreen(mx, my, pt);
    }

    @Override
    protected void actionPerformed(GuiButton b) throws IOException {
        if (b.id == 0) {
            SmkzOverlay.I.cfg.accent = Theme.DEFAULT_A1;
            SmkzOverlay.I.cfg.accent2 = Theme.DEFAULT_A2;
            SmkzOverlay.I.cfg.save();
        } else if (b.id == 1) {
            back();
        }
    }

    private void back() {
        SmkzOverlay.I.cfg.save();
        mc.displayGuiScreen(prev);
    }

    @Override
    protected void mouseClicked(int mx, int my, int btn) throws IOException {
        super.mouseClicked(mx, my, btn);
        if (btn != 0) return;
        layout();
        for (int i = 0; i < PRESETS.length; i++) {
            int x = presetX(i), y = py + 36;
            if (mx >= x && mx < x + 28 && my >= y && my < y + 16) {
                SmkzOverlay.I.cfg.accent = 0xFF000000 | PRESETS[i][0];
                SmkzOverlay.I.cfg.accent2 = 0xFF000000 | PRESETS[i][1];
                SmkzOverlay.I.cfg.save();
                return;
            }
        }
        for (int i = 0; i < 6; i++) {
            if (mx >= sx() - 3 && mx <= sx() + sw() + 3 && my >= sy(i) - 2 && my <= sy(i) + 9) {
                drag = i;
                applySlider(mx);
                return;
            }
        }
    }

    @Override
    protected void mouseClickMove(int mx, int my, int btn, long time) {
        applySlider(mx);
    }

    @Override
    protected void mouseReleased(int mx, int my, int state) {
        super.mouseReleased(mx, my, state);
        if (drag >= 0) { drag = -1; SmkzOverlay.I.cfg.save(); }
    }

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        if (key == 1) { back(); return; }
        super.keyTyped(c, key);
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}
