package br.smkzoverlay;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Mouse;

public class StatsGui extends GuiScreen {
    private static final int W = 380, RH = 18;
    private static final int ACC = 0xFF8B5CF6, ACC2 = 0xFF22D3EE;
    private int scroll;
    private String sel;
    private SmkzButton bMode, bBl, bWatch;
    private List<Rows.Row> rows = new ArrayList<>();
    private int px, py, ph, ry, dy, by;

    private void layout() {
        px = width / 2 - W / 2; py = 10; ph = height - 20;
        by = py + ph - 26; dy = by - 64; ry = py + 48;
    }

    private int vis() { return Math.max(1, (dy - 4 - ry) / RH); }

    private static void box(int x1, int y1, int x2, int y2, int fill, int border) {
        drawRect(x1, y1, x2, y2, border);
        drawRect(x1 + 1, y1 + 1, x2 - 1, y2 - 1, fill);
    }

    @Override
    public void initGui() {
        layout();
        buttonList.clear();
        int bw = (W - 16 - 12) / 4;
        bMode = new SmkzButton(0, px + 8, by, bw, 20, "");
        bBl = new SmkzButton(1, px + 8 + (bw + 4), by, bw, 20, "");
        bWatch = new SmkzButton(2, px + 8 + 2 * (bw + 4), by, bw, 20, "");
        buttonList.add(bMode);
        buttonList.add(bBl);
        buttonList.add(bWatch);
        buttonList.add(new SmkzButton(3, px + 8 + 3 * (bw + 4), by, bw, 20, "Fechar"));
    }

    private Rows.Row byName(String l) {
        if (l == null) return null;
        for (Rows.Row r : rows) if (r.lname.equals(l)) return r;
        return null;
    }

    private void head(Rows.Row r, int x, int y, int size, int borderColor) {
        drawRect(x - 1, y - 1, x + size + 1, y + size + 1, borderColor);
        if (r.info != null) {
            mc.getTextureManager().bindTexture(r.info.getLocationSkin());
            GlStateManager.color(1f, 1f, 1f, 1f);
            Gui.drawScaledCustomSizeModalRect(x, y, 8, 8, 8, 8, size, size, 64, 64);
        } else {
            drawRect(x, y, x + size, y + size, 0xFF333344);
        }
    }

    private void pill(String text, int color, int x, int y) {
        if (text.isEmpty()) return;
        int w = fontRendererObj.getStringWidth(text) + 8;
        drawRect(x, y, x + w, y + 11, color);
        drawString(fontRendererObj, "\u00a7f\u00a7l" + text, x + 4, y + 2, 0xFFFFFF);
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        layout();
        drawGradientRect(0, 0, width, height, 0xC0080810, 0xD01A0C30);
        SmkzOverlay o = SmkzOverlay.I;
        Config.Mode m = o.curMode();
        rows = Rows.build(m);
        int vis = vis();
        scroll = Math.max(0, Math.min(scroll, Math.max(0, rows.size() - vis)));

        // painel
        box(px - 1, py - 1, px + W + 1, py + ph + 1, 0xEC12121F, ACC);
        drawGradientRect(px, py, px + W, py + 28, 0xFF4B2A9C, 0xFF1D1946);
        drawRect(px, py + 28, px + W, py + 30, ACC2);
        drawString(fontRendererObj, "\u00a7b\u00a7lSmkz\u00a7d\u00a7lOverlay", px + 10, py + 6, 0xFFFFFF);
        drawString(fontRendererObj, "\u00a77" + rows.size() + " jogadores na partida", px + 10, py + 17, 0xFFFFFF);
        int bwid = fontRendererObj.getStringWidth(m.name) + 14;
        box(px + W - bwid - 8, py + 7, px + W - 8, py + 21, 0xFF241B4D, ACC2);
        drawCenteredString(fontRendererObj, "\u00a7b" + m.name, px + W - 8 - bwid / 2, py + 11, 0xFFFFFF);

        // cabe\u00e7alho das colunas
        int cx = px + 178, cw = (W - 178 - 16) / Math.max(1, m.cols.size());
        drawRect(px + 6, py + 33, px + W - 6, py + 45, 0x55000000);
        drawString(fontRendererObj, "\u00a73JOGADOR", px + 32, py + 36, 0xFFFFFF);
        for (int i = 0; i < m.cols.size(); i++)
            drawString(fontRendererObj, "\u00a73" + m.cols.get(i).label.toUpperCase(), cx + i * cw, py + 36, 0xFFFFFF);

        // linhas
        if (rows.isEmpty()) drawCenteredString(fontRendererObj, "\u00a77Nenhum jogador detectado ainda...", width / 2, ry + 24, 0xFFFFFF);
        int right = px + W - (rows.size() > vis ? 14 : 6);
        for (int i = 0; i < vis && scroll + i < rows.size(); i++) {
            Rows.Row r = rows.get(scroll + i);
            int y = ry + i * RH;
            int sc = r.res.color();
            boolean hov = mx >= px + 6 && mx < right && my >= y && my < y + RH - 1;
            if (i % 2 == 0) drawRect(px + 6, y, right, y + RH - 1, 0x22FFFFFF);
            if (hov) drawRect(px + 6, y, right, y + RH - 1, 0x33FFFFFF);
            if (r.lname.equals(sel)) {
                drawRect(px + 6, y, right, y + RH - 1, 0x558B5CF6);
                drawRect(right - 2, y, right, y + RH - 1, ACC2);
            }
            drawRect(px + 6, y, px + 9, y + RH - 1, sc);
            head(r, px + 14, y + 3, 12, 0xFF000000);
            drawString(fontRendererObj, r.color + r.name, px + 32, y + 5, 0xFFFFFF);
            pill(r.res.pill(), sc, px + 132, y + 3);
            for (int c = 0; c < m.cols.size(); c++)
                drawString(fontRendererObj, Rows.colText(r.d, m, m.cols.get(c)), cx + c * cw, y + 5, 0xFFFFFF);
        }
        if (rows.size() > vis) {
            int track = vis * RH, thumb = Math.max(12, track * vis / rows.size());
            int ty = ry + (track - thumb) * scroll / Math.max(1, rows.size() - vis);
            drawRect(px + W - 11, ry, px + W - 7, ry + track, 0x44FFFFFF);
            drawRect(px + W - 11, ty, px + W - 7, ty + thumb, ACC);
        }

        // cart\u00e3o de detalhes
        box(px + 6, dy, px + W - 6, dy + 58, 0xE00C0C18, 0xFF2E2A5A);
        Rows.Row s = byName(sel);
        if (s == null) {
            drawCenteredString(fontRendererObj, "\u00a77Clique em um jogador para ver os detalhes \u00a78| \u00a77role para ver mais", width / 2, dy + 25, 0xFFFFFF);
        } else {
            int sc = s.res.color();
            head(s, px + 13, dy + 6, 24, sc);
            drawString(fontRendererObj, "\u00a7l" + s.color + s.name, px + 44, dy + 6, 0xFFFFFF);
            pill(s.res.pill(), sc, px + 52 + fontRendererObj.getStringWidth("\u00a7l" + s.name), dy + 5);
            drawString(fontRendererObj, "\u00a77Suspeita", px + 44, dy + 18, 0xFFFFFF);
            drawRect(px + 92, dy + 19, px + 212, dy + 25, 0xFF2A2A3A);
            drawRect(px + 92, dy + 19, px + 92 + 120 * s.res.score / 100, dy + 25, sc);
            drawString(fontRendererObj, "\u00a7f" + s.res.score + "\u00a77/100", px + 218, dy + 18, 0xFFFFFF);
            drawString(fontRendererObj, fontRendererObj.trimStringToWidth("\u00a77Motivos: \u00a7f" + s.res.reasons(), W - 60), px + 44, dy + 30, 0xFFFFFF);
            String line;
            if (s.d != null && s.d.loaded && !s.d.notFound) {
                StringBuilder sb = new StringBuilder();
                String first = s.d.firstLogin > 0 ? new SimpleDateFormat("dd/MM/yy").format(new Date(s.d.firstLogin)) : "?";
                sb.append("\u00a77Entrou: \u00a7f").append(first).append("  \u00a77Visto 24h: \u00a7f").append(o.history.recent(s.lname, 86400000L)).append("  ");
                for (Config.Col c : m.cols) sb.append("\u00a77").append(c.label).append(' ').append(Rows.colText(s.d, m, c)).append("  ");
                line = sb.toString();
            } else {
                line = "\u00a77Sem dados da API para este jogador.";
            }
            drawString(fontRendererObj, fontRendererObj.trimStringToWidth(line, W - 60), px + 44, dy + 42, 0xFFFFFF);
        }

        bMode.displayString = fontRendererObj.trimStringToWidth("Modo: " + m.name, 84);
        bBl.enabled = bWatch.enabled = sel != null;
        bBl.displayString = sel != null && o.lists.has(true, sel) ? "- Blacklist" : "+ Blacklist";
        bWatch.displayString = sel != null && o.lists.has(false, sel) ? "- Interesse" : "+ Interesse";
        super.drawScreen(mx, my, pt);
    }

    @Override
    protected void actionPerformed(GuiButton b) throws IOException {
        SmkzOverlay o = SmkzOverlay.I;
        Rows.Row s = byName(sel);
        if (b.id == 0) o.cycleMode();
        else if (b.id == 1 && s != null) o.lists.toggle(true, s.name);
        else if (b.id == 2 && s != null) o.lists.toggle(false, s.name);
        else if (b.id == 3) mc.displayGuiScreen(null);
    }

    @Override
    protected void mouseClicked(int mx, int my, int btn) throws IOException {
        super.mouseClicked(mx, my, btn);
        layout();
        if (btn == 0 && mx >= px + 6 && mx <= px + W - 6 && my >= ry && my < ry + vis() * RH) {
            int idx = scroll + (my - ry) / RH;
            if (idx >= 0 && idx < rows.size()) sel = rows.get(idx).lname;
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int w = Mouse.getEventDWheel();
        if (w != 0) scroll += w > 0 ? -1 : 1;
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}
