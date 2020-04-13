package io.jooby;

import io.jooby.internal.ProxyPeerAddress;

import javax.annotation.Nonnull;

/**
 * A handler that handles X-Forwarded-* headers by updating the values on the current context to
 * match what was sent in the header(s).
 *
 * This should only be installed behind a reverse proxy that has been configured to send the
 * X-Forwarded-* header, otherwise a remote user can spoof their address by sending a header with
 * bogus values.
 *
 * The headers that are read/set are:
 * <ul>
 *  <li>X-Forwarded-For: Set/update the remote address {@link Context#setRemoteAddress(String)}.</li>
 *  <li>X-Forwarded-Proto: Set/update request scheme {@link Context#setScheme(String)}.</li>
 *  <li>X-Forwarded-Host: Set/update the request host {@link Context#setHost(String)}.</li>
 *  <li>X-Forwarded-Port: Set/update the request port {@link Context#setPort(int)}.</li>
 * </ul>
 *
 * @author edgar
 * @since 2.8.1
 */
public class ProxyPeerAddressHandler implements Route.Decorator {
  @Nonnull @Override public Route.Handler apply(@Nonnull Route.Handler next) {
    return ctx -> {
      ProxyPeerAddress result = ProxyPeerAddress.parse(ctx);
      ctx.setRemoteAddress(result.getRemoteAddress());
      ctx.setHost(result.getHost());
      ctx.setScheme(result.getScheme());
      ctx.setPort(result.getPort());
      return next.apply(ctx);
    };
  }
}
