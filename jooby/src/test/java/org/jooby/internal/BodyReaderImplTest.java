package org.jooby.internal;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.jooby.util.ExSupplier;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;

public class BodyReaderImplTest {

  @Test
  public void text() throws Exception {
    byte[] bytes = "bytes".getBytes();
    InputStream stream = new ByteArrayInputStream(bytes);
    ExSupplier<InputStream> supplier = () -> stream;
    assertEquals("bytes",
        new BodyReaderImpl(Charsets.UTF_8, supplier).text(r -> {
          assertEquals("UTF8", ((InputStreamReader) r).getEncoding());
          return CharStreams.toString(r);
        }));
  }

  @Test
  public void textEnconding() throws Exception {
    byte[] bytes = "bytes".getBytes();
    InputStream stream = new ByteArrayInputStream(bytes);
    ExSupplier<InputStream> supplier = () -> stream;
    assertEquals("bytes",
        new BodyReaderImpl(Charsets.US_ASCII, supplier).text(r -> {
          assertEquals("ASCII", ((InputStreamReader) r).getEncoding());
          return CharStreams.toString(r);
        }));
  }

  @Test(expected = IOException.class)
  public void textError() throws Exception {
    ExSupplier<InputStream> supplier = () -> {
      throw new IOException();
    };
    new BodyReaderImpl(Charsets.UTF_8, supplier).text(r -> CharStreams.toString(r));
  }

  @Test
  public void bytes() throws Exception {
    byte[] bytes = "bytes".getBytes();
    InputStream stream = new ByteArrayInputStream(bytes);
    ExSupplier<InputStream> supplier = () -> stream;
    assertEquals("bytes",
        new BodyReaderImpl(Charsets.UTF_8, supplier).bytes(s ->
            new String(ByteStreams.toByteArray(s))
            ));
  }

  @Test(expected = IOException.class)
  public void bytesErr() throws Exception {
    ExSupplier<InputStream> supplier = () -> {
      throw new IOException();
    };

    new BodyReaderImpl(Charsets.UTF_8, supplier).bytes(s ->
        new String(ByteStreams.toByteArray(s))
        );
  }

}
