package org.jooby.servlet;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import org.jooby.Jooby;
import org.jooby.test.MockUnit;
import org.junit.Test;

public class ServerInitializerTest {

  @SuppressWarnings({"rawtypes", "unchecked" })
  @Test
  public void contextInitialized() throws Exception {
    new MockUnit(ServletContextEvent.class)
        .expect(unit -> {
          Class appClass = Jooby.class;
          String appClassname = appClass.getName();

          ClassLoader loader = unit.mock(ClassLoader.class);
          expect(loader.loadClass(appClassname)).andReturn(appClass);

          ServletContext ctx = unit.mock(ServletContext.class);
          expect(ctx.getInitParameter("application.class")).andReturn(appClassname);
          expect(ctx.getClassLoader()).andReturn(loader);
          expect(ctx.getContextPath()).andReturn("/");
          ctx.setAttribute(eq(Jooby.class.getName()), isA(Jooby.class));

          ServletContextEvent sce = unit.get(ServletContextEvent.class);
          expect(sce.getServletContext()).andReturn(ctx);
        })
        .run(unit -> {
          try {
          ServerInitializer initializer = new ServerInitializer();
          initializer.contextInitialized(unit.get(ServletContextEvent.class));
          } catch (Throwable ex) {
            ex.printStackTrace();
          }
        });
  }

  @SuppressWarnings({"rawtypes" })
  @Test(expected = IllegalStateException.class)
  public void contextInitializedShouldReThrowException() throws Exception {
    new MockUnit(ServletContextEvent.class)
        .expect(
            unit -> {
              Class appClass = Jooby.class;
              String appClassname = appClass.getName();

              ClassLoader loader = unit.mock(ClassLoader.class);
              expect(loader.loadClass(appClassname)).andThrow(
                  new ClassNotFoundException("intentional err"));

              ServletContext ctx = unit.mock(ServletContext.class);
              expect(ctx.getInitParameter("application.class")).andReturn(appClassname);
              expect(ctx.getClassLoader()).andReturn(loader);
              expect(ctx.getContextPath()).andReturn("/");
              ctx.setAttribute(eq(Jooby.class.getName()), isA(Jooby.class));

              ServletContextEvent sce = unit.get(ServletContextEvent.class);
              expect(sce.getServletContext()).andReturn(ctx);
            })
        .run(unit -> {
          ServerInitializer initializer = new ServerInitializer();
          initializer.contextInitialized(unit.get(ServletContextEvent.class));
        });
  }

  @SuppressWarnings({"rawtypes" })
  @Test
  public void contextDestroyed() throws Exception {
    new MockUnit(ServletContextEvent.class)
        .expect(unit -> {
          Class appClass = Jooby.class;
          String appClassname = appClass.getName();

          Jooby app = unit.mock(Jooby.class);
          app.stop();

          ServletContext ctx = unit.mock(ServletContext.class);
          expect(ctx.getAttribute(appClassname)).andReturn(app);

          ServletContextEvent sce = unit.get(ServletContextEvent.class);
          expect(sce.getServletContext()).andReturn(ctx);
        })
        .run(unit -> {
          new ServerInitializer().contextDestroyed(unit.get(ServletContextEvent.class));
        });
  }

  @SuppressWarnings({"rawtypes" })
  @Test
  public void contextDestroyedShouldIgnoreMissingAttr() throws Exception {
    new MockUnit(ServletContextEvent.class)
        .expect(unit -> {
          Class appClass = Jooby.class;
          String appClassname = appClass.getName();

          ServletContext ctx = unit.mock(ServletContext.class);
          expect(ctx.getAttribute(appClassname)).andReturn(null);

          ServletContextEvent sce = unit.get(ServletContextEvent.class);
          expect(sce.getServletContext()).andReturn(ctx);
        })
        .run(unit -> {
          new ServerInitializer().contextDestroyed(unit.get(ServletContextEvent.class));
        });
  }
}
