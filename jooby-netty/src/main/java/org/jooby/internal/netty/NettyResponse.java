package org.jooby.internal.netty;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.DefaultCookie;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.ServerCookieEncoder;
import io.netty.util.Attribute;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jooby.spi.NativeResponse;

import com.google.common.collect.ImmutableList;

public class NettyResponse implements NativeResponse {

  private ChannelHandlerContext ctx;

  private NettyRequest req;

  private FullHttpResponse rsp;

  private boolean keepAlive;

  private HttpResponseStatus status = HttpResponseStatus.OK;

  private Map<String, Cookie> cookies = new HashMap<>();

  public NettyResponse(final ChannelHandlerContext ctx, final NettyRequest req,
      final boolean keepAlive) {
    this.ctx = ctx;
    this.req = req;
    this.keepAlive = keepAlive;
    // TODO: make me better
    this.rsp = new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.OK,
        Unpooled.buffer(1204));
  }

  @Override
  public void cookie(final org.jooby.Cookie cookie) {
    cookies.put(cookie.name(), toCookie(cookie));
  }

  @Override
  public void clearCookie(final String name) {
    cookies.remove(name);
    req.cookies().stream().filter(c -> c.name().equals(name)).findFirst().ifPresent(c -> {
      Cookie cookie = toCookie(c);
      cookie.setMaxAge(0);
      cookies.put(name, cookie);
    });

  }

  @Override
  public List<String> headers(final String name) {
    List<String> headers = rsp.headers().getAll(name);
    return headers == null ? Collections.emptyList() : ImmutableList.copyOf(headers);
  }

  @Override
  public void header(final String name, final String value) {
    rsp.headers().add(name, value);
  }

  @Override
  public OutputStream out() throws IOException {
    return new ByteBufOutputStream(rsp.content());
  }

  @Override
  public int statusCode() {
    return status.code();
  }

  @Override
  public void statusCode(final int code) {
    this.status = HttpResponseStatus.valueOf(code);
    rsp.setStatus(status);
  }

  @Override
  public boolean committed() {
    return ctx == null;
  }

  @Override
  public void end() throws IOException {
    if (ctx != null) {
      Attribute<NettyWebSocket> ws = ctx.attr(NettyHandler.WS);
      if (ws != null && ws.get() != null) {
        status = HttpResponseStatus.SWITCHING_PROTOCOLS;
        ws.get().hankshake();
        rsp = null;
        ctx = null;
        return;
      }
      if (cookies.size() > 0) {
        rsp.headers().set(HttpHeaders.Names.SET_COOKIE,
            ServerCookieEncoder.encode(cookies.values()));
      }
      if (!keepAlive) {
        ctx.write(rsp).addListener(ChannelFutureListener.CLOSE);
      } else {
        rsp.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
        ctx.write(rsp);
      }
      rsp = null;
      ctx = null;
    }
  }

  @Override
  public void reset() {
    rsp.headers().clear();
    rsp.content().clear();
  }

  private Cookie toCookie(final org.jooby.Cookie cookie) {
    Cookie result =
        new DefaultCookie(cookie.name(), cookie.value().orElse(null));

    cookie.comment().ifPresent(result::setComment);
    cookie.domain().ifPresent(result::setDomain);
    result.setHttpOnly(cookie.httpOnly());
    long maxAge = cookie.maxAge();
    if (maxAge >= 0) {
      result.setMaxAge(maxAge);
    } else {
      result.setMaxAge(Long.MIN_VALUE);
    }
    result.setPath(cookie.path());
    result.setSecure(cookie.secure());
    result.setVersion(cookie.version());

    return result;
  }
}
