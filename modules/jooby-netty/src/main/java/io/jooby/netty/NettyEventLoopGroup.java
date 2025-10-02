/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.netty;

import io.netty.channel.EventLoopGroup;

public interface NettyEventLoopGroup {
  EventLoopGroup getParent();

  EventLoopGroup getChild();

  void shutdown();
}
