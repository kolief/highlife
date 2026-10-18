import java.io.IOException;
import java.net.*;
import java.util.*;

public final class Block extends ProxySelector {
    private static final Set<String> HOSTS = Set.of(
        "api.highlife.software",
        "104.21.28.134",
        "172.67.170.212"
    );

    private static ProxySelector def;
    private static boolean installed;

    private Block() {
    }

    public static void install() {
        if (installed) return;
        def = ProxySelector.getDefault();
        ProxySelector.setDefault(new Block());
        installed = true;
    }

    @Override
    public List<Proxy> select(URI uri) {
        String host = uri.getHost();
        if (host != null && blocked(host)) {
            return List.of(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 1)));
        }
        return def == null ? List.of(Proxy.NO_PROXY) : def.select(uri);
    }

    @Override
    public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
        if (def != null) def.connectFailed(uri, sa, ioe);
    }

    private static boolean blocked(String host) {
        String h = host.toLowerCase(Locale.ROOT);
        return HOSTS.contains(h) || h.endsWith(".highlife.software");
    }
}
