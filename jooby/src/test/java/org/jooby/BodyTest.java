package org.jooby;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Date;
import java.util.Optional;

import org.junit.Test;

public class BodyTest {

  @Test
  public void accepted() {
    Result body = Results.accepted();
    assertEquals(Optional.empty(), body.get());
    assertEquals(Optional.empty(), body.type());
    assertEquals(Status.ACCEPTED, body.status().get());
  }

  @Test
  public void acceptedWithConent() {
    Result body = Results.accepted("s");
    assertEquals(Optional.empty(), body.type());
    assertEquals("s", body.get().get());
    assertEquals(Status.ACCEPTED, body.status().get());
  }

  @Test
  public void ok() {
    Result body = Results.ok();
    assertEquals(Optional.empty(), body.get());
    assertEquals(Optional.empty(), body.type());
    assertEquals(Status.OK, body.status().get());
  }

  @Test
  public void okWithConent() {
    Result body = Results.ok("s");
    assertEquals(Optional.empty(), body.type());
    assertEquals("s", body.get().get());
    assertEquals(Status.OK, body.status().get());
  }

  @Test
  public void withStatusCode() {
    Result body = Results.with(200);
    assertEquals(Optional.empty(), body.get());
    assertEquals(Optional.empty(), body.type());
    assertEquals(Status.OK, body.status().get());
  }

  @Test
  public void chainStatusCode() {
    Result body = Results.with("b").status(200);
    assertEquals(Optional.empty(), body.type());
    assertEquals("b", body.get().get());
    assertEquals(Status.OK, body.status().get());
  }

  @Test
  public void type() {
    Result body = Results.with("b").type("json");
    assertEquals(MediaType.json, body.type().get());
    assertEquals("b", body.get().get());
    assertEquals(Optional.empty(), body.status());
  }

  @Test
  public void header() {
    Result body = Results.ok().header("char", 'c')
        .header("byte", (byte) 3)
        .header("short", (short) 4)
        .header("int", 5)
        .header("long", 6l)
        .header("float", 7f)
        .header("double", 8d)
        .header("date", new Date());

    assertEquals("c", body.headers().get("char"));
    assertEquals("3", body.headers().get("byte"));
    assertEquals("4", body.headers().get("short"));
    assertEquals("5", body.headers().get("int"));
    assertEquals("6", body.headers().get("long"));
    assertEquals("7.0", body.headers().get("float"));
    assertEquals("8.0", body.headers().get("double"));
    assertNotNull(body.headers().get("date"));
  }

  @Test
  public void chainStatus() {
    Result body = Results.with("b").status(Status.OK);
    assertEquals(Optional.empty(), body.type());
    assertEquals("b", body.get().get());
    assertEquals(Status.OK, body.status().get());
  }

  @Test
  public void noContent() {
    Result body = Results.noContent();
    assertEquals(Optional.empty(), body.get());
    assertEquals(Optional.empty(), body.type());
    assertEquals(Status.NO_CONTENT, body.status().get());
  }

  @Test
  public void withStatus() {
    Result body = Results.with(Status.CREATED);
    assertEquals(Optional.empty(), body.get());
    assertEquals(Optional.empty(), body.type());
    assertEquals(Status.CREATED, body.status().get());
  }

  @Test
  public void bodyWithConent() {
    Result body = Results.with("s");
    assertEquals(Optional.empty(), body.type());
    assertEquals(Optional.empty(), body.status());
    assertEquals("s", body.get().get());
  }

  @Test
  public void moved() {
    Result body = Results.moved("/location");
    assertEquals(Optional.empty(), body.get());
    assertEquals(Optional.empty(), body.type());
    assertEquals(Status.MOVED_PERMANENTLY, body.status().get());
    assertEquals("/location", body.headers().get("location"));
  }

  @Test
  public void redirect() {
    Result body = Results.redirect("/location");
    assertEquals(Optional.empty(), body.get());
    assertEquals(Optional.empty(), body.type());
    assertEquals(Status.FOUND, body.status().get());
    assertEquals("/location", body.headers().get("location"));
  }

  @Test
  public void seeOther() {
    Result body = Results.seeOther("/location");
    assertEquals(Optional.empty(), body.get());
    assertEquals(Optional.empty(), body.type());
    assertEquals(Status.SEE_OTHER, body.status().get());
    assertEquals("/location", body.headers().get("location"));
  }

  @Test
  public void temporaryRedirect() {
    Result body = Results.tempRedirect("/location");
    assertEquals(Optional.empty(), body.get());
    assertEquals(Optional.empty(), body.type());
    assertEquals(Status.TEMPORARY_REDIRECT, body.status().get());
    assertEquals("/location", body.headers().get("location"));
  }

}
