package com.trieu.tripplanner.support;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

/**
 * A stand-in for an outside service that listens on a real socket of this machine, for the failures a mocked
 * HTTP layer cannot show: a service that accepts the call and never answers, and a service that is not there.
 * Built on the HTTP server of the JDK, so no test dependency is added and no test leaves the machine
 * (CLAUDE.md rule 24). What a service answers is tested with MockRestServiceServer instead.
 */
public final class StubHttpServer implements AutoCloseable {

    private final HttpServer server;
    private final CountDownLatch closed = new CountDownLatch(1);

    private StubHttpServer() {
        try {
            server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        }
        catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        // The call is accepted and then kept waiting until the test is over
        server.createContext("/", exchange -> {
            try {
                closed.await();
            }
            catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            finally {
                exchange.close();
            }
        });
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();
    }

    /** A service that takes every call and never answers it. Close it at the end of the test. */
    public static StubHttpServer neverAnswering() {
        return new StubHttpServer();
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
