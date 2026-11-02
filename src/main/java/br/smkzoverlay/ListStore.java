package br.smkzoverlay;

import com.google.gson.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/** Blacklist (snipers conhecidos) e lista de interesse, salvas em JSON local. */
public class ListStore {
    public static class Entry { public String name, reason = "", date = ""; }
    private static class Data { Map<String, Entry> black = new LinkedHashMap<>(), watch = new LinkedHashMap<>(), ignore = new LinkedHashMap<>(); int imported; }

    public Map<String, Entry> black = new LinkedHashMap<>(), watch = new LinkedHashMap<>(), ignore = new LinkedHashMap<>();
    public int importedVer;   // versao da lista inicial ja importada
    private final File f;

    public ListStore(File f) {
        this.f = f;
        try {
            if (f.exists()) {
                Reader r = new InputStreamReader(new FileInputStream(f), "UTF-8");
                Data d = Config.G.fromJson(r, Data.class);
                r.close();
                if (d != null) { if (d.black != null) black = d.black; if (d.watch != null) watch = d.watch; if (d.ignore != null) ignore = d.ignore; importedVer = d.imported; }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private Map<String, Entry> map(boolean bl) { return bl ? black : watch; }
    public boolean has(boolean bl, String lname) { return map(bl).containsKey(lname.toLowerCase()); }

    public void add(boolean bl, String name, String reason) {
        Entry e = new Entry();
        e.name = name; e.reason = reason == null ? "" : reason;
        e.date = new SimpleDateFormat("dd/MM/yyyy").format(new Date());
        map(bl).put(name.toLowerCase(), e);
        save();
    }

    public boolean remove(boolean bl, String name) {
        boolean r = map(bl).remove(name.toLowerCase()) != null;
        save();
        return r;
    }

    public boolean toggle(boolean bl, String name) {
        if (has(bl, name)) { remove(bl, name); return false; }
        add(bl, name, "via menu");
        return true;
    }

    /** Nicks do snipers.txt embutido no mod (um por linha ou separados por virgula). */
    public static List<String> readBundled() {
        List<String> out = new ArrayList<>();
        try {
            InputStream in = ListStore.class.getResourceAsStream("/snipers.txt");
            if (in == null) return out;
            BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            String line;
            while ((line = br.readLine()) != null)
                for (String p : line.split("[,\\s]+")) if (p.matches("[A-Za-z0-9_]{1,16}")) out.add(p);
            br.close();
        } catch (Exception e) { e.printStackTrace(); }
        return out;
    }

    /** Adiciona nicks a blacklist sem repetir; devolve quantos eram novos. */
    public int importBlack(Collection<String> nicks, String reason) {
        int added = 0;
        String date = new SimpleDateFormat("dd/MM/yyyy").format(new Date());
        for (String n : nicks) {
            String l = n.toLowerCase();
            if (black.containsKey(l)) continue;
            Entry e = new Entry();
            e.name = n; e.reason = reason; e.date = date;
            black.put(l, e);
            added++;
        }
        save();
        return added;
    }

    public void markImported(int v) { importedVer = v; save(); }

    public boolean isIgnored(String name) { return ignore.containsKey(name.toLowerCase()); }

    public void addIgnore(String name) {
        Entry e = new Entry();
        e.name = name;
        e.date = new SimpleDateFormat("dd/MM/yyyy").format(new Date());
        ignore.put(name.toLowerCase(), e);
        save();
    }

    public boolean removeIgnore(String name) {
        boolean r = ignore.remove(name.toLowerCase()) != null;
        save();
        return r;
    }

    private void save() {
        try {
            Data d = new Data(); d.black = black; d.watch = watch; d.ignore = ignore; d.imported = importedVer;
            Writer w = new OutputStreamWriter(new FileOutputStream(f), "UTF-8");
            Config.G.toJson(d, w);
            w.close();
        } catch (Exception e) { e.printStackTrace(); }
    }
}
