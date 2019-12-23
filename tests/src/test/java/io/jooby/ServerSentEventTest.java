package io.jooby;

import io.jooby.json.JacksonModule;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ServerSentEventTest {
  @ServerTest
  public void shouldSupportsSse(ServerTestRunner runner) {
    runner.define(app -> {

      app.install(new JacksonModule());

      app.sse("/", sse -> {
        sse.send("message 1");
        sse.send("message 2");
      });
    }).ready(client -> {
      client.sse("/")
          .next(message -> {
            assertEquals("message 1", message.getData());
          })
          .next(message -> {
            assertEquals("message 2", message.getData());
          })
          .verify();
    });
  }

  @ServerTest(executionMode = ExecutionMode.EVENT_LOOP)
  public void shouldSupportsSseFromEventLoop(ServerTestRunner runner) {
    runner.define(app -> {
      app.sse("/", sse -> {
        sse.send("message 1");
      });
    }).ready(client -> {
      client.sse("/")
          .next(message -> {
            assertEquals("message 1", message.getData());
          })
          .verify();
    });
  }

  @ServerTest
  public void shouldUseEncoder(ServerTestRunner runner) {
    runner.define(app -> {

      app.install(new JacksonModule());

      app.sse("/json", sse -> {
        Map<String, Object> json = new HashMap<>();
        json.put("message", "see");

        sse.send(json);
      });
    }).ready(client -> {
      client.sse("/json").next(message -> {
        assertEquals("{\"message\":\"see\"}", message.getData());
      })
          .verify();
    });
  }

  @ServerTest
  public void shouldWorkWithEventType(ServerTestRunner runner) {
    runner.define(app -> {

      app.install(new JacksonModule());

      app.sse("/event-type", sse -> {
        Map<String, Object> json = new HashMap<>();
        json.put("username", "bobby");

        sse.send("userconnect", json);
      });
    }).ready(client -> {
      client.sse("/event-type")
          .next(message -> {
            assertEquals("{\"username\":\"bobby\"}", message.getData());
            assertEquals("userconnect", message.getEvent());
          })
          .verify();
    });
  }

  @ServerTest
  public void shouldWorkWithPathVariable(ServerTestRunner runner) {
    runner.define(app -> {
      app.sse("/full-message/{id}", sse -> {
        String id = sse.getContext().path("id").value();
        sse.send(new ServerSentMessage("full").setId(id).setEvent("myevent").setRetry(100L));
      });
    }).ready(client -> {
      String id = UUID.randomUUID().toString();
      client.sse("/full-message/" + id)
          .next(message -> {
            assertEquals("full", message.getData());
            assertEquals("myevent", message.getEvent());
            assertEquals(id, message.getId());
          })
          .verify();
    });
  }

  @ServerTest
  public void shouldWorkWithMultilineMessages(ServerTestRunner runner) {
    runner.define(app -> {

      app.install(new JacksonModule());

      app.sse("/multi-line", sse -> {
        sse.send("Multi\nLine\nMessage");

        Map<String, Object> json = new HashMap<>();
        json.put("text", "Multi\nLine\nMessage");
        sse.send(json);
      });
    }).ready(client -> {
      client.sse("/multi-line")
          .next(message -> {
            assertEquals("Multi\nLine\nMessage", message.getData());
          })
          .next(message -> {
            assertEquals("{\"text\":\"Multi\\n"
                + "Line\\n"
                + "Message\"}", message.getData());
          })
          .verify();
    });
  }

  @ServerTest
  public void shouldSendFromExecutor(ServerTestRunner runner) {
    runner.define(app -> {

      app.sse("/executor", sse -> {
        ScheduledExecutorService scheduler = Executors
            .newSingleThreadScheduledExecutor();

        AtomicLong inc = new AtomicLong(0);

        ScheduledFuture<?> future = scheduler.scheduleWithFixedDelay(() -> {
          try {
            sse.send("Message#" + inc.incrementAndGet());
          } catch (Exception x) {
            x.printStackTrace();
          }
        }, 0, 50, TimeUnit.MILLISECONDS);

        sse.onClose(() -> {
          future.cancel(true);
          scheduler.shutdown();
        });
      });
    }).ready(client -> {
      client.sse("/executor")
          .next(message -> {
            assertEquals("Message#1", message.getData());
          })
          .next(message -> {
            assertEquals("Message#2", message.getData());
          })
          .next((message, source) -> {
            assertEquals("Message#3", message.getData());
            source.cancel();
          })
          .verify();
    });
  }

  @ServerTest
  public void shouldSendKeepAliveMessage(ServerTestRunner runner) {
    String sseId = UUID.randomUUID().toString();
    runner.define(app -> {
      app.sse("/keep-alive", sse -> {
        sse.setId(sseId);
        sse.keepAlive(100);
        sse.send("ready");
      });
    }).ready(client -> {
      List<String> messages = new ArrayList<>();
      messages.add(":" + sseId);
      messages.add(":" + sseId);
      messages.add("ready");

      client.sse("/keep-alive")
          .next(message -> {
            messages.remove(message.getData());
          })
          .next(message -> {
            messages.remove(message.getData());
          })
          .next(message -> {
            messages.remove(message.getData());
          })
          .verify();
      assertEquals(0, messages.size());
    });
  }

  @ServerTest
  public void shouldHaveAccessToLastEventID(ServerTestRunner runner) {
    runner.define(app -> {
      AtomicInteger nextId = new AtomicInteger();
      app.sse("/id", sse -> {
        String lastID = Optional.ofNullable(sse.getLastEventId()).orElse("-1");
        sse.send(new ServerSentMessage(lastID).setId(Integer.toString(nextId.incrementAndGet())));
      });
    }).ready(client -> {
      client.sse("/id")
          .next(message -> {
            assertEquals("-1", message.getData());
            client.header("Last-Event-ID", message.getId().toString());
            client.sse("/id")
                .next(msg -> {
                  assertEquals("1", message.getData());
                });
          })
          .verify();
    });
  }
}
