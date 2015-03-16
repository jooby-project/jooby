package org.jooby.internal;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;

public class WriterNoCloseTest {

  private static class WriterMock extends Writer {

    @Override
    public void write(final char[] cbuf, final int off, final int len) throws IOException {
    }

    @Override
    public void write(final String str) throws IOException {
    }

    @Override
    public void write(final int c) throws IOException {
    }

    @Override
    public void write(final String str, final int off, final int len) throws IOException {
    }

    @Override
    public void write(final char[] cbuf) throws IOException {
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public void flush() throws IOException {
    }
  }

  @SuppressWarnings("resource")
  @Test
  public void writeBuffWithOffset() throws Exception {
    char[] buff = {'a', 'b', 'c' };
    CountDownLatch latch = new CountDownLatch(1);
    new WriterNoClose(new WriterMock() {
      @Override
      public void write(final char[] cbuf, final int off, final int len) throws IOException {
        assertArrayEquals(buff, cbuf);
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
    char[] buff = {'a', 'b', 'c' };
    CountDownLatch latch = new CountDownLatch(1);
    new WriterNoClose(new WriterMock() {
      @Override
      public void write(final char[] cbuf) throws IOException {
        assertArrayEquals(buff, cbuf);
        latch.countDown();
      }
    }).write(buff);
    latch.await();
  }

  @SuppressWarnings("resource")
  @Test
  public void writeChar() throws Exception {
    char ch = 'c';
    CountDownLatch latch = new CountDownLatch(1);
    new WriterNoClose(new WriterMock() {
      @Override
      public void write(final int c) throws IOException {
        assertEquals(ch, c);
        latch.countDown();
      }
    }).write(ch);
    latch.await();
  }

  @SuppressWarnings("resource")
  @Test
  public void writeStr() throws Exception {
    String str = "str";
    CountDownLatch latch = new CountDownLatch(1);
    new WriterNoClose(new WriterMock() {
      @Override
      public void write(final String data) throws IOException {
        assertEquals(str, data);
        latch.countDown();
      }
    }).write(str);
    latch.await();
  }

  @SuppressWarnings("resource")
  @Test
  public void writeStrWithOffSet() throws Exception {
    String str = "str";
    CountDownLatch latch = new CountDownLatch(1);
    new WriterNoClose(new WriterMock() {
      @Override
      public void write(final String data, final int off, final int len) throws IOException {
        assertEquals(str, data);
        assertEquals(1, off);
        assertEquals(data.length() - 1, len);
        latch.countDown();
      }
    }).write(str, 1, str.length() -1);
    latch.await();
  }

  @Test
  public void close() throws Exception {
    new WriterNoClose(new WriterMock() {
      @Override
      public void close() throws IOException {
        throw new IOException("Shouldn't be here");
      }
    }).close();
  }

  @SuppressWarnings("resource")
  @Test
  public void flush() throws Exception {
    new WriterNoClose(new WriterMock() {
      @Override
      public void flush() throws IOException {
        throw new IOException("Shouldn't be here");
      }
    }).flush();
  }

}
