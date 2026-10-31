package br.smkzoverlay;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.client.Minecraft;

/**
 * Compara a quickbuy (layout da loja de Bed Wars) dos jogadores da partida com a de snipers conhecidos.
 * Rota usada: GET <qbUrl> (padrao https://mush.com.br/api/player/name/NICK/bedwars) -> quick_buy_layout.items
 */
public class QuickBuy {
    public static class Match {
        public final String central;
        public final int pct;
        Match(String central, int pct) { this.central = central; this.pct = pct; }
    }

    /** kind: 0 = jogador da partida, 1 = adicionar manualmente, 2 = aprendizado em segundo plano. */
    private static class Task implements Comparable<Task> {
        final String nick;
        final int kind;
        final long seq;
        Task(String nick, int kind, long seq) { this.nick = nick; this.kind = kind; this.seq = seq; }
        public int compareTo(Task o) { return kind != o.kind ? kind - o.kind : Long.compare(seq, o.seq); }
    }

    private static final int MIN_ITEMS = 4;
    private final File file;
    private final Map<String, List<String>> centrals = new LinkedHashMap<>();   // nick minusculo -> itens
    private final Map<String, String> names = new HashMap<>();                  // nick minusculo -> nick
    private final Map<String, String> keyOf = new HashMap<>();
    private final Map<String, Integer> sameCount = new HashMap<>();
    private final Map<String, List<String>> layouts = new HashMap<>();          // jogadores desta partida
    private final Map<String, Match> matches = new HashMap<>();
    private final Set<String> requested = new HashSet<>();
    private final PriorityBlockingQueue<Task> queue = new PriorityBlockingQueue<>();
    private final AtomicLong seq = new AtomicLong();
    private int learnTotal, learnDone, learnSaved;
    private boolean learning;

    public QuickBuy(File file) {
        this.file = file;
        try {
            if (file.exists()) {
                Reader r = new InputStreamReader(new FileInputStream(file), "UTF-8");
                Map<String, List<String>> x = Config.G.fromJson(r, new TypeToken<Map<String, List<String>>>() {}.getType());
                r.close();
                if (x != null) for (Map.Entry<String, List<String>> en : x.entrySet()) put(en.getKey(), en.getValue());
            }
        } catch (Exception e) { e.printStackTrace(); }
        rebuild();
        Thread t = new Thread(new Runnable() { public void run() { loop(); } }, "SmkzOverlay-QB");
        t.setDaemon(true);
        t.start();
    }

    // ---------------- dados ----------------
    private void put(String nick, List<String> items) {
        List<String> clean = new ArrayList<>();
        for (String s : items) clean.add(s == null ? "" : s.trim().toLowerCase());
        centrals.put(nick.toLowerCase(), clean);
        names.put(nick.toLowerCase(), nick);
    }

    private static int nonEmpty(List<String> l) {
        int n = 0;
        for (String s : l) if (!s.isEmpty()) n++;
        return n;
    }

    /** Layouts que aparecem em varios snipers provavelmente sao o padrao do jogo: sao ignorados. */
    private void rebuild() {
        keyOf.clear();
        sameCount.clear();
        for (Map.Entry<String, List<String>> en : centrals.entrySet()) {
            if (nonEmpty(en.getValue()) < MIN_ITEMS) continue;
            StringBuilder sb = new StringBuilder();
            for (String s : en.getValue()) sb.append(s).append('|');
            String k = sb.toString();
            keyOf.put(en.getKey(), k);
            Integer c = sameCount.get(k);
            sameCount.put(k, c == null ? 1 : c + 1);
        }
    }

    private void save() {
        try {
            Map<String, List<String>> out = new LinkedHashMap<>();
            for (Map.Entry<String, List<String>> en : centrals.entrySet()) out.put(names.get(en.getKey()), en.getValue());
            Writer w = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
            Config.G.toJson(out, w);
            w.close();
        } catch (Exception e) { e.printStackTrace(); }
    }

    static int similarity(List<String> central, List<String> p) {
        int n = 0, hit = 0;
        for (int i = 0; i < central.size(); i++) {
            String c = central.get(i);
            if (c.isEmpty()) continue;
            n++;
            if (i < p.size() && c.equals(p.get(i))) hit++;
        }
        return n == 0 ? 0 : hit * 100 / n;
    }

    private Match bestMatch(String lname, List<String> items) {
        if (items.isEmpty()) return null;
        int generic = SmkzOverlay.I.cfg.qbGeneric;
        Match best = null;
        for (Map.Entry<String, List<String>> en : centrals.entrySet()) {
            if (en.getKey().equals(lname)) continue;
            String k = keyOf.get(en.getKey());
            if (k == null) continue;                                   // layout curto/vazio
            Integer same = sameCount.get(k);
            if (same != null && same >= generic) continue;             // layout comum (padrao)
            int p = similarity(en.getValue(), items);
            if (best == null || p > best.pct) best = new Match(names.get(en.getKey()), p);
        }
        return best == null || best.pct <= 0 ? null : best;
    }

    // ---------------- consultas publicas (thread principal) ----------------
    public Match matchFor(String lname) { return matches.get(lname); }
    public boolean hasCentrals() { return !keyOf.isEmpty(); }
    public int size() { return centrals.size(); }
    public boolean isLearning() { return learning; }
    public String progress() { return learnDone + "/" + learnTotal; }

    public int genericCount() {
        int g = 0, min = SmkzOverlay.I.cfg.qbGeneric;
        for (String k : keyOf.values()) if (sameCount.get(k) >= min) g++;
        return g;
    }

    public boolean want(String lname) { return !requested.contains(lname); }

    public void request(String nick) {
        requested.add(nick.toLowerCase());
        queue.offer(new Task(nick, 0, seq.incrementAndGet()));
    }

    public void addCentral(String nick) { queue.offer(new Task(nick, 1, seq.incrementAndGet())); }

    public boolean remove(String nick) {
        String l = nick.toLowerCase();
        boolean r = centrals.remove(l) != null;
        names.remove(l);
        rebuild();
        save();
        return r;
    }

    public List<String> nicks() {
        List<String> l = new ArrayList<>();
        for (String k : centrals.keySet()) if (keyOf.containsKey(k)) l.add(names.get(k));
        return l;
    }

    /** Enfileira (em segundo plano) os nicks que ainda nao tem layout salvo. */
    public int learn(Collection<String> nicks) {
        stopLearn();
        int n = 0;
        for (String nick : nicks) {
            if (centrals.containsKey(nick.toLowerCase())) continue;
            queue.offer(new Task(nick, 2, seq.incrementAndGet()));
            n++;
        }
        learnTotal = n; learnDone = 0; learnSaved = 0; learning = n > 0;
        return n;
    }

    public void stopLearn() {
        for (Iterator<Task> it = queue.iterator(); it.hasNext(); ) if (it.next().kind == 2) it.remove();
        learning = false;
    }

    public void resetSession() {
        layouts.clear();
        matches.clear();
        requested.clear();
        for (Iterator<Task> it = queue.iterator(); it.hasNext(); ) if (it.next().kind == 0) it.remove();
    }

    /** Mostra no chat as chaves da resposta da API (para conferir o formato). */
    public void debug(final String nick) {
        new Thread(new Runnable() {
            public void run() {
                final JsonElement root = fetchRaw(nick);
                Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                    public void run() {
                        SmkzOverlay o = SmkzOverlay.I;
                        if (root == null) { o.msg("\u00a7cSem resposta da rota de quickbuy (confira qbUrl no config.json)."); return; }
                        o.msg("\u00a77Chaves: \u00a7f" + Json.keys(root, 30));
                        JsonElement q = Json.find(root, "quick_buy_layout", false);
                        o.msg(q == null ? "\u00a7cCampo quick_buy_layout n\u00e3o encontrado." : "\u00a7aquick_buy_layout: \u00a7f" + (q.isJsonObject() ? Json.keys(q, 30) : q.toString()));
                        o.msg("\u00a77Itens lidos: \u00a7f" + parseLayout(root).size());
                    }
                });
            }
        }, "SmkzOverlay-QB-debug").start();
    }

    // ---------------- rede (thread propria) ----------------
    private void loop() {
        while (true) {
            try {
                final Task t = queue.take();
                final List<String> items = fetch(t.nick);   // null = erro (nao grava)
                Minecraft.getMinecraft().addScheduledTask(new Runnable() { public void run() { done(t, items); } });
                Config c = SmkzOverlay.I.cfg;
                Thread.sleep(t.kind == 2 ? c.qbLearnDelayMs : c.qbDelayMs);
            } catch (InterruptedException e) {
                return;
            } catch (Throwable ignored) {}
        }
    }

    private JsonElement fetchRaw(String nick) {
        for (int attempt = 0; attempt < 3; attempt++) {
            HttpURLConnection c = null;
            try {
                String url = String.format(SmkzOverlay.I.cfg.qbUrl, URLEncoder.encode(nick, "UTF-8"));
                c = (HttpURLConnection) new URL(url).openConnection();
                c.setConnectTimeout(5000);
                c.setReadTimeout(5000);
                c.setRequestProperty("User-Agent", "SmkzOverlay/1.0");
                c.setRequestProperty("Accept", "application/json");
                int code = c.getResponseCode();
                if (code == 429) { Thread.sleep(2000L * (attempt + 1)); continue; }
                if (code == 404) return new JsonObject();   // jogador sem dados
                if (code != 200) continue;
                InputStream in = c.getInputStream();
                ByteArrayOutputStream b = new ByteArrayOutputStream();
                byte[] buf = new byte[4096];
                int n;
                while ((n = in.read(buf)) > 0) b.write(buf, 0, n);
                in.close();
                return new JsonParser().parse(new String(b.toByteArray(), "UTF-8"));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            } catch (Exception e) {
                // tenta de novo
            } finally {
                if (c != null) c.disconnect();
            }
        }
        return null;
    }

    private List<String> fetch(String nick) {
        JsonElement root = fetchRaw(nick);
        return root == null ? null : parseLayout(root);
    }

    /** quick_buy_layout.items (lista de ids de item, na ordem dos slots). */
    static List<String> parseLayout(JsonElement root) {
        List<String> out = new ArrayList<>();
        JsonElement q = Json.find(root, "quick_buy_layout", false);
        if (q == null) return out;
        JsonElement arr = q.isJsonObject() ? Json.find(q, "items", false) : q;
        if (arr == null || !arr.isJsonArray()) return out;
        for (JsonElement e : arr.getAsJsonArray()) {
            String s = "";
            if (e.isJsonPrimitive()) s = e.getAsString();
            else if (e.isJsonObject()) {
                for (String k : new String[]{"id", "item", "name", "type"}) {
                    JsonElement v = e.getAsJsonObject().get(k);
                    if (v != null && v.isJsonPrimitive()) { s = v.getAsString(); break; }
                }
            }
            out.add(s.trim().toLowerCase());
        }
        return out;
    }

    // ---------------- resultado (thread principal) ----------------
    private void done(Task t, List<String> items) {
        SmkzOverlay o = SmkzOverlay.I;
        String l = t.nick.toLowerCase();
        if (t.kind == 0) {
            if (items == null) return;   // falhou: nao tenta de novo nesta partida
            layouts.put(l, items);
            check(l, t.nick, items);
        } else if (t.kind == 1) {
            if (items == null) { o.msg("\u00a7cN\u00e3o consegui buscar a quickbuy de \u00a7f" + t.nick + "\u00a7c."); return; }
            if (nonEmpty(items) < MIN_ITEMS) { o.msg("\u00a7e" + t.nick + " \u00a77n\u00e3o tem quickbuy personalizada na API (nada salvo)."); return; }
            put(t.nick, items);
            rebuild();
            save();
            o.msg("\u00a7aQuickbuy de \u00a7f" + t.nick + " \u00a7asalva (" + nonEmpty(items) + " itens).");
            rematchAll();
        } else {
            learnDone++;
            if (items != null) {
                put(t.nick, items);   // vazio tambem fica salvo, para nao tentar de novo
                if (nonEmpty(items) >= MIN_ITEMS) learnSaved++;
            }
            boolean end = learnDone >= learnTotal;
            if (learnDone % 100 == 0 || end) {
                rebuild();
                save();
                rematchAll();
                o.msg("\u00a77Quickbuy: aprendendo snipers... \u00a7f" + learnDone + "/" + learnTotal + " \u00a77(" + learnSaved + " layouts salvos)");
            }
            if (end) { learning = false; o.msg("\u00a7aAprendizado de quickbuy conclu\u00eddo: \u00a7f" + learnSaved + " \u00a7alayouts de snipers."); }
        }
    }

    private void check(String l, String nick, List<String> items) {
        Match m = bestMatch(l, items);
        if (m == null) { matches.remove(l); return; }
        matches.put(l, m);
        if (m.pct >= SmkzOverlay.I.cfg.qbMin) SmkzOverlay.I.qbAlert(l, nick, m);
    }

    private void rematchAll() {
        for (Map.Entry<String, List<String>> en : new ArrayList<>(layouts.entrySet())) {
            String nick = SmkzOverlay.I.present.containsKey(en.getKey()) ? SmkzOverlay.I.present.get(en.getKey()) : en.getKey();
            check(en.getKey(), nick, en.getValue());
        }
    }
}
