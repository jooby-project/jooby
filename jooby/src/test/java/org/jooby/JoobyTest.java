package org.jooby;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;

import javax.inject.Singleton;

import org.jooby.Body.Formatter;
import org.jooby.Body.Parser;
import org.jooby.Route.Handler;
import org.jooby.Session.Definition;
import org.jooby.Session.Store;
import org.jooby.internal.AppManager;
import org.jooby.internal.AppPrinter;
import org.jooby.internal.AssetFormatter;
import org.jooby.internal.BuiltinBodyConverter;
import org.jooby.internal.RouteImpl;
import org.jooby.internal.RouteMetadata;
import org.jooby.internal.Server;
import org.jooby.internal.SessionManager;
import org.jooby.internal.TypeConverters;
import org.jooby.internal.undertow.UndertowServer;
import org.jooby.mvc.GET;
import org.jooby.mvc.POST;
import org.jooby.mvc.Path;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Scopes;
import com.google.inject.Stage;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.binder.ScopedBindingBuilder;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.multibindings.OptionalBinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.google.inject.util.Types;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Jooby.class, Guice.class, TypeConverters.class, Multibinder.class,
    OptionalBinder.class, Runtime.class, Thread.class })
@SuppressWarnings("unchecked")
public class JoobyTest {

  @Path("/singleton")
  @Singleton
  public static class SingletonTestRoute {

    @GET
    @POST
    public Object m1() {
      return "";
    }

  }

  @Path("/singleton")
  @com.google.inject.Singleton
  public static class GuiceSingletonTestRoute {

    @GET
    @POST
    public Object m1() {
      return "";
    }

  }

  @Path("/proto")
  public static class ProtoTestRoute {

    @GET
    public Object m1() {
      return "";
    }

  }

  @SuppressWarnings("rawtypes")
  private MockUnit.Block config = unit -> {
    LinkedBindingBuilder<String> strLinkedBinding = unit.mock(LinkedBindingBuilder.class);
    strLinkedBinding.toInstance(isA(String.class));
    expectLastCall().anyTimes();
    AnnotatedBindingBuilder<String> strAnnotatedBinding = unit.mock(AnnotatedBindingBuilder.class);
    expect(strAnnotatedBinding.annotatedWith(isA(Named.class))).andReturn(strLinkedBinding)
        .anyTimes();

    LinkedBindingBuilder<Integer> intLinkedBinding = unit.mock(LinkedBindingBuilder.class);
    intLinkedBinding.toInstance(isA(Integer.class));
    expectLastCall().anyTimes();
    AnnotatedBindingBuilder<Integer> intAnnotatedBinding = unit.mock(AnnotatedBindingBuilder.class);
    expect(intAnnotatedBinding.annotatedWith(isA(Named.class))).andReturn(intLinkedBinding)
        .anyTimes();

    LinkedBindingBuilder<Boolean> boolLinkedBinding = unit.mock(LinkedBindingBuilder.class);
    boolLinkedBinding.toInstance(isA(Boolean.class));
    expectLastCall().anyTimes();
    AnnotatedBindingBuilder<Boolean> boolAnnotatedBinding = unit
        .mock(AnnotatedBindingBuilder.class);
    expect(boolAnnotatedBinding.annotatedWith(isA(Named.class))).andReturn(boolLinkedBinding)
        .anyTimes();

    LinkedBindingBuilder<List<String>> listOfString = unit.mock(LinkedBindingBuilder.class);
    listOfString.toInstance(isA(List.class));
    expectLastCall().anyTimes();

    Binder binder = unit.get(Binder.class);
    expect(binder.bind(String.class)).andReturn(strAnnotatedBinding).anyTimes();
    expect(binder.bind(Integer.class)).andReturn(intAnnotatedBinding).anyTimes();
    expect(binder.bind(Boolean.class)).andReturn(boolAnnotatedBinding).anyTimes();
    expect(binder.bind(Key.get(Types.listOf(String.class), Names.named("hotswap.reload.ext"))))
        .andReturn((LinkedBindingBuilder) listOfString).anyTimes();

    AnnotatedBindingBuilder<Config> configAnnotatedBinding = unit
        .mock(AnnotatedBindingBuilder.class);
    configAnnotatedBinding.toInstance(isA(Config.class));
    expect(binder.bind(Config.class)).andReturn(configAnnotatedBinding);
  };

  private MockUnit.Block env = unit -> {
    Binder binder = unit.get(Binder.class);

    AnnotatedBindingBuilder<Env> binding = unit.mock(AnnotatedBindingBuilder.class);
    binding.toInstance(isA(Env.class));

    expect(binder.bind(Env.class)).andReturn(binding);
  };

  private MockUnit.Block classInfo = unit -> {
    Binder binder = unit.get(Binder.class);

    AnnotatedBindingBuilder<RouteMetadata> binding = unit.mock(AnnotatedBindingBuilder.class);
    binding.toInstance(isA(RouteMetadata.class));

    expect(binder.bind(RouteMetadata.class)).andReturn(binding);
  };

  private MockUnit.Block reload = unit -> {
    Binder binder = unit.get(Binder.class);

    AnnotatedBindingBuilder<String> binding = unit.mock(AnnotatedBindingBuilder.class);
    binding.toInstance("org.jooby.Jooby");

    AnnotatedBindingBuilder<AppManager> appmanager = unit.mock(AnnotatedBindingBuilder.class);
    appmanager.toInstance(isA(AppManager.class));

    expect(binder.bind(Key.get(String.class, Names.named("internal.appClass")))).andReturn(binding);

    expect(binder.bind(AppManager.class)).andReturn(appmanager);
  };

  private MockUnit.Block charset = unit -> {
    Binder binder = unit.get(Binder.class);

    AnnotatedBindingBuilder<Charset> binding = unit.mock(AnnotatedBindingBuilder.class);
    binding.toInstance(isA(Charset.class));

    expect(binder.bind(Charset.class)).andReturn(binding);
  };

  private MockUnit.Block locale = unit -> {
    Binder binder = unit.get(Binder.class);

    AnnotatedBindingBuilder<Locale> binding = unit.mock(AnnotatedBindingBuilder.class);
    binding.toInstance(isA(Locale.class));

    expect(binder.bind(Locale.class)).andReturn(binding);
  };

  private MockUnit.Block zoneId = unit -> {
    Binder binder = unit.get(Binder.class);

    AnnotatedBindingBuilder<ZoneId> binding = unit.mock(AnnotatedBindingBuilder.class);
    binding.toInstance(isA(ZoneId.class));

    expect(binder.bind(ZoneId.class)).andReturn(binding);
  };

  private MockUnit.Block timeZone = unit -> {
    Binder binder = unit.get(Binder.class);

    AnnotatedBindingBuilder<TimeZone> binding = unit.mock(AnnotatedBindingBuilder.class);
    binding.toInstance(isA(TimeZone.class));

    expect(binder.bind(TimeZone.class)).andReturn(binding);
  };

  private MockUnit.Block dateTimeFormatter = unit -> {
    Binder binder = unit.get(Binder.class);

    AnnotatedBindingBuilder<DateTimeFormatter> binding = unit.mock(AnnotatedBindingBuilder.class);
    binding.toInstance(isA(DateTimeFormatter.class));

    expect(binder.bind(DateTimeFormatter.class)).andReturn(binding);
  };

  private MockUnit.Block numberFormat = unit -> {
    Binder binder = unit.get(Binder.class);

    AnnotatedBindingBuilder<NumberFormat> binding = unit.mock(AnnotatedBindingBuilder.class);
    binding.toInstance(isA(NumberFormat.class));

    expect(binder.bind(NumberFormat.class)).andReturn(binding);
  };

  private MockUnit.Block decimalFormat = unit -> {
    Binder binder = unit.get(Binder.class);

    AnnotatedBindingBuilder<DecimalFormat> binding = unit.mock(AnnotatedBindingBuilder.class);
    binding.toInstance(isA(DecimalFormat.class));

    expect(binder.bind(DecimalFormat.class)).andReturn(binding);
  };

  private MockUnit.Block bodyParser = unit -> {
    Multibinder<Parser> multibinder = unit.mock(Multibinder.class);

    Binder binder = unit.get(Binder.class);
    unit.mockStatic(Multibinder.class);

    expect(Multibinder.newSetBinder(binder, Body.Parser.class)).andReturn(multibinder);

    LinkedBindingBuilder<Parser> parseString = unit.mock(LinkedBindingBuilder.class);
    parseString.toInstance(BuiltinBodyConverter.parseString);

    expect(multibinder.addBinding()).andReturn(parseString);
  };

  private MockUnit.Block bodyFormatter = unit -> {
    Multibinder<Body.Formatter> multibinder = unit.mock(Multibinder.class);

    Binder binder = unit.get(Binder.class);

    expect(Multibinder.newSetBinder(binder, Body.Formatter.class)).andReturn(multibinder);

    LinkedBindingBuilder<Formatter> formatReader = unit.mock(LinkedBindingBuilder.class);
    formatReader.toInstance(BuiltinBodyConverter.formatReader);

    LinkedBindingBuilder<Formatter> formatStream = unit.mock(LinkedBindingBuilder.class);
    formatStream.toInstance(BuiltinBodyConverter.formatStream);

    LinkedBindingBuilder<Formatter> formatByteArray = unit.mock(LinkedBindingBuilder.class);
    formatByteArray.toInstance(BuiltinBodyConverter.formatByteArray);

    LinkedBindingBuilder<Formatter> formatByteBuffer = unit.mock(LinkedBindingBuilder.class);
    formatByteBuffer.toInstance(BuiltinBodyConverter.formatByteBuffer);

    LinkedBindingBuilder<Formatter> formatAny = unit.mock(LinkedBindingBuilder.class);
    formatAny.toInstance(BuiltinBodyConverter.formatAny);

    expect(multibinder.addBinding()).andReturn(formatReader);
    expect(multibinder.addBinding()).andReturn(formatStream);
    expect(multibinder.addBinding()).andReturn(formatByteArray);
    expect(multibinder.addBinding()).andReturn(formatByteBuffer);

    expect(multibinder.addBinding()).andReturn(formatAny);

  };

  private MockUnit.Block routes = unit -> {
    Multibinder<Route.Definition> multibinder = unit.mock(Multibinder.class);

    Binder binder = unit.get(Binder.class);

    expect(Multibinder.newSetBinder(binder, Route.Definition.class)).andReturn(multibinder);
  };

  private MockUnit.Block webSockets = unit -> {
    Multibinder<WebSocket.Definition> multibinder = unit.mock(Multibinder.class);

    Binder binder = unit.get(Binder.class);

    expect(Multibinder.newSetBinder(binder, WebSocket.Definition.class)).andReturn(multibinder);
  };

  private MockUnit.Block reqModules = unit -> {
    Multibinder<Request.Module> multibinder = unit.mock(Multibinder.class);

    Binder binder = unit.get(Binder.class);

    expect(Multibinder.newSetBinder(binder, Request.Module.class)).andReturn(multibinder);
  };

  private MockUnit.Block tmpdir = unit -> {
    Binder binder = unit.get(Binder.class);

    LinkedBindingBuilder<File> instance = unit.mock(LinkedBindingBuilder.class);
    instance.toInstance(isA(File.class));

    AnnotatedBindingBuilder<File> named = unit.mock(AnnotatedBindingBuilder.class);
    expect(named.annotatedWith(Names.named("application.tmpdir"))).andReturn(instance);

    expect(binder.bind(java.io.File.class)).andReturn(named);
  };

  private MockUnit.Block err = unit -> {
    Binder binder = unit.get(Binder.class);

    AnnotatedBindingBuilder<Err.Handler> binding = unit.mock(AnnotatedBindingBuilder.class);
    binding.toInstance(isA(Err.Default.class));

    expect(binder.bind(Err.Handler.class)).andReturn(binding);
  };

  private MockUnit.Block session = unit -> {
    Binder binder = unit.get(Binder.class);

    AnnotatedBindingBuilder<SessionManager> smbinding = unit.mock(AnnotatedBindingBuilder.class);
    smbinding.toInstance(isA(SessionManager.class));

    expect(binder.bind(SessionManager.class)).andReturn(smbinding);

    AnnotatedBindingBuilder<Session.Definition> binding = unit.mock(AnnotatedBindingBuilder.class);
    binding.toInstance(isA(Session.Definition.class));

    expect(binder.bind(Session.Definition.class)).andReturn(binding);
  };

  private MockUnit.Block boot = unit -> {
    Module module = unit.captured(Module.class).iterator().next();

    module.configure(unit.get(Binder.class));

    unit.captured(Runnable.class).get(0).run();
  };

  private MockUnit.Block shutdown = unit -> {
    unit.mockStatic(Runtime.class);

    Thread thread = unit.mockConstructor(Thread.class, new Class<?>[]{Runnable.class },
        unit.capture(Runnable.class));

    Runtime runtime = unit.mock(Runtime.class);
    expect(Runtime.getRuntime()).andReturn(runtime);
    runtime.addShutdownHook(thread);
  };

  private MockUnit.Block guice = unit -> {
    Server server = unit.mock(Server.class);
    server.start();
    server.stop();

    AppPrinter printer = unit.mock(AppPrinter.class);

    Injector injector = unit.mock(Injector.class);
    expect(injector.getInstance(Server.class)).andReturn(server).times(1, 2);
    expect(injector.getInstance(AppPrinter.class)).andReturn(printer);

    unit.mockStatic(Guice.class);
    expect(Guice.createInjector(eq(Stage.DEVELOPMENT), unit.capture(Module.class))).andReturn(
        injector);

    Binder binder = unit.get(Binder.class);
    unit.mockStatic(OptionalBinder.class);

    ScopedBindingBuilder serverScope = unit.mock(ScopedBindingBuilder.class);
    serverScope.in(Singleton.class);

    AnnotatedBindingBuilder<Server> serverBinding = unit.mock(AnnotatedBindingBuilder.class);
    expect(serverBinding.to(UndertowServer.class)).andReturn(serverScope);

    expect(binder.bind(Server.class)).andReturn(serverBinding);

    unit.mockStatic(TypeConverters.class);
    TypeConverters.configure(unit.get(Binder.class));
  };

  @Test
  public void applicationSecret() throws Exception {

    new MockUnit(Binder.class)
        .expect(unit -> {
          Server server = unit.mock(Server.class);
          server.start();
          server.stop();

          AppPrinter printer = unit.mock(AppPrinter.class);

          Injector injector = unit.mock(Injector.class);
          expect(injector.getInstance(Server.class)).andReturn(server).times(1, 2);
          expect(injector.getInstance(AppPrinter.class)).andReturn(printer);

          unit.mockStatic(Guice.class);
          expect(Guice.createInjector(eq(Stage.PRODUCTION), unit.capture(Module.class))).andReturn(
              injector);

          Binder binder = unit.get(Binder.class);
          unit.mockStatic(OptionalBinder.class);

          ScopedBindingBuilder serverScope = unit.mock(ScopedBindingBuilder.class);
          serverScope.in(Singleton.class);

          AnnotatedBindingBuilder<Server> serverBinding = unit.mock(AnnotatedBindingBuilder.class);
          expect(serverBinding.to(UndertowServer.class)).andReturn(serverScope);

          expect(binder.bind(Server.class)).andReturn(serverBinding);

          unit.mockStatic(TypeConverters.class);
          TypeConverters.configure(unit.get(Binder.class));
        })
        .expect(shutdown)
        .expect(config)
        .expect(env)
        .expect(classInfo)
        .expect(charset)
        .expect(locale)
        .expect(zoneId)
        .expect(timeZone)
        .expect(dateTimeFormatter)
        .expect(numberFormat)
        .expect(decimalFormat)
        .expect(bodyParser)
        .expect(bodyFormatter)
        .expect(session)
        .expect(routes)
        .expect(webSockets)
        .expect(reqModules)
        .expect(tmpdir)
        .expect(err)
        .run(
            unit -> {

              Jooby jooby = new Jooby();

              jooby.use(ConfigFactory.empty()
                  .withValue("application.env", ConfigValueFactory.fromAnyRef("prod"))
                  .withValue("application.secret", ConfigValueFactory.fromAnyRef("234"))
                  );

              jooby.start();

            }, boot);
  }

  @Test
  public void defaults() throws Exception {

    new MockUnit(Binder.class)
        .expect(guice)
        .expect(shutdown)
        .expect(config)
        .expect(env)
        .expect(reload)
        .expect(classInfo)
        .expect(charset)
        .expect(locale)
        .expect(zoneId)
        .expect(timeZone)
        .expect(dateTimeFormatter)
        .expect(numberFormat)
        .expect(decimalFormat)
        .expect(bodyParser)
        .expect(bodyFormatter)
        .expect(session)
        .expect(routes)
        .expect(webSockets)
        .expect(reqModules)
        .expect(tmpdir)
        .expect(err)
        .run(unit -> {

          Jooby jooby = new Jooby();

          jooby.start();

        }, boot);
  }

  @Test
  public void customEnv() throws Exception {

    new MockUnit(Binder.class, Env.Builder.class)
        .expect(guice)
        .expect(shutdown)
        .expect(config)
        .expect(unit -> {
          Env env = unit.mock(Env.class);
          expect(env.name()).andReturn("dev").times(3);

          Env.Builder builder = unit.get(Env.Builder.class);
          expect(builder.build(isA(Config.class))).andReturn(env);

          Binder binder = unit.get(Binder.class);

          AnnotatedBindingBuilder<Env> binding = unit.mock(AnnotatedBindingBuilder.class);
          binding.toInstance(env);

          expect(binder.bind(Env.class)).andReturn(binding);
        })
        .expect(reload)
        .expect(classInfo)
        .expect(charset)
        .expect(locale)
        .expect(zoneId)
        .expect(timeZone)
        .expect(dateTimeFormatter)
        .expect(numberFormat)
        .expect(decimalFormat)
        .expect(bodyParser)
        .expect(bodyFormatter)
        .expect(session)
        .expect(routes)
        .expect(webSockets)
        .expect(reqModules)
        .expect(tmpdir)
        .expect(err)
        .run(unit -> {

          Jooby jooby = new Jooby();

          jooby.env(unit.get(Env.Builder.class));

          jooby.start();

        }, boot);
  }

  @Test
  public void noInjector() throws Exception {

    new MockUnit(Binder.class, Jooby.Module.class)
        .expect(
            unit -> {
              unit.mockStatic(Guice.class);
              expect(Guice.createInjector(eq(Stage.DEVELOPMENT), unit.capture(Module.class)))
                  .andThrow(new RuntimeException());

              Binder binder = unit.get(Binder.class);
              unit.mockStatic(OptionalBinder.class);

              ScopedBindingBuilder serverScope = unit.mock(ScopedBindingBuilder.class);
              serverScope.in(Singleton.class);

              AnnotatedBindingBuilder<Server> serverBinding = unit
                  .mock(AnnotatedBindingBuilder.class);
              expect(serverBinding.to(UndertowServer.class)).andReturn(serverScope);

              expect(binder.bind(Server.class)).andReturn(serverBinding);
              unit.mockStatic(TypeConverters.class);
              TypeConverters.configure(unit.get(Binder.class));
            })
        .expect(shutdown)
        .expect(config)
        .expect(env)
        .expect(reload)
        .expect(classInfo)
        .expect(charset)
        .expect(locale)
        .expect(zoneId)
        .expect(timeZone)
        .expect(dateTimeFormatter)
        .expect(numberFormat)
        .expect(decimalFormat)
        .expect(bodyParser)
        .expect(bodyFormatter)
        .expect(session)
        .expect(routes)
        .expect(webSockets)
        .expect(reqModules)
        .expect(tmpdir)
        .expect(err)
        .expect(unit -> {
          Binder binder = unit.get(Binder.class);
          Jooby.Module module = unit.get(Jooby.Module.class);

          Config config = ConfigFactory.empty();

          expect(module.config()).andReturn(config);

          module.configure(isA(Env.class), isA(Config.class), eq(binder));

          module.stop();
        })
        .run(unit -> {

          Jooby jooby = new Jooby();

          jooby.use(unit.get(Jooby.Module.class));

          try {
            jooby.start();
          } catch (RuntimeException ex) {
            // we are OK
          }

        }, boot);
  }

  @Test
  public void customLang() throws Exception {

    new MockUnit(Binder.class)
        .expect(guice)
        .expect(shutdown)
        .expect(config)
        .expect(env)
        .expect(reload)
        .expect(classInfo)
        .expect(charset)
        .expect(locale)
        .expect(zoneId)
        .expect(timeZone)
        .expect(dateTimeFormatter)
        .expect(numberFormat)
        .expect(decimalFormat)
        .expect(bodyParser)
        .expect(bodyFormatter)
        .expect(session)
        .expect(routes)
        .expect(webSockets)
        .expect(reqModules)
        .expect(tmpdir)
        .expect(err)
        .run(
            unit -> {

              Jooby jooby = new Jooby();
              jooby.use(ConfigFactory.empty().withValue("application.lang",
                  ConfigValueFactory.fromAnyRef("es")));

              jooby.start();

            }, boot);
  }

  @Test
  public void stopOnServerFailure() throws Exception {

    new MockUnit(Binder.class)
        .expect(
            unit -> {
              Server server = unit.mock(Server.class);
              server.start();
              server.stop();
              expectLastCall().andThrow(new Exception());

              AppPrinter printer = unit.mock(AppPrinter.class);

              Injector injector = unit.mock(Injector.class);
              expect(injector.getInstance(Server.class)).andReturn(server).times(1, 2);
              expect(injector.getInstance(AppPrinter.class)).andReturn(printer);

              unit.mockStatic(Guice.class);
              expect(Guice.createInjector(eq(Stage.DEVELOPMENT), unit.capture(Module.class)))
                  .andReturn(
                      injector);

              Binder binder = unit.get(Binder.class);
              unit.mockStatic(OptionalBinder.class);

              ScopedBindingBuilder serverScope = unit.mock(ScopedBindingBuilder.class);
              serverScope.in(Singleton.class);

              AnnotatedBindingBuilder<Server> serverBinding = unit
                  .mock(AnnotatedBindingBuilder.class);
              expect(serverBinding.to(UndertowServer.class)).andReturn(serverScope);

              expect(binder.bind(Server.class)).andReturn(serverBinding);

              unit.mockStatic(TypeConverters.class);
              TypeConverters.configure(unit.get(Binder.class));
            })
        .expect(shutdown)
        .expect(config)
        .expect(env)
        .expect(reload)
        .expect(classInfo)
        .expect(charset)
        .expect(locale)
        .expect(zoneId)
        .expect(timeZone)
        .expect(dateTimeFormatter)
        .expect(numberFormat)
        .expect(decimalFormat)
        .expect(bodyParser)
        .expect(bodyFormatter)
        .expect(session)
        .expect(routes)
        .expect(webSockets)
        .expect(reqModules)
        .expect(tmpdir)
        .expect(err)
        .run(unit -> {

          Jooby jooby = new Jooby();

          jooby.start();

        }, boot);
  }

  @Test
  public void stopOnModuleFailure() throws Exception {

    new MockUnit(Binder.class, Jooby.Module.class)
        .expect(
            unit -> {
              Server server = unit.mock(Server.class);
              server.start();
              server.stop();

              AppPrinter printer = unit.mock(AppPrinter.class);

              Injector injector = unit.mock(Injector.class);
              expect(injector.getInstance(Server.class)).andReturn(server).times(1, 2);
              expect(injector.getInstance(AppPrinter.class)).andReturn(printer);

              unit.mockStatic(Guice.class);
              expect(Guice.createInjector(eq(Stage.DEVELOPMENT), unit.capture(Module.class)))
                  .andReturn(
                      injector);

              Binder binder = unit.get(Binder.class);
              unit.mockStatic(OptionalBinder.class);

              ScopedBindingBuilder serverScope = unit.mock(ScopedBindingBuilder.class);
              serverScope.in(Singleton.class);

              AnnotatedBindingBuilder<Server> serverBinding = unit
                  .mock(AnnotatedBindingBuilder.class);
              expect(serverBinding.to(UndertowServer.class)).andReturn(serverScope);

              expect(binder.bind(Server.class)).andReturn(serverBinding);

              unit.mockStatic(TypeConverters.class);
              TypeConverters.configure(unit.get(Binder.class));
            })
        .expect(shutdown)
        .expect(config)
        .expect(env)
        .expect(reload)
        .expect(classInfo)
        .expect(charset)
        .expect(locale)
        .expect(zoneId)
        .expect(timeZone)
        .expect(dateTimeFormatter)
        .expect(numberFormat)
        .expect(decimalFormat)
        .expect(bodyParser)
        .expect(bodyFormatter)
        .expect(session)
        .expect(routes)
        .expect(webSockets)
        .expect(reqModules)
        .expect(tmpdir)
        .expect(err)
        .expect(unit -> {
          Binder binder = unit.get(Binder.class);

          Jooby.Module module = unit.get(Jooby.Module.class);

          expect(module.config()).andReturn(ConfigFactory.empty());

          module.configure(isA(Env.class), isA(Config.class), eq(binder));

          module.start();

          module.stop();
          expectLastCall().andThrow(new IllegalArgumentException());
        })
        .run(unit -> {

          Jooby jooby = new Jooby();

          jooby.use(unit.get(Jooby.Module.class));

          jooby.start();

        }, boot);
  }

  @Test
  public void useFilter() throws Exception {

    List<Route.Definition> expected = new LinkedList<>();

    new MockUnit(Binder.class, Route.Filter.class)
        .expect(guice)
        .expect(shutdown)
        .expect(config)
        .expect(env)
        .expect(reload)
        .expect(classInfo)
        .expect(charset)
        .expect(locale)
        .expect(zoneId)
        .expect(timeZone)
        .expect(dateTimeFormatter)
        .expect(numberFormat)
        .expect(decimalFormat)
        .expect(bodyParser)
        .expect(bodyFormatter)
        .expect(session)
        .expect(unit -> {
          Multibinder<Route.Definition> multibinder = unit.mock(Multibinder.class);

          Binder binder = unit.get(Binder.class);

          expect(Multibinder.newSetBinder(binder, Route.Definition.class)).andReturn(multibinder);

          LinkedBindingBuilder<Route.Definition> binding = unit.mock(LinkedBindingBuilder.class);
          expect(multibinder.addBinding()).andReturn(binding);
          expect(multibinder.addBinding()).andReturn(binding);

          binding.toInstance(unit.capture(Route.Definition.class));
          binding.toInstance(unit.capture(Route.Definition.class));
        })
        .expect(webSockets)
        .expect(reqModules)
        .expect(tmpdir)
        .expect(err)
        .run(unit -> {

          Jooby jooby = new Jooby();

          Route.Definition first = jooby.use("/filter", unit.get(Route.Filter.class));
          assertNotNull(first);
          assertEquals("/filter", first.pattern());
          assertEquals("*", first.verb());
          assertEquals("anonymous", first.name());
          assertEquals(MediaType.ALL, first.consumes());
          assertEquals(MediaType.ALL, first.produces());

          expected.add(first);

          Route.Definition second = jooby.use("GET", "*", unit.get(Route.Filter.class));
          assertNotNull(second);
          assertEquals("/**/*", second.pattern());
          assertEquals("GET", second.verb());
          assertEquals("anonymous", second.name());
          assertEquals(MediaType.ALL, second.consumes());
          assertEquals(MediaType.ALL, second.produces());

          expected.add(second);

          jooby.start();

        }, boot,
            unit -> {
              List<Route.Definition> found = unit.captured(Route.Definition.class);
              assertEquals(expected, found);
            });
  }

  @Test
  public void useHandler() throws Exception {

    List<Route.Definition> expected = new LinkedList<>();

    new MockUnit(Binder.class, Route.Handler.class)
        .expect(guice)
        .expect(shutdown)
        .expect(config)
        .expect(env)
        .expect(reload)
        .expect(classInfo)
        .expect(charset)
        .expect(locale)
        .expect(zoneId)
        .expect(timeZone)
        .expect(dateTimeFormatter)
        .expect(numberFormat)
        .expect(decimalFormat)
        .expect(bodyParser)
        .expect(bodyFormatter)
        .expect(session)
        .expect(unit -> {
          Multibinder<Route.Definition> multibinder = unit.mock(Multibinder.class);

          Binder binder = unit.get(Binder.class);

          expect(Multibinder.newSetBinder(binder, Route.Definition.class)).andReturn(multibinder);

          LinkedBindingBuilder<Route.Definition> binding = unit.mock(LinkedBindingBuilder.class);
          expect(multibinder.addBinding()).andReturn(binding);
          expect(multibinder.addBinding()).andReturn(binding);

          binding.toInstance(unit.capture(Route.Definition.class));
          binding.toInstance(unit.capture(Route.Definition.class));
        })
        .expect(webSockets)
        .expect(reqModules)
        .expect(tmpdir)
        .expect(err)
        .run(unit -> {

          Jooby jooby = new Jooby();

          Route.Definition first = jooby.use("/first", unit.get(Route.Handler.class));
          assertNotNull(first);
          assertEquals("/first", first.pattern());
          assertEquals("*", first.verb());
          assertEquals("anonymous", first.name());
          assertEquals(MediaType.ALL, first.consumes());
          assertEquals(MediaType.ALL, first.produces());

          expected.add(first);

          Route.Definition second = jooby.use("GET", "*", unit.get(Route.Handler.class));
          assertNotNull(second);
          assertEquals("/**/*", second.pattern());
          assertEquals("GET", second.verb());
          assertEquals("anonymous", second.name());
          assertEquals(MediaType.ALL, second.consumes());
          assertEquals(MediaType.ALL, second.produces());

          expected.add(second);

          jooby.start();

        }, boot,
            unit -> {
              List<Route.Definition> found = unit.captured(Route.Definition.class);
              assertEquals(expected, found);
            });
  }

  @Test
  public void getHandlers() throws Exception {

    List<Route.Definition> expected = new LinkedList<>();

    new MockUnit(Binder.class, Route.Handler.class, Route.OneArgHandler.class,
        Route.ZeroArgHandler.class, Route.Filter.class)
        .expect(guice)
        .expect(shutdown)
        .expect(config)
        .expect(env)
        .expect(reload)
        .expect(classInfo)
        .expect(charset)
        .expect(locale)
        .expect(zoneId)
        .expect(timeZone)
        .expect(dateTimeFormatter)
        .expect(numberFormat)
        .expect(decimalFormat)
        .expect(bodyParser)
        .expect(unit -> {
          Multibinder<Body.Formatter> multibinder = unit.mock(Multibinder.class);

          Binder binder = unit.get(Binder.class);

          expect(Multibinder.newSetBinder(binder, Body.Formatter.class)).andReturn(multibinder);

          LinkedBindingBuilder<Formatter> formatReader = unit.mock(LinkedBindingBuilder.class);
          formatReader.toInstance(BuiltinBodyConverter.formatReader);

          LinkedBindingBuilder<Formatter> formatStream = unit.mock(LinkedBindingBuilder.class);
          formatStream.toInstance(BuiltinBodyConverter.formatStream);

          LinkedBindingBuilder<Formatter> formatByteArray = unit.mock(LinkedBindingBuilder.class);
          formatByteArray.toInstance(BuiltinBodyConverter.formatByteArray);

          LinkedBindingBuilder<Formatter> formatByteBuffer = unit.mock(LinkedBindingBuilder.class);
          formatByteBuffer.toInstance(BuiltinBodyConverter.formatByteBuffer);

          LinkedBindingBuilder<Formatter> formatAny = unit.mock(LinkedBindingBuilder.class);
          formatAny.toInstance(BuiltinBodyConverter.formatAny);

          LinkedBindingBuilder<Formatter> assertFormatter = unit.mock(LinkedBindingBuilder.class);
          assertFormatter.toInstance(isA(AssetFormatter.class));

          expect(multibinder.addBinding()).andReturn(assertFormatter);

          expect(multibinder.addBinding()).andReturn(formatReader);
          expect(multibinder.addBinding()).andReturn(formatStream);
          expect(multibinder.addBinding()).andReturn(formatByteArray);
          expect(multibinder.addBinding()).andReturn(formatByteBuffer);

          expect(multibinder.addBinding()).andReturn(formatAny);

        })
        .expect(session)
        .expect(unit -> {
          Multibinder<Route.Definition> multibinder = unit.mock(Multibinder.class);

          Binder binder = unit.get(Binder.class);

          expect(Multibinder.newSetBinder(binder, Route.Definition.class)).andReturn(multibinder);

          LinkedBindingBuilder<Route.Definition> binding = unit.mock(LinkedBindingBuilder.class);
          expect(multibinder.addBinding()).andReturn(binding).times(5);

          binding.toInstance(unit.capture(Route.Definition.class));
          binding.toInstance(unit.capture(Route.Definition.class));
          binding.toInstance(unit.capture(Route.Definition.class));
          binding.toInstance(unit.capture(Route.Definition.class));
          binding.toInstance(unit.capture(Route.Definition.class));
        })
        .expect(webSockets)
        .expect(reqModules)
        .expect(tmpdir)
        .expect(err)
        .run(unit -> {

          Jooby jooby = new Jooby();

          Route.Definition first = jooby.get("first.html");
          assertNotNull(first);
          assertEquals("/first.html", first.pattern());
          assertEquals("GET", first.verb());
          assertEquals("anonymous", first.name());
          assertEquals(MediaType.ALL, first.consumes());
          assertEquals(MediaType.ALL, first.produces());

          expected.add(first);

          Route.Definition second = jooby.get("/second", unit.get(Route.Handler.class));
          assertNotNull(second);
          assertEquals("/second", second.pattern());
          assertEquals("GET", second.verb());
          assertEquals("anonymous", second.name());
          assertEquals(MediaType.ALL, second.consumes());
          assertEquals(MediaType.ALL, second.produces());

          expected.add(second);

          Route.Definition third = jooby.get("/third", unit.get(Route.OneArgHandler.class));
          assertNotNull(third);
          assertEquals("/third", third.pattern());
          assertEquals("GET", third.verb());
          assertEquals("anonymous", third.name());
          assertEquals(MediaType.ALL, third.consumes());
          assertEquals(MediaType.ALL, third.produces());

          expected.add(third);

          Route.Definition fourth = jooby.get("/fourth", unit.get(Route.ZeroArgHandler.class));
          assertNotNull(fourth);
          assertEquals("/fourth", fourth.pattern());
          assertEquals("GET", fourth.verb());
          assertEquals("anonymous", fourth.name());
          assertEquals(MediaType.ALL, fourth.consumes());
          assertEquals(MediaType.ALL, fourth.produces());

          expected.add(fourth);

          Route.Definition fifth = jooby.get("/fifth", unit.get(Route.Filter.class));
          assertNotNull(fifth);
          assertEquals("/fifth", fifth.pattern());
          assertEquals("GET", fifth.verb());
          assertEquals("anonymous", fifth.name());
          assertEquals(MediaType.ALL, fifth.consumes());
          assertEquals(MediaType.ALL, fifth.produces());

          expected.add(fifth);

          jooby.start();

        }, boot,
            unit -> {
              List<Route.Definition> found = unit.captured(Route.Definition.class);
              assertEquals(expected, found);
            });
  }

  @Test
  public void postHandlers() throws Exception {

    List<Route.Definition> expected = new LinkedList<>();

    new MockUnit(Binder.class, Route.Handler.class, Route.OneArgHandler.class,
        Route.ZeroArgHandler.class, Route.Filter.class)
        .expect(guice)
        .expect(shutdown)
        .expect(config)
        .expect(env)
        .expect(reload)
        .expect(classInfo)
        .expect(charset)
        .expect(locale)
        .expect(zoneId)
        .expect(timeZone)
        .expect(dateTimeFormatter)
        .expect(numberFormat)
        .expect(decimalFormat)
        .expect(bodyParser)
        .expect(bodyFormatter)
        .expect(session)
        .expect(unit -> {
          Multibinder<Route.Definition> multibinder = unit.mock(Multibinder.class);

          Binder binder = unit.get(Binder.class);

          expect(Multibinder.newSetBinder(binder, Route.Definition.class)).andReturn(multibinder);

          LinkedBindingBuilder<Route.Definition> binding = unit.mock(LinkedBindingBuilder.class);
          expect(multibinder.addBinding()).andReturn(binding).times(4);

          binding.toInstance(unit.capture(Route.Definition.class));
          binding.toInstance(unit.capture(Route.Definition.class));
          binding.toInstance(unit.capture(Route.Definition.class));
          binding.toInstance(unit.capture(Route.Definition.class));
        })
        .expect(webSockets)
        .expect(reqModules)
        .expect(tmpdir)
        .expect(err)
        .run(unit -> {

          Jooby jooby = new Jooby();

          Route.Definition first = jooby.post("/first", unit.get(Route.Handler.class));
          assertNotNull(first);
          assertEquals("/first", first.pattern());
          assertEquals("POST", first.verb());
          assertEquals("anonymous", first.name());
          assertEquals(MediaType.ALL, first.consumes());
          assertEquals(MediaType.ALL, first.produces());

          expected.add(first);

          Route.Definition second = jooby.post("/second", unit.get(Route.OneArgHandler.class));
          assertNotNull(second);
          assertEquals("/second", second.pattern());
          assertEquals("POST", second.verb());
          assertEquals("anonymous", second.name());
          assertEquals(MediaType.ALL, second.consumes());
          assertEquals(MediaType.ALL, second.produces());

          expected.add(second);

          Route.Definition third = jooby.post("/third", unit.get(Route.ZeroArgHandler.class));
          assertNotNull(third);
          assertEquals("/third", third.pattern());
          assertEquals("POST", third.verb());
          assertEquals("anonymous", third.name());
          assertEquals(MediaType.ALL, third.consumes());
          assertEquals(MediaType.ALL, third.produces());

          expected.add(third);

          Route.Definition fourth = jooby.post("/fourth", unit.get(Route.Filter.class));
          assertNotNull(fourth);
          assertEquals("/fourth", fourth.pattern());
          assertEquals("POST", fourth.verb());
          assertEquals("anonymous", fourth.name());
          assertEquals(MediaType.ALL, fourth.consumes());
          assertEquals(MediaType.ALL, fourth.produces());

          expected.add(fourth);

          jooby.start();

        }, boot,
            unit -> {
              List<Route.Definition> found = unit.captured(Route.Definition.class);
              assertEquals(expected, found);
            });
  }

  @Test
  public void headHandlers() throws Exception {

    List<Route.Definition> expected = new LinkedList<>();

    new MockUnit(Binder.class, Route.Handler.class, Route.OneArgHandler.class,
        Route.ZeroArgHandler.class, Route.Filter.class)
        .expect(guice)
        .expect(shutdown)
        .expect(config)
        .expect(env)
        .expect(reload)
        .expect(classInfo)
        .expect(charset)
        .expect(locale)
        .expect(zoneId)
        .expect(timeZone)
        .expect(dateTimeFormatter)
        .expect(numberFormat)
        .expect(decimalFormat)
        .expect(bodyParser)
        .expect(bodyFormatter)
        .expect(session)
        .expect(unit -> {
          Multibinder<Route.Definition> multibinder = unit.mock(Multibinder.class);

          Binder binder = unit.get(Binder.class);

          expect(Multibinder.newSetBinder(binder, Route.Definition.class)).andReturn(multibinder);

          LinkedBindingBuilder<Route.Definition> binding = unit.mock(LinkedBindingBuilder.class);
          expect(multibinder.addBinding()).andReturn(binding).times(4);

          binding.toInstance(unit.capture(Route.Definition.class));
          binding.toInstance(unit.capture(Route.Definition.class));
          binding.toInstance(unit.capture(Route.Definition.class));
          binding.toInstance(unit.capture(Route.Definition.class));
        })
        .expect(webSockets)
        .expect(reqModules)
        .expect(tmpdir)
        .expect(err)
        .run(unit -> {

          Jooby jooby = new Jooby();

          Route.Definition first = jooby.head("/first", unit.get(Route.Handler.class));
          assertNotNull(first);
          assertEquals("/first", first.pattern());
          assertEquals("HEAD", first.verb());
          assertEquals("anonymous", first.name());
          assertEquals(MediaType.ALL, first.consumes());
          assertEquals(MediaType.ALL, first.produces());

          expected.add(first);

          Route.Definition second = jooby.head("/second", unit.get(Route.OneArgHandler.class));
          assertNotNull(second);
          assertEquals("/second", second.pattern());
          assertEquals("HEAD", second.verb());
          assertEquals("anonymous", second.name());
          assertEquals(MediaType.ALL, second.consumes());
          assertEquals(MediaType.ALL, second.produces());

          expected.add(second);

          Route.Definition third = jooby.head("/third", unit.get(Route.ZeroArgHandler.class));
          assertNotNull(third);
          assertEquals("/third", third.pattern());
          assertEquals("HEAD", third.verb());
          assertEquals("anonymous", third.name());
          assertEquals(MediaType.ALL, third.consumes());
          assertEquals(MediaType.ALL, third.produces());

          expected.add(third);

          Route.Definition fourth = jooby.head("/fourth", unit.get(Route.Filter.class));
          assertNotNull(fourth);
          assertEquals("/fourth", fourth.pattern());
          assertEquals("HEAD", fourth.verb());
          assertEquals("anonymous", fourth.name());
          assertEquals(MediaType.ALL, fourth.consumes());
          assertEquals(MediaType.ALL, fourth.produces());

          expected.add(fourth);

          jooby.start();

        }, boot,
            unit -> {
              List<Route.Definition> found = unit.captured(Route.Definition.class);
              assertEquals(expected, found);
            });
  }

  @Test
  public void optionsHandlers() throws Exception {

    List<Route.Definition> expected = new LinkedList<>();

    new MockUnit(Binder.class, Route.Handler.class, Route.OneArgHandler.class,
        Route.ZeroArgHandler.class, Route.Filter.class)
        .expect(guice)
        .expect(shutdown)
        .expect(config)
        .expect(env)
        .expect(reload)
        .expect(classInfo)
        .expect(charset)
        .expect(locale)
        .expect(zoneId)
        .expect(timeZone)
        .expect(dateTimeFormatter)
        .expect(numberFormat)
        .expect(decimalFormat)
        .expect(bodyParser)
        .expect(bodyFormatter)
        .expect(session)
        .expect(unit -> {
          Multibinder<Route.Definition> multibinder = unit.mock(Multibinder.class);

          Binder binder = unit.get(Binder.class);

          expect(Multibinder.newSetBinder(binder, Route.Definition.class)).andReturn(multibinder);

          LinkedBindingBuilder<Route.Definition> binding = unit.mock(LinkedBindingBuilder.class);
          expect(multibinder.addBinding()).andReturn(binding).times(4);

          binding.toInstance(unit.capture(Route.Definition.class));
          binding.toInstance(unit.capture(Route.Definition.class));
          binding.toInstance(unit.capture(Route.Definition.class));
          binding.toInstance(unit.capture(Route.Definition.class));
        })
        .expect(webSockets)
        .expect(reqModules)
        .expect(tmpdir)
        .expect(err)
        .run(unit -> {

          Jooby jooby = new Jooby();

          Route.Definition first = jooby.options("/first", unit.get(Route.Handler.class));
          assertNotNull(first);
          assertEquals("/first", first.pattern());
          assertEquals("OPTIONS", first.verb());
          assertEquals("anonymous", first.name());
          assertEquals(MediaType.ALL, first.consumes());
          assertEquals(MediaType.ALL, first.produces());

          expected.add(first);

          Route.Definition second = jooby.options("/second", unit.get(Route.OneArgHandler.class));
          assertNotNull(second);
          assertEquals("/second", second.pattern());
          assertEquals("OPTIONS", second.verb());
          assertEquals("anonymous", second.name());
          assertEquals(MediaType.ALL, second.consumes());
          assertEquals(MediaType.ALL, second.produces());

          expected.add(second);

          Route.Definition third = jooby.options("/third", unit.get(Route.ZeroArgHandler.class));
          assertNotNull(third);
          assertEquals("/third", third.pattern());
          assertEquals("OPTIONS", third.verb());
          assertEquals("anonymous", third.name());
          assertEquals(MediaType.ALL, third.consumes());
          assertEquals(MediaType.ALL, third.produces());

          expected.add(third);

          Route.Definition fourth = jooby.options("/fourth", unit.get(Route.Filter.class));
          assertNotNull(fourth);
          assertEquals("/fourth", fourth.pattern());
          assertEquals("OPTIONS", fourth.verb());
          assertEquals("anonymous", fourth.name());
          assertEquals(MediaType.ALL, fourth.consumes());
          assertEquals(MediaType.ALL, fourth.produces());

          expected.add(fourth);

          jooby.start();

        }, boot,
            unit -> {
              List<Route.Definition> found = unit.captured(Route.Definition.class);
              assertEquals(expected, found);
            });
  }

  @Test
  public void putHandlers() throws Exception {

    List<Route.Definition> expected = new LinkedList<>();

    new MockUnit(Binder.class, Route.Handler.class, Route.OneArgHandler.class,
        Route.ZeroArgHandler.class, Route.Filter.class)
        .expect(guice)
        .expect(shutdown)
        .expect(config)
        .expect(env)
        .expect(reload)
        .expect(classInfo)
        .expect(charset)
        .expect(locale)
        .expect(zoneId)
        .expect(timeZone)
        .expect(dateTimeFormatter)
        .expect(numberFormat)
        .expect(decimalFormat)
        .expect(bodyParser)
        .expect(bodyFormatter)
        .expect(session)
        .expect(unit -> {
          Multibinder<Route.Definition> multibinder = unit.mock(Multibinder.class);

          Binder binder = unit.get(Binder.class);

          expect(Multibinder.newSetBinder(binder, Route.Definition.class)).andReturn(multibinder);

          LinkedBindingBuilder<Route.Definition> binding = unit.mock(LinkedBindingBuilder.class);
          expect(multibinder.addBinding()).andReturn(binding).times(4);

          binding.toInstance(unit.capture(Route.Definition.class));
          binding.toInstance(unit.capture(Route.Definition.class));
          binding.toInstance(unit.capture(Route.Definition.class));
          binding.toInstance(unit.capture(Route.Definition.class));
        })
        .expect(webSockets)
        .expect(reqModules)
        .expect(tmpdir)
        .expect(err)
        .run(unit -> {

          Jooby jooby = new Jooby();

          Route.Definition first = jooby.put("/first", unit.get(Route.Handler.class));
          assertNotNull(first);
          assertEquals("/first", first.pattern());
          assertEquals("PUT", first.verb());
          assertEquals("anonymous", first.name());
          assertEquals(MediaType.ALL, first.consumes());
          assertEquals(MediaType.ALL, first.produces());

          expected.add(first);

          Route.Definition second = jooby.put("/second", unit.get(Route.OneArgHandler.class));
          assertNotNull(second);
          assertEquals("/second", second.pattern());
          assertEquals("PUT", second.verb());
          assertEquals("anonymous", second.name());
          assertEquals(MediaType.ALL, second.consumes());
          assertEquals(MediaType.ALL, second.produces());

          expected.add(second);

          Route.Definition third = jooby.put("/third", unit.get(Route.ZeroArgHandler.class));
          assertNotNull(third);
          assertEquals("/third", third.pattern());
          assertEquals("PUT", third.verb());
          assertEquals("anonymous", third.name());
          assertEquals(MediaType.ALL, third.consumes());
          assertEquals(MediaType.ALL, third.produces());

          expected.add(third);

          Route.Definition fourth = jooby.put("/fourth", unit.get(Route.Filter.class));
          assertNotNull(fourth);
          assertEquals("/fourth", fourth.pattern());
          assertEquals("PUT", fourth.verb());
          assertEquals("anonymous", fourth.name());
          assertEquals(MediaType.ALL, fourth.consumes());
          assertEquals(MediaType.ALL, fourth.produces());

          expected.add(fourth);

          jooby.start();

        }, boot,
            unit -> {
              List<Route.Definition> found = unit.captured(Route.Definition.class);
              assertEquals(expected, found);
            });
  }

  @Test
  public void patchHandlers() throws Exception {

    List<Route.Definition> expected = new LinkedList<>();

    new MockUnit(Binder.class, Route.Handler.class, Route.OneArgHandler.class,
        Route.ZeroArgHandler.class, Route.Filter.class)
        .expect(guice)
        .expect(shutdown)
        .expect(config)
        .expect(env)
        .expect(reload)
        .expect(classInfo)
        .expect(charset)
        .expect(locale)
        .expect(zoneId)
        .expect(timeZone)
        .expect(dateTimeFormatter)
        .expect(numberFormat)
        .expect(decimalFormat)
        .expect(bodyParser)
        .expect(bodyFormatter)
        .expect(session)
        .expect(unit -> {
          Multibinder<Route.Definition> multibinder = unit.mock(Multibinder.class);

          Binder binder = unit.get(Binder.class);

          expect(Multibinder.newSetBinder(binder, Route.Definition.class)).andReturn(multibinder);

          LinkedBindingBuilder<Route.Definition> binding = unit.mock(LinkedBindingBuilder.class);
          expect(multibinder.addBinding()).andReturn(binding).times(4);

          binding.toInstance(unit.capture(Route.Definition.class));
          binding.toInstance(unit.capture(Route.Definition.class));
          binding.toInstance(unit.capture(Route.Definition.class));
          binding.toInstance(unit.capture(Route.Definition.class));
        })
        .expect(webSockets)
        .expect(reqModules)
        .expect(tmpdir)
        .expect(err)
        .run(unit -> {

          Jooby jooby = new Jooby();

          Route.Definition first = jooby.patch("/first", unit.get(Route.Handler.class));
          assertNotNull(first);
          assertEquals("/first", first.pattern());
          assertEquals("PATCH", first.verb());
          assertEquals("anonymous", first.name());
          assertEquals(MediaType.ALL, first.consumes());
          assertEquals(MediaType.ALL, first.produces());

          expected.add(first);

          Route.Definition second = jooby.patch("/second", unit.get(Route.OneArgHandler.class));
          assertNotNull(second);
          assertEquals("/second", second.pattern());
          assertEquals("PATCH", second.verb());
          assertEquals("anonymous", second.name());
          assertEquals(MediaType.ALL, second.consumes());
          assertEquals(MediaType.ALL, second.produces());

          expected.add(second);

          Route.Definition third = jooby.patch("/third", unit.get(Route.ZeroArgHandler.class));
          assertNotNull(third);
          assertEquals("/third", third.pattern());
          assertEquals("PATCH", third.verb());
          assertEquals("anonymous", third.name());
          assertEquals(MediaType.ALL, third.consumes());
          assertEquals(MediaType.ALL, third.produces());

          expected.add(third);

          Route.Definition fourth = jooby.patch("/fourth", unit.get(Route.Filter.class));
          assertNotNull(fourth);
          assertEquals("/fourth", fourth.pattern());
          assertEquals("PATCH", fourth.verb());
          assertEquals("anonymous", fourth.name());
          assertEquals(MediaType.ALL, fourth.consumes());
          assertEquals(MediaType.ALL, fourth.produces());

          expected.add(fourth);

          jooby.start();

        }, boot,
            unit -> {
              List<Route.Definition> found = unit.captured(Route.Definition.class);
              assertEquals(expected, found);
            });
  }

  @Test
  public void deleteHandlers() throws Exception {

    List<Route.Definition> expected = new LinkedList<>();

    new MockUnit(Binder.class, Route.Handler.class, Route.OneArgHandler.class,
        Route.ZeroArgHandler.class, Route.Filter.class)
        .expect(guice)
        .expect(shutdown)
        .expect(config)
        .expect(env)
        .expect(reload)
        .expect(classInfo)
        .expect(charset)
        .expect(locale)
        .expect(zoneId)
        .expect(timeZone)
        .expect(dateTimeFormatter)
        .expect(numberFormat)
        .expect(decimalFormat)
        .expect(bodyParser)
        .expect(bodyFormatter)
        .expect(session)
        .expect(unit -> {
          Multibinder<Route.Definition> multibinder = unit.mock(Multibinder.class);

          Binder binder = unit.get(Binder.class);

          expect(Multibinder.newSetBinder(binder, Route.Definition.class)).andReturn(multibinder);

          LinkedBindingBuilder<Route.Definition> binding = unit.mock(LinkedBindingBuilder.class);
          expect(multibinder.addBinding()).andReturn(binding).times(4);

          binding.toInstance(unit.capture(Route.Definition.class));
          binding.toInstance(unit.capture(Route.Definition.class));
          binding.toInstance(unit.capture(Route.Definition.class));
          binding.toInstance(unit.capture(Route.Definition.class));
        })
        .expect(webSockets)
        .expect(reqModules)
        .expect(tmpdir)
        .expect(err)
        .run(unit -> {

          Jooby jooby = new Jooby();

          Route.Definition first = jooby.delete("/first", unit.get(Route.Handler.class));
          assertNotNull(first);
          assertEquals("/first", first.pattern());
          assertEquals("DELETE", first.verb());
          assertEquals("anonymous", first.name());
          assertEquals(MediaType.ALL, first.consumes());
          assertEquals(MediaType.ALL, first.produces());

          expected.add(first);

          Route.Definition second = jooby.delete("/second", unit.get(Route.OneArgHandler.class));
          assertNotNull(second);
          assertEquals("/second", second.pattern());
          assertEquals("DELETE", second.verb());
          assertEquals("anonymous", second.name());
          assertEquals(MediaType.ALL, second.consumes());
          assertEquals(MediaType.ALL, second.produces());

          expected.add(second);

          Route.Definition third = jooby.delete("/third", unit.get(Route.ZeroArgHandler.class));
          assertNotNull(third);
          assertEquals("/third", third.pattern());
          assertEquals("DELETE", third.verb());
          assertEquals("anonymous", third.name());
          assertEquals(MediaType.ALL, third.consumes());
          assertEquals(MediaType.ALL, third.produces());

          expected.add(third);

          Route.Definition fourth = jooby.delete("/fourth", unit.get(Route.Filter.class));
          assertNotNull(fourth);
          assertEquals("/fourth", fourth.pattern());
          assertEquals("DELETE", fourth.verb());
          assertEquals("anonymous", fourth.name());
          assertEquals(MediaType.ALL, fourth.consumes());
          assertEquals(MediaType.ALL, fourth.produces());

          expected.add(fourth);

          jooby.start();

        }, boot,
            unit -> {
              List<Route.Definition> found = unit.captured(Route.Definition.class);
              assertEquals(expected, found);
            });
  }

  @Test
  public void connectHandlers() throws Exception {

    List<Route.Definition> expected = new LinkedList<>();

    new MockUnit(Binder.class, Route.Handler.class, Route.OneArgHandler.class,
        Route.ZeroArgHandler.class, Route.Filter.class)
        .expect(guice)
        .expect(shutdown)
        .expect(config)
        .expect(env)
        .expect(reload)
        .expect(classInfo)
        .expect(charset)
        .expect(locale)
        .expect(zoneId)
        .expect(timeZone)
        .expect(dateTimeFormatter)
        .expect(numberFormat)
        .expect(decimalFormat)
        .expect(bodyParser)
        .expect(bodyFormatter)
        .expect(session)
        .expect(unit -> {
          Multibinder<Route.Definition> multibinder = unit.mock(Multibinder.class);

          Binder binder = unit.get(Binder.class);

          expect(Multibinder.newSetBinder(binder, Route.Definition.class)).andReturn(multibinder);

          LinkedBindingBuilder<Route.Definition> binding = unit.mock(LinkedBindingBuilder.class);
          expect(multibinder.addBinding()).andReturn(binding).times(4);

          binding.toInstance(unit.capture(Route.Definition.class));
          binding.toInstance(unit.capture(Route.Definition.class));
          binding.toInstance(unit.capture(Route.Definition.class));
          binding.toInstance(unit.capture(Route.Definition.class));
        })
        .expect(webSockets)
        .expect(reqModules)
        .expect(tmpdir)
        .expect(err)
        .run(unit -> {

          Jooby jooby = new Jooby();

          Route.Definition first = jooby.connect("/first", unit.get(Route.Handler.class));
          assertNotNull(first);
          assertEquals("/first", first.pattern());
          assertEquals("CONNECT", first.verb());
          assertEquals("anonymous", first.name());
          assertEquals(MediaType.ALL, first.consumes());
          assertEquals(MediaType.ALL, first.produces());

          expected.add(first);

          Route.Definition second = jooby.connect("/second", unit.get(Route.OneArgHandler.class));
          assertNotNull(second);
          assertEquals("/second", second.pattern());
          assertEquals("CONNECT", second.verb());
          assertEquals("anonymous", second.name());
          assertEquals(MediaType.ALL, second.consumes());
          assertEquals(MediaType.ALL, second.produces());

          expected.add(second);

          Route.Definition third = jooby.connect("/third", unit.get(Route.ZeroArgHandler.class));
          assertNotNull(third);
          assertEquals("/third", third.pattern());
          assertEquals("CONNECT", third.verb());
          assertEquals("anonymous", third.name());
          assertEquals(MediaType.ALL, third.consumes());
          assertEquals(MediaType.ALL, third.produces());

          expected.add(third);

          Route.Definition fourth = jooby.connect("/fourth", unit.get(Route.Filter.class));
          assertNotNull(fourth);
          assertEquals("/fourth", fourth.pattern());
          assertEquals("CONNECT", fourth.verb());
          assertEquals("anonymous", fourth.name());
          assertEquals(MediaType.ALL, fourth.consumes());
          assertEquals(MediaType.ALL, fourth.produces());

          expected.add(fourth);

          jooby.start();

        }, boot,
            unit -> {
              List<Route.Definition> found = unit.captured(Route.Definition.class);
              assertEquals(expected, found);
            });
  }

  @Test
  public void traceHandlers() throws Exception {

    List<Route.Definition> expected = new LinkedList<>();

    new MockUnit(Binder.class, Route.Handler.class, Route.OneArgHandler.class,
        Route.ZeroArgHandler.class, Route.Filter.class)
        .expect(guice)
        .expect(shutdown)
        .expect(config)
        .expect(env)
        .expect(reload)
        .expect(classInfo)
        .expect(charset)
        .expect(locale)
        .expect(zoneId)
        .expect(timeZone)
        .expect(dateTimeFormatter)
        .expect(numberFormat)
        .expect(decimalFormat)
        .expect(bodyParser)
        .expect(bodyFormatter)
        .expect(session)
        .expect(unit -> {
          Multibinder<Route.Definition> multibinder = unit.mock(Multibinder.class);

          Binder binder = unit.get(Binder.class);

          expect(Multibinder.newSetBinder(binder, Route.Definition.class)).andReturn(multibinder);

          LinkedBindingBuilder<Route.Definition> binding = unit.mock(LinkedBindingBuilder.class);
          expect(multibinder.addBinding()).andReturn(binding).times(4);

          binding.toInstance(unit.capture(Route.Definition.class));
          binding.toInstance(unit.capture(Route.Definition.class));
          binding.toInstance(unit.capture(Route.Definition.class));
          binding.toInstance(unit.capture(Route.Definition.class));
        })
        .expect(webSockets)
        .expect(reqModules)
        .expect(tmpdir)
        .expect(err)
        .run(unit -> {

          Jooby jooby = new Jooby();

          Route.Definition first = jooby.trace("/first", unit.get(Route.Handler.class));
          assertNotNull(first);
          assertEquals("/first", first.pattern());
          assertEquals("TRACE", first.verb());
          assertEquals("anonymous", first.name());
          assertEquals(MediaType.ALL, first.consumes());
          assertEquals(MediaType.ALL, first.produces());

          expected.add(first);

          Route.Definition second = jooby.trace("/second", unit.get(Route.OneArgHandler.class));
          assertNotNull(second);
          assertEquals("/second", second.pattern());
          assertEquals("TRACE", second.verb());
          assertEquals("anonymous", second.name());
          assertEquals(MediaType.ALL, second.consumes());
          assertEquals(MediaType.ALL, second.produces());

          expected.add(second);

          Route.Definition third = jooby.trace("/third", unit.get(Route.ZeroArgHandler.class));
          assertNotNull(third);
          assertEquals("/third", third.pattern());
          assertEquals("TRACE", third.verb());
          assertEquals("anonymous", third.name());
          assertEquals(MediaType.ALL, third.consumes());
          assertEquals(MediaType.ALL, third.produces());

          expected.add(third);

          Route.Definition fourth = jooby.trace("/fourth", unit.get(Route.Filter.class));
          assertNotNull(fourth);
          assertEquals("/fourth", fourth.pattern());
          assertEquals("TRACE", fourth.verb());
          assertEquals("anonymous", fourth.name());
          assertEquals(MediaType.ALL, fourth.consumes());
          assertEquals(MediaType.ALL, fourth.produces());

          expected.add(fourth);

          jooby.start();

        }, boot,
            unit -> {
              List<Route.Definition> found = unit.captured(Route.Definition.class);
              assertEquals(expected, found);
            });
  }

  @Test
  public void assets() throws Exception {

    List<Route.Definition> expected = new LinkedList<>();

    String path = "/assets/js/file.js";
    new MockUnit(Binder.class, Request.class, Response.class, Route.Chain.class)
        .expect(guice)
        .expect(shutdown)
        .expect(config)
        .expect(env)
        .expect(reload)
        .expect(classInfo)
        .expect(charset)
        .expect(locale)
        .expect(zoneId)
        .expect(timeZone)
        .expect(dateTimeFormatter)
        .expect(numberFormat)
        .expect(decimalFormat)
        .expect(bodyParser)
        .expect(unit -> {
          Multibinder<Body.Formatter> multibinder = unit.mock(Multibinder.class);

          Binder binder = unit.get(Binder.class);

          expect(Multibinder.newSetBinder(binder, Body.Formatter.class)).andReturn(multibinder);

          LinkedBindingBuilder<Formatter> formatReader = unit.mock(LinkedBindingBuilder.class);
          formatReader.toInstance(BuiltinBodyConverter.formatReader);

          LinkedBindingBuilder<Formatter> formatStream = unit.mock(LinkedBindingBuilder.class);
          formatStream.toInstance(BuiltinBodyConverter.formatStream);

          LinkedBindingBuilder<Formatter> formatString = unit.mock(LinkedBindingBuilder.class);
          formatString.toInstance(BuiltinBodyConverter.formatAny);

          LinkedBindingBuilder<Formatter> assetFormatter = unit.mock(LinkedBindingBuilder.class);
          assetFormatter.toInstance(isA(AssetFormatter.class));

          LinkedBindingBuilder<Formatter> formatByteArray = unit.mock(LinkedBindingBuilder.class);
          formatByteArray.toInstance(BuiltinBodyConverter.formatByteArray);

          LinkedBindingBuilder<Formatter> formatByteBuffer = unit.mock(LinkedBindingBuilder.class);
          formatByteBuffer.toInstance(BuiltinBodyConverter.formatByteBuffer);

          expect(multibinder.addBinding()).andReturn(assetFormatter);
          expect(multibinder.addBinding()).andReturn(formatReader);
          expect(multibinder.addBinding()).andReturn(formatStream);
          expect(multibinder.addBinding()).andReturn(formatByteArray);
          expect(multibinder.addBinding()).andReturn(formatByteBuffer);
          expect(multibinder.addBinding()).andReturn(formatString);
        })
        .expect(session)
        .expect(unit -> {
          Multibinder<Route.Definition> multibinder = unit.mock(Multibinder.class);

          Binder binder = unit.get(Binder.class);

          expect(Multibinder.newSetBinder(binder, Route.Definition.class)).andReturn(multibinder);

          LinkedBindingBuilder<Route.Definition> binding = unit.mock(LinkedBindingBuilder.class);
          expect(multibinder.addBinding()).andReturn(binding).times(2);

          binding.toInstance(unit.capture(Route.Definition.class));
          binding.toInstance(unit.capture(Route.Definition.class));
        })
        .expect(webSockets)
        .expect(reqModules)
        .expect(tmpdir)
        .expect(err)
        .expect(
            unit -> {
              Mutant ifModifiedSince = unit.mock(Mutant.class);
              expect(ifModifiedSince.toOptional(Long.class)).andReturn(Optional.empty());

              Request req = unit.get(Request.class);
              expect(req.path()).andReturn(path);
              expect(req.header("If-Modified-Since")).andReturn(ifModifiedSince);

              Response rsp = unit.get(Response.class);
              expect(rsp.header(eq("Last-Modified"), unit.capture(java.util.Date.class)))
                  .andReturn(rsp);
              expect(rsp.type(MediaType.js)).andReturn(rsp);
              expect(rsp.length(20)).andReturn(rsp);
              rsp.send(unit.capture(Asset.class));
            })
        .run(unit -> {
          Jooby jooby = new Jooby();

          Route.Definition assets = jooby.assets("/assets/**");
          expected.add(assets);

          Route.Definition dir = jooby.assets("/dir/**");
          expected.add(dir);

          jooby.start();

          Optional<Route> route = assets.matches(Verb.GET, "/assets/js/file.js",
              MediaType.all, MediaType.ALL);
          assertNotNull(route);
          assertTrue(route.isPresent());

          ((RouteImpl) route.get()).handle(unit.get(Request.class), unit.get(Response.class),
              unit.get(Route.Chain.class));

        }, boot, unit -> {
          Asset asset = unit.captured(Asset.class).iterator().next();
          assertTrue(asset.name().equals(("file.js")));
        }, unit -> {
          List<Route.Definition> found = unit.captured(Route.Definition.class);
          assertEquals(expected, found);
        });
  }

  @Test
  public void mvcRoute() throws Exception {

    new MockUnit(Binder.class)
        .expect(guice)
        .expect(shutdown)
        .expect(config)
        .expect(env)
        .expect(reload)
        .expect(classInfo)
        .expect(charset)
        .expect(locale)
        .expect(zoneId)
        .expect(timeZone)
        .expect(dateTimeFormatter)
        .expect(numberFormat)
        .expect(decimalFormat)
        .expect(bodyParser)
        .expect(bodyFormatter)
        .expect(session)
        .expect(
            unit -> {
              Multibinder<Route.Definition> multibinder = unit.mock(Multibinder.class);

              Binder binder = unit.get(Binder.class);

              expect(Multibinder.newSetBinder(binder, Route.Definition.class)).andReturn(
                  multibinder);

              LinkedBindingBuilder<Route.Definition> binding = unit
                  .mock(LinkedBindingBuilder.class);
              expect(multibinder.addBinding()).andReturn(binding).times(5);

              binding.toInstance(unit.capture(Route.Definition.class));
              binding.toInstance(unit.capture(Route.Definition.class));
              binding.toInstance(unit.capture(Route.Definition.class));
              binding.toInstance(unit.capture(Route.Definition.class));
              binding.toInstance(unit.capture(Route.Definition.class));

              AnnotatedBindingBuilder<SingletonTestRoute> singletonBinding = unit
                  .mock(AnnotatedBindingBuilder.class);
              singletonBinding.in(Scopes.SINGLETON);

              expect(binder.bind(SingletonTestRoute.class)).andReturn(singletonBinding);

              AnnotatedBindingBuilder<GuiceSingletonTestRoute> guiceSingletonBinding = unit
                  .mock(AnnotatedBindingBuilder.class);
              guiceSingletonBinding.in(Scopes.SINGLETON);

              expect(binder.bind(GuiceSingletonTestRoute.class)).andReturn(guiceSingletonBinding);
            })
        .expect(webSockets)
        .expect(unit -> {
          Multibinder<Request.Module> multibinder = unit.mock(Multibinder.class);

          LinkedBindingBuilder<Request.Module> binding = unit.mock(LinkedBindingBuilder.class);
          binding.toInstance(unit.capture(Request.Module.class));

          expect(multibinder.addBinding()).andReturn(binding);

          Binder binder = unit.get(Binder.class);

          expect(Multibinder.newSetBinder(binder, Request.Module.class)).andReturn(multibinder);
        })
        .expect(tmpdir)
        .expect(err)
        .run(unit -> {

          Jooby jooby = new Jooby();
          jooby.use(SingletonTestRoute.class);
          jooby.use(GuiceSingletonTestRoute.class);
          jooby.use(ProtoTestRoute.class);
          jooby.start();

        }, boot, unit -> {
          // assert routes
            List<Route.Definition> defs = unit.captured(Route.Definition.class);
            assertEquals(5, defs.size());

            assertEquals("GET", defs.get(0).verb());
            assertEquals("/singleton", defs.get(0).pattern());
            assertEquals("SingletonTestRoute.m1", defs.get(0).name());

            assertEquals("POST", defs.get(1).verb());
            assertEquals("/singleton", defs.get(1).pattern());
            assertEquals("SingletonTestRoute.m1", defs.get(1).name());

            assertEquals("GET", defs.get(2).verb());
            assertEquals("/singleton", defs.get(2).pattern());
            assertEquals("GuiceSingletonTestRoute.m1", defs.get(2).name());

            assertEquals("POST", defs.get(3).verb());
            assertEquals("/singleton", defs.get(3).pattern());
            assertEquals("GuiceSingletonTestRoute.m1", defs.get(3).name());

            assertEquals("GET", defs.get(4).verb());
            assertEquals("/proto", defs.get(4).pattern());
            assertEquals("ProtoTestRoute.m1", defs.get(4).name());
          }, unit -> {
            // assert proto route are in the correct scope
            Request.Module module = unit.captured(Request.Module.class).iterator().next();
            Binder binder = unit.mock(Binder.class);
            expect(binder.bind(ProtoTestRoute.class)).andReturn(null);
            replay(binder);
            module.configure(binder);
            verify(binder);
          });
  }

  @Test
  public void redirect() throws Exception {
    new MockUnit(Request.class, Response.class)
        .expect(unit -> {
          Response rsp = unit.get(Response.class);
          rsp.redirect(Status.FOUND, "/location");

          rsp.redirect(Status.MOVED_PERMANENTLY, "/location");
        })
        .run(unit -> {
          Jooby jooby = new Jooby();

          Handler redirect = jooby.redirect("/location");
          assertNotNull(redirect);
          redirect.handle(unit.get(Request.class), unit.get(Response.class));

          redirect = jooby.redirect(Status.MOVED_PERMANENTLY, "/location");
          assertNotNull(redirect);
          redirect.handle(unit.get(Request.class), unit.get(Response.class));
        });
  }

  @Test
  public void globHead() throws Exception {
    new MockUnit(Request.class, Response.class)
        .run(unit -> {
          Jooby jooby = new Jooby();

          Route.Definition head = jooby.head();
          assertNotNull(head);
          assertEquals("/**/*", head.pattern());
          assertEquals("HEAD", head.verb());
        });
  }

  @Test
  public void globOptions() throws Exception {
    new MockUnit(Request.class, Response.class)
        .run(unit -> {
          Jooby jooby = new Jooby();

          Route.Definition options = jooby.options();
          assertNotNull(options);
          assertEquals("/**/*", options.pattern());
          assertEquals("OPTIONS", options.verb());
        });
  }

  @Test
  public void globTrace() throws Exception {
    new MockUnit(Request.class, Response.class)
        .run(unit -> {
          Jooby jooby = new Jooby();

          Route.Definition trace = jooby.trace();
          assertNotNull(trace);
          assertEquals("/**/*", trace.pattern());
          assertEquals("TRACE", trace.verb());
        });
  }

  @Test
  public void staticFile() throws Exception {
    new MockUnit(Request.class, Response.class, Route.Chain.class)
        .expect(unit -> {
          Mutant ifModifiedSince = unit.mock(Mutant.class);
          expect(ifModifiedSince.toOptional(Long.class)).andReturn(Optional.empty());

          Request req = unit.get(Request.class);
          expect(req.header("If-Modified-Since")).andReturn(ifModifiedSince);
          expect(req.route()).andReturn(unit.mock(Route.class));

          Response rsp = unit.get(Response.class);
          expect(rsp.header(eq("Last-Modified"), unit.capture(java.util.Date.class)))
              .andReturn(rsp);
          expect(rsp.type(MediaType.js)).andReturn(rsp);
          expect(rsp.length(20)).andReturn(rsp);
          rsp.send(unit.capture(Asset.class));
        })
        .run(
            unit -> {
              Jooby jooby = new Jooby();

              Route.Filter handler = jooby.staticFile("/assets/js/file.js");
              assertNotNull(handler);

              handler.handle(unit.get(Request.class), unit.get(Response.class),
                  unit.get(Route.Chain.class));
            });
  }

  @Test
  public void ws() throws Exception {

    List<WebSocket.Definition> defs = new LinkedList<>();

    new MockUnit(Binder.class)
        .expect(guice)
        .expect(shutdown)
        .expect(config)
        .expect(env)
        .expect(reload)
        .expect(classInfo)
        .expect(charset)
        .expect(locale)
        .expect(zoneId)
        .expect(timeZone)
        .expect(dateTimeFormatter)
        .expect(numberFormat)
        .expect(decimalFormat)
        .expect(bodyParser)
        .expect(bodyFormatter)
        .expect(session)
        .expect(routes)
        .expect(
            unit -> {
              Multibinder<WebSocket.Definition> multibinder = unit.mock(Multibinder.class);

              LinkedBindingBuilder<WebSocket.Definition> binding = unit
                  .mock(LinkedBindingBuilder.class);
              binding.toInstance(unit.capture(WebSocket.Definition.class));

              expect(multibinder.addBinding()).andReturn(binding);

              Binder binder = unit.get(Binder.class);

              expect(Multibinder.newSetBinder(binder, WebSocket.Definition.class)).andReturn(
                  multibinder);
            })
        .expect(reqModules)
        .expect(tmpdir)
        .expect(err)
        .run(unit -> {

          Jooby jooby = new Jooby();

          WebSocket.Definition ws = jooby.ws("/", (socket) -> {
          });
          assertEquals("/", ws.pattern());
          assertEquals(MediaType.all, ws.consumes());
          assertEquals(MediaType.all, ws.produces());
          defs.add(ws);

          jooby.start();

        }, boot, unit -> {
          assertEquals(defs, unit.captured(WebSocket.Definition.class));
        });
  }

  @Test
  public void useStore() throws Exception {

    new MockUnit(Store.class, Binder.class)
        .expect(guice)
        .expect(shutdown)
        .expect(config)
        .expect(env)
        .expect(reload)
        .expect(classInfo)
        .expect(charset)
        .expect(locale)
        .expect(zoneId)
        .expect(timeZone)
        .expect(dateTimeFormatter)
        .expect(numberFormat)
        .expect(decimalFormat)
        .expect(bodyParser)
        .expect(bodyFormatter)
        .expect(unit -> {
          Binder binder = unit.get(Binder.class);

          AnnotatedBindingBuilder<SessionManager> smbinding = unit
              .mock(AnnotatedBindingBuilder.class);
          smbinding.toInstance(isA(SessionManager.class));

          expect(binder.bind(SessionManager.class)).andReturn(smbinding);

          AnnotatedBindingBuilder<Session.Definition> binding = unit
              .mock(AnnotatedBindingBuilder.class);
          binding.toInstance(unit.capture(Session.Definition.class));

          expect(binder.bind(Session.Definition.class)).andReturn(binding);
        })
        .expect(routes)
        .expect(webSockets)
        .expect(reqModules)
        .expect(tmpdir)
        .expect(err)
        .run(unit -> {

          Jooby jooby = new Jooby();
          jooby.use(unit.get(Store.class));

          jooby.start();

        }, boot,
            unit -> {
              Definition def = unit.captured(Session.Definition.class).iterator().next();
              assertEquals(unit.get(Store.class), def.store());
            });
  }

  @Test
  public void useFormatter() throws Exception {

    new MockUnit(Formatter.class, Binder.class)
        .expect(guice)
        .expect(shutdown)
        .expect(config)
        .expect(env)
        .expect(reload)
        .expect(classInfo)
        .expect(charset)
        .expect(locale)
        .expect(zoneId)
        .expect(timeZone)
        .expect(dateTimeFormatter)
        .expect(numberFormat)
        .expect(decimalFormat)
        .expect(session)
        .expect(bodyParser)
        .expect(unit -> {
          Multibinder<Body.Formatter> multibinder = unit.mock(Multibinder.class);

          Binder binder = unit.get(Binder.class);

          expect(Multibinder.newSetBinder(binder, Body.Formatter.class)).andReturn(multibinder);

          LinkedBindingBuilder<Formatter> formatReader = unit.mock(LinkedBindingBuilder.class);
          formatReader.toInstance(BuiltinBodyConverter.formatReader);

          LinkedBindingBuilder<Formatter> formatStream = unit.mock(LinkedBindingBuilder.class);
          formatStream.toInstance(BuiltinBodyConverter.formatStream);

          LinkedBindingBuilder<Formatter> formatString = unit.mock(LinkedBindingBuilder.class);
          formatString.toInstance(BuiltinBodyConverter.formatAny);

          LinkedBindingBuilder<Formatter> customFormatter = unit.mock(LinkedBindingBuilder.class);
          customFormatter.toInstance(unit.get(Formatter.class));

          LinkedBindingBuilder<Formatter> formatByteArray = unit.mock(LinkedBindingBuilder.class);
          formatByteArray.toInstance(BuiltinBodyConverter.formatByteArray);

          LinkedBindingBuilder<Formatter> formatByteBuffer = unit.mock(LinkedBindingBuilder.class);
          formatByteBuffer.toInstance(BuiltinBodyConverter.formatByteBuffer);

          expect(multibinder.addBinding()).andReturn(customFormatter);
          expect(multibinder.addBinding()).andReturn(formatReader);
          expect(multibinder.addBinding()).andReturn(formatStream);
          expect(multibinder.addBinding()).andReturn(formatByteArray);
          expect(multibinder.addBinding()).andReturn(formatByteBuffer);
          expect(multibinder.addBinding()).andReturn(formatString);
        })
        .expect(routes)
        .expect(webSockets)
        .expect(reqModules)
        .expect(tmpdir)
        .expect(err)
        .run(unit -> {

          Jooby jooby = new Jooby();
          jooby.use(unit.get(Formatter.class));

          jooby.start();

        }, boot);
  }

  @Test
  public void useParser() throws Exception {

    new MockUnit(Body.Parser.class, Binder.class)
        .expect(guice)
        .expect(shutdown)
        .expect(config)
        .expect(env)
        .expect(reload)
        .expect(classInfo)
        .expect(charset)
        .expect(locale)
        .expect(zoneId)
        .expect(timeZone)
        .expect(dateTimeFormatter)
        .expect(numberFormat)
        .expect(decimalFormat)
        .expect(unit -> {
          Multibinder<Parser> multibinder = unit.mock(Multibinder.class);

          Binder binder = unit.get(Binder.class);
          unit.mockStatic(Multibinder.class);

          expect(Multibinder.newSetBinder(binder, Body.Parser.class)).andReturn(multibinder);

          LinkedBindingBuilder<Body.Parser> parseString = unit.mock(LinkedBindingBuilder.class);
          parseString.toInstance(BuiltinBodyConverter.parseString);

          LinkedBindingBuilder<Body.Parser> customParser = unit.mock(LinkedBindingBuilder.class);
          customParser.toInstance(unit.get(Body.Parser.class));

          expect(multibinder.addBinding()).andReturn(customParser);
          expect(multibinder.addBinding()).andReturn(parseString);
        })
        .expect(bodyFormatter)
        .expect(session)
        .expect(routes)
        .expect(webSockets)
        .expect(reqModules)
        .expect(tmpdir)
        .expect(err)
        .run(unit -> {

          Jooby jooby = new Jooby();

          jooby.use(unit.get(Body.Parser.class));

          jooby.start();

        }, boot);
  }

  @Test
  public void useModule() throws Exception {

    new MockUnit(Binder.class, Jooby.Module.class)
        .expect(guice)
        .expect(shutdown)
        .expect(config)
        .expect(env)
        .expect(reload)
        .expect(classInfo)
        .expect(charset)
        .expect(locale)
        .expect(zoneId)
        .expect(timeZone)
        .expect(dateTimeFormatter)
        .expect(numberFormat)
        .expect(decimalFormat)
        .expect(bodyParser)
        .expect(bodyFormatter)
        .expect(session)
        .expect(routes)
        .expect(webSockets)
        .expect(reqModules)
        .expect(tmpdir)
        .expect(err)
        .expect(unit -> {
          Binder binder = unit.get(Binder.class);
          Jooby.Module module = unit.get(Jooby.Module.class);

          Config config = ConfigFactory.empty();

          expect(module.config()).andReturn(config);

          module.configure(isA(Env.class), isA(Config.class), eq(binder));

          module.start();

          module.stop();
        })
        .run(unit -> {

          Jooby jooby = new Jooby();

          jooby.use(unit.get(Jooby.Module.class));

          jooby.start();

        }, boot);
  }

  @Test(expected = IllegalStateException.class)
  public void useModuleWithError() throws Exception {

    new MockUnit(Binder.class, Jooby.Module.class)
        .expect(guice)
        .expect(shutdown)
        .expect(config)
        .expect(env)
        .expect(reload)
        .expect(classInfo)
        .expect(charset)
        .expect(locale)
        .expect(zoneId)
        .expect(timeZone)
        .expect(dateTimeFormatter)
        .expect(numberFormat)
        .expect(decimalFormat)
        .expect(bodyParser)
        .expect(bodyFormatter)
        .expect(session)
        .expect(routes)
        .expect(webSockets)
        .expect(reqModules)
        .expect(tmpdir)
        .expect(err)
        .expect(unit -> {
          Binder binder = unit.get(Binder.class);
          Jooby.Module module = unit.get(Jooby.Module.class);

          Config config = ConfigFactory.empty();

          expect(module.config()).andReturn(config);

          module.configure(isA(Env.class), isA(Config.class), eq(binder));
          expectLastCall().andThrow(new NullPointerException());

          module.start();
        })
        .run(unit -> {

          Jooby jooby = new Jooby();

          jooby.use(unit.get(Jooby.Module.class));

          jooby.start();

        }, boot);
  }

  @Test
  public void useRequestModule() throws Exception {

    new MockUnit(Binder.class, Request.Module.class)
        .expect(guice)
        .expect(shutdown)
        .expect(config)
        .expect(env)
        .expect(reload)
        .expect(classInfo)
        .expect(charset)
        .expect(locale)
        .expect(zoneId)
        .expect(timeZone)
        .expect(dateTimeFormatter)
        .expect(numberFormat)
        .expect(decimalFormat)
        .expect(bodyParser)
        .expect(bodyFormatter)
        .expect(session)
        .expect(routes)
        .expect(webSockets)
        .expect(unit -> {
          Multibinder<Request.Module> multibinder = unit.mock(Multibinder.class);

          Binder binder = unit.get(Binder.class);

          expect(Multibinder.newSetBinder(binder, Request.Module.class)).andReturn(multibinder);

          LinkedBindingBuilder<Request.Module> binding = unit.mock(LinkedBindingBuilder.class);
          binding.toInstance(unit.get(Request.Module.class));

          expect(multibinder.addBinding()).andReturn(binding);
        })
        .expect(tmpdir)
        .expect(err)

        .run(unit -> {

          Jooby jooby = new Jooby();

          jooby.use(unit.get(Request.Module.class));

          jooby.start();

        }, boot);
  }

  @Test
  public void useConfig() throws Exception {

    new MockUnit(Binder.class)
        .expect(guice)
        .expect(shutdown)
        .expect(config)
        .expect(env)
        .expect(reload)
        .expect(classInfo)
        .expect(charset)
        .expect(locale)
        .expect(zoneId)
        .expect(timeZone)
        .expect(dateTimeFormatter)
        .expect(numberFormat)
        .expect(decimalFormat)
        .expect(bodyParser)
        .expect(bodyFormatter)
        .expect(session)
        .expect(routes)
        .expect(webSockets)
        .expect(reqModules)
        .expect(tmpdir)
        .expect(err)
        .expect(
            unit -> {
              AnnotatedBindingBuilder<List<Integer>> listAnnotatedBinding = unit
                  .mock(AnnotatedBindingBuilder.class);
              listAnnotatedBinding.toInstance(Arrays.asList(1, 2, 3));

              Binder binder = unit.get(Binder.class);
              Key<List<Integer>> key = (Key<List<Integer>>) Key.get(Types.listOf(Integer.class),
                  Names.named("list"));
              expect(binder.bind(key)).andReturn(listAnnotatedBinding);
            })
        .run(unit -> {

          Jooby jooby = new Jooby();

          jooby.use(ConfigFactory.parseResources("demo.conf"));

          jooby.start();

        }, boot);
  }

  @Test
  public void useMissingConfig() throws Exception {

    new MockUnit(Binder.class)
        .expect(guice)
        .expect(shutdown)
        .expect(config)
        .expect(env)
        .expect(reload)
        .expect(classInfo)
        .expect(charset)
        .expect(locale)
        .expect(zoneId)
        .expect(timeZone)
        .expect(dateTimeFormatter)
        .expect(numberFormat)
        .expect(decimalFormat)
        .expect(bodyParser)
        .expect(bodyFormatter)
        .expect(session)
        .expect(routes)
        .expect(webSockets)
        .expect(reqModules)
        .expect(tmpdir)
        .expect(err)
        .run(unit -> {

          Jooby jooby = new Jooby();

          jooby.use(ConfigFactory.parseResources("missing.conf"));

          jooby.start();

        }, boot);
  }

  @Test
  public void useErr() throws Exception {

    new MockUnit(Binder.class, Err.Handler.class)
        .expect(guice)
        .expect(shutdown)
        .expect(config)
        .expect(env)
        .expect(reload)
        .expect(classInfo)
        .expect(charset)
        .expect(locale)
        .expect(zoneId)
        .expect(timeZone)
        .expect(dateTimeFormatter)
        .expect(numberFormat)
        .expect(decimalFormat)
        .expect(bodyParser)
        .expect(bodyFormatter)
        .expect(session)
        .expect(routes)
        .expect(webSockets)
        .expect(reqModules)
        .expect(tmpdir)
        .expect(unit -> {
          Binder binder = unit.get(Binder.class);

          AnnotatedBindingBuilder<Err.Handler> binding = unit.mock(AnnotatedBindingBuilder.class);
          binding.toInstance(unit.get(Err.Handler.class));

          expect(binder.bind(Err.Handler.class)).andReturn(binding);
        })
        .run(unit -> {

          Jooby jooby = new Jooby();

          jooby.err(unit.get(Err.Handler.class));

          jooby.start();

        }, boot);
  }
}
