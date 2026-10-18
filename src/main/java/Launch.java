import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Launch {
    public static void main(String[] args) throws Exception {
        Path patchedJar = Path.of(args.length > 0 ? args[0] : "client.jar").toAbsolutePath();
        Path scripts = Path.of(args.length > 1 ? args[1]
            : System.getProperty("scripts", "scripts")).toAbsolutePath();
        Path argsFile = Path.of(args.length > 2 ? args[2]
            : System.getProperty("args", "args.txt")).toAbsolutePath();
        if (!Files.isRegularFile(argsFile)) {
            throw new IllegalStateException("args file not found: " + argsFile);
        }

        List<String> src = Files.readAllLines(argsFile);
        List<String> out = new ArrayList<>();
        String original = null;
        for (String line : src) {
            String s = line.trim();
            if (s.startsWith("\"") && s.endsWith("\"") && s.length() > 1) {
                s = s.substring(1, s.length() - 1);
            }
            if (s.startsWith("-Dhl.src=")) {
                original = s.substring("-Dhl.src=".length());
                s = "-Dhl.src=" + patchedJar;
            }
            if (s.startsWith("-Dhl.localscripts=")) continue;
            out.add(s);
        }
        if (original == null) throw new IllegalStateException("no -Dhl.src in args.txt");
        for (int i = 0; i < out.size(); i++) {
            out.set(i, out.get(i).replace(original, patchedJar.toString()));
        }
        int main = out.indexOf("HlBoot");
        if (main < 0) throw new IllegalStateException("HlBoot not found");
        out.add(main, "-Dhl.localscripts=" + scripts);
        out.add(main, "-Dhl.local=1");

        String java = Path.of(System.getProperty("java.home"), "bin", "java.exe").toString();
        List<String> cmd = new ArrayList<>();
        cmd.add(java);
        cmd.addAll(out);
        System.out.println("ARGS=" + argsFile);
        System.out.println("JAVA=" + java);
        System.out.println("PATCHED=" + patchedJar);
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(argsFile.getParent().toFile());
        pb.inheritIO();
        Process p = pb.start();
        System.out.println("PID=" + p.pid());
    }

}
