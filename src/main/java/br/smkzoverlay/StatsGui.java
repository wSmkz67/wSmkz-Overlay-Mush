package br.smkzoverlay;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Mouse;

public class StatsGui extends GuiScreen {
    private static final int RH = 16;
    private int W = 380, px, py, ph, ry, dy, by, left, pillX, pad = 12;
    private int[] cwid = new int[0];
    private int scroll;
    private String sel;
    private SmkzButton bMode, bBl, bWatch, bIgnore, bClose, bTheme;
    private List<Rows.Row> rows = new ArrayList<>();

    /** Calcula largura do painel e posicao das colunas conforme o modo e o tamanho da tela. */
    private void layout() {
        Config.Mode m = SmkzOverlay.I.curMode();
        int nc = m.cols.size();
        int nameW = 100;
        int pillZone = fontRendererObj.getStringWidth("\u00a7lSNIPE?") + 8;
        pillX = 32 + nameW + 4;
        left = pillX + pillZone + 6;
        cwid = new int[nc];
        int sum = 0;
        for (int c = 0; c < nc; c++) {
            cwid[c] = Math.max(fontRendererObj.getStringWidth(m.cols.get(c).label.toUpperCase()), 26);
            sum += cwid[c];
        }
        pad = 12;
        while (pad > 3 && left + 14 + sum + pad * nc > width - 10) pad -= 3;
        W = Math.max(340, left + 14 + sum + pad * nc);
        px = Math.max(2, width / 2 - W / 2);
        py = 10;
        ph = height - 20;
        by = py + ph - 26;
        dy = by - 56;
        ry = py + 48;
    }

    private int colX(int c) {
        int x = px + left;
        for (int k = 0; k < c; k++) x += cwid[k] + pad;
        return x;
    }

    private void placeButtons() {
        Config.Mode cm = SmkzOverlay.I.curMode();
        bTheme.xPosition = px + W - (fontRendererObj.getStringWidth(cm.name) + 14) - 8 - 50;
        bTheme.yPosition = py + 7;
        int bw = (W - 16 - 16) / 5;
        SmkzButton[] bs = {bMode, bBl, bWatch, bIgnore, bClose};
        for (int i = 0; i < bs.length; i++) {
            bs[i].xPosition = px + 8 + i * (bw + 4);
            bs[i].yPosition = by;
            bs[i].width = bw;
        }
    }

    private int players() {
        int n = 0;
        for (Rows.Row r : rows) if (!r.header) n++;
        return n;
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
        bMode = new SmkzButton(0, 0, 0, 80, 20, "");
        bBl = new SmkzButton(1, 0, 0, 80, 20, "");
        bWatch = new SmkzButton(2, 0, 0, 80, 20, "");
        bIgnore = new SmkzButton(5, 0, 0, 80, 20, "Ignorar");
        bClose = new SmkzButton(3, 0, 0, 80, 20, "Fechar");
        bTheme = new SmkzButton(4, 0, 0, 44, 14, "Cores");
        buttonList.add(bMode);
        buttonList.add(bBl);
        buttonList.add(bWatch);
        buttonList.add(bIgnore);
        buttonList.add(bClose);
        buttonList.add(bTheme);
        placeButtons();
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
        int w = fontRendererObj.getStringWidth("\u00a7l" + text) + 6;
        drawRect(x, y, x + w, y + 11, color);
        drawString(fontRendererObj, "\u00a7f\u00a7l" + text, x + 3, y + 2, 0xFFFFFF);
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        layout();
        placeButtons();
        drawGradientRect(0, 0, width, height, 0xC0080810, 0xD01A0C30);
        SmkzOverlay o = SmkzOverlay.I;
        Config.Mode m = o.curMode();
        rows = Rows.build(m);
        int vis = vis();
        scroll = Math.max(0, Math.min(scroll, Math.max(0, rows.size() - vis)));

        // painel + cabecalho
        box(px - 1, py - 1, px + W + 1, py + ph + 1, 0xEC12121F, Theme.a1());
        drawGradientRect(px, py, px + W, py + 28, Theme.dark(Theme.a1(), 0.35f), Theme.dark(Theme.a1(), 0.75f));
        drawRect(px, py + 28, px + W, py + 30, Theme.a2());
        Theme.title(fontRendererObj, px + 10, py + 6);
        drawString(fontRendererObj, "\u00a77" + players() + " jogadores na partida", px + 10, py + 17, 0xFFFFFF);
        int bwid = fontRendererObj.getStringWidth(m.name) + 14;
        box(px + W - bwid - 8, py + 7, px + W - 8, py + 21, Theme.dark(Theme.a1(), 0.8f), Theme.a2());
        drawCenteredString(fontRendererObj, m.name, px + W - 8 - bwid / 2, py + 11, Theme.a2());

        // cabecalho das colunas (valores alinhados a direita)
        drawRect(px + 6, py + 33, px + W - 6, py + 45, 0x55000000);
        drawString(fontRendererObj, "JOGADOR", px + 32, py + 36, Theme.a2());
        for (int c = 0; c < m.cols.size(); c++) {
            String lb = m.cols.get(c).label.toUpperCase();
            drawString(fontRendererObj, lb, colX(c) + cwid[c] - fontRendererObj.getStringWidth(lb), py + 36, Theme.a2());
        }

        // linhas
        if (rows.isEmpty()) drawCenteredString(fontRendererObj, "\u00a77Nenhum jogador detectado ainda...", width / 2, ry + 24, 0xFFFFFF);
        int right = px + W - (rows.size() > vis ? 14 : 6);
        for (int i = 0; i < vis && scroll + i < rows.size(); i++) {
            Rows.Row r = rows.get(scroll + i);
            int y = ry + i * RH;
            if (r.header) {   // cabecalho do time
                int rgb = Utils.rgbOf(r.teamColor);
                drawRect(px + 6, y, right, y + RH - 1, Theme.alpha(rgb, 0x38));
                drawRect(px + 6, y, px + 9, y + RH - 1, 0xFF000000 | rgb);
                drawString(fontRendererObj, r.headerText, px + 14, y + 4, 0xFFFFFF);
                continue;
            }
            int sc = r.pillColor();
            boolean hov = mx >= px + 6 && mx < right && my >= y && my < y + RH - 1;
            if (i % 2 == 0) drawRect(px + 6, y, right, y + RH - 1, 0x22FFFFFF);
            if (hov) drawRect(px + 6, y, right, y + RH - 1, 0x33FFFFFF);
            if (r.lname.equals(sel)) {
                drawRect(px + 6, y, right, y + RH - 1, Theme.alpha(Theme.a1(), 0x55));
                drawRect(right - 2, y, right, y + RH - 1, Theme.a2());
            }
            drawRect(px + 6, y, px + 9, y + RH - 1, sc);
            head(r, px + 14, y + 2, 12, 0xFF000000);
            drawString(fontRendererObj, r.nameStr(), px + 32, y + 4, 0xFFFFFF);
            pill(r.pill(), sc, px + pillX, y + 2);
            for (int c = 0; c < m.cols.size(); c++) {
                String t = Rows.colText(r.d, m, m.cols.get(c));
                int tw = fontRendererObj.getStringWidth(t), tx = colX(c) + cwid[c] - tw;
                int bg = Rows.cellBg(r.d, m, m.cols.get(c));
                if (bg != 0) drawRect(tx - 2, y + 1, tx + tw + 2, y + RH - 2, bg);
                drawString(fontRendererObj, t, tx, y + 4, 0xFFFFFF);
            }
        }
        if (rows.size() > vis) {
            int track = vis * RH, thumb = Math.max(12, track * vis / rows.size());
            int ty = ry + (track - thumb) * scroll / Math.max(1, rows.size() - vis);
            drawRect(px + W - 11, ry, px + W - 7, ry + track, 0x44FFFFFF);
            drawRect(px + W - 11, ty, px + W - 7, ty + thumb, Theme.a1());
        }

        // cartao de detalhes
        box(px + 6, dy, px + W - 6, dy + 52, 0xE00C0C18, Theme.dark(Theme.a1(), 0.6f));
        Rows.Row s = byName(sel);
        if (s == null) {
            drawCenteredString(fontRendererObj, "\u00a77Clique em um jogador para ver os detalhes \u00a78| \u00a77role para ver mais", px + W / 2, dy + 22, 0xFFFFFF);
        } else {
            int sc = s.pillColor();
            head(s, px + 13, dy + 5, 22, sc);
            drawString(fontRendererObj, "\u00a7l" + s.color + (s.nicked ? "\u00a7o" : "") + s.name, px + 42, dy + 4, 0xFFFFFF);
            pill(s.pill(), sc, px + 50 + fontRendererObj.getStringWidth("\u00a7l" + s.name), dy + 3);
            drawString(fontRendererObj, "\u00a77Suspeita", px + 42, dy + 15, 0xFFFFFF);
            drawRect(px + 90, dy + 16, px + 210, dy + 22, 0xFF2A2A3A);
            drawRect(px + 90, dy + 16, px + 90 + 120 * s.res.score / 100, dy + 22, sc);
            drawString(fontRendererObj, "\u00a7f" + s.res.score + "\u00a77/100", px + 216, dy + 15, 0xFFFFFF);
            drawString(fontRendererObj, fontRendererObj.trimStringToWidth("\u00a77Motivos: \u00a7f" + s.res.reasons(), W - 56), px + 42, dy + 26, 0xFFFFFF);
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
            drawString(fontRendererObj, fontRendererObj.trimStringToWidth(line, W - 56), px + 42, dy + 38, 0xFFFFFF);
        }

        bMode.displayString = fontRendererObj.trimStringToWidth("Modo: " + m.name, bMode.width - 8);
        bBl.enabled = bWatch.enabled = bIgnore.enabled = sel != null;
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
        else if (b.id == 5 && s != null) { o.lists.addIgnore(s.name); o.present.remove(s.lname); sel = null; }
        else if (b.id == 3) mc.displayGuiScreen(null);
        else if (b.id == 4) mc.displayGuiScreen(new ThemeGui(this));
    }

    @Override
    protected void mouseClicked(int mx, int my, int btn) throws IOException {
        super.mouseClicked(mx, my, btn);
        layout();
        if (btn == 0 && mx >= px + 6 && mx <= px + W - 6 && my >= ry && my < ry + vis() * RH) {
            int idx = scroll + (my - ry) / RH;
            if (idx >= 0 && idx < rows.size() && !rows.get(idx).header) sel = rows.get(idx).lname;
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
