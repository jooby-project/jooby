package org.jooby.metrics;

import static org.easymock.EasyMock.expect;

import java.io.ByteArrayOutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

import org.jooby.MediaType;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Status;
import org.jooby.test.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.codahale.metrics.jvm.ThreadDump;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ThreadDumpHandler.class, ThreadDump.class, ManagementFactory.class,
    ByteArrayOutputStream.class })
public class ThreadDumpHandlerTest {

  @Test
  public void dump() throws Exception {
    byte[] bytes = new byte[0];
    new MockUnit(Request.class, Response.class, ByteArrayOutputStream.class)
        .expect(unit -> {
          ThreadMXBean tmxb = unit.mock(ThreadMXBean.class);
          unit.mockStatic(ManagementFactory.class);
          expect(ManagementFactory.getThreadMXBean()).andReturn(tmxb);

          ByteArrayOutputStream stream = unit.constructor(ByteArrayOutputStream.class).build();
          expect(stream.toByteArray()).andReturn(bytes);

          ThreadDump td = unit.constructor(ThreadDump.class)
              .args(ThreadMXBean.class)
              .build(tmxb);

          td.dump(stream);

          unit.registerMock(ThreadDump.class, td);
        })
        .expect(unit -> {
          Response rsp = unit.get(Response.class);
          expect(rsp.type(MediaType.plain)).andReturn(rsp);
          expect(rsp.status(Status.OK)).andReturn(rsp);
          expect(rsp.header("Cache-Control", "must-revalidate,no-cache,no-store")).andReturn(rsp);
          rsp.send(bytes);
        })
        .run(unit -> {
          new ThreadDumpHandler().handle(unit.get(Request.class), unit.get(Response.class));
        });
  }

  @Test
  public void nodump() throws Exception {
    Object data = "Sorry your runtime environment does not allow to dump threads.";
    new MockUnit(Request.class, Response.class)
        .expect(unit -> {
          unit.mockStatic(ManagementFactory.class);
          expect(ManagementFactory.getThreadMXBean())
              .andThrow(new IllegalStateException("intentional err"));

        })
        .expect(unit -> {
          Response rsp = unit.get(Response.class);
          expect(rsp.type(MediaType.plain)).andReturn(rsp);
          expect(rsp.status(Status.NOT_IMPLEMENTED)).andReturn(rsp);
          expect(rsp.header("Cache-Control", "must-revalidate,no-cache,no-store")).andReturn(rsp);
          rsp.send(data);
        })
        .run(unit -> {
          new ThreadDumpHandler().handle(unit.get(Request.class), unit.get(Response.class));
        });
  }

}
