package com.church.festival;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartingEvent;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.awt.Desktop;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Logs the application startup lifecycle, captures the effective web server port,
 * builds the final URL, and opens the browser with detailed diagnostics.
 */
@Component
public class StartupLifecycleLogger {

    private static final Logger log = LoggerFactory.getLogger(StartupLifecycleLogger.class);

    // Hold the effective port from WebServerInitializedEvent (thread-safe)
    private static final AtomicInteger ACTUAL_PORT = new AtomicInteger(-1);

    // Flag to indicate startup failure (to clearly log and suppress any browser attempts)
    private static volatile boolean FAILED_STARTUP = false;

    private final Environment environment;

    public StartupLifecycleLogger(Environment environment) {
        this.environment = environment;
    }

    // INFO: Capture the actual port even if server.port=0 (Tomcat random port assignment)
    @EventListener
    public void onWebServerInitialized(WebServerInitializedEvent event) {
        int port = event.getWebServer().getPort();
        ACTUAL_PORT.set(port);
        log.info("Web server initialized. Effective port: {}", port);
        if (log.isDebugEnabled()) {
            log.debug("WebServer class: {}", event.getWebServer().getClass().getName());
        }
    }

    // INFO: Build the final URL and try to open in browser with detailed DEBUG logs
    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        if (FAILED_STARTUP) {
            // Safety guard; shouldn't normally happen because Ready doesn't fire on failure.
            log.info("Application ready event received but startup was flagged as failed; skipping browser open.");
            return;
        }

        // DEBUG: Compute scheme, host, port, and context-path step by step
        boolean sslEnabled = Boolean.parseBoolean(environment.getProperty("server.ssl.enabled", "false"));
        String scheme = sslEnabled ? "https" : "http";

        String rawHost = environment.getProperty("server.address", "localhost");
        String host = normalizeHost(rawHost);

        int port = ACTUAL_PORT.get();
        if (port <= 0) {
            // Fallback to environment if web server event wasn't seen yet (should be rare)
            String localPort = environment.getProperty("local.server.port");
            if (localPort != null) {
                try {
                    port = Integer.parseInt(localPort.trim());
                } catch (NumberFormatException ignore) {
                    // ignore
                }
            }
            if (port <= 0) {
                String configuredPort = environment.getProperty("server.port", "8080");
                try {
                    port = Integer.parseInt(configuredPort.trim());
                } catch (NumberFormatException e) {
                    port = 8080; // final fallback
                }
            }
            log.debug("Effective port resolved via fallback: {}", port);
        }

        String rawContextPath = environment.getProperty("server.servlet.context-path", "");
        String contextPath = normalizeContextPath(rawContextPath);

        if (log.isDebugEnabled()) {
            log.debug("SSL enabled: {}", sslEnabled);
            log.debug("Detected host (raw={}): {}", rawHost, host);
            log.debug("Effective port: {}", port);
            log.debug("Context path (raw={}): {}", rawContextPath, contextPath);
        }

        String url = scheme + "://" + host + ":" + port + contextPath + "/home";
        log.info("Application ready. Final URL: {}", url);
        log.debug("Opening browser with URL: {}", url);

        // INFO: Attempt to open the browser asynchronously
        launchInBrowserAsync(url);
    }

    // ERROR: Log root cause on application failure and mark startup as failed
    @EventListener
    public void onApplicationFailed(ApplicationFailedEvent event) {
        FAILED_STARTUP = true;
        Throwable ex = event.getException();
        log.error("Application failed to start.", ex);
    }

    // Normalize 0.0.0.0/:: to localhost for browser friendliness
    private static String normalizeHost(String host) {
        if (host == null || host.isBlank()) return "localhost";
        String h = host.trim();
        if ("0.0.0.0".equals(h) || "::".equals(h)) return "localhost";
        return h;
    }

    // Normalize context-path: ensure leading '/', remove trailing '/', drop "/" to empty
    private static String normalizeContextPath(String cp) {
        if (cp == null || cp.isBlank() || "/".equals(cp.trim())) {
            return "";
        }
        String out = cp.trim();
        if (!out.startsWith("/")) out = "/" + out;
        if (out.length() > 1 && out.endsWith("/")) {
            out = out.substring(0, out.length() - 1);
        }
        return out;
    }

    // INFO/DEBUG/ERROR: Open browser with detailed diagnostics and fallback commands
    private static void launchInBrowserAsync(String url) {
        // DEBUG: OS and Desktop support diagnostics
        String osName = System.getProperty("os.name", "unknown");
        boolean desktopSupported = Desktop.isDesktopSupported();
        boolean browseSupported = desktopSupported && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE);
        if (log.isDebugEnabled()) {
            log.debug("OS detected: {}", osName);
            log.debug("Desktop.isDesktopSupported(): {}", desktopSupported);
            log.debug("Desktop.Action.BROWSE supported: {}", browseSupported);
        }

        CompletableFuture
                .runAsync(() -> {
                    try {
                        if (browseSupported) {
                            // INFO: Attempt opening via Desktop API
                            log.info("Opening browser via Desktop#browse");
                            Desktop.getDesktop().browse(URI.create(url));
                        } else {
                            // INFO: Fallback to OS-specific command and log the command used
                            String os = osName.toLowerCase();
                            List<String> command;
                            if (os.contains("win")) {
                                command = Arrays.asList("cmd", "/c", "start", "", url);
                            } else if (os.contains("mac")) {
                                command = Arrays.asList("open", url);
                            } else {
                                command = Arrays.asList("xdg-open", url);
                            }
                            log.info("Opening browser via OS command: {}", String.join(" ", command));
                            new ProcessBuilder(command).start();
                        }
                    } catch (Exception e) {
                        // Ensure exception is propagated to whenComplete for ERROR logging
                        throw new RuntimeException("Failed to dispatch browser open command", e);
                    }
                })
                .whenComplete((ignored, throwable) -> {
                    if (throwable != null) {
                        log.error("Failed to open browser.", throwable);
                    } else {
                        log.info("Browser open command dispatched successfully.");
                    }
                });
    }

    /**
     * Early startup listener to capture events fired before the ApplicationContext exists.
     * Registered from main() via SpringApplication.addListeners(...).
     */
    public static class EarlyStartupListener implements ApplicationListener<ApplicationEvent> {

        private static final Logger earlyLog = LoggerFactory.getLogger(EarlyStartupListener.class);

        @Override
        public void onApplicationEvent(ApplicationEvent event) {
            // INFO: ApplicationStartingEvent - very early phase
            if (event instanceof ApplicationStartingEvent) {
                earlyLog.info("ApplicationStartingEvent received. Starting bootstrap...");
                return;
            }

            // INFO/DEBUG: ApplicationEnvironmentPreparedEvent - environment and profiles are ready
            if (event instanceof ApplicationEnvironmentPreparedEvent envEvent) {
                var env = envEvent.getEnvironment();
                String[] profiles = env.getActiveProfiles();
                String serverPort = env.getProperty("server.port", "8080");
                String serverAddress = env.getProperty("server.address", "localhost");
                String contextPath = env.getProperty("server.servlet.context-path", "");

                earlyLog.info("ApplicationEnvironmentPreparedEvent received. Active profiles: {}",
                        Arrays.toString(profiles));

                if (earlyLog.isDebugEnabled()) {
                    earlyLog.debug("server.port: {}", serverPort);
                    earlyLog.debug("server.address: {}", serverAddress);
                    earlyLog.debug("server.servlet.context-path: {}", contextPath);
                }
            }
        }
    }
}
