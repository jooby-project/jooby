/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.jooby.Route.Chain;
import org.jooby.internal.SseRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

import javaslang.concurrent.Future;
import javaslang.concurrent.Promise;
import javaslang.control.Try;
import javaslang.control.Try.CheckedRunnable;

/**
 * <h1>Server Sent Events</h1>
 * <p>
 * Server-Sent Events (SSE) is a mechanism that allows server to push the data from the server to
 * the client once the client-server connection is established by the client. Once the connection is
 * established by the client, it is the server who provides the data and decides to send it to the
 * client whenever new <strong>chunk</strong> of data is available.
 * </p>
 *
 * <h2>usage</h2>
 *
 * <pre>{@code
 * {
 *   sse("/path", sse -> {
 *     // 1. connected
 *     sse.send("data"); // 2. send/push data
 *   });
 * }
 * }</pre>
 *
 * <p>
 * Simple, effective and easy to use. The callback will be executed once when a new client is
 * connected. Inside the callback we can send data, listen for connection close events, etc.
 * </p>
 *
 * <p>
 * There is a factory method {@link #event(Object)} that let you set event attributes:
 * </p>
 *
 * <pre>{@code
 * {
 *   sse("/path", sse -> {
 *     sse.event("data")
 *         .id("id")
 *         .name("myevent")
 *         .retry(5000L)
 *         .send();
 *   });
 * }
 * }</pre>
 *
 * <h2>structured data</h2>
 * <p>
 * Beside raw/string data you can also send structured data, like <code>json</code>,
 * <code>xml</code>, etc..
 * </p>
 *
 * <p>
 * The next example will send two message one in <code>json</code> format and one in
 * <code>text/plain</code> format:
 * </p>
 * :
 *
 * <pre>{@code
 * {
 *   use(new MyJsonRenderer());
 *
 *   sse("/path", sse -> {
 *     MyObject object = ...
 *     sse.send(object, "json");
 *     sse.send(object, "plain");
 *   });
 * }
 * }</pre>
 *
 * <p>
 * Or if your need only one format, just:
 * </p>
 *
 * <pre>{@code
 * {
 *   use(new MyJsonRenderer());
 *
 *   sse("/path", sse -> {
 *     MyObject object = ...
 *     sse.send(object);
 *   }).produces("json"); // by default always send json
 * }
 * }</pre>
 *
 * <h2>request params</h2>
 * <p>
 * We provide request access via two arguments callback:
 * </p>
 *
 * <pre>{@code
 * {
 *   sse("/events/:id", (req, sse) -> {
 *     String id = req.param("id").value();
 *     MyObject object = findObject(id);
 *     sse.send(object);
 *   });
 * }
 * }</pre>
 *
 * <h2>connection lost</h2>
 * <p>
 * The {@link #onClose(CheckedRunnable)} callback allow you to clean and release resources on
 * connection close. A connection is closed by calling {@link #close()} or when the client/browser
 * close the connection.
 * </p>
 *
 * <pre>{@code
 * {
 *   sse("/events/:id", sse -> {
 *     sse.onClose(() -> {
 *       // clean up resources
 *     });
 *   });
 * }
 * }</pre>
 *
 * <p>
 * The close event will be generated if you try to send an event on a closed connection.
 * </p>
 *
 * <h2>keep alive time</h2>
 * <p>
 * The keep alive time feature can be used to prevent connections from timing out:
 * </p>
 *
 * <pre>{@code
 * {
 *   sse("/events/:id", sse -> {
 *     sse.keepAlive(15, TimeUnit.SECONDS);
 *   });
 * }
 * }</pre>
 *
 * <p>
 * The previous example will sent a <code>':'</code> message (empty comment) every 15 seconds to
 * keep the connection alive. If the client drop the connection, then the
 * {@link #onClose(CheckedRunnable)} event will be fired it.
 * </p>
 *
 * <p>
 * This feature is useful when you want to detect {@link #onClose(CheckedRunnable)} events without
 * waiting for the next time you send a new event. But for example, if your application already
 * generate events every 15s, then the use of keep alive is useless and you can avoid it.
 * </p>
 *
 * <h2>require</h2>
 * <p>
 * The {@link #require(Class)} methods let you access to application services:
 * </p>
 *
 * <pre>{@code
 * {
 *   sse("/events/:id", sse -> {
 *     MyService service = sse.require(MyService.class);
 *   });
 * }
 * }</pre>
 *
 * <h2>example</h2>
 * <p>
 * The next example will generate a new event every 60s. It recovers from a server shutdown by using
 * the {@link #lastEventId()} and clean resources on connection close.
 * </p>
 * <pre>{@code
 * {
 *   // creates an executor service
 *   ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
 *
 *   sse("/events", sse -> {
 *     // if we go down, recover from last event ID we sent. Otherwise, start from zero.
 *     int lastId = sse.lastEventId(Integer.class).orElse(0);
 *
 *     AtomicInteger next = new AtomicInteger(lastId);
 *
 *     // send events every 60s
 *     ScheduledFuture<?> future = executor.scheduleAtFixedRate(() -> {
 *        Integer id = next.incrementAndGet();
 *        Object data = findDataById(id);
 *
 *        // send data and id
 *        sse.event(data).id(id).send();
 *      }, 0, 60, TimeUnit.SECONDS);
 *
 *      // on connection lost, cancel 60s task
 *      sse.onClose(() -> {
 *       future.cancel(true);
 *      });
 *   });
 * }
 *
 * }</pre>
 *
 * @author edgar
 * @since 1.0.0.CR
 */
public abstract class Sse implements AutoCloseable {

  /**
   * Event representation of Server sent event.
   *
   * @author edgar
   * @since 1.0.0.CR
   */
  public static class Event {
    private Object id;

    private String name;

    private Object data;

    private Long retry;

    private MediaType type;

    private String comment;

    private Sse sse;

    private Event(final Sse sse, final Object data) {
      this.sse = sse;
      this.data = data;
    }

    /**
     * @return Event data (if any).
     */
    public Optional<Object> data() {
      return Optional.ofNullable(data);
    }

    /**
     * Event media type helps to render or format event data.
     *
     * @return Event media type (if any).
     */
    public Optional<MediaType> type() {
      return Optional.ofNullable(type);
    }

    /**
     * Set event media type. Useful for sengin json, xml, etc..
     *
     * @param type Media Type.
     * @return This event.
     */
    public Event type(final MediaType type) {
      this.type = requireNonNull(type, "Type is required.");
      return this;
    }

    /**
     * Set event media type. Useful for sengin json, xml, etc..
     *
     * @param type Media Type.
     * @return This event.
     */
    public Event type(final String type) {
      return type(MediaType.valueOf(type));
    }

    /**
     * @return Event id (if any).
     */
    public Optional<Object> id() {
      return Optional.ofNullable(id);
    }

    /**
     * Set event id.
     *
     * @param id An event id.
     * @return This event.
     */
    public Event id(final Object id) {
      this.id = requireNonNull(id, "Id is required.");
      return this;
    }

    /**
     * @return Event name (a.k.a type).
     */
    public Optional<String> name() {
      return Optional.ofNullable(name);
    }

    /**
     * Set event name (a.k.a type).
     *
     * @param name Event's name.
     * @return This event.
     */
    public Event name(final String name) {
      this.name = requireNonNull(name, "Name is required.");
      return this;
    }

    /**
     * Clients (browsers) will attempt to reconnect every 3 seconds. The retry option allow you to
     * specify the number of millis a browser should wait before try to reconnect.
     *
     * @param retry Retry value.
     * @param unit Time unit.
     * @return This event.
     */
    public Event retry(final int retry, final TimeUnit unit) {
      this.retry = unit.toMillis(retry);
      return this;
    }

    /**
     * Clients (browsers) will attempt to reconnect every 3 seconds. The retry option allow you to
     * specify the number of millis a browser should wait before try to reconnect.
     *
     * @param retry Retry value in millis.
     * @return This event.
     */
    public Event retry(final long retry) {
      this.retry = retry;
      return this;
    }

    /**
     * @return Event comment (if any).
     */
    public Optional<String> comment() {
      return Optional.ofNullable(comment);
    }

    /**
     * Set event comment.
     *
     * @param comment An event comment.
     * @return This event.
     */
    public Event comment(final String comment) {
      this.comment = requireNonNull(comment, "Comment is required.");
      return this;
    }

    /**
     * @return Retry event line (if any).
     */
    public Optional<Long> retry() {
      return Optional.ofNullable(retry);
    }

    /**
     * Send an event and optionally listen for success confirmation or error:
     *
     * <pre>{@code
     * sse.event(data).send().onSuccess(id -> {
     *   // success
     * }).onFailure(cause -> {
     *   // handle error
     * });
     * }</pre>
     *
     * @return A future callback.
     */
    public Future<Optional<Object>> send() {
      Future<Optional<Object>> future = sse.send(this);
      this.id = null;
      this.name = null;
      this.data = null;
      this.type = null;
      this.sse = null;
      return future;
    }

  }

  /**
   * Server-sent event handler.
   *
   * @author edgar
   * @since 1.0.0.CR
   */
  public interface Handler extends Route.Filter {

    @Override
    default void handle(final Request req, final Response rsp, final Chain chain) throws Throwable {
      Sse sse = req.require(Sse.class);
      String path = req.path();
      rsp.send(new Deferred(deferred -> {
        try {
          sse.handshake(req, () -> {
            Try.run(() -> handle(req, sse)).onSuccess(deferred::resolve).onFailure(ex -> {
              deferred.reject(ex);
              Logger log = LoggerFactory.getLogger(Sse.class);
              log.error("execution of {} resulted in error", path, ex);
            });
          });
        } catch (Exception ex) {
          deferred.reject(ex);
        }
      }));
    }

    /**
     * Event handler.
     *
     * @param req Current request.
     * @param sse Sse object.
     * @throws Exception If something goes wrong.
     */
    void handle(Request req, Sse sse) throws Exception;
  }

  /**
   * Single argument event handler.
   *
   * @author edgar
   * @since 1.0.0.CR
   */
  public interface Handler1 extends Handler {
    @Override
    default void handle(final Request req, final Sse sse) throws Exception {
      handle(sse);
    }

    void handle(Sse sse) throws Exception;
  }

  /* package */static class KeepAlive implements Runnable {

    /** The logging system. */
    private final Logger log = LoggerFactory.getLogger(Sse.class);

    private Sse sse;

    private long retry;

    public KeepAlive(final Sse sse, final long retry) {
      this.sse = sse;
      this.retry = retry;
    }

    @Override
    public void run() {
      String sseId = sse.id();
      log.debug("running heart beat for {}", sseId);
      Try.run(() -> sse.send(Optional.of(sseId), HEART_BEAT).future().onFailure(ex -> {
        log.debug("connection lost for {}", sseId);
        sse.fireCloseEvent();
        Try.run(sse::close);
      }).onSuccess(id -> {
        log.debug("reschedule heart beat for {}", id);
        // reschedule
        sse.keepAlive(retry);
      }));
    }

  }

  /** Keep alive scheduler. */
  private static final ScheduledExecutorService scheduler = Executors
      .newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "sse-heartbeat");
        thread.setDaemon(true);
        return thread;
      });

  /** Empty comment. */
  static final byte[] HEART_BEAT = ":\n".getBytes(StandardCharsets.UTF_8);

  /** The logging system. */
  protected final Logger log = LoggerFactory.getLogger(Sse.class);

  private Injector injector;

  private List<Renderer> renderers;

  private final String id;

  private List<MediaType> produces;

  private Map<String, Object> locals;

  private AtomicReference<CheckedRunnable> onclose = new AtomicReference<>(null);

  private Mutant lastEventId;

  private boolean closed;

  private Locale locale;

  public Sse() {
    id = UUID.randomUUID().toString();
  }

  protected void handshake(final Request req, final Runnable handler) throws Exception {
    this.injector = req.require(Injector.class);
    this.renderers = ImmutableList.copyOf(injector.getInstance(Renderer.KEY));
    this.produces = req.route().produces();
    this.locals = req.attributes();
    this.lastEventId = req.header("Last-Event-ID");
    this.locale = req.locale();
    handshake(handler);
  }

  protected abstract void handshake(Runnable handler) throws Exception;

  /**
   * A unique ID (like a session ID).
   *
   * @return Sse unique ID (like a session ID).
   */
  public String id() {
    return id;
  }

  /**
   * Server sent event will send a Last-Event-ID header if the server goes down.
   *
   * @return Last event id.
   */
  public Optional<String> lastEventId() {
    return lastEventId(String.class);
  }

  /**
   * Server sent event will send a Last-Event-ID header if the server goes down.
   *
   * @param type Last event id type.
   * @param <T> Event id type.
   * @return Last event id.
   */
  public <T> Optional<T> lastEventId(final Class<T> type) {
    return lastEventId.toOptional(type);
  }

  /**
   * Listen for connection close (usually client drop the connection). This method is useful for
   * resources cleanup.
   *
   * @param task Task to run.
   * @return This instance.
   */
  public Sse onClose(final CheckedRunnable task) {
    onclose.set(task);
    return this;
  }

  /**
   * Send an event and set media type.
   *
   * <pre>{@code
   *   sse.send(new MyObject(), "json");
   * }</pre>
   *
   * On success the {@link Future#onSuccess(java.util.function.Consumer)} callback will be executed:
   *
   * <pre>{@code
   *   sse.send(new MyObject(), "json").onSuccess(id -> {
   *     //
   *   });
   * }</pre>
   *
   * The <code>id</code> of the success callback correspond to the {@link Event#id()}.
   *
   * On error the {@link Future#onFailure(java.util.function.Consumer)} callback will be executed:
   *
   * <pre>{@code
   *   sse.send(new MyObject(), "json").onFailure(cause -> {
   *     //
   *   });
   * }</pre>
   *
   * @param data Event data.
   * @param type Media type, like: json, xml.
   * @return A future. The success callback contains the {@link Event#id()}.
   */
  public Future<Optional<Object>> send(final Object data, final String type) {
    return send(data, MediaType.valueOf(type));
  }

  /**
   * Send an event and set media type.
   *
   * <pre>{@code
   *   sse.send(new MyObject(), "json");
   * }</pre>
   *
   * On success the {@link Future#onSuccess(java.util.function.Consumer)} callback will be executed:
   *
   * <pre>{@code
   *   sse.send(new MyObject(), "json").onSuccess(id -> {
   *     //
   *   });
   * }</pre>
   *
   * The <code>id</code> of the success callback correspond to the {@link Event#id()}.
   *
   * On error the {@link Future#onFailure(java.util.function.Consumer)} callback will be executed:
   *
   * <pre>{@code
   *   sse.send(new MyObject(), "json").onFailure(cause -> {
   *     //
   *   });
   * }</pre>
   *
   * @param data Event data.
   * @param type Media type, like: json, xml.
   * @return A future. The success callback contains the {@link Event#id()}.
   */
  public Future<Optional<Object>> send(final Object data, final MediaType type) {
    return event(data).type(type).send();
  }

  /**
   * Send an event.
   *
   * <pre>{@code
   *   sse.send(new MyObject());
   * }</pre>
   *
   * On success the {@link Future#onSuccess(java.util.function.Consumer)} callback will be executed:
   *
   * <pre>{@code
   *   sse.send(new MyObject()).onSuccess(id -> {
   *     //
   *   });
   * }</pre>
   *
   * The <code>id</code> of the success callback correspond to the {@link Event#id()}.
   *
   * On error the {@link Future#onFailure(java.util.function.Consumer)} callback will be executed:
   *
   * <pre>{@code
   *   sse.send(new MyObject()).onFailure(cause -> {
   *     //
   *   });
   * }</pre>
   *
   * @param data Event data.
   * @return A future. The success callback contains the {@link Event#id()}.
   */
  public Future<Optional<Object>> send(final Object data) {
    return event(data).send();
  }

  /**
   * Factory method for creating {@link Event} instances.
   *
   * Please note event won't be sent unless you call {@link Event#send()}:
   *
   * <pre>{@code
   *   sse.event(new MyObject()).send();
   * }</pre>
   *
   * The factory allow you to set event attributes:
   *
   * <pre>{@code
   *   // send data
   *   MyObject data = ...;
   *   sse.event(data).send();
   *
   *   // send data with event name
   *   sse.event(data).name("myevent").send();
   *
   *   // send data with event name and id
   *   sse.event(data).name("myevent").id(id).send();
   *
   *   // send data with event name, id and retry interval
   *   sse.event(data).name("myevent").id(id).retry(1500).send();
   * }</pre>
   *
   * @param data Event data.
   * @return A new event.
   */
  public Event event(final Object data) {
    return new Event(this, data);
  }

  /**
   * Ask Guice for the given type.
   *
   * @param type A service type.
   * @param <T> Service type.
   * @return A ready to use object.
   */
  public <T> T require(final Class<T> type) {
    return require(Key.get(type));
  }

  /**
   * Ask Guice for the given type.
   *
   * @param name A service name.
   * @param type A service type.
   * @param <T> Service type.
   * @return A ready to use object.
   */
  public <T> T require(final String name, final Class<T> type) {
    return require(Key.get(type, Names.named(name)));
  }

  /**
   * Ask Guice for the given type.
   *
   * @param type A service type.
   * @param <T> Service type.
   * @return A ready to use object.
   */
  public <T> T require(final TypeLiteral<T> type) {
    return require(Key.get(type));
  }

  /**
   * Ask Guice for the given type.
   *
   * @param key A service key.
   * @param <T> Service type.
   * @return A ready to use object.
   */
  public <T> T require(final Key<T> key) {
    return injector.getInstance(key);
  }

  /**
   * The keep alive time can be used to prevent connections from timing out:
   *
   * <pre>{@code
   * {
   *   sse("/events/:id", sse -> {
   *     sse.keepAlive(15, TimeUnit.SECONDS);
   *   });
   * }
   * }</pre>
   *
   * <p>
   * The previous example will sent a <code>':'</code> message (empty comment) every 15 seconds to
   * keep the connection alive. If the client drop the connection, then the
   * {@link #onClose(CheckedRunnable)} event will be fired it.
   * </p>
   *
   * <p>
   * This feature is useful when you want to detect {@link #onClose(CheckedRunnable)} events without
   * waiting until you send a new event. But for example, if your application already generate
   * events
   * every 15s, then the use of keep alive is useless and you should avoid it.
   * </p>
   *
   * @param time Keep alive time.
   * @param unit Time unit.
   * @return This instance.
   */
  public Sse keepAlive(final int time, final TimeUnit unit) {
    return keepAlive(unit.toMillis(time));
  }

  /**
   * The keep alive time can be used to prevent connections from timing out:
   *
   * <pre>{@code
   * {
   *   sse("/events/:id", sse -> {
   *     sse.keepAlive(15, TimeUnit.SECONDS);
   *   });
   * }
   * }</pre>
   *
   * <p>
   * The previous example will sent a <code>':'</code> message (empty comment) every 15 seconds to
   * keep the connection alive. If the client drop the connection, then the
   * {@link #onClose(CheckedRunnable)} event will be fired it.
   * </p>
   *
   * <p>
   * This feature is useful when you want to detect {@link #onClose(CheckedRunnable)} events without
   * waiting until you send a new event. But for example, if your application already generate
   * events
   * every 15s, then the use of keep alive is useless and you should avoid it.
   * </p>
   *
   * @param millis Keep alive time in millis.
   * @return This instance.
   */
  public Sse keepAlive(final long millis) {
    scheduler.schedule(new KeepAlive(this, millis), millis, TimeUnit.MILLISECONDS);
    return this;
  }

  /**
   * Close the connection and fire an {@link #onClose(CheckedRunnable)} event.
   */
  @Override
  public final void close() throws Exception {
    closeAll();
  }

  private void closeAll() {
    synchronized (this) {
      if (!closed) {
        closed = true;
        fireCloseEvent();
        closeInternal();
      }
    }
  }

  protected abstract void closeInternal();

  protected abstract Promise<Optional<Object>> send(Optional<Object> id, byte[] data);

  protected void ifClose(final Throwable cause) {
    if (shouldClose(cause)) {
      closeAll();
    }
  }

  protected void fireCloseEvent() {
    CheckedRunnable task = onclose.getAndSet(null);
    if (task != null) {
      Try.run(task).onFailure(ex -> log.error("close callback resulted in error", ex));
    }
  }

  protected boolean shouldClose(final Throwable ex) {
    if (ex instanceof IOException) {
      // is there a better way?
      boolean brokenPipe = Optional.ofNullable(ex.getMessage())
          .map(m -> m.toLowerCase().contains("broken pipe"))
          .orElse(false);
      return brokenPipe || ex instanceof ClosedChannelException;
    }
    return false;
  }

  private Future<Optional<Object>> send(final Event event) {
    List<MediaType> produces = event.type().<List<MediaType>> map(ImmutableList::of)
        .orElse(this.produces);
    SseRenderer ctx = new SseRenderer(renderers, produces, StandardCharsets.UTF_8, locale, locals);
    return Try.of(() -> {
      byte[] bytes = ctx.format(event);
      return send(event.id(), bytes);
    }).recover(cause -> {
      Promise<Optional<Object>> promise = Promise.make(MoreExecutors.newDirectExecutorService());
      promise.failure(cause);
      return promise;
    }).get().future();
  }

}
