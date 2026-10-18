import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

final class Version {
    final String name;
    final String lease;
    final String leaseMethod;
    final String leaseDesc;
    final String result;
    final String store;
    final String catalog;
    final String open;

    private Version(String[] p) {
        name = p[1];
        lease = p[2];
        leaseMethod = p[3];
        leaseDesc = p[4];
        result = p[5];
        store = p[6];
        catalog = p[7];
        open = p[8];
    }

    static Version find(Path jar, String map) throws Exception {
        String hash = hash(jar);
        InputStream in = map == null
            ? Version.class.getResourceAsStream("/versions.tsv")
            : Files.newInputStream(Path.of(map));
        if (in == null) throw new IllegalStateException("versions.tsv missing");

        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("hash\t")) continue;
                String[] p = line.split("\t", -1);
                if (p.length >= 9 && p[0].equalsIgnoreCase(hash)) {
                    System.out.println("version=" + p[1] + " hash=" + hash);
                    return new Version(p);
                }
            }
        }
        throw new IllegalStateException("unsupported client " + hash);
    }

    static String hash(Path jar) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (InputStream in = Files.newInputStream(jar)) {
            byte[] b = new byte[8192];
            int n;
            while ((n = in.read(b)) > 0) md.update(b, 0, n);
        }
        StringBuilder s = new StringBuilder();
        for (byte b : md.digest()) s.append(String.format("%02X", b));
        return s.toString();
    }
}
