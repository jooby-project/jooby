package org.jooby.servlet;

import java.util.Set;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;

import org.jooby.Jooby;
import org.jooby.spi.Dispatcher;
import org.jooby.spi.Server;

import com.typesafe.config.Config;

public class ServerInitializer implements ServletContainerInitializer {

  private final Server NOOP = new Server() {
    @Override
    public void stop() throws Exception {
    }

    @Override
    public void start() throws Exception {
    }

    @Override
    public void join() throws InterruptedException {

    }
  };

  @Override
  public void onStartup(final Set<Class<?>> c, final ServletContext ctx) throws ServletException {
    try {
      Class<?> appClass = getClass().getClassLoader()
          .loadClass(ctx.getInitParameter("application"));

      Jooby app = (Jooby) appClass.newInstance();

      app.use((env, conf, binder) -> binder.bind(Server.class).toInstance(NOOP));

      ctx.addListener(new ServletContextListener() {

        @Override
        public void contextInitialized(final ServletContextEvent sce) {
        }

        @Override
        public void contextDestroyed(final ServletContextEvent sce) {
          app.stop();
        }
      });

      app.start();

      Config config = app.require(Config.class);

      ctx.addServlet("jooby-dispatcher",
          new ServletHandler(app.require(Dispatcher.class), config.getString("application.tmpdir")))
          .addMapping("/*");

    } catch (ClassNotFoundException ex) {
      // TODO Auto-generated catch block
      ex.printStackTrace();
    } catch (InstantiationException ex) {
      // TODO Auto-generated catch block
      ex.printStackTrace();
    } catch (IllegalAccessException ex) {
      // TODO Auto-generated catch block
      ex.printStackTrace();
    } catch (Exception ex) {
      // TODO Auto-generated catch block
      ex.printStackTrace();
    }
  }

}
