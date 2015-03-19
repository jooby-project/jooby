package org.jooby;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Err.Default.class, LoggerFactory.class })
public class DefaultErrHandlerTest {

  @SuppressWarnings({"unchecked" })
  @Test
  public void handleNoErrMessage() throws Exception {
    Exception ex = new Exception();

    StringWriter writer = new StringWriter();
    ex.printStackTrace(new PrintWriter(writer));
    String[] stacktrace = writer.toString().replace("\r", "").split("\\n");

    new MockUnit(Request.class, Response.class)
        .expect(
            unit -> {
              Logger log = unit.mock(Logger.class);
              log.error("execution of: GET /path resulted in exception", ex);

              unit.mockStatic(LoggerFactory.class);
              expect(LoggerFactory.getLogger(Err.class)).andReturn(log);

              Mutant referer = unit.mock(Mutant.class);
              expect(referer.toOptional(String.class)).andReturn(Optional.of("referer"));

              Request req = unit.get(Request.class);

              expect(req.path()).andReturn("/path");
              expect(req.method()).andReturn("GET");
              expect(req.header("referer")).andReturn(referer);

              Response rsp = unit.get(Response.class);

              rsp.send(unit.capture(Result.class));
              expect(rsp.status()).andReturn(Optional.of(Status.SERVER_ERROR));
            })
        .run(unit -> {

          Request req = unit.get(Request.class);
          Response rsp = unit.get(Response.class);

          new Err.Default().handle(req, rsp, ex);
        }, unit -> {
          Result result = unit.captured(Result.class).iterator().next();
          View view = (View) result.get(ImmutableList.of(MediaType.html)).get();
          assertEquals("/err", view.name());
          checkErr(stacktrace, "Server Error", (Map<String, Object>) view.model()
              .get("err"));

          Object hash = result.get(MediaType.ALL).get();
          assertEquals(0, ((Map<String, Object>) hash).size());
        });
  }

  @SuppressWarnings({"unchecked" })
  @Test
  public void handleWithErrMessage() throws Exception {
    Exception ex = new Exception("Something something dark");

    StringWriter writer = new StringWriter();
    ex.printStackTrace(new PrintWriter(writer));
    String[] stacktrace = writer.toString().replace("\r", "").split("\\n");

    new MockUnit(Request.class, Response.class)
        .expect(
            unit -> {
              Logger log = unit.mock(Logger.class);
              log.error("execution of: GET /path resulted in exception", ex);

              unit.mockStatic(LoggerFactory.class);
              expect(LoggerFactory.getLogger(Err.class)).andReturn(log);

              Mutant referer = unit.mock(Mutant.class);
              expect(referer.toOptional(String.class)).andReturn(Optional.of("referer"));

              Request req = unit.get(Request.class);

              expect(req.path()).andReturn("/path");
              expect(req.method()).andReturn("GET");
              expect(req.header("referer")).andReturn(referer);

              Response rsp = unit.get(Response.class);

              rsp.send(unit.capture(Result.class));
              expect(rsp.status()).andReturn(Optional.of(Status.SERVER_ERROR));
            })
        .run(unit -> {

          Request req = unit.get(Request.class);
          Response rsp = unit.get(Response.class);

          new Err.Default().handle(req, rsp, ex);
        }, unit -> {
          Result result = unit.captured(Result.class).iterator().next();
          View view = (View) result.get(ImmutableList.of(MediaType.html)).get();
          assertEquals("/err", view.name());
          checkErr(stacktrace, "Something something dark", (Map<String, Object>) view.model()
              .get("err"));

          Object hash = result.get(MediaType.ALL).get();
          assertEquals(0, ((Map<String, Object>) hash).size());
        });
  }

  private void checkErr(final String[] stacktrace, final String message,
      final Map<String, Object> err) {
    assertEquals(message, err.remove("message"));
    assertEquals("Server Error", err.remove("reason"));
    assertEquals(500, err.remove("status"));
    assertArrayEquals(stacktrace, (String[]) err.remove("stacktrace"));
    assertEquals("referer", err.remove("referer"));
    assertEquals(err.toString(), 0, err.size());
  }

}
