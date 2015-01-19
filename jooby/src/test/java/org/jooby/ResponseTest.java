package org.jooby;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.LinkedList;
import java.util.Optional;

import org.junit.Test;

import com.google.common.base.Charsets;

public class ResponseTest {
  public static class ResponseMock implements Response {

    @Override
    public void download(final String filename, final InputStream stream) throws Exception {
      throw new UnsupportedOperationException();
    }

    @Override
    public void download(final String filename, final Reader reader) throws Exception {
      throw new UnsupportedOperationException();
    }

    @Override
    public Response cookie(final Cookie cookie) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Response clearCookie(final String name) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Mutant header(final String name) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Response header(final String name, final char value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Response header(final String name, final byte value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Response header(final String name, final short value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Response header(final String name, final int value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Response header(final String name, final long value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Response header(final String name, final float value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Response header(final String name, final double value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Response header(final String name, final CharSequence value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Response header(final String name, final Date value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Charset charset() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Response charset(final Charset charset) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Response length(final long length) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void end() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<MediaType> type() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Response type(final MediaType type) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void send(final Body body) throws Exception {
      throw new UnsupportedOperationException();
    }

    @Override
    public Formatter format() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void redirect(final Status status, final String location) throws Exception {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Status> status() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Response status(final Status status) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean committed() {
      throw new UnsupportedOperationException();
    }

  }

  @Test
  public void type() {
    LinkedList<MediaType> types = new LinkedList<>();
    new ResponseMock() {
      @Override
      public Response type(final MediaType type) {
        types.add(type);
        return this;
      }
    }.type("json");
    assertEquals(MediaType.json, types.getFirst());
  }

  @Test
  public void sendObject() throws Exception {
    Object data = new Object();
    LinkedList<Object> dataList = new LinkedList<>();
    new ResponseMock() {
      @Override
      public void send(final Body body) throws Exception {
        assertNotNull(body);
        assertEquals(Status.OK, body.status().get());
        assertEquals(MediaType.json, body.type().get());
        dataList.add(body.content().get());
      }

      @Override
      public Optional<Status> status() {
        return Optional.of(Status.OK);
      }

      @Override
      public Optional<MediaType> type() {
        return Optional.of(MediaType.json);
      }

    }.send(data);

    assertEquals(data, dataList.getFirst());
  }

  @Test
  public void sendBody() throws Exception {
    Object data = Body.noContent();
    LinkedList<Object> dataList = new LinkedList<>();
    new ResponseMock() {
      @Override
      public void send(final Body body) throws Exception {
        assertNotNull(body);
        dataList.add(body);
      }

      @Override
      public Optional<Status> status() {
        return Optional.of(Status.OK);
      }

      @Override
      public Optional<MediaType> type() {
        return Optional.of(MediaType.json);
      }

    }.send(data);

    assertEquals(data, dataList.getFirst());
  }

  @Test
  public void redirect() throws Exception {
    LinkedList<Object> dataList = new LinkedList<>();
    new ResponseMock() {
      @Override
      public void redirect(final Status status, final String location) throws Exception {
        assertEquals(Status.FOUND, status);
        dataList.add(location);
      }
    }.redirect("/red");
    assertEquals("/red", dataList.getFirst());
  }

  @Test
  public void statusCode() throws Exception {
    LinkedList<Object> dataList = new LinkedList<>();
    new ResponseMock() {
      @Override
      public Response status(final Status status) {
        dataList.add(status);
        return this;
      }
    }.status(200);
    assertEquals(Status.OK, dataList.getFirst());
  }

  @Test
  public void downloadWithRelativeCpLocation() throws Exception {
    LinkedList<Object> dataList = new LinkedList<>();
    String resource = "assets/js/file.js";
    new ResponseMock() {
      @Override
      public void download(final String filename, final Reader stream) throws Exception {
        assertNotNull(stream);
        stream.close();
        dataList.add(filename);
      }

      @Override
      public Charset charset() {
        return Charsets.UTF_8;
      }

      @Override
      public Optional<MediaType> type() {
        return Optional.empty();
      }

      @Override
      public Response type(final MediaType type) {
        assertEquals(MediaType.js, type);
        return this;
      }

    }.download(resource);
    assertEquals(resource, dataList.getFirst());
  }

  @Test
  public void downloadWithAbsoluteCpLocation() throws Exception {
    LinkedList<Object> dataList = new LinkedList<>();
    String resource = "/assets/js/file.js";
    new ResponseMock() {
      @Override
      public void download(final String filename, final Reader stream) throws Exception {
        assertNotNull(stream);
        stream.close();
        dataList.add(filename);
      }

      @Override
      public Charset charset() {
        return Charsets.UTF_8;
      }

      @Override
      public Optional<MediaType> type() {
        return Optional.empty();
      }

      @Override
      public Response type(final MediaType type) {
        assertEquals(MediaType.js, type);
        return this;
      }
    }.download(resource);
    assertEquals(resource, dataList.getFirst());
  }

  @Test
  public void downloadFile() throws Exception {
    LinkedList<Object> dataList = new LinkedList<>();
    File resource = new File("src/test/resources/assets/js/file.js");
    new ResponseMock() {
      @Override
      public void download(final String filename, final Reader stream) throws Exception {
        assertNotNull(stream);
        stream.close();
        dataList.add(filename);
      }
    }.download(resource);
    assertEquals(resource.getName(), dataList.getFirst());
  }

  @Test
  public void downloadReader() throws Exception {
    LinkedList<Object> dataList = new LinkedList<>();
    FileReader resource = new FileReader("src/test/resources/assets/js/file.js");
    new ResponseMock() {
      @Override
      public void download(final String filename, final Reader reader) throws Exception {
        assertNotNull(reader);
        reader.close();
        dataList.add(filename);
      }
    }.download("alias", resource);
    assertEquals("alias", dataList.getFirst());
  }

  @Test
  public void downloadFileWithName() throws Exception {
    LinkedList<Object> dataList = new LinkedList<>();
    File resource = new File("src/test/resources/assets/js/file.js");
    new ResponseMock() {
      @Override
      public void download(final String filename, final InputStream stream) throws Exception {
        assertNotNull(stream);
        stream.close();
        dataList.add(filename);
      }
    }.download("alias.js", resource);
    assertEquals("alias.js", dataList.getFirst());
  }

  @Test
  public void cookieWithNameAndValue() throws Exception {
    LinkedList<Cookie> dataList = new LinkedList<>();
    new ResponseMock() {
      @Override
      public Response cookie(final Cookie cookie) {
        dataList.add(cookie);
        return this;
      }
    }.cookie("name", "value");

    assertEquals("name", dataList.getFirst().name());
    assertEquals("value", dataList.getFirst().value().get());
  }

  @Test
  public void cookieWith() throws Exception {
    LinkedList<Cookie> dataList = new LinkedList<>();
    new ResponseMock() {
      @Override
      public Response cookie(final Cookie cookie) {
        dataList.add(cookie);
        return this;
      }
    }.cookie(new Cookie.Definition("name", "value"));

    assertEquals("name", dataList.getFirst().name());
    assertEquals("value", dataList.getFirst().value().get());
  }

}
