package org.jooby;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

import com.typesafe.config.Config;
import org.jooby.test.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Err.DefHandler.class, LoggerFactory.class })
public class DefaultErrHandlerTest {

  @SuppressWarnings({"unchecked" })
  @Test
  public void handleNoErrMessage() throws Exception {
    Err ex = new Err(500);

    StringWriter writer = new StringWriter();
    ex.printStackTrace(new PrintWriter(writer));
    String[] stacktrace = writer.toString().replace("\r", "").split("\\n");

    new MockUnit(Request.class, Response.class, Route.class, Config.class, Env.class)
        .expect(unit -> {
          Logger log = unit.mock(Logger.class);
          log.error("execution of: {}{} resulted in exception\nRoute:\n{}\n\nStacktrace:", "GET",
              "/path", "route", ex);

          unit.mockStatic(LoggerFactory.class);
          expect(LoggerFactory.getLogger(Err.class)).andReturn(log);

          Route route = unit.get(Route.class);
          expect(route.print(6)).andReturn("route");

          Config conf = unit.get(Config.class);
          Env env = unit.get(Env.class);
          expect(env.name()).andReturn("dev");

          Request req = unit.get(Request.class);

          expect(req.require(Config.class)).andReturn(conf);
          expect(req.require(Env.class)).andReturn(env);
          expect(req.path()).andReturn("/path");
          expect(req.method()).andReturn("GET");
          expect(req.route()).andReturn(route);

          Response rsp = unit.get(Response.class);

          rsp.send(unit.capture(Result.class));
        })
        .run(unit -> {

          Request req = unit.get(Request.class);
          Response rsp = unit.get(Response.class);

          new Err.DefHandler().handle(req, rsp, ex);
        }, unit -> {
          Result result = unit.captured(Result.class).iterator().next();
          View view = (View) result.ifGet(ImmutableList.of(MediaType.html)).get();
          assertEquals("err", view.name());
          checkErr(stacktrace, "Server Error(500)", (Map<String, Object>) view.model()
              .get("err"));

          Object hash = result.ifGet(MediaType.ALL).get();
          assertEquals(4, ((Map<String, Object>) hash).size());
        });
  }

  @SuppressWarnings({"unchecked" })
  @Test
  public void handleWithErrMessage() throws Exception {
    Err ex = new Err(500, "Something something dark");

    StringWriter writer = new StringWriter();
    ex.printStackTrace(new PrintWriter(writer));
    String[] stacktrace = writer.toString().replace("\r", "").split("\\n");

    new MockUnit(Request.class, Response.class, Route.class, Env.class, Config.class)
        .expect(unit -> {
          Logger log = unit.mock(Logger.class);
          log.error("execution of: {}{} resulted in exception\nRoute:\n{}\n\nStacktrace:", "GET",
              "/path", "route", ex);

          unit.mockStatic(LoggerFactory.class);
          expect(LoggerFactory.getLogger(Err.class)).andReturn(log);

          Route route = unit.get(Route.class);
          expect(route.print(6)).andReturn("route");

          Config conf = unit.get(Config.class);
          Env env = unit.get(Env.class);
          expect(env.name()).andReturn("dev");

          Request req = unit.get(Request.class);

          expect(req.require(Config.class)).andReturn(conf);
          expect(req.require(Env.class)).andReturn(env);

          expect(req.path()).andReturn("/path");
          expect(req.method()).andReturn("GET");
          expect(req.route()).andReturn(route);

          Response rsp = unit.get(Response.class);

          rsp.send(unit.capture(Result.class));
        })
        .run(
            unit -> {

              Request req = unit.get(Request.class);
              Response rsp = unit.get(Response.class);

              new Err.DefHandler().handle(req, rsp, ex);
            },
            unit -> {
              Result result = unit.captured(Result.class).iterator().next();
              View view = (View) result.ifGet(ImmutableList.of(MediaType.html)).get();
              assertEquals("err", view.name());
              checkErr(stacktrace, "Server Error(500): Something something dark",
                  (Map<String, Object>) view.model()
                      .get("err"));

              Object hash = result.ifGet(MediaType.ALL).get();
              assertEquals(4, ((Map<String, Object>) hash).size());
            });
  }

  private void checkErr(final String[] stacktrace, final String message,
      final Map<String, Object> err) {
    assertEquals(message, err.remove("message"));
    assertEquals("Server Error", err.remove("reason"));
    assertEquals(500, err.remove("status"));
    assertArrayEquals(stacktrace, (String[]) err.remove("stacktrace"));
    assertEquals(err.toString(), 0, err.size());
  }

}
