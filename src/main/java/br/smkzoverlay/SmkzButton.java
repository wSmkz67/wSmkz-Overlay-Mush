package br.smkzoverlay;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;

/** Bot\u00e3o no tema do SmkzOverlay (roxo/ciano). */
public class SmkzButton extends GuiButton {
    public SmkzButton(int id, int x, int y, int w, int h, String text) { super(id, x, y, w, h, text); }

    @Override
    public void drawButton(Minecraft mc, int mx, int my) {
        if (!visible) return;
        boolean hov = mx >= xPosition && my >= yPosition && mx < xPosition + width && my < yPosition + height;
        int border = !enabled ? 0xFF3A3A4A : hov ? 0xFF22D3EE : 0xFF8B5CF6;
        int fill = !enabled ? 0xCC1A1A24 : hov ? 0xE02E2766 : 0xE0201B40;
        Gui.drawRect(xPosition, yPosition, xPosition + width, yPosition + height, border);
        Gui.drawRect(xPosition + 1, yPosition + 1, xPosition + width - 1, yPosition + height - 1, fill);
        if (enabled) Gui.drawRect(xPosition + 1, yPosition + height - 3, xPosition + width - 1, yPosition + height - 1, hov ? 0xFF22D3EE : 0xFF6D45D6);
        drawCenteredString(mc.fontRendererObj, displayString, xPosition + width / 2, yPosition + (height - 8) / 2 - 1, enabled ? 0xFFFFFFFF : 0xFF6B6B7B);
    }
}
