package com.trieu.tripplanner.security.ratelimit;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.SequenceInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * A request whose body RateLimitFilter has already looked at, and that the controller can still read in full.
 * <p>
 * The body of a request can be read only once. This wrapper reads the start of it (at most {@code limit} bytes)
 * when it is created, then serves the controller those bytes followed by whatever was not read yet. A body longer
 * than the limit is therefore passed on untouched, it is only not offered to the filter: nobody can make the
 * filter hold a large body in memory.
 */
public class CachedBodyRequest extends HttpServletRequestWrapper {

    private final byte[] head;
    private final boolean complete;
    private ServletInputStream stream;
    private BufferedReader reader;

    /**
     * @param request the request as it came in, body not read yet
     * @param limit   the most bytes kept in memory
     * @throws IOException when the body cannot be read, for example the caller hung up
     */
    public CachedBodyRequest(HttpServletRequest request, int limit) throws IOException {
        super(request);
        // One byte more than the limit tells "exactly at the limit" from "longer than the limit"
        this.head = request.getInputStream().readNBytes(limit + 1);
        this.complete = head.length <= limit;
    }

    /** The whole body, or empty when it is longer than the limit. */
    public Optional<byte[]> body() {
        return complete ? Optional.of(head.clone()) : Optional.empty();
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        if (stream == null) {
            InputStream rest = complete ? InputStream.nullInputStream() : super.getInputStream();
            stream = new ReplayingInputStream(new SequenceInputStream(new ByteArrayInputStream(head), rest));
        }
        return stream;
    }

    @Override
    public BufferedReader getReader() throws IOException {
        if (reader == null) {
            Charset charset = getCharacterEncoding() == null
                    ? StandardCharsets.UTF_8
                    : Charset.forName(getCharacterEncoding());
            reader = new BufferedReader(new InputStreamReader(getInputStream(), charset));
        }
        return reader;
    }

    /** The bytes already read, then the rest of the original body, as the servlet API expects them. */
    private static final class ReplayingInputStream extends ServletInputStream {

        private final InputStream source;
        private boolean finished;

        ReplayingInputStream(InputStream source) {
            this.source = source;
        }

        @Override
        public int read() throws IOException {
            int next = source.read();
            finished = next == -1;
            return next;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            int count = source.read(buffer, offset, length);
            finished = count == -1;
            return count;
        }

        @Override
        public boolean isFinished() {
            return finished;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener listener) {
            // Controllers here read the body blocking; non-blocking reads are not offered
            throw new UnsupportedOperationException("Non-blocking reads are not supported");
        }

    }

}
