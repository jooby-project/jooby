package org.jooby.assets;

import java.util.concurrent.atomic.AtomicInteger;

public class TestEngineFactory implements EngineFactory {

  public static final AtomicInteger count = new AtomicInteger();

  @Override public Engine get(String id, String scope) {
    return new TestEngine();
  }

  @Override public void release() {
    count.incrementAndGet();
  }
}
