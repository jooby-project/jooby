/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.CombinedChannelDuplexHandler;
import io.netty.handler.codec.http.*;

/**
 * Copy of {@link HttpServerCodec} with a custom request method parser and optimized header response
 * writer.
 */
public class NettyServerCodec
    extends CombinedChannelDuplexHandler<HttpRequestDecoder, HttpResponseEncoder>
    implements HttpServerUpgradeHandler.SourceCodec {

  /** A queue that is used for correlating a request and a response. */
  private final Queue<HttpMethod> queue = new ArrayDeque<HttpMethod>();

  private final HttpDecoderConfig decoderConfig;

  /** Creates a new instance with the specified decoder configuration. */
  public NettyServerCodec(HttpDecoderConfig decoderConfig) {
    this.decoderConfig = decoderConfig;
    init(new HttpServerRequestDecoder(decoderConfig), new HttpServerResponseEncoder());
  }

  /**
   * Web socket looks for these two component while doing the upgrade.
   *
   * @param ctx Channel context.
   */
  /*package*/ void webSocketHandshake(ChannelHandlerContext ctx) {
    var p = ctx.pipeline();
    var codec = p.context(getClass()).name();
    p.addBefore(codec, "encoder", new HttpServerResponseEncoder());
    p.addBefore(codec, "decoder", new HttpServerRequestDecoder(decoderConfig));
    p.remove(this);
  }

  /**
   * Upgrades to another protocol from HTTP. Removes the {@link HttpRequestDecoder} and {@link
   * HttpResponseEncoder} from the pipeline.
   */
  @Override
  public void upgradeFrom(ChannelHandlerContext ctx) {
    ctx.pipeline().remove(this);
  }

  private final class HttpServerRequestDecoder extends HttpRequestDecoder {
    HttpServerRequestDecoder(HttpDecoderConfig config) {
      super(config);
    }

    @Override
    protected HttpMessage createMessage(String[] initialLine) {
      return new DefaultHttpRequest(
          // Do strict version checking
          HttpVersion.valueOf(initialLine[2]),
          httpMethod(initialLine[0]),
          initialLine[1],
          headersFactory);
    }

    public static HttpMethod httpMethod(String name) {
      return switch (name) {
        case "OPTIONS" -> HttpMethod.OPTIONS;
        case "GET" -> HttpMethod.GET;
        case "HEAD" -> HttpMethod.HEAD;
        case "POST" -> HttpMethod.POST;
        case "PUT" -> HttpMethod.PUT;
        case "PATCH" -> HttpMethod.PATCH;
        case "DELETE" -> HttpMethod.DELETE;
        case "TRACE" -> HttpMethod.TRACE;
        case "CONNECT" -> HttpMethod.CONNECT;
        default -> new HttpMethod(name.toUpperCase());
      };
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> out)
        throws Exception {
      int oldSize = out.size();
      super.decode(ctx, buffer, out);
      int size = out.size();
      for (int i = oldSize; i < size; i++) {
        Object obj = out.get(i);
        if (obj instanceof HttpRequest) {
          queue.add(((HttpRequest) obj).method());
        }
      }
    }
  }

  private final class HttpServerResponseEncoder extends HttpResponseEncoder {

    private HttpMethod method;

    @Override
    protected void sanitizeHeadersBeforeEncode(HttpResponse msg, boolean isAlwaysEmpty) {
      if (!isAlwaysEmpty
          && HttpMethod.CONNECT.equals(method)
          && msg.status().codeClass() == HttpStatusClass.SUCCESS) {
        // Stripping Transfer-Encoding:
        // See https://tools.ietf.org/html/rfc7230#section-3.3.1
        msg.headers().remove(HttpHeaderNames.TRANSFER_ENCODING);
        return;
      }

      super.sanitizeHeadersBeforeEncode(msg, isAlwaysEmpty);
    }

    @Override
    protected void encodeHeaders(HttpHeaders headers, ByteBuf buf) {
      if (headers.getClass() == HeadersMultiMap.class) {
        ((HeadersMultiMap) headers).encode(buf);
      } else {
        super.encodeHeaders(headers, buf);
      }
    }

    @Override
    protected boolean isContentAlwaysEmpty(@SuppressWarnings("unused") HttpResponse msg) {
      method = queue.poll();
      return HttpMethod.HEAD.equals(method) || super.isContentAlwaysEmpty(msg);
    }
  }
}
