package org.jooby.internal;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;

public class OutputStreamNoCloseTest {

  private static class OutputStreamMock extends OutputStream {

    @Override
    public void write(final int b) throws IOException {
    }

    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
    }

    @Override
    public void write(final byte[] b) throws IOException {
    }
  }

  @SuppressWarnings("resource")
  @Test
  public void writeBuffWithOffset() throws Exception {
    byte[] buff = {'a', 'b', 'c' };
    CountDownLatch latch = new CountDownLatch(1);
    new OutputStreamNoClose(new OutputStreamMock() {
      @Override
      public void write(final byte[] buf, final int off, final int len) throws IOException {
        assertArrayEquals(buff, buf);
        assertEquals(1, off);
        assertEquals(buff.length - 1, len);
        latch.countDown();
      }
    }).write(buff, 1, buff.length - 1);
    latch.await();
  }

  @SuppressWarnings("resource")
  @Test
  public void writeBuff() throws Exception {
    byte[] buff = {'a', 'b', 'c' };
    CountDownLatch latch = new CountDownLatch(1);
    new OutputStreamNoClose(new OutputStreamMock() {
      @Override
      public void write(final byte[] buf) throws IOException {
        assertArrayEquals(buff, buf);
        latch.countDown();
      }
    }).write(buff);
    latch.await();
  }

  @SuppressWarnings("resource")
  @Test
  public void writeByte() throws Exception {
    byte ch = 'c';
    CountDownLatch latch = new CountDownLatch(1);
    new OutputStreamNoClose(new OutputStreamMock() {
      @Override
      public void write(final int c) throws IOException {
        assertEquals(ch, c);
        latch.countDown();
      }
    }).write(ch);
    latch.await();
  }

  @Test
  public void close() throws Exception {
    new OutputStreamNoClose(new OutputStreamMock() {
      @Override
      public void close() throws IOException {
        throw new IOException("Shouldn't be here");
      }
    }).close();
  }

  @SuppressWarnings("resource")
  @Test
  public void flush() throws Exception {
    new OutputStreamNoClose(new OutputStreamMock() {
      @Override
      public void flush() throws IOException {
        throw new IOException("Shouldn't be here");
      }
    }).flush();
  }

}
