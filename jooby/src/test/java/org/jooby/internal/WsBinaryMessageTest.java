package org.jooby.internal;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.List;

import org.jooby.Err;
import org.jooby.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.base.Charsets;

@RunWith(PowerMockRunner.class)
@PrepareForTest({WsBinaryMessage.class, ByteArrayInputStream.class, InputStreamReader.class })
public class WsBinaryMessageTest {

  @Test
  public void toByteArray() {
    byte[] bytes = "bytes".getBytes();
    ByteBuffer buffer = ByteBuffer.wrap(bytes);
    assertArrayEquals(bytes, new WsBinaryMessage(buffer).to(byte[].class));
  }

  @Test
  public void toByteBuffer() {
    byte[] bytes = "bytes".getBytes();
    ByteBuffer buffer = ByteBuffer.wrap(bytes);
    assertEquals(buffer, new WsBinaryMessage(buffer).to(ByteBuffer.class));
  }

  @Test
  public void toInputStream() throws Exception {
    byte[] bytes = "bytes".getBytes();
    ByteBuffer buffer = ByteBuffer.wrap(bytes);

    new MockUnit()
        .expect(unit -> {
          InputStream stream = unit.mockConstructor(ByteArrayInputStream.class,
              new Class[]{byte[].class }, bytes);
          unit.registerMock(InputStream.class, stream);
        })
        .run(unit -> {
          assertEquals(unit.get(InputStream.class),
              new WsBinaryMessage(buffer).to(InputStream.class));
        });
  }

  @Test
  public void toReader() throws Exception {
    byte[] bytes = "bytes".getBytes();
    ByteBuffer buffer = ByteBuffer.wrap(bytes);

    new MockUnit()
        .expect(
            unit -> {
              InputStream stream = unit.mockConstructor(ByteArrayInputStream.class,
                  new Class[]{byte[].class }, bytes);

              Reader reader = unit.mockConstructor(InputStreamReader.class, new Class[]{
                  InputStream.class, Charset.class }, stream, Charsets.UTF_8);

              unit.registerMock(Reader.class, reader);
            })
        .run(unit -> {
          assertEquals(unit.get(Reader.class),
              new WsBinaryMessage(buffer).to(Reader.class));
        });
  }

  @Test(expected = Err.class)
  public void toUnsupportedType() throws Exception {
    byte[] bytes = "bytes".getBytes();
    ByteBuffer buffer = ByteBuffer.wrap(bytes);

    new WsBinaryMessage(buffer).to(List.class);
  }

  @Test(expected = Err.class)
  public void booleanValue() throws Exception {
    new WsBinaryMessage(ByteBuffer.wrap("bytes".getBytes())).booleanValue();
  }

  @Test(expected = Err.class)
  public void byteValue() throws Exception {
    new WsBinaryMessage(ByteBuffer.wrap("bytes".getBytes())).byteValue();
  }

  @Test(expected = Err.class)
  public void shortValue() throws Exception {
    new WsBinaryMessage(ByteBuffer.wrap("bytes".getBytes())).shortValue();
  }

  @Test(expected = Err.class)
  public void intValue() throws Exception {
    new WsBinaryMessage(ByteBuffer.wrap("bytes".getBytes())).intValue();
  }

  @Test(expected = Err.class)
  public void longValue() throws Exception {
    new WsBinaryMessage(ByteBuffer.wrap("bytes".getBytes())).longValue();
  }

  @Test(expected = Err.class)
  public void stringValue() throws Exception {
    new WsBinaryMessage(ByteBuffer.wrap("bytes".getBytes())).value();
  }

  @Test(expected = Err.class)
  public void floatValue() throws Exception {
    new WsBinaryMessage(ByteBuffer.wrap("bytes".getBytes())).floatValue();
  }

  @Test(expected = Err.class)
  public void doubleValue() throws Exception {
    new WsBinaryMessage(ByteBuffer.wrap("bytes".getBytes())).doubleValue();
  }

  @SuppressWarnings("unchecked")
  @Test(expected = Err.class)
  public void enumValue() throws Exception {
    new WsBinaryMessage(ByteBuffer.wrap("bytes".getBytes())).toEnum(Enum.class);
  }

  @Test(expected = Err.class)
  public void toList() throws Exception {
    new WsBinaryMessage(ByteBuffer.wrap("bytes".getBytes())).toList(String.class);
  }

  @Test(expected = Err.class)
  public void toSet() throws Exception {
    new WsBinaryMessage(ByteBuffer.wrap("bytes".getBytes())).toSet(String.class);
  }

  @Test(expected = Err.class)
  public void toSortedSet() throws Exception {
    new WsBinaryMessage(ByteBuffer.wrap("bytes".getBytes())).toSortedSet(String.class);
  }

  @Test(expected = Err.class)
  public void toOptional() throws Exception {
    new WsBinaryMessage(ByteBuffer.wrap("bytes".getBytes())).toOptional(String.class);
  }

  @Test
  public void isPresent() throws Exception {
    assertEquals(true, new WsBinaryMessage(ByteBuffer.wrap("bytes".getBytes())).isPresent());
  }

}
