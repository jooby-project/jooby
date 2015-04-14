package org.jooby;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.CountDownLatch;

import org.junit.Test;

import com.google.inject.Key;
import com.google.inject.TypeLiteral;

public class ParamContextTest {

  @Test
  public void requireTypeLiteral() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    new ParamConverter.Context() {

      @Override
      public <T> T require(final Key<T> key) {
        assertEquals(Key.get(ParamContextTest.class), key);
        latch.countDown();
        return null;
      }

      @Override
      public Object convert(final TypeLiteral<?> type, final Object[] values) throws Exception {
        return null;
      }
    }.require(TypeLiteral.get(ParamContextTest.class));
    latch.await();
  }

  @Test
  public void requireClass() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    new ParamConverter.Context() {

      @Override
      public <T> T require(final Key<T> key) {
        assertEquals(Key.get(ParamContextTest.class), key);
        latch.countDown();
        return null;
      }

      @Override
      public Object convert(final TypeLiteral<?> type, final Object[] values) throws Exception {
        return null;
      }
    }.require(ParamContextTest.class);
    latch.await();
  }

}
