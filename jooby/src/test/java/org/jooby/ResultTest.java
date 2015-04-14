package org.jooby;

import static org.junit.Assert.assertEquals;

import java.util.Date;
import java.util.Optional;

import org.junit.Test;

import com.google.common.collect.Lists;

public class ResultTest {

  @Test
  public void sillyJacocoWithStaticMethods() {
    new Results();
  }

  @Test
  public void entityAndStatus() {
    Result result = Results.with("x", 200);
    assertEquals("x", result.get().get());
    assertEquals(Optional.empty(), result.type());
    assertEquals(Status.OK, result.status().get());
  }

  @Test
  public void json() {
    Result result = Results.json("{}");
    assertEquals("{}", result.get().get());
    assertEquals(MediaType.json, result.type().get());
    assertEquals(Status.OK, result.status().get());
  }

  @Test
  public void xml() {
    Result result = Results.xml("{}");
    assertEquals("{}", result.get().get());
    assertEquals(MediaType.xml, result.type().get());
    assertEquals(Status.OK, result.status().get());
  }

  @Test
  public void accepted() {
    Result result = Results.accepted();
    assertEquals(Optional.empty(), result.get());
    assertEquals(Optional.empty(), result.type());
    assertEquals(Status.ACCEPTED, result.status().get());
  }

  @Test
  public void acceptedWithConent() {
    Result result = Results.accepted("s");
    assertEquals(Optional.empty(), result.type());
    assertEquals("s", result.get().get());
    assertEquals(Status.ACCEPTED, result.status().get());
  }

  @Test
  public void ok() {
    Result result = Results.ok();
    assertEquals(Optional.empty(), result.get());
    assertEquals(Optional.empty(), result.type());
    assertEquals(Status.OK, result.status().get());
  }

  @Test
  public void okWithConent() {
    Result result = Results.ok("s");
    assertEquals(Optional.empty(), result.type());
    assertEquals("s", result.get().get());
    assertEquals(Status.OK, result.status().get());
  }

  @Test
  public void withStatusCode() {
    Result result = Results.with(200);
    assertEquals(Optional.empty(), result.get());
    assertEquals(Optional.empty(), result.type());
    assertEquals(Status.OK, result.status().get());
  }

  @Test
  public void chainStatusCode() {
    Result result = Results.with("b").status(200);
    assertEquals(Optional.empty(), result.type());
    assertEquals("b", result.get().get());
    assertEquals(Status.OK, result.status().get());
  }

  @Test
  public void type() {
    Result result = Results.with("b").type("json");
    assertEquals(MediaType.json, result.type().get());
    assertEquals("b", result.get().get());
    assertEquals(Optional.empty(), result.status());
  }

  @Test
  public void header() {
    Date date = new Date();
    Result result = Results.ok().header("char", 'c')
        .header("byte", (byte) 3)
        .header("short", (short) 4)
        .header("int", 5)
        .header("long", 6l)
        .header("float", 7f)
        .header("double", 8d)
        .header("date", date)
        .header("list", 1, 2, 3);

    assertEquals('c', result.headers().get("char"));
    assertEquals((byte) 3, result.headers().get("byte"));
    assertEquals((short) 4, result.headers().get("short"));
    assertEquals(5, result.headers().get("int"));
    assertEquals((long) 6, result.headers().get("long"));
    assertEquals(7.0f, result.headers().get("float"));
    assertEquals(8.0d, result.headers().get("double"));
    assertEquals(date, result.headers().get("date"));
    assertEquals(Lists.newArrayList(1, 2, 3), result.headers().get("list"));
  }

  @Test
  public void chainStatus() {
    Result result = Results.with("b").status(Status.OK);
    assertEquals(Optional.empty(), result.type());
    assertEquals("b", result.get().get());
    assertEquals(Status.OK, result.status().get());
  }

  @Test
  public void noContent() {
    Result result = Results.noContent();
    assertEquals(Optional.empty(), result.get());
    assertEquals(Optional.empty(), result.type());
    assertEquals(Status.NO_CONTENT, result.status().get());
  }

  @Test
  public void withStatus() {
    Result result = Results.with(Status.CREATED);
    assertEquals(Optional.empty(), result.get());
    assertEquals(Optional.empty(), result.type());
    assertEquals(Status.CREATED, result.status().get());
  }

  @Test
  public void resultWithConent() {
    Result result = Results.with("s");
    assertEquals(Optional.empty(), result.type());
    assertEquals(Optional.empty(), result.status());
    assertEquals("s", result.get().get());
  }

  @Test
  public void moved() {
    Result result = Results.moved("/location");
    assertEquals(Optional.empty(), result.get());
    assertEquals(Optional.empty(), result.type());
    assertEquals(Status.MOVED_PERMANENTLY, result.status().get());
    assertEquals("/location", result.headers().get("location"));
  }

  @Test
  public void redirect() {
    Result result = Results.redirect("/location");
    assertEquals(Optional.empty(), result.get());
    assertEquals(Optional.empty(), result.type());
    assertEquals(Status.FOUND, result.status().get());
    assertEquals("/location", result.headers().get("location"));
  }

  @Test
  public void seeOther() {
    Result result = Results.seeOther("/location");
    assertEquals(Optional.empty(), result.get());
    assertEquals(Optional.empty(), result.type());
    assertEquals(Status.SEE_OTHER, result.status().get());
    assertEquals("/location", result.headers().get("location"));
  }

  @Test
  public void temporaryRedirect() {
    Result result = Results.tempRedirect("/location");
    assertEquals(Optional.empty(), result.get());
    assertEquals(Optional.empty(), result.type());
    assertEquals(Status.TEMPORARY_REDIRECT, result.status().get());
    assertEquals("/location", result.headers().get("location"));
  }

}
