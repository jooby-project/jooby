package org.jooby;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.Optional;

import org.jooby.Cookie.Definition;
import org.jooby.Route.After;
import org.jooby.Route.Complete;
import org.junit.Test;

public class ResponseTest {
  public static class ResponseMock implements Response {

    @Override
    public void download(final String filename, final InputStream stream) throws Exception {
      throw new UnsupportedOperationException();
    }

    @Override
    public void download(final String filename, final String location) throws Exception {
    }

    @Override
    public Response cookie(final Definition cookie) {
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
    public Response header(final String name, final Object value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Response header(final String name, final Iterable<Object> values) {
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
    public void send(final Result result) throws Exception {
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

    @Override
    public void after(final After handler) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void complete(final Complete handler) {
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
  public void sendObject() throws Throwable {
    Object data = new Object();
    LinkedList<Object> dataList = new LinkedList<>();
    new ResponseMock() {
      @Override
      public void send(final Result result) {
        assertNotNull(result);
        assertEquals(Status.OK, result.status().get());
        assertEquals(MediaType.json, result.type().get());
        dataList.add(result.ifGet().get());
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
  public void sendBody() throws Throwable {
    Object data = Results.noContent();
    LinkedList<Object> dataList = new LinkedList<>();
    new ResponseMock() {
      @Override
      public void send(final Result body) throws Exception {
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
  public void redirect() throws Throwable {
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
  public void downloadFileWithName() throws Throwable {
    LinkedList<Object> dataList = new LinkedList<>();
    File resource = file("src/test/resources/org/jooby/ResponseTest.js");
    new ResponseMock() {
      @Override
      public void download(final String filename, final InputStream stream) throws Exception {
        assertNotNull(stream);
        stream.close();
        dataList.add(filename);
      }

      @Override
      public Response length(final long length) {
        dataList.add(length);
        return this;
      }
    }.download("alias.js", resource);
    assertEquals("[20, alias.js]", dataList.toString());
  }

  @Test
  public void cookieWithNameAndValue() throws Exception {
    LinkedList<Cookie.Definition> dataList = new LinkedList<>();
    new ResponseMock() {
      @Override
      public Response cookie(final Cookie.Definition cookie) {
        dataList.add(cookie);
        return this;
      }
    }.cookie("name", "value");

    assertEquals("name", dataList.getFirst().name().get());
    assertEquals("value", dataList.getFirst().value().get());
  }

  /**
   * Attempt to load a file from multiple location. required by unit and integration tests.
   *
   * @param location
   * @return
   */
  private File file(final String location) {
    for (String candidate : new String[]{location, "jooby/" + location,
        "../../jooby/" + location }) {
      File file = new File(candidate);
      if (file.exists()) {
        return file;
      }
    }
    return file(location);
  }

}
