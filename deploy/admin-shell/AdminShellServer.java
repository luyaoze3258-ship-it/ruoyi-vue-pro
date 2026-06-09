import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;

public class AdminShellServer {
    private static final Set<String> HOP_BY_HOP_HEADERS = Set.of(
            "connection",
            "keep-alive",
            "proxy-authenticate",
            "proxy-authorization",
            "te",
            "trailers",
            "transfer-encoding",
            "upgrade"
    );

    private static final byte[] INDEX_HTML = loadIndex();
    private static final String BACKEND_BASE = trimTrailingSlash(
            System.getenv().getOrDefault("BACKEND_BASE", "http://ruoyi-vue-pro-server:48080")
    );

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", AdminShellServer::handle);
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
    }

    private static void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if ("/healthz".equals(path)) {
            send(exchange, 200, "text/plain; charset=utf-8", "ok\n".getBytes(StandardCharsets.UTF_8));
            return;
        }
        if (path.startsWith("/admin-api/") || path.startsWith("/app-api/") || path.startsWith("/actuator/")) {
            proxy(exchange);
            return;
        }
        send(exchange, 200, "text/html; charset=utf-8", INDEX_HTML);
    }

    private static void proxy(HttpExchange exchange) throws IOException {
        URI requestUri = exchange.getRequestURI();
        URL target = URI.create(BACKEND_BASE + requestUri).toURL();
        HttpURLConnection connection = (HttpURLConnection) target.openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(30000);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestMethod(exchange.getRequestMethod());

        copyRequestHeaders(exchange, connection);

        if (hasRequestBody(exchange.getRequestMethod())) {
            connection.setDoOutput(true);
            try (OutputStream out = connection.getOutputStream()) {
                exchange.getRequestBody().transferTo(out);
            }
        }

        int status = connection.getResponseCode();
        copyResponseHeaders(connection, exchange.getResponseHeaders());
        try (InputStream in = responseStream(connection);
             OutputStream out = exchange.getResponseBody()) {
            exchange.sendResponseHeaders(status, 0);
            if (in != null) {
                in.transferTo(out);
            }
        } finally {
            connection.disconnect();
        }
    }

    private static void copyRequestHeaders(HttpExchange exchange, HttpURLConnection connection) {
        Headers headers = exchange.getRequestHeaders();
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            String name = entry.getKey();
            if (name == null || HOP_BY_HOP_HEADERS.contains(name.toLowerCase())) {
                continue;
            }
            for (String value : entry.getValue()) {
                connection.addRequestProperty(name, value);
            }
        }
        connection.setRequestProperty("X-Forwarded-Proto", "https");
    }

    private static void copyResponseHeaders(HttpURLConnection connection, Headers headers) {
        for (Map.Entry<String, List<String>> entry : connection.getHeaderFields().entrySet()) {
            String name = entry.getKey();
            if (name == null || HOP_BY_HOP_HEADERS.contains(name.toLowerCase())) {
                continue;
            }
            headers.put(name, entry.getValue());
        }
    }

    private static boolean hasRequestBody(String method) {
        return !("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method) || "OPTIONS".equalsIgnoreCase(method));
    }

    private static InputStream responseStream(HttpURLConnection connection) throws IOException {
        InputStream error = connection.getErrorStream();
        if (error != null) {
            return error;
        }
        return connection.getInputStream();
    }

    private static void send(HttpExchange exchange, int status, String contentType, byte[] body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    private static byte[] loadIndex() {
        try {
            return Files.readAllBytes(Path.of("index.html"));
        } catch (IOException e) {
            throw new IllegalStateException("index.html is required", e);
        }
    }

    private static String trimTrailingSlash(String value) {
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }
}
