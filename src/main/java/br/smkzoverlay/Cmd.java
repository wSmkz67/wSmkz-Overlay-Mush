package br.smkzoverlay;

import java.util.*;
import net.minecraft.command.*;

public class Cmd extends CommandBase {
    @Override public String getCommandName() { return "ov"; }
    @Override public List<String> getCommandAliases() { return Arrays.asList("smkzoverlay"); }
    @Override public String getCommandUsage(ICommandSender s) { return "/ov <menu|bl|watch|ws|mode|hud|sound|clear|debug>"; }
    @Override public int getRequiredPermissionLevel() { return 0; }
    @Override public boolean canCommandSenderUseCommand(ICommandSender s) { return true; }

    private static String join(String[] a, int from) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < a.length; i++) { if (sb.length() > 0) sb.append(' '); sb.append(a[i]); }
        return sb.toString();
    }

    @Override
    public void processCommand(ICommandSender s, String[] a) throws CommandException {
        SmkzOverlay o = SmkzOverlay.I;
        if (a.length == 0 || a[0].equalsIgnoreCase("menu")) { o.openMenu = true; return; }
        String c = a[0].toLowerCase();

        if (c.equals("bl") || c.equals("watch")) {
            boolean bl = c.equals("bl");
            String sub = a.length > 1 ? a[1].toLowerCase() : "list";
            if (sub.equals("list")) {
                Map<String, ListStore.Entry> m = bl ? o.lists.black : o.lists.watch;
                if (m.isEmpty()) o.msg("\u00a77Lista vazia.");
                for (ListStore.Entry e : m.values()) o.msg("\u00a7f" + e.name + (e.reason.isEmpty() ? "" : " \u00a77- " + e.reason) + " \u00a78(" + e.date + ")");
            } else if (sub.equals("add") && a.length > 2) {
                o.lists.add(bl, a[2], join(a, 3));
                o.msg("\u00a7aAdicionado: \u00a7f" + a[2]);
            } else if (sub.equals("remove") && a.length > 2) {
                o.msg(o.lists.remove(bl, a[2]) ? "\u00a7aRemovido: \u00a7f" + a[2] : "\u00a7cEsse nick n\u00e3o estava na lista.");
            } else {
                o.msg("\u00a77Uso: /ov " + c + " <add|remove|list> [nick] [motivo]");
            }
        } else if (c.equals("ws")) {
            Config.Mode m = a.length > 2 ? o.cfg.find(a[1]) : null;
            if (m == null) { o.msg("\u00a77Uso: /ov ws <modo> <valor>  (0 desliga). Modos: " + o.modeIds()); return; }
            try { m.wsAlert = Integer.parseInt(a[2]); } catch (NumberFormatException e) { o.msg("\u00a7cValor inv\u00e1lido."); return; }
            o.cfg.save();
            o.msg("\u00a7aAlerta de winstreak em \u00a7f" + m.name + "\u00a7a: \u00a7f" + m.wsAlert);
        } else if (c.equals("mode")) {
            if (a.length < 2) { o.msg("\u00a77Uso: /ov mode <auto|" + o.modeIds() + ">"); return; }
            if (a[1].equalsIgnoreCase("auto")) { o.modeOverride = null; o.msg("\u00a7aModo: autom\u00e1tico"); }
            else if (o.cfg.find(a[1]) != null) { o.modeOverride = o.cfg.find(a[1]).id; o.msg("\u00a7aModo fixado: \u00a7f" + o.modeOverride); }
            else o.msg("\u00a7cModo desconhecido. Dispon\u00edveis: " + o.modeIds());
        } else if (c.equals("hud")) {
            o.cfg.hud = !o.cfg.hud; o.cfg.save();
            o.msg("HUD " + (o.cfg.hud ? "\u00a7aligado" : "\u00a7cdesligado"));
        } else if (c.equals("sound")) {
            o.cfg.sound = !o.cfg.sound; o.cfg.save();
            o.msg("Sons " + (o.cfg.sound ? "\u00a7aligados" : "\u00a7cdesligados"));
        } else if (c.equals("clear")) {
            o.cache.clear(); o.pending.clear(); o.alerted.clear(); o.api.clear();
            o.msg("\u00a7aCache limpo.");
        } else if (c.equals("debug") && a.length > 1) {
            o.debug(a[1]);
        } else {
            o.msg("\u00a77" + getCommandUsage(s));
        }
    }
}
