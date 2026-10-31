package br.smkzoverlay;

import com.google.gson.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/** Blacklist (snipers conhecidos) e lista de interesse, salvas em JSON local. */
public class ListStore {
    public static class Entry { public String name, reason = "", date = ""; }
    private static class Data { Map<String, Entry> black = new LinkedHashMap<>(), watch = new LinkedHashMap<>(), ignore = new LinkedHashMap<>(); }

    public Map<String, Entry> black = new LinkedHashMap<>(), watch = new LinkedHashMap<>(), ignore = new LinkedHashMap<>();
    private final File f;

    public ListStore(File f) {
        this.f = f;
        try {
            if (f.exists()) {
                Reader r = new InputStreamReader(new FileInputStream(f), "UTF-8");
                Data d = Config.G.fromJson(r, Data.class);
                r.close();
                if (d != null) { if (d.black != null) black = d.black; if (d.watch != null) watch = d.watch; if (d.ignore != null) ignore = d.ignore; }
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
            Data d = new Data(); d.black = black; d.watch = watch; d.ignore = ignore;
            Writer w = new OutputStreamWriter(new FileOutputStream(f), "UTF-8");
            Config.G.toJson(d, w);
            w.close();
        } catch (Exception e) { e.printStackTrace(); }
    }
}
