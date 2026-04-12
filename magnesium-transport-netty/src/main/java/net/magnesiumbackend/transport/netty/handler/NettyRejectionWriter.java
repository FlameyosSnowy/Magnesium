package net.magnesiumbackend.transport.netty.handler;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import net.magnesiumbackend.core.backpressure.RejectionResponse;

import java.nio.charset.StandardCharsets;

/**
 * Writes a {@link RejectionResponse} synchronously to a Netty channel.
 *
 * <p>Called <em>on the Netty I/O thread</em> when {@link net.magnesiumbackend.core.backpressure.QueueRejectedError}
 * is thrown — before any worker thread is allocated. This keeps the rejection path
 * allocation-free beyond the response buffer itself.
 */
public final class NettyRejectionWriter {

    private NettyRejectionWriter() {}

    /**
     * Writes {@code rejection} to the channel and either keeps the connection alive
     * or closes it, matching the keep-alive semantics of the original request.
     *
     * @param ctx       the handler context whose channel receives the response
     * @param nettyReq  the original request (used for protocol version + keep-alive)
     * @param rejection the configured rejection response
     */
    public static void write(
        ChannelHandlerContext ctx,
        FullHttpRequest nettyReq,
        RejectionResponse rejection
    ) {
        byte[] bodyBytes = rejection.body() != null
            ? rejection.body().getBytes(StandardCharsets.UTF_8)
            : new byte[0];

        DefaultFullHttpResponse resp = new DefaultFullHttpResponse(
            nettyReq.protocolVersion(),
            HttpResponseStatus.valueOf(rejection.statusCode()),
            Unpooled.copiedBuffer(bodyBytes)
        );

        resp.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, bodyBytes.length);

        // Retry-After header (RFC 9110 §10.2.4)
        long retryAfterSeconds = rejection.retryAfterSeconds();
        if (retryAfterSeconds > 0) {
            resp.headers().set(HttpHeaderNames.RETRY_AFTER, String.valueOf(retryAfterSeconds));
        }

        if (HttpUtil.isKeepAlive(nettyReq)) {
            resp.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            ctx.writeAndFlush(resp);
        } else {
            ctx.writeAndFlush(resp).addListener(ChannelFutureListener.CLOSE);
        }
    }
}