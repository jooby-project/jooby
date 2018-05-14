package org.jooby.issues;

import com.google.common.collect.ImmutableMap;
import org.jooby.MediaType;
import org.jooby.json.Jackson;
import org.jooby.test.SseFeature;
import org.jooby.funzy.Try;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class Issue308 extends SseFeature {

  {
    use(new Jackson());

    sse("/sse", sse -> {
      sse.send("d1");
      sse.event("d2").name("e2").send();
      sse.event("d3").name("e3").id("i3").type("plain").send();
      sse.event("d4").name("e4").id("i4").retry(5000L).send();
      sse.event("d5").name("e5").id("i5").retry(1, TimeUnit.MINUTES).send()
          .whenComplete((id, x) -> {
            if (x == null) {
              assertEquals("i5", id.get());
              Try.run(sse::close);
            }
          });
    }).produces(MediaType.plain);

    sse("/sse-json-local", sse -> {
      sse.send(ImmutableMap.of("username", "bobby", "text", "Hi everyone."), MediaType.plain);
      sse.send(ImmutableMap.of("username", "bobby", "text", "Hi everyone."), "json")
          .whenComplete((id, x) -> {
            if (x == null) {
              Try.run(sse::close);
            }
          });
    });

    sse("/sse-json-global", sse -> {
      sse.send(ImmutableMap.of("username", "bobby", "text", "Hi everyone."))
          .whenComplete((id, x) -> {
            Try.run(sse::close);
          });
    }).produces(MediaType.json);

    sse("/sse-multiline", sse -> {
      sse.send("<html>\n<body>\n\n</body>\n</html>\n")
          .whenComplete((id, x) -> {
            Try.run(sse::close);
          });
    }).produces(MediaType.plain);

    sse("/sse-bytebuffer", (req, sse) -> {
      boolean array = req.param("array").booleanValue(false);
      byte[] bytebuffer = "bytebuffer".getBytes(StandardCharsets.UTF_8);
      ByteBuffer buffer = array
          ? ByteBuffer.wrap(bytebuffer)
          : ByteBuffer.allocateDirect(bytebuffer.length).put(bytebuffer);
      buffer.flip();
      sse.send(buffer)
          .whenComplete((id, x) -> {
            Try.run(sse::close);
          });
    }).produces(MediaType.plain);

    sse("/sse-comment", sse -> {
      sse.keepAlive(10, TimeUnit.MILLISECONDS);
      sse.event("data").comment("this is a comment").send().whenComplete((id, x) -> {
        Try.run(sse::close);
      });
    }).produces(MediaType.plain);

    sse("/sse-last-event-id", sse -> {
      sse.send(sse.lastEventId().get()).whenComplete((id, x) -> {
        Try.run(sse::close);
      });
    }).produces(MediaType.plain);

  }

  @Test
  public void sse() throws Exception {
    assertEquals("data:d1\n" +
        "\n" +
        "event:e2\n" +
        "data:d2\n" +
        "\n" +
        "id:i3\n" +
        "event:e3\n" +
        "data:d3\n" +
        "\n" +
        "id:i4\n" +
        "event:e4\n" +
        "retry:5000\n" +
        "data:d4\n" +
        "\n" +
        "id:i5\n" +
        "event:e5\n" +
        "retry:60000\n" +
        "data:d5\n" +
        "\n" +
        "", sse("/sse", 1));
  }

  @Test
  public void ssejson() throws Exception {
    assertEquals("data:{username=bobby, text=Hi everyone.}\n\n" +
            "data:{\"username\":\"bobby\",\"text\":\"Hi everyone.\"}\n\n",
        sse("/sse-json-local", 1));
    assertEquals("data:{\"username\":\"bobby\",\"text\":\"Hi everyone.\"}\n\n",
        sse("/sse-json-global", 1));
  }

  @Test
  public void sseml() throws Exception {
    assertEquals("data:<html>\n" +
        "data:<body>\n" +
        "data:</body>\n" +
        "data:</html>\n" +
        "\n", sse("/sse-multiline", 1));
  }

  @Test
  public void bytebuffer() throws Exception {
    assertEquals("data:bytebuffer\n\n", sse("/sse-bytebuffer", 1));
    assertEquals("data:bytebuffer\n\n", sse("/sse-bytebuffer?array=true", 1));
  }

  @Test
  public void comments() throws Exception {
    assertEquals(":this is a comment\n" +
        "data:data\n" +
        "\n" +
        "", sse("/sse-comment", 1));
  }

  @Test
  public void lastEventId() throws Exception {
    assertEquals("data:1\n\n", sse("/sse-last-event-id", 1));
  }

}
