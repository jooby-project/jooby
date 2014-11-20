package org.jooby;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Date;
import java.util.Optional;

import org.junit.Test;

public class BodyTest {

  @Test
  public void accepted() {
    Body body = Body.accepted();
    assertEquals(Optional.empty(), body.content());
    assertEquals(Optional.empty(), body.type());
    assertEquals(Status.ACCEPTED, body.status().get());
  }

  @Test
  public void acceptedWithConent() {
    Body body = Body.accepted("s");
    assertEquals(Optional.empty(), body.type());
    assertEquals("s", body.content().get());
    assertEquals(Status.ACCEPTED, body.status().get());
  }

  @Test
  public void ok() {
    Body body = Body.ok();
    assertEquals(Optional.empty(), body.content());
    assertEquals(Optional.empty(), body.type());
    assertEquals(Status.OK, body.status().get());
  }

  @Test
  public void okWithConent() {
    Body body = Body.ok("s");
    assertEquals(Optional.empty(), body.type());
    assertEquals("s", body.content().get());
    assertEquals(Status.OK, body.status().get());
  }

  @Test
  public void withStatusCode() {
    Body body = Body.body(200);
    assertEquals(Optional.empty(), body.content());
    assertEquals(Optional.empty(), body.type());
    assertEquals(Status.OK, body.status().get());
  }

  @Test
  public void chainStatusCode() {
    Body body = Body.body("b").status(200);
    assertEquals(Optional.empty(), body.type());
    assertEquals("b", body.content().get());
    assertEquals(Status.OK, body.status().get());
  }

  @Test
  public void type() {
    Body body = Body.body("b").type("json");
    assertEquals(MediaType.json, body.type().get());
    assertEquals("b", body.content().get());
    assertEquals(Optional.empty(), body.status());
  }

  @Test
  public void header() {
    Body body = Body.ok().header("char", 'c')
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
    Body body = Body.body("b").status(Status.OK);
    assertEquals(Optional.empty(), body.type());
    assertEquals("b", body.content().get());
    assertEquals(Status.OK, body.status().get());
  }

  @Test
  public void noContent() {
    Body body = Body.noContent();
    assertEquals(Optional.empty(), body.content());
    assertEquals(Optional.empty(), body.type());
    assertEquals(Status.NO_CONTENT, body.status().get());
  }

  @Test
  public void withStatus() {
    Body body = Body.body(Status.CREATED);
    assertEquals(Optional.empty(), body.content());
    assertEquals(Optional.empty(), body.type());
    assertEquals(Status.CREATED, body.status().get());
  }

  @Test
  public void bodyWithConent() {
    Body body = Body.body("s");
    assertEquals(Optional.empty(), body.type());
    assertEquals(Optional.empty(), body.status());
    assertEquals("s", body.content().get());
  }

  @Test
  public void moved() {
    Body body = Body.moved("/location");
    assertEquals(Optional.empty(), body.content());
    assertEquals(Optional.empty(), body.type());
    assertEquals(Status.MOVED_PERMANENTLY, body.status().get());
    assertEquals("/location", body.headers().get("location"));
  }

  @Test
  public void redirect() {
    Body body = Body.redirect("/location");
    assertEquals(Optional.empty(), body.content());
    assertEquals(Optional.empty(), body.type());
    assertEquals(Status.FOUND, body.status().get());
    assertEquals("/location", body.headers().get("location"));
  }

  @Test
  public void seeOther() {
    Body body = Body.seeOther("/location");
    assertEquals(Optional.empty(), body.content());
    assertEquals(Optional.empty(), body.type());
    assertEquals(Status.SEE_OTHER, body.status().get());
    assertEquals("/location", body.headers().get("location"));
  }

  @Test
  public void temporaryRedirect() {
    Body body = Body.tempRedirect("/location");
    assertEquals(Optional.empty(), body.content());
    assertEquals(Optional.empty(), body.type());
    assertEquals(Status.TEMPORARY_REDIRECT, body.status().get());
    assertEquals("/location", body.headers().get("location"));
  }

}
