/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpServerUpgradeHandler;

public class Http2Extension {

  private Http2Settings settings;

  private Consumer<ChannelPipeline> http11;

  private BiConsumer<ChannelPipeline, Supplier<HttpServerUpgradeHandler.UpgradeCodec>> http11Upgrade;

  private BiConsumer<ChannelPipeline, Supplier<ChannelOutboundHandler>> http2;

  private BiConsumer<ChannelPipeline, Supplier<ChannelOutboundHandler>> http2c;

  public Http2Extension(Http2Settings settings,
      Consumer<ChannelPipeline> http11,
      BiConsumer<ChannelPipeline, Supplier<HttpServerUpgradeHandler.UpgradeCodec>> http11Upgrade,
      BiConsumer<ChannelPipeline, Supplier<ChannelOutboundHandler>> http2,
      BiConsumer<ChannelPipeline, Supplier<ChannelOutboundHandler>> http2c) {
    this.settings = settings;
    this.http11 = http11;
    this.http11Upgrade = http11Upgrade;
    this.http2 = http2;
    this.http2c = http2c;
  }

  public boolean isSecure() {
    return settings.isSecure();
  }

  public void http11(ChannelPipeline pipeline) {
    this.http11.accept(pipeline);
  }

  public void http2(ChannelPipeline pipeline,
      Function<Http2Settings, ChannelOutboundHandler> factory) {
    this.http2.accept(pipeline, () -> factory.apply(settings));
  }

  public void http2c(ChannelPipeline pipeline,
      Function<Http2Settings, ChannelOutboundHandler> factory) {
    this.http2c.accept(pipeline, () -> factory.apply(settings));
  }

  public void http11Upgrade(ChannelPipeline pipeline,
      Function<Http2Settings, HttpServerUpgradeHandler.UpgradeCodec> factory) {
    this.http11Upgrade.accept(pipeline, () -> factory.apply(settings));
  }
}
