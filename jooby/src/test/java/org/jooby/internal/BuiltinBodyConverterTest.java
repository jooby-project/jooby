package org.jooby.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import org.jooby.Body.Formatter;
import org.jooby.Body.Parser;
import org.jooby.MediaType;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;

public class BuiltinBodyConverterTest {

  @Test
  public void formatReader() throws Exception {
    Formatter formatter = BuiltinBodyConverter.formatReader;

    assertEquals(ImmutableList.of(MediaType.html), formatter.types());

    assertTrue(formatter.canFormat(Readable.class));
    assertTrue(formatter.canFormat(Reader.class));
    assertTrue(formatter.canFormat(BufferedReader.class));
    assertTrue(formatter.canFormat(CharBuffer.class));
    assertTrue(formatter.canFormat(CharArrayReader.class));
    assertFalse(formatter.canFormat(String.class));
    assertFalse(formatter.canFormat(InputStream.class));

    StringWriter writer = new StringWriter();
    formatter.format(new StringReader("string"),
        new BodyWriterImpl(Charsets.UTF_8,
            () -> {
              throw new IOException();
            },
            () -> writer));
    assertEquals("string", writer.toString());

    StringWriter writer2 = new StringWriter();
    formatter.format(CharBuffer.wrap("string"),
        new BodyWriterImpl(Charsets.UTF_8,
            () -> {
              throw new IOException();
            },
            () -> writer2));
    assertEquals("string", writer2.toString());

    assertEquals("Formatter for: java.lang.Readable", formatter.toString());
  }

  @Test
  public void formatByteArray() throws Exception {
    Formatter formatter = BuiltinBodyConverter.formatByteArray;

    assertEquals(ImmutableList.of(MediaType.octetstream), formatter.types());

    assertTrue(formatter.canFormat(byte[].class));
    assertFalse(formatter.canFormat(int[].class));
    assertFalse(formatter.canFormat(InputStream.class));

    OutputStream stream = new ByteArrayOutputStream();
    formatter.format("string".getBytes(), new BodyWriterImpl(Charsets.UTF_8, () -> stream,
        () -> {
          throw new IOException();
        }));
    assertEquals("string", stream.toString());

    assertEquals("Formatter for: byte[]", formatter.toString());
  }

  @Test
  public void formatByteBuffer() throws Exception {
    Formatter formatter = BuiltinBodyConverter.formatByteBuffer;

    assertEquals(ImmutableList.of(MediaType.octetstream), formatter.types());

    assertTrue(formatter.canFormat(ByteBuffer.class));
    assertFalse(formatter.canFormat(InputStream.class));

    OutputStream stream = new ByteArrayOutputStream();
    formatter.format(ByteBuffer.wrap("string".getBytes()), new BodyWriterImpl(Charsets.UTF_8,
        () -> stream,
        () -> {
          throw new IOException();
        }));
    assertEquals("string", stream.toString());

    assertEquals("Formatter for: java.nio.ByteBuffer", formatter.toString());
  }

  @Test
  public void formatStream() throws Exception {
    Formatter formatter = BuiltinBodyConverter.formatStream;

    assertEquals(ImmutableList.of(MediaType.octetstream), formatter.types());

    assertTrue(formatter.canFormat(InputStream.class));
    assertTrue(formatter.canFormat(ByteArrayInputStream.class));
    assertTrue(formatter.canFormat(FileInputStream.class));
    assertFalse(formatter.canFormat(String.class));
    assertFalse(formatter.canFormat(Readable.class));

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    formatter.format(new ByteArrayInputStream("string".getBytes()),
        new BodyWriterImpl(Charsets.UTF_8,
            () -> out,
            () -> {
              throw new IOException();
            }));
    assertEquals("string", out.toString());

    assertEquals("Formatter for: java.io.InputStream", formatter.toString());
  }

  @Test
  public void formatAny() throws Exception {
    Formatter formatter = BuiltinBodyConverter.formatAny;

    assertEquals(ImmutableList.of(MediaType.html), formatter.types());

    assertTrue(formatter.canFormat(InputStream.class));
    assertTrue(formatter.canFormat(ByteArrayInputStream.class));
    assertTrue(formatter.canFormat(FileInputStream.class));
    assertTrue(formatter.canFormat(String.class));
    assertTrue(formatter.canFormat(Readable.class));

    StringWriter writer = new StringWriter();
    formatter.format("string",
        new BodyWriterImpl(Charsets.UTF_8,
            () -> {
              throw new IOException();
            },
            () -> writer));
    assertEquals("string", writer.toString());

    StringWriter writer2 = new StringWriter();
    formatter.format(76.8,
        new BodyWriterImpl(Charsets.UTF_8,
            () -> {
              throw new IOException();
            },
            () -> writer2));
    assertEquals("76.8", writer2.toString());

    StringWriter writer3 = new StringWriter();
    formatter.format(
        new Object() {
          @Override
          public String toString() {
            return "toString";
          }
        },
        new BodyWriterImpl(Charsets.UTF_8,
            () -> {
              throw new IOException();
            },
            () -> writer3));
    assertEquals("toString", writer3.toString());

    assertEquals("Formatter for: Object.toString()", formatter.toString());
  }

  @Test
  public void parseString() throws Exception {
    Parser parser = BuiltinBodyConverter.parseString;

    assertEquals(ImmutableList.of(MediaType.plain), parser.types());

    assertTrue(parser.canParse(CharSequence.class));
    assertTrue(parser.canParse(String.class));
    assertFalse(parser.canParse(InputStream.class));

    InputStream stream = new ByteArrayInputStream("string".getBytes());
    assertEquals("string",
        parser.parse(String.class, new BodyReaderImpl(Charsets.UTF_8, () -> stream)));

    assertEquals("Parser for: java.lang.CharSequence", parser.toString());
  }

}
