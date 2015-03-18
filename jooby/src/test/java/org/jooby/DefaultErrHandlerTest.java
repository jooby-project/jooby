package org.jooby;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jooby.Response.Formatter;
import org.jooby.util.ExSupplier;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Err.Default.class, LoggerFactory.class })
public class DefaultErrHandlerTest {

  @SuppressWarnings({"unchecked", "rawtypes" })
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
              expect(req.verb()).andReturn(Verb.GET);
              expect(req.header("referer")).andReturn(referer);

              Formatter formatter = unit.mock(Formatter.class);
              expect(formatter.when(eq(MediaType.html), unit.capture(ExSupplier.class))).andReturn(
                  formatter);
              expect(formatter.when(eq(MediaType.all), unit.capture(ExSupplier.class))).andReturn(
                  formatter);
              formatter.send();

              Response rsp = unit.get(Response.class);

              expect(rsp.status()).andReturn(Optional.of(Status.SERVER_ERROR));
              expect(rsp.format()).andReturn(formatter);
            })
        .run(unit -> {

          Request req = unit.get(Request.class);
          Response rsp = unit.get(Response.class);

          new Err.Default().handle(req, rsp, ex);
        }, unit -> {
          List<ExSupplier> suppliers = unit.captured(ExSupplier.class);
          View view = (View) suppliers.get(0).get();
          assertEquals("/err", view.name());
          checkErr(stacktrace, "Server Error", (Map<String, Object>) view.model().get("err"));

          assertEquals(0, ((Map<String, Object>) suppliers.get(1).get()).size());
        });
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
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
              expect(req.verb()).andReturn(Verb.GET);
              expect(req.header("referer")).andReturn(referer);

              Formatter formatter = unit.mock(Formatter.class);
              expect(formatter.when(eq(MediaType.html), unit.capture(ExSupplier.class))).andReturn(
                  formatter);
              expect(formatter.when(eq(MediaType.all), unit.capture(ExSupplier.class))).andReturn(
                  formatter);
              formatter.send();

              Response rsp = unit.get(Response.class);

              expect(rsp.status()).andReturn(Optional.of(Status.SERVER_ERROR));
              expect(rsp.format()).andReturn(formatter);
            })
        .run(unit -> {

          Request req = unit.get(Request.class);
          Response rsp = unit.get(Response.class);

          new Err.Default().handle(req, rsp, ex);
        }, unit -> {
          List<ExSupplier> suppliers = unit.captured(ExSupplier.class);
          View view = (View) suppliers.get(0).get();
          assertEquals("/err", view.name());
          checkErr(stacktrace, "Something something dark", (Map<String, Object>) view.model().get("err"));

          assertEquals(0, ((Map<String, Object>) suppliers.get(1).get()).size());
        });
  }

  private void checkErr(final String[] stacktrace, final String message, final Map<String, Object> err) {
    assertEquals(message, err.remove("message"));
    assertEquals("Server Error", err.remove("reason"));
    assertEquals(500, err.remove("status"));
    assertArrayEquals(stacktrace, (String[]) err.remove("stacktrace"));
    assertEquals("referer", err.remove("referer"));
    assertEquals(err.toString(), 0, err.size());
  }

}
