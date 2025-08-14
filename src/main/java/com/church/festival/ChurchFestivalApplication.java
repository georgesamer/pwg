package com.church.festival;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.core.env.Environment;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Objects;

/**
 * Main application class for Church Festival Art Platform
 */
@SpringBootApplication
@EnableConfigurationProperties
public class ChurchFestivalApplication {

    private static final Logger log = LoggerFactory.getLogger(ChurchFestivalApplication.class);

    public static void main(String[] args) {
        // INFO: Log bootstrap start as early as possible
        log.info("Application bootstrap starting...");

        // Register early lifecycle listeners to capture events before context creation
        SpringApplication app = new SpringApplication(ChurchFestivalApplication.class);
        app.addListeners(new StartupLifecycleLogger.EarlyStartupListener());

        // Run the application
        app.run(args);
    }

    /**
     * Listens for the ApplicationReadyEvent and performs detailed step-by-step logging,
     * including reading raw configuration values, normalizing the context path, constructing
     * the final URL, and attempting to open the default browser.
     */
    @Component
    static class AppReadyListener implements ApplicationListener<ApplicationReadyEvent> {
        private static final Logger log = LoggerFactory.getLogger(AppReadyListener.class);

        private final Environment env;

        AppReadyListener(Environment env) {
            this.env = env;
        }

        @Override
        public void onApplicationEvent(ApplicationReadyEvent event) {
            log.info("[Startup] onApplicationReady START");
            try {
                // 1. Read raw values from the environment
                String rawLocalPort = getProp("local.server.port");
                String rawServerPort = getProp("server.port");
                String rawServerAddr = getProp("server.address");
                String rawCtxPath   = getProp("server.servlet.context-path");

                // 2. Log raw values
                log.info("[Startup] Raw property values:");
                log.info("  local.server.port='{}'", rawLocalPort);
                log.info("  server.port='{}'", rawServerPort);
                log.info("  server.address='{}'", rawServerAddr);
                log.info("  server.servlet.context-path='{}'", rawCtxPath);

                // Resolve effective port and host
                int port = resolvePort(rawLocalPort, rawServerPort);
                log.info("[Startup] Effective port resolved to {} (priority: local.server.port -> server.port -> 8080)", port);

                String host = resolveHost(rawServerAddr);
                log.info("[Startup] Effective host resolved to '{}' (0.0.0.0/:: mapped to 'localhost' for browser use)", host);

                // 3. Normalize the context path
                String contextPath = normalizeContextPath(rawCtxPath);
                log.info("[Startup] Normalized context path='{}'", contextPath);

                // 4. Construct the final URL
                String url = String.format("http://%s:%d%s", host, port, contextPath);
                log.info("[Startup] Final application URL constructed: {}", url);

                // 5-6. Attempt to open the browser
                openInBrowser(url);
            } catch (Exception ex) {
                // 7. Log any errors with full stack trace
                log.error("[Startup] Error during onApplicationReady processing", ex);
            } finally {
                log.info("[Startup] onApplicationReady END");
            }
        }

        private String getProp(String key) {
            try {
                String value = env.getProperty(key);
                return value;
            } catch (Exception e) {
                log.warn("[Startup] Failed to read property '{}': {}", key, e.toString());
                return null;
            }
        }

        private int resolvePort(String rawLocalPort, String rawServerPort) {
            // Try local.server.port first (set by embedded server once bound)
            Integer port = parsePortOrNull(rawLocalPort);
            if (port != null) {
                log.info("[Startup] Using port from local.server.port -> {}", port);
                return port;
            }
            // Fallback to server.port (configured desired port)
            port = parsePortOrNull(rawServerPort);
            if (port != null) {
                log.info("[Startup] Using port from server.port -> {}", port);
                return port;
            }
            // Default Spring Boot port
            log.info("[Startup] Falling back to default port 8080");
            return 8080;
        }

        private Integer parsePortOrNull(String v) {
            if (v == null || v.isBlank()) return null;
            try {
                return Integer.parseInt(v.trim());
            } catch (NumberFormatException nfe) {
                log.warn("[Startup] Port value is not a valid integer: '{}'", v);
                return null;
            }
        }

        private String resolveHost(String rawServerAddr) {
            String host = (rawServerAddr == null || rawServerAddr.isBlank()) ? "localhost" : rawServerAddr.trim();
            if (Objects.equals(host, "0.0.0.0") || Objects.equals(host, "::") || Objects.equals(host, "[::]")) {
                log.info("[Startup] Server bound to '{}', mapping to 'localhost' for browser URL", host);
                host = "localhost";
            }
            return host;
        }

        private String normalizeContextPath(String cp) {
            if (cp == null) {
                // Spring default context path is empty
                return "";
            }
            String v = cp.trim();
            if (v.isEmpty() || "/".equals(v)) {
                return ""; // represent root as empty for URL building
            }
            // Ensure it starts with a single '/'
            if (!v.startsWith("/")) {
                v = "/" + v;
            }
            // Remove trailing '/' unless it is root
            if (v.endsWith("/") && v.length() > 1) {
                v = v.substring(0, v.length() - 1);
            }
            return v;
        }

        private void openInBrowser(String url) {
            // Decide platform-specific strategy and log each step
            try {
                // Try Desktop API first
                if (Desktop.isDesktopSupported()) {
                    Desktop desktop = Desktop.getDesktop();
                    if (desktop.isSupported(Desktop.Action.BROWSE)) {
                        log.info("[Startup] Starting browser open process via method: Desktop.browse");
                        try {
                            desktop.browse(toUri(url));
                            log.info("[Startup] Browser open command executed (Desktop.browse)");
                            return;
                        } catch (Exception ex) {
                            log.error("[Startup] Desktop.browse failed", ex);
                            // fall through to OS-specific methods
                        }
                    } else {
                        log.info("[Startup] Desktop supported but BROWSE action not available");
                    }
                } else {
                    log.info("[Startup] Desktop API not supported on this environment");
                }

                String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
                if (os.contains("win")) {
                    log.info("[Startup] Starting browser open process via method: cmd (Windows)");
                    // Use 'start' to delegate to default browser; empty title argument to handle URLs with '&'
                    Process process = new ProcessBuilder("cmd", "/c", "start", "", url).start();
                    log.info("[Startup] Browser open command executed (cmd). Process alive={}", process.isAlive());
                } else if (os.contains("mac")) {
                    log.info("[Startup] Starting browser open process via method: open (macOS)");
                    Process process = new ProcessBuilder("open", url).start();
                    log.info("[Startup] Browser open command executed (open). Process alive={}", process.isAlive());
                } else {
                    log.info("[Startup] Starting browser open process via method: xdg-open (Linux/Unix)");
                    Process process = new ProcessBuilder("xdg-open", url).start();
                    log.info("[Startup] Browser open command executed (xdg-open). Process alive={}", process.isAlive());
                }
            } catch (IOException io) {
                log.error("[Startup] I/O error while attempting to open browser", io);
            } catch (Exception e) {
                log.error("[Startup] Unexpected error while attempting to open browser", e);
            }
        }

        private URI toUri(String url) throws URISyntaxException {
            return new URI(url);
        }
    }
}
