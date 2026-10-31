package br.smkzoverlay;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;

/** Botao no tema do SmkzOverlay (usa as cores escolhidas). */
public class SmkzButton extends GuiButton {
    public SmkzButton(int id, int x, int y, int w, int h, String text) { super(id, x, y, w, h, text); }

    @Override
    public void drawButton(Minecraft mc, int mx, int my) {
        if (!visible) return;
        boolean hov = mx >= xPosition && my >= yPosition && mx < xPosition + width && my < yPosition + height;
        int a1 = Theme.a1(), a2 = Theme.a2();
        int border = !enabled ? 0xFF3A3A4A : hov ? a2 : a1;
        int fill = !enabled ? 0xCC1A1A24 : Theme.alpha(Theme.dark(a1, hov ? 0.5f : 0.75f), 0xE0);
        Gui.drawRect(xPosition, yPosition, xPosition + width, yPosition + height, border);
        Gui.drawRect(xPosition + 1, yPosition + 1, xPosition + width - 1, yPosition + height - 1, fill);
        if (enabled && height >= 16) Gui.drawRect(xPosition + 1, yPosition + height - 3, xPosition + width - 1, yPosition + height - 1, hov ? a2 : Theme.dark(a1, 0.2f));
        drawCenteredString(mc.fontRendererObj, displayString, xPosition + width / 2, yPosition + (height - 8) / 2 - (height >= 16 ? 1 : 0), enabled ? 0xFFFFFFFF : 0xFF6B6B7B);
    }
}
