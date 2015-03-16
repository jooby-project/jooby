package org.jooby.internal;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.Test;

public class ByteByfferInputStreamTest {

  @Test
  public void available() throws IOException {
    byte[] bytes = "hello".getBytes();
    ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
    buffer.put("hello".getBytes());
    buffer.flip();
    ByteByfferInputStream stream = new ByteByfferInputStream(buffer);
    assertEquals(5, stream.available());
    stream.read();
    assertEquals(4, stream.available());
    buffer.clear();

    stream.close();
  }

  @Test
  public void read() throws IOException {
    byte[] bytes = "hello".getBytes();
    ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
    buffer.put("hello".getBytes());
    buffer.flip();
    ByteByfferInputStream stream = new ByteByfferInputStream(buffer);
    assertEquals('h', stream.read());
    assertEquals('e', stream.read());
    assertEquals('l', stream.read());
    assertEquals('l', stream.read());
    assertEquals('o', stream.read());
    assertEquals(-1, stream.read());
    assertEquals(-1, stream.read());

    stream.close();
  }

  @Test
  public void readBuff() throws IOException {
    byte[] bytes = "hello".getBytes();
    ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
    buffer.put("hello".getBytes());
    buffer.flip();
    byte[] buff = new byte[10];
    ByteByfferInputStream stream = new ByteByfferInputStream(buffer);
    assertEquals(bytes.length, stream.read(buff));
    assertEquals(-1, stream.read());
    assertEquals("hello", new String(buff, 0, bytes.length));

    stream.close();
  }

  @Test
  public void readBuffWithOffset() throws IOException {
    byte[] bytes = "hello".getBytes();
    ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
    buffer.put("hello".getBytes());
    buffer.flip();
    byte[] buff = new byte[10];
    ByteByfferInputStream stream = new ByteByfferInputStream(buffer);
    assertEquals(bytes.length - 1, stream.read(buff, 0, bytes.length - 1));
    assertEquals('o', stream.read());
    assertEquals(-1, stream.read());
    assertEquals(-1, stream.read(buff, 0, bytes.length - 1));
    assertEquals("hell", new String(buff, 0, bytes.length - 1));

    stream.close();
  }

  @Test
  public void reset() throws IOException {
    byte[] bytes = "hello".getBytes();
    ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
    buffer.put("hello".getBytes());
    buffer.flip();
    ByteByfferInputStream stream = new ByteByfferInputStream(buffer);
    assertEquals('h', stream.read());
    assertEquals('e', stream.read());
    assertEquals('l', stream.read());
    assertEquals('l', stream.read());
    assertEquals('o', stream.read());
    assertEquals(-1, stream.read());
    stream.reset();
    assertEquals('h', stream.read());
    assertEquals('e', stream.read());
    assertEquals('l', stream.read());
    assertEquals('l', stream.read());
    assertEquals('o', stream.read());
    assertEquals(-1, stream.read());

    stream.close();
  }

}
