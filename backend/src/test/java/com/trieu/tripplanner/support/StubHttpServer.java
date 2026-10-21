package com.trieu.tripplanner.support;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.zip.GZIPOutputStream;

/**
 * A stand-in for an outside service that listens on a real socket of this machine, for the failures a mocked
 * HTTP layer cannot show: a service that accepts the call and never answers, and a service that is not there.
 * Built on the HTTP server of the JDK, so no test dependency is added and no test leaves the machine
 * (CLAUDE.md rule 24). What a service answers is tested with MockRestServiceServer instead.
 * <p>
 * {@link #scripted()} gives a server whose answers a test decides per path, one after the other, and which
 * counts the calls it got: for tests of the whole application, where the HTTP layer is the real one and what
 * matters is how many times a service was called.
 * <p>
 * {@link #compressingLikeOsrm(String)} gives a server that compresses its answer the way the public OSRM server
 * does, which only shows with the real HTTP client of the JDK: MockRestServiceServer skips the decompression.
 */
public final class StubHttpServer implements AutoCloseable {

    private final HttpServer server;
    private final CountDownLatch closed = new CountDownLatch(1);

    // Guarded by this
    private final Map<String, Deque<Answer>> scripts = new HashMap<>();
    private final Map<String, Integer> calls = new HashMap<>();

    /**
     * One answer of a scripted server.
     *
     * @param status HTTP status, or 0: take the call and never answer it
     * @param body   sent as JSON
     */
    public record Answer(int status, String body) {

        public static Answer ok(String body) {
            return new Answer(200, body);
        }

        public static Answer status(int status) {
            return new Answer(status, "{\"message\":\"scripted\"}");
        }

        public static Answer silence() {
            return new Answer(0, "");
        }
    }

    private StubHttpServer() {
        try {
            server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        }
        catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
    }

    private StubHttpServer start(HttpHandler handler) {
        server.createContext("/", handler);
        server.start();
        return this;
    }

    /** A service that takes every call and never answers it. Close it at the end of the test. */
    public static StubHttpServer neverAnswering() {
        StubHttpServer stub = new StubHttpServer();
        return stub.start(stub::keepWaiting);
    }

    /**
     * A service that answers what {@link #script} says and 404 to every other path. Close it at the end of
     * the tests.
     */
    public static StubHttpServer scripted() {
        StubHttpServer stub = new StubHttpServer();
        return stub.start(stub::answerFromScript);
    }

    /**
     * A service that answers every call with {@code json}, compressed the way the public OSRM server does
     * (seen on 2026-10-21): when the call sends Accept-Encoding on several lines, it reads only the last line,
     * names that encoding in Content-Encoding, and still sends gzip bytes. So "gzip" then "deflate" gets gzip
     * bytes said to be deflate. A call with a single "gzip" line gets a correct gzip answer. Close it at the end
     * of the test.
     */
    public static StubHttpServer compressingLikeOsrm(String json) {
        byte[] gzipped = gzip(json.getBytes(StandardCharsets.UTF_8));
        StubHttpServer stub = new StubHttpServer();
        return stub.start(exchange -> {
            List<String> lines = exchange.getRequestHeaders().getOrDefault("Accept-Encoding", List.of());
            String lastLine = lines.isEmpty() ? "gzip" : lines.getLast().split(",")[0].trim();
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.getResponseHeaders().add("Content-Encoding", lastLine);
            exchange.sendResponseHeaders(200, gzipped.length);
            exchange.getResponseBody().write(gzipped);
            exchange.close();
        });
    }

    private static byte[] gzip(byte[] content) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (GZIPOutputStream out = new GZIPOutputStream(bytes)) {
            out.write(content);
        }
        catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        return bytes.toByteArray();
    }

    private void answerFromScript(HttpExchange exchange) throws IOException {
        Answer answer = nextAnswer(exchange.getRequestURI().getPath());
        if (answer.status() == 0) {
            keepWaiting(exchange);
            return;
        }
        byte[] body = answer.body().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(answer.status(), body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    /**
     * What calls to paths starting with {@code pathPrefix} get from now on: the answers in the order given,
     * one per call; the last one is repeated for every call after that.
     */
    public synchronized void script(String pathPrefix, Answer... answers) {
        scripts.put(pathPrefix, new ArrayDeque<>(List.of(answers)));
    }

    /** How many calls to paths starting with {@code pathPrefix} arrived since the last {@link #reset()}. */
    public synchronized int calls(String pathPrefix) {
        return calls.getOrDefault(pathPrefix, 0);
    }

    /** Forgets every script and every count. */
    public synchronized void reset() {
        scripts.clear();
        calls.clear();
    }

    private synchronized Answer nextAnswer(String path) {
        for (Map.Entry<String, Deque<Answer>> script : scripts.entrySet()) {
            if (path.startsWith(script.getKey())) {
                calls.merge(script.getKey(), 1, Integer::sum);
                Deque<Answer> answers = script.getValue();
                return (answers.size() > 1) ? answers.poll() : answers.peek();
            }
        }
        return Answer.status(404);
    }

    /** The call is accepted and then kept waiting until the server is closed. */
    private void keepWaiting(HttpExchange exchange) {
        try {
            closed.await();
        }
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        finally {
            exchange.close();
        }
    }

    /** Where the service listens, for example {@code http://127.0.0.1:51234}. Free again after {@link #close()}. */
    public String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @Override
    public void close() {
        closed.countDown();
        server.stop(0);
    }

}
