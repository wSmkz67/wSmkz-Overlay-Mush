package br.smkzoverlay;

import java.util.*;
import net.minecraft.command.*;

public class Cmd extends CommandBase {
    @Override public String getCommandName() { return "smkz"; }
    @Override public List<String> getCommandAliases() { return Arrays.asList("Smkz", "SMKZ", "ov", "smkzoverlay"); }
    @Override public String getCommandUsage(ICommandSender s) { return "/smkz ajuda  (lista todos os comandos)"; }
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
                if (m.isEmpty()) { o.msg("\u00a77Lista vazia."); return; }
                int page = 1;
                try { if (a.length > 2) page = Math.max(1, Integer.parseInt(a[2])); } catch (NumberFormatException ignored) {}
                int per = 12, pages = (m.size() + per - 1) / per;
                page = Math.min(page, pages);
                o.msg("\u00a77" + m.size() + " nicks - p\u00e1gina " + page + "/" + pages + " (use /smkz " + c + " list <p\u00e1gina>)");
                int idx = 0;
                for (ListStore.Entry e : m.values()) {
                    if (idx >= (page - 1) * per && idx < page * per)
                        o.msg("\u00a7f" + e.name + (e.reason.isEmpty() ? "" : " \u00a77- " + e.reason) + " \u00a78(" + e.date + ")");
                    idx++;
                }
            } else if (sub.equals("add") && a.length > 2) {
                o.lists.add(bl, a[2], join(a, 3));
                o.msg("\u00a7aAdicionado: \u00a7f" + a[2]);
            } else if (sub.equals("remove") && a.length > 2) {
                o.msg(o.lists.remove(bl, a[2]) ? "\u00a7aRemovido: \u00a7f" + a[2] : "\u00a7cEsse nick n\u00e3o estava na lista.");
            } else {
    o.msg("\u00a77Uso: /smkz " + c + " <add|remove|list> [nick] [motivo]");
}
        } else if (c.equals("ws")) {
            Config.Mode m = a.length > 2 ? o.cfg.find(a[1]) : null;
            if (m == null) { o.msg("\u00a77Uso: /smkz ws <modo> <valor>  (0 desliga). Modos: " + o.modeIds()); return; }
            try { m.wsAlert = Integer.parseInt(a[2]); } catch (NumberFormatException e) { o.msg("\u00a7cValor inv\u00e1lido."); return; }
            o.cfg.save();
            o.msg("\u00a7aAlerta de winstreak em \u00a7f" + m.name + "\u00a7a: \u00a7f" + m.wsAlert);
        } else if (c.equals("mode")) {
            if (a.length < 2) { o.msg("\u00a77Uso: /smkz mode <auto|" + o.modeIds() + ">"); return; }
            if (a[1].equalsIgnoreCase("auto")) { o.modeOverride = null; o.msg("\u00a7aModo: autom\u00e1tico"); }
            else if (o.cfg.find(a[1]) != null) { o.modeOverride = o.cfg.find(a[1]).id; o.msg("\u00a7aModo fixado: \u00a7f" + o.modeOverride); }
            else o.msg("\u00a7cModo desconhecido. Dispon\u00edveis: " + o.modeIds());
        } else if (c.equals("ajuda") || c.equals("help") || c.equals("?")) {
            String[] h = {
                "\u00a7b\u00a7lSmkzOverlay \u00a77- comandos (tamb\u00e9m /Smkz, /ov):",
                "\u00a7f/smkz \u00a77abre o menu (tecla ' tamb\u00e9m)",
                "\u00a7f/smkz ajuda \u00a77esta lista",
                "\u00a7f/smkz cores \u00a77seletor de cores do tema",
                "\u00a7f/smkz bl add <nick> [motivo] \u00a77adiciona \u00e0 blacklist",
                "\u00a7f/smkz bl remove <nick> \u00a77remove da blacklist",
                "\u00a7f/smkz bl list [p\u00e1gina] \u00a77mostra a blacklist",
                "\u00a7f/smkz watch add|remove|list <nick> \u00a77lista de interesse",
                "\u00a7f/smkz ignore add|remove|list <nick> \u00a77esconde NPCs/jogadores",
                "\u00a7f/smkz qb learn \u00a77aprende a quickbuy dos snipers da blacklist (segundo plano)",
                "\u00a7f/smkz qb stop|status|list \u00a77para / mostra o estado / lista as salvas",
                "\u00a7f/smkz qb add|remove <nick> \u00a77salva/remove a quickbuy de um sniper",
                "\u00a7f/smkz qb min <1-100> \u00a77porcentagem m\u00ednima para alertar",
                "\u00a7f/smkz qb on|off \u00a77liga/desliga o alerta de quickbuy",
                "\u00a7f/smkz qb debug <nick> \u00a77confere o formato da resposta da API",
                "\u00a7f/smkz ws <modo> <valor> \u00a77limite do alerta de winstreak (0 desliga)",
                "\u00a7f/smkz mode <auto|id> \u00a77fixa o modo (" + o.modeIds() + ")",
                "\u00a7f/smkz hud \u00a77liga/desliga o HUD",
                "\u00a7f/smkz sound \u00a77liga/desliga os sons",
                "\u00a7f/smkz times \u00a77liga/desliga a separa\u00e7\u00e3o por times",
                "\u00a7f/smkz timesdebug \u00a77mostra time/prefixo de cada jogador",
                "\u00a7f/smkz npc \u00a77liga/desliga o filtro autom\u00e1tico de NPCs",
                "\u00a7f/smkz clear \u00a77limpa o cache de stats",
                "\u00a7f/smkz debug <nick> \u00a77mostra as chaves da API"};
            for (String line : h) o.msg(line);
        } else if (c.equals("qb")) {
            String sub = a.length > 1 ? a[1].toLowerCase() : "status";
            if (sub.equals("learn")) {
                List<String> nicks = new ArrayList<>();
                for (ListStore.Entry e : o.lists.black.values()) nicks.add(e.name);
                int n = o.qb.learn(nicks);
                o.msg(n == 0 ? "\u00a77Nada novo para aprender (todos os snipers da blacklist j\u00e1 foram verificados)."
                        : "\u00a7aAprendendo a quickbuy de \u00a7f" + n + " \u00a7asnipers em segundo plano (~" + (n * o.cfg.qbLearnDelayMs / 60000) + " min). \u00a77Use /smkz qb stop para parar.");
            } else if (sub.equals("stop")) {
                o.qb.stopLearn();
                o.msg("\u00a77Aprendizado parado. O que j\u00e1 foi salvo continua valendo.");
            } else if (sub.equals("add") && a.length > 2) {
                o.qb.addCentral(a[2]);
                o.msg("\u00a77Buscando a quickbuy de \u00a7f" + a[2] + "\u00a77...");
            } else if (sub.equals("remove") && a.length > 2) {
                o.msg(o.qb.remove(a[2]) ? "\u00a7aQuickbuy removida: \u00a7f" + a[2] : "\u00a7cN\u00e3o havia quickbuy salva desse nick.");
            } else if (sub.equals("list")) {
                List<String> l = o.qb.nicks();
                o.msg("\u00a77" + l.size() + " quickbuys de snipers salvas" + (l.isEmpty() ? "." : ". \u00daltimas: \u00a7f" + l.subList(Math.max(0, l.size() - 15), l.size())));
            } else if (sub.equals("min") && a.length > 2) {
                try { o.cfg.qbMin = Math.max(1, Math.min(100, Integer.parseInt(a[2]))); } catch (NumberFormatException e) { o.msg("\u00a7cValor inv\u00e1lido."); return; }
                o.cfg.save();
                o.msg("\u00a7aAlerta de quickbuy a partir de \u00a7f" + o.cfg.qbMin + "%");
            } else if (sub.equals("on") || sub.equals("off")) {
                o.cfg.qb = sub.equals("on"); o.cfg.save();
                o.msg("Alerta de quickbuy " + (o.cfg.qb ? "\u00a7aligado" : "\u00a7cdesligado"));
            } else if (sub.equals("debug") && a.length > 2) {
                o.qb.debug(a[2]);
            } else {
                o.msg("\u00a77Quickbuy: \u00a7f" + o.qb.size() + " \u00a77salvas (" + o.qb.genericCount() + " ignoradas por serem comuns) | alerta a partir de \u00a7f"
                        + o.cfg.qbMin + "% \u00a77| " + (o.cfg.qb ? "\u00a7aligado" : "\u00a7cdesligado")
                        + (o.qb.isLearning() ? " \u00a77| aprendendo " + o.qb.progress() : ""));
            }
        } else if (c.equals("timesdebug")) {
            o.debugTeams();
        } else if (c.equals("ignore")) {
            String sub = a.length > 1 ? a[1].toLowerCase() : "list";
            if (sub.equals("list")) {
                if (o.lists.ignore.isEmpty()) o.msg("\u00a77Nenhum jogador ignorado.");
                for (ListStore.Entry e : o.lists.ignore.values()) o.msg("\u00a7f" + e.name + " \u00a78(" + e.date + ")");
            } else if (sub.equals("add") && a.length > 2) {
                o.lists.addIgnore(a[2]);
                o.present.remove(a[2].toLowerCase());
                o.msg("\u00a7aIgnorando: \u00a7f" + a[2]);
            } else if (sub.equals("remove") && a.length > 2) {
                o.msg(o.lists.removeIgnore(a[2]) ? "\u00a7aVoltou a aparecer: \u00a7f" + a[2] : "\u00a7cEsse nick n\u00e3o estava ignorado.");
            } else {
                o.msg("\u00a77Uso: /smkz ignore <add|remove|list> [nick]");
            }
        } else if (c.equals("times")) {
            o.cfg.teams = !o.cfg.teams; o.cfg.save();
            o.msg("Separar por times " + (o.cfg.teams ? "\u00a7aligado" : "\u00a7cdesligado"));
        } else if (c.equals("npc")) {
            o.cfg.npcPing = !o.cfg.npcPing; o.cfg.save();
            o.msg("Filtro autom\u00e1tico de NPCs (sem ping) " + (o.cfg.npcPing ? "\u00a7aligado" : "\u00a7cdesligado"));
        } else if (c.equals("cores") || c.equals("cor")) {
            o.openTheme = true;
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
