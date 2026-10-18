import java.io.*;
import java.nio.file.*;
import java.lang.reflect.*;
import java.util.*;

public final class Local {
    private static Path dir() {
        String s = System.getProperty("hl.localscripts", "scripts");
        return Paths.get(s);
    }

    public static byte[] open(String id) {
        try {
            Path p = dir().resolve(id + ".jar");
            if (Files.isRegularFile(p)) {
                System.out.println("[Local] loading " + p);
                return Files.readAllBytes(p);
            }
            throw new IllegalStateException("No local script: " + p);
        } catch (IOException e) {
            throw new IllegalStateException("Local script read failed: " + id, e);
        }
    }

    public static List<Object> catalog() {
        List<Object> out = new ArrayList<>();
        try {
            Path d = dir();
            if (!Files.isDirectory(d)) return out;
            Class<?> entryClass = Class.forName("HlStore$Eintrag");
            Constructor<?> ctor = entryClass.getDeclaredConstructor();
            ctor.setAccessible(true);

            Class<?> storeClass = Class.forName("HlStore");
            Field catalogField = storeClass.getDeclaredField("KATALOG");
            catalogField.setAccessible(true);
            Map<Object, Object> map = (Map<Object, Object>) catalogField.get(null);

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(d, "*.jar")) {
                for (Path p : stream) {
                    String id = p.getFileName().toString();
                    if (id.endsWith(".jar")) id = id.substring(0, id.length() - 4);
                    Object e = ctor.newInstance();
                    set(e, "kennung", id);
                    set(e, "name", id);
                    set(e, "autor", "local");
                    set(e, "beschreibung", "Local script");
                    set(e, "version", "1.0");
                    set(e, "kategorie", "local");
                    set(e, "premium", false);
                    out.add(e);
                    map.put(id, e);
                    System.out.println("[Local] catalog " + id);
                }
            }
        } catch (Throwable t) {
            System.out.println("[Local] catalog failed: " + t);
        }
        return out;
    }

    private static void set(Object o, String name, Object value) throws Exception {
        Field f = o.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(o, value);
    }
}
