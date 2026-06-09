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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;

public class VbenAdminServer {
    private static final Set<String> HOP_BY_HOP_HEADERS = Set.of(
            "connection",
            "content-length",
            "host",
            "keep-alive",
            "proxy-authenticate",
            "proxy-authorization",
            "te",
            "trailers",
            "transfer-encoding",
            "upgrade"
    );

    private static final Path STATIC_ROOT = Path.of("dist").toAbsolutePath().normalize();
    private static final String BACKEND_BASE = trimTrailingSlash(
            System.getenv().getOrDefault("BACKEND_BASE", "http://ruoyi-vue-pro-server:48080")
    );
    private static final int HTTP_BACKLOG = Integer.parseInt(System.getenv().getOrDefault("HTTP_BACKLOG", "2048"));

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), HTTP_BACKLOG);
        server.createContext("/", VbenAdminServer::handle);
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
    }

    private static void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if ("/healthz".equals(path)) {
            send(exchange, 200, "text/plain; charset=utf-8", "no-store", "ok\n".getBytes());
            return;
        }
        if (shouldProxy(path)) {
            proxy(exchange);
            return;
        }
        serveStatic(exchange, path);
    }

    private static boolean shouldProxy(String path) {
        return path.startsWith("/admin-api/")
                || path.startsWith("/app-api/")
                || path.startsWith("/actuator/")
                || path.startsWith("/infra/")
                || path.startsWith("/druid/")
                || path.startsWith("/jmreport/")
                || path.startsWith("/drag/")
                || path.startsWith("/admin/")
                || path.startsWith("/webjars/")
                || path.startsWith("/v3/api-docs")
                || "/doc.html".equals(path)
                || path.startsWith("/swagger-ui");
    }

    private static void serveStatic(HttpExchange exchange, String path) throws IOException {
        Path file = resolveStaticFile(path);
        boolean index = STATIC_ROOT.resolve("index.html").equals(file);
        String cacheControl = index ? "no-store" : "public, max-age=31536000, immutable";
        sendFile(exchange, file, contentType(file), cacheControl);
    }

    private static Path resolveStaticFile(String path) {
        String relative = "/".equals(path) ? "index.html" : path.replaceFirst("^/+", "");
        Path candidate = STATIC_ROOT.resolve(relative).normalize();
        if (!candidate.startsWith(STATIC_ROOT) || !Files.isRegularFile(candidate)) {
            return STATIC_ROOT.resolve("index.html");
        }
        return candidate;
    }

    private static void proxy(HttpExchange exchange) throws IOException {
        URI requestUri = exchange.getRequestURI();
        String targetUrl = BACKEND_BASE + requestUri.getRawPath()
                + (requestUri.getRawQuery() == null ? "" : "?" + requestUri.getRawQuery());
        URL target = URI.create(targetUrl).toURL();
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
        for (Map.Entry<String, List<String>> entry : exchange.getRequestHeaders().entrySet()) {
            String name = entry.getKey();
            if (shouldSkipHeader(name)) {
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
            if (shouldSkipHeader(name)) {
                continue;
            }
            headers.put(name, entry.getValue());
        }
    }

    private static boolean shouldSkipHeader(String name) {
        return name == null || HOP_BY_HOP_HEADERS.contains(name.toLowerCase(Locale.ROOT));
    }

    private static boolean hasRequestBody(String method) {
        return !("GET".equalsIgnoreCase(method)
                || "HEAD".equalsIgnoreCase(method)
                || "OPTIONS".equalsIgnoreCase(method));
    }

    private static InputStream responseStream(HttpURLConnection connection) throws IOException {
        InputStream error = connection.getErrorStream();
        if (error != null) {
            return error;
        }
        return connection.getInputStream();
    }

    private static void send(HttpExchange exchange, int status, String contentType, String cacheControl, byte[] body)
            throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Cache-Control", cacheControl);
        if (isHead(exchange)) {
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
            return;
        }
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    private static void sendFile(HttpExchange exchange, Path file, String contentType, String cacheControl)
            throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Cache-Control", cacheControl);
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        long length = Files.size(file);
        if (isHead(exchange)) {
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
            return;
        }
        exchange.sendResponseHeaders(200, length);
        try (InputStream in = Files.newInputStream(file);
             OutputStream out = exchange.getResponseBody()) {
            in.transferTo(out);
        }
    }

    private static String contentType(Path file) throws IOException {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".js") || name.endsWith(".mjs")) {
            return "text/javascript; charset=utf-8";
        }
        if (name.endsWith(".css")) {
            return "text/css; charset=utf-8";
        }
        if (name.endsWith(".svg")) {
            return "image/svg+xml";
        }
        if (name.endsWith(".json")) {
            return "application/json; charset=utf-8";
        }
        if (name.endsWith(".woff2")) {
            return "font/woff2";
        }
        String probed = Files.probeContentType(file);
        if (probed != null) {
            return probed;
        }
        return "application/octet-stream";
    }

    private static boolean isHead(HttpExchange exchange) {
        return "HEAD".equalsIgnoreCase(exchange.getRequestMethod());
    }

    private static String trimTrailingSlash(String value) {
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }
}
