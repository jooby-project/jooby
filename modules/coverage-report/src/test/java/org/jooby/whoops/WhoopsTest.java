package org.jooby.whoops;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jooby.Err;
import org.jooby.Err.Handler;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.test.MockUnit;
import org.jooby.whoops.SourceLocator.Source;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableMap;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Whoops.class, Throwable.class, StackTraceElement.class })
public class WhoopsTest {

  @SuppressWarnings("rawtypes")
  @Test
  public void shouldFindClass() {
    Optional<Class> clazz = Whoops.findClass(getClass().getClassLoader(),
        WhoopsTest.class.getName());
    assertEquals(true, clazz.isPresent());
  }

  @SuppressWarnings("rawtypes")
  @Test
  public void shouldIgnoreMissingClass() {
    Optional<Class> clazz = Whoops.findClass(getClass().getClassLoader(), "XxNotFoUnd");
    assertEquals(false, clazz.isPresent());
  }

  @Test
  public void locationOf() {
    assertTrue(Whoops.locationOf(Whoops.class) + " file does not exist", new File(Whoops.locationOf(Whoops.class)).exists());

    assertEquals("rt.jar", Whoops.locationOf(Object.class));
  }

  @Test
  public void frame() throws Exception {
    int line = 15;
    int[] range = {5, 15 };
    String className = WhoopsApp.class.getName();
    String fileName = WhoopsApp.class.getSimpleName() + ".java";
    String methodName = "main";
    String src = "{...}";
    String message = "Something broken!";
    new MockUnit(SourceLocator.class, Source.class, Throwable.class, StackTraceElement.class)
        .expect(unit -> {
          Throwable cause = unit.get(Throwable.class);
          expect(cause.getMessage()).andReturn(message);
        })
        .expect(unit -> {
          StackTraceElement ste = unit.get(StackTraceElement.class);
          expect(ste.getLineNumber()).andReturn(line);
          expect(ste.getClassName()).andReturn(className);
          expect(ste.getFileName()).andReturn(fileName);
          expect(ste.getMethodName()).andReturn(methodName);
        })
        .expect(unit -> {
          Source source = unit.get(Source.class);
          expect(source.range(line, 10)).andReturn(range);
          expect(source.source(range[0], range[1])).andReturn(src);
          expect(source.getPath()).andReturn(Paths.get(fileName));

          SourceLocator locator = unit.get(SourceLocator.class);
          expect(locator.source(className)).andReturn(source);
        })
        .run(unit -> {
          Map<String, Object> frame = Whoops.frame(getClass().getClassLoader(),
              unit.get(SourceLocator.class),
              unit.get(Throwable.class),
              unit.get(StackTraceElement.class));

          assertEquals("WhoopsApp.java", frame.get("fileName"));
          assertEquals("main", frame.get("methodName"));
          assertEquals(15, frame.get("lineNumber"));
          assertEquals(6, frame.get("lineStart"));
          assertEquals(10, frame.get("lineNth"));
          assertEquals("target/test-classes", frame.get("location"));
          assertEquals("{...}", frame.get("source"));
          assertEquals("WhoopsApp", frame.get("type"));
          assertEquals(
              Arrays.asList(ImmutableMap.of("context",
                  unit.get(Throwable.class).getClass().getName(), "text", message)),
              frame.get("comments"));
        });
  }

  @Test
  public void frames() throws Exception {
    int line = 15;
    int[] range = {5, 15 };
    String className = WhoopsApp.class.getName();
    String fileName = WhoopsApp.class.getSimpleName() + ".java";
    String methodName = "main";
    String src = "{...}";
    String message = "Something broken!";
    new MockUnit(SourceLocator.class, Source.class, Throwable.class, StackTraceElement.class)
        .expect(unit -> {
          StackTraceElement ignored = unit.mock(StackTraceElement.class);
          expect(ignored.getClassName()).andReturn("org.jooby.internal.HttpHandlerImpl");

          StackTraceElement[] stacktrace = {unit.get(StackTraceElement.class), ignored };

          Throwable cause = unit.get(Throwable.class);
          expect(cause.getMessage()).andReturn(message);
          expect(cause.getStackTrace()).andReturn(stacktrace);
        })
        .expect(unit -> {
          StackTraceElement ste = unit.get(StackTraceElement.class);
          expect(ste.getLineNumber()).andReturn(line);
          expect(ste.getClassName()).andReturn(className).times(2);
          expect(ste.getFileName()).andReturn(fileName);
          expect(ste.getMethodName()).andReturn(methodName);
        })
        .expect(unit -> {
          Source source = unit.get(Source.class);
          expect(source.range(line, 10)).andReturn(range);
          expect(source.source(range[0], range[1])).andReturn(src);
          expect(source.getPath()).andReturn(Paths.get(fileName));

          SourceLocator locator = unit.get(SourceLocator.class);
          expect(locator.source(className)).andReturn(source);
        })
        .run(unit -> {
          List<Map<String, Object>> frames = Whoops.frames(getClass().getClassLoader(),
              unit.get(SourceLocator.class),
              unit.get(Throwable.class));
          Map<String, Object> frame = frames.get(0);

          assertEquals("WhoopsApp.java", frame.get("fileName"));
          assertEquals("main", frame.get("methodName"));
          assertEquals(15, frame.get("lineNumber"));
          assertEquals(6, frame.get("lineStart"));
          assertEquals(10, frame.get("lineNth"));
          assertEquals("target/test-classes", frame.get("location"));
          assertEquals("{...}", frame.get("source"));
          assertEquals("WhoopsApp", frame.get("type"));
          assertEquals(
              Arrays.asList(ImmutableMap.of("context",
                  unit.get(Throwable.class).getClass().getName(), "text", message)),
              frame.get("comments"));
        });
  }

  @Test
  public void tryPage() throws Exception {
    Err err = new Err(500);
    Throwable cause = new IllegalStateException();
    new MockUnit(Err.Handler.class, Request.class, Response.class, Logger.class)
        .expect(unit -> {
          Handler handler = unit.get(Err.Handler.class);
          handler.handle(unit.get(Request.class), unit.get(Response.class), err);
          expectLastCall().andThrow(cause);

          Logger log = unit.get(Logger.class);
          log.debug("execution of pretty err page resulted in exception", cause);
        })
        .run(unit -> {
          Whoops.tryPage(unit.get(Err.Handler.class), unit.get(org.slf4j.Logger.class))
              .handle(unit.get(Request.class), unit.get(Response.class), err);
        });
  }

}
