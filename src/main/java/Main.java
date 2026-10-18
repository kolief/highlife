import java.util.Arrays;

public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("usage: java -jar local.jar patch|run ...");
            return;
        }
        String cmd = args[0];
        String[] rest = Arrays.copyOfRange(args, 1, args.length);
        if ("patch".equals(cmd)) {
            Patch.main(rest);
        } else if ("run".equals(cmd)) {
            Launch.main(rest);
        } else {
            System.out.println("unknown: " + cmd);
        }
    }
}
