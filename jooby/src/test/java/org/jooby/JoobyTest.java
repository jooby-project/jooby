package org.jooby;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Function;

import javax.inject.Provider;
import javax.inject.Singleton;
import javax.net.ssl.SSLContext;

import org.jooby.Session.Definition;
import org.jooby.Session.Store;
import org.jooby.internal.AppPrinter;
import org.jooby.internal.BuiltinParser;
import org.jooby.internal.BuiltinRenderer;
import org.jooby.internal.CookieSessionManager;
import org.jooby.internal.DefaulErrRenderer;
import org.jooby.internal.HttpHandlerImpl;
import org.jooby.internal.ParameterNameProvider;
import org.jooby.internal.RequestScope;
import org.jooby.internal.RouteImpl;
import org.jooby.internal.RouteMetadata;
import org.jooby.internal.ServerSessionManager;
import org.jooby.internal.SessionManager;
import org.jooby.internal.TypeConverters;
import org.jooby.internal.parser.BeanParser;
import org.jooby.internal.parser.DateParser;
import org.jooby.internal.parser.LocalDateParser;
import org.jooby.internal.parser.LocaleParser;
import org.jooby.internal.parser.ParserExecutor;
import org.jooby.internal.parser.StaticMethodParser;
import org.jooby.internal.parser.StringConstructorParser;
import org.jooby.internal.ssl.SslContextProvider;
import org.jooby.mvc.GET;
import org.jooby.mvc.POST;
import org.jooby.mvc.Path;
import org.jooby.scope.RequestScoped;
import org.jooby.spi.HttpHandler;
import org.jooby.spi.Server;
import org.jooby.test.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.escape.Escaper;
import com.google.common.html.HtmlEscapers;
import com.google.common.net.UrlEscapers;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.TypeLiteral;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.binder.AnnotatedConstantBindingBuilder;
import com.google.inject.binder.ConstantBindingBuilder;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.binder.ScopedBindingBuilder;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.multibindings.OptionalBinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.google.inject.util.Types;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigOrigin;
import com.typesafe.config.ConfigValueFactory;

import javaslang.control.Try.CheckedRunnable;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Jooby.class, Guice.class, TypeConverters.class, Multibinder.class,
    OptionalBinder.class, Runtime.class, Thread.class, UrlEscapers.class, HtmlEscapers.class })
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
    ConstantBindingBuilder strCBB = unit.mock(ConstantBindingBuilder.class);
    strCBB.to(isA(String.class));
    expectLastCall().anyTimes();

    AnnotatedConstantBindingBuilder strACBB = unit.mock(AnnotatedConstantBindingBuilder.class);
    expect(strACBB.annotatedWith(isA(Named.class))).andReturn(strCBB).anyTimes();

    LinkedBindingBuilder<List<String>> listOfString = unit.mock(LinkedBindingBuilder.class);
    listOfString.toInstance(isA(List.class));
    expectLastCall().anyTimes();

    LinkedBindingBuilder<Config> configBinding = unit.mock(LinkedBindingBuilder.class);
    configBinding.toInstance(isA(Config.class));
    expectLastCall().anyTimes();
    AnnotatedBindingBuilder<Config> configAnnotatedBinding = unit
        .mock(AnnotatedBindingBuilder.class);

    expect(configAnnotatedBinding.annotatedWith(isA(Named.class))).andReturn(configBinding)
        .anyTimes();
    // root config
    configAnnotatedBinding.toInstance(isA(Config.class));

    Binder binder = unit.get(Binder.class);
    expect(binder.bindConstant()).andReturn(strACBB).anyTimes();
    expect(binder.bind(Config.class)).andReturn(configAnnotatedBinding).anyTimes();
    expect(binder.bind(Key.get(Types.listOf(String.class), Names.named("cors.allowedHeaders"))))
        .andReturn((LinkedBindingBuilder) listOfString);
    expect(binder.bind(Key.get(Types.listOf(String.class), Names.named("cors.allowedMethods"))))
        .andReturn((LinkedBindingBuilder) listOfString);
  };

  private MockUnit.Block env = unit -> {
    Binder binder = unit.get(Binder.class);

    AnnotatedBindingBuilder<Env> binding = unit.mock(AnnotatedBindingBuilder.class);
    binding.toInstance(isA(Env.class));

    expect(binder.bind(Env.class)).andReturn(binding);
  };

  private MockUnit.Block ssl = unit -> {
    Binder binder = unit.get(Binder.class);

    ScopedBindingBuilder sbbSsl = unit.mock(ScopedBindingBuilder.class);

    AnnotatedBindingBuilder<SSLContext> binding = unit.mock(AnnotatedBindingBuilder.class);
    expect(binding.toProvider(SslContextProvider.class)).andReturn(sbbSsl);

    expect(binder.bind(SSLContext.class)).andReturn(binding);
  };

  private MockUnit.Block classInfo = unit -> {
    Binder binder = unit.get(Binder.class);

    AnnotatedBindingBuilder<ParameterNameProvider> binding = unit
        .mock(AnnotatedBindingBuilder.class);
    binding.toInstance(isA(RouteMetadata.class));

    expect(binder.bind(ParameterNameProvider.class)).andReturn(binding);
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

    AnnotatedBindingBuilder<List<Locale>> bindings = unit.mock(AnnotatedBindingBuilder.class);
    bindings.toInstance(isA(List.class));

    expect(binder.bind(Locale.class)).andReturn(binding);

    TypeLiteral<List<Locale>> localeType = (TypeLiteral<List<Locale>>) TypeLiteral
        .get(Types.listOf(Locale.class));
    expect(binder.bind(localeType)).andReturn(bindings);
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

  private MockUnit.Block renderers = unit -> {
    Multibinder<Renderer> multibinder = unit.mock(Multibinder.class);

    Binder binder = unit.get(Binder.class);
    unit.mockStatic(Multibinder.class);

    expect(Multibinder.newSetBinder(binder, Renderer.class)).andReturn(multibinder);

    LinkedBindingBuilder<Renderer> formatAsset = unit.mock(LinkedBindingBuilder.class);
    formatAsset.toInstance(BuiltinRenderer.asset);

    LinkedBindingBuilder<Renderer> formatByteArray = unit.mock(LinkedBindingBuilder.class);
    formatByteArray.toInstance(BuiltinRenderer.bytes);

    LinkedBindingBuilder<Renderer> formatByteBuffer = unit.mock(LinkedBindingBuilder.class);
    formatByteBuffer.toInstance(BuiltinRenderer.byteBuffer);

    LinkedBindingBuilder<Renderer> file = unit.mock(LinkedBindingBuilder.class);
    file.toInstance(BuiltinRenderer.file);

    LinkedBindingBuilder<Renderer> formatStream = unit.mock(LinkedBindingBuilder.class);
    formatStream.toInstance(BuiltinRenderer.stream);

    LinkedBindingBuilder<Renderer> reader = unit.mock(LinkedBindingBuilder.class);
    reader.toInstance(BuiltinRenderer.reader);

    LinkedBindingBuilder<Renderer> charBuffer = unit.mock(LinkedBindingBuilder.class);
    charBuffer.toInstance(BuiltinRenderer.charBuffer);

    LinkedBindingBuilder<Renderer> fchannel = unit.mock(LinkedBindingBuilder.class);
    fchannel.toInstance(BuiltinRenderer.fileChannel);

    LinkedBindingBuilder<Renderer> err = unit.mock(LinkedBindingBuilder.class);
    err.toInstance(isA(DefaulErrRenderer.class));

    LinkedBindingBuilder<Renderer> formatAny = unit.mock(LinkedBindingBuilder.class);
    formatAny.toInstance(BuiltinRenderer.text);

    expect(multibinder.addBinding()).andReturn(formatAsset);
    expect(multibinder.addBinding()).andReturn(formatByteArray);
    expect(multibinder.addBinding()).andReturn(formatByteBuffer);
    expect(multibinder.addBinding()).andReturn(file);
    expect(multibinder.addBinding()).andReturn(charBuffer);
    expect(multibinder.addBinding()).andReturn(formatStream);
    expect(multibinder.addBinding()).andReturn(reader);
    expect(multibinder.addBinding()).andReturn(fchannel);
    expect(multibinder.addBinding()).andReturn(err);
    expect(multibinder.addBinding()).andReturn(formatAny);

  };

  private MockUnit.Block routes = unit -> {
    Multibinder<Route.Definition> multibinder = unit.mock(Multibinder.class);

    Binder binder = unit.get(Binder.class);

    expect(Multibinder.newSetBinder(binder, Route.Definition.class)).andReturn(multibinder);
  };

  private MockUnit.Block routeHandler = unit -> {
    ScopedBindingBuilder routehandlerscope = unit.mock(ScopedBindingBuilder.class);
    routehandlerscope.in(Singleton.class);

    AnnotatedBindingBuilder<HttpHandler> routehandlerbinding = unit
        .mock(AnnotatedBindingBuilder.class);
    expect(routehandlerbinding.to(HttpHandlerImpl.class)).andReturn(routehandlerscope);

    expect(unit.get(Binder.class).bind(HttpHandler.class)).andReturn(routehandlerbinding);
  };

  private MockUnit.Block webSockets = unit -> {
    Multibinder<WebSocket.Definition> multibinder = unit.mock(Multibinder.class);

    Binder binder = unit.get(Binder.class);

    expect(Multibinder.newSetBinder(binder, WebSocket.Definition.class)).andReturn(multibinder);
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

    LinkedBindingBuilder<Err.Handler> ehlbb = unit.mock(LinkedBindingBuilder.class);
    ehlbb.toInstance(isA(Err.DefHandler.class));

    Multibinder<Err.Handler> multibinder = unit.mock(Multibinder.class);
    expect(Multibinder.newSetBinder(binder, Err.Handler.class)).andReturn(multibinder);

    expect(multibinder.addBinding()).andReturn(ehlbb);
  };

  private MockUnit.Block session = unit -> {
    Binder binder = unit.get(Binder.class);

    AnnotatedBindingBuilder<SessionManager> smABB = unit.mock(AnnotatedBindingBuilder.class);
    expect(smABB.to(ServerSessionManager.class)).andReturn(smABB);
    smABB.asEagerSingleton();

    ScopedBindingBuilder ssSBB = unit.mock(ScopedBindingBuilder.class);
    ssSBB.asEagerSingleton();

    AnnotatedBindingBuilder<Store> ssABB = unit.mock(AnnotatedBindingBuilder.class);
    expect(ssABB.to(Session.Mem.class)).andReturn(ssSBB);

    expect(binder.bind(SessionManager.class)).andReturn(smABB);
    expect(binder.bind(Session.Store.class)).andReturn(ssABB);

    AnnotatedBindingBuilder<Session.Definition> sdABB = unit.mock(AnnotatedBindingBuilder.class);
    expect(sdABB.toProvider(isA(com.google.inject.Provider.class))).andReturn(sdABB);
    sdABB.asEagerSingleton();

    expect(binder.bind(Session.Definition.class)).andReturn(sdABB);
  };

  private MockUnit.Block boot = unit -> {
    Module module = unit.captured(Module.class).iterator().next();

    module.configure(unit.get(Binder.class));

    unit.captured(Runnable.class).get(0).run();
  };

  private MockUnit.Block requestScope = unit -> {
    Binder binder = unit.get(Binder.class);

    AnnotatedBindingBuilder<RequestScope> reqscopebinding = unit
        .mock(AnnotatedBindingBuilder.class);
    reqscopebinding.toInstance(isA(RequestScope.class));

    expect(binder.bind(RequestScope.class)).andReturn(reqscopebinding);
    binder.bindScope(eq(RequestScoped.class), isA(RequestScope.class));

    ScopedBindingBuilder reqscope = unit.mock(ScopedBindingBuilder.class);
    reqscope.in(RequestScoped.class);
    reqscope.in(RequestScoped.class);

    AnnotatedBindingBuilder<Request> reqbinding = unit.mock(AnnotatedBindingBuilder.class);
    expect(reqbinding.toProvider(isA(Provider.class))).andReturn(reqscope);

    expect(binder.bind(Request.class)).andReturn(reqbinding);

    ScopedBindingBuilder rspscope = unit.mock(ScopedBindingBuilder.class);
    rspscope.in(RequestScoped.class);
    AnnotatedBindingBuilder<Response> rspbinding = unit.mock(AnnotatedBindingBuilder.class);
    expect(rspbinding.toProvider(isA(Provider.class))).andReturn(rspscope);

    expect(binder.bind(Response.class)).andReturn(rspbinding);

    ScopedBindingBuilder sessionscope = unit.mock(ScopedBindingBuilder.class);
    sessionscope.in(RequestScoped.class);

    AnnotatedBindingBuilder<Session> sessionbinding = unit.mock(AnnotatedBindingBuilder.class);
    expect(sessionbinding.toProvider(isA(Provider.class)))
        .andReturn(sessionscope);

    expect(binder.bind(Session.class)).andReturn(sessionbinding);

    AnnotatedBindingBuilder<Sse> sseb = unit.mock(AnnotatedBindingBuilder.class);
    expect(sseb.toProvider(isA(Provider.class)))
        .andReturn(reqscope);
    expect(binder.bind(Sse.class)).andReturn(sseb);
  };

  private MockUnit.Block params = unit -> {
    Binder binder = unit.get(Binder.class);

    AnnotatedBindingBuilder<ParserExecutor> parambinding = unit
        .mock(AnnotatedBindingBuilder.class);
    parambinding.in(Singleton.class);

    expect(binder.bind(ParserExecutor.class)).andReturn(parambinding);

    Multibinder<Parser> multibinder = unit.mock(Multibinder.class, true);

    for (Parser parser : BuiltinParser.values()) {
      LinkedBindingBuilder<Parser> converterBinding = unit.mock(LinkedBindingBuilder.class);
      converterBinding.toInstance(parser);
      expect(multibinder.addBinding()).andReturn(converterBinding);
    }

    @SuppressWarnings("rawtypes")
    Class[] parserClasses = {
        DateParser.class,
        LocalDateParser.class,
        LocaleParser.class,
        StaticMethodParser.class,
        StaticMethodParser.class,
        StaticMethodParser.class,
        StringConstructorParser.class,
        BeanParser.class
    };

    for (Class<? extends Parser> converter : parserClasses) {
      LinkedBindingBuilder<Parser> converterBinding = unit.mock(LinkedBindingBuilder.class);
      converterBinding.toInstance(isA(converter));
      expect(multibinder.addBinding()).andReturn(converterBinding);
    }

    expect(Multibinder.newSetBinder(binder, Parser.class)).andReturn(multibinder);

  };

  private MockUnit.Block shutdown = unit -> {
    unit.mockStatic(Runtime.class);

    Thread thread = unit.mockConstructor(Thread.class, new Class<?>[]{Runnable.class },
        unit.capture(Runnable.class));

    Runtime runtime = unit.mock(Runtime.class);
    expect(Runtime.getRuntime()).andReturn(runtime).times(2);
    runtime.addShutdownHook(thread);
    expect(runtime.availableProcessors()).andReturn(1);
  };

  private MockUnit.Block guice = unit -> {
    Server server = unit.mock(Server.class);

    server.start();
    server.join();
    server.stop();

    ScopedBindingBuilder serverScope = unit.mock(ScopedBindingBuilder.class);
    serverScope.in(Singleton.class);
    expectLastCall().times(0, 1);

    AnnotatedBindingBuilder<Server> serverBinding = unit.mock(AnnotatedBindingBuilder.class);
    expect(serverBinding.to(isA(Class.class))).andReturn(serverScope).times(0, 1);

    Binder binder = unit.get(Binder.class);
    expect(binder.bind(Server.class)).andReturn(serverBinding).times(0, 1);

    ConfigOrigin configOrigin = unit.mock(ConfigOrigin.class);
    expect(configOrigin.description()).andReturn("test.conf, mock.conf");

    Config config = unit.mock(Config.class);
    expect(config.getString("application.env")).andReturn("dev");
    expect(config.hasPath("server.join")).andReturn(true);
    expect(config.getBoolean("server.join")).andReturn(true);
    expect(config.origin()).andReturn(configOrigin);

    unit.constructor(AppPrinter.class)
        .args(Set.class, Set.class, Config.class)
        .build(isA(Set.class), isA(Set.class), isA(Config.class));

    Injector injector = unit.mock(Injector.class);
    expect(injector.getInstance(Server.class)).andReturn(server).times(1, 2);
    expect(injector.getInstance(Config.class)).andReturn(config);
    expect(injector.getInstance(Route.KEY)).andReturn(Collections.emptySet());
    expect(injector.getInstance(WebSocket.KEY)).andReturn(Collections.emptySet());

    unit.mockStatic(Guice.class);
    expect(Guice.createInjector(eq(Stage.DEVELOPMENT), unit.capture(Module.class))).andReturn(
        injector);

    unit.mockStatic(OptionalBinder.class);

    TypeConverters tc = unit.mockConstructor(TypeConverters.class);
    tc.configure(binder);
  };

  @Test
  public void applicationSecret() throws Exception {

    new MockUnit(Binder.class)
        .expect(
            unit -> {
              Server server = unit.mock(Server.class);
              server.start();
              server.join();
              server.stop();

              ScopedBindingBuilder serverScope = unit.mock(ScopedBindingBuilder.class);
              serverScope.in(Singleton.class);
              expectLastCall().times(0, 1);

              AnnotatedBindingBuilder<Server> serverBinding = unit
                  .mock(AnnotatedBindingBuilder.class);
              expect(serverBinding.to(isA(Class.class))).andReturn(serverScope).times(0, 1);

              Binder binder = unit.get(Binder.class);
              expect(binder.bind(Server.class)).andReturn(serverBinding).times(0, 1);

              unit.constructor(AppPrinter.class)
                  .args(Set.class, Set.class, Config.class)
                  .build(isA(Set.class), isA(Set.class), isA(Config.class));

              ConfigOrigin configOrigin = unit.mock(ConfigOrigin.class);
              expect(configOrigin.description()).andReturn("test.conf, mock.conf");

              Config config = unit.mock(Config.class);
              expect(config.getString("application.env")).andReturn("dev");
              expect(config.hasPath("server.join")).andReturn(true);
              expect(config.getBoolean("server.join")).andReturn(true);
              expect(config.origin()).andReturn(configOrigin);

              Injector injector = unit.mock(Injector.class);
              expect(injector.getInstance(Server.class)).andReturn(server).times(1, 2);
              expect(injector.getInstance(Config.class)).andReturn(config);
              expect(injector.getInstance(Route.KEY)).andReturn(Collections.emptySet());
              expect(injector.getInstance(WebSocket.KEY)).andReturn(Collections.emptySet());

              unit.mockStatic(Guice.class);
              expect(Guice.createInjector(eq(Stage.PRODUCTION), unit.capture(Module.class)))
                  .andReturn(
                      injector);

              unit.mockStatic(OptionalBinder.class);

              TypeConverters tc = unit.mockConstructor(TypeConverters.class);
              tc.configure(binder);
            })
        .expect(shutdown)
        .expect(config)
        .expect(ssl)
        .expect(env)
        .expect(classInfo)
        .expect(charset)
        .expect(locale)
        .expect(zoneId)
        .expect(timeZone)
        .expect(dateTimeFormatter)
        .expect(numberFormat)
        .expect(decimalFormat)
        .expect(renderers)
        .expect(session)
        .expect(routes)
        .expect(routeHandler)
        .expect(params)
        .expect(requestScope)
        .expect(webSockets)
        .expect(tmpdir)
        .expect(err)
        .run(unit -> {

          Jooby jooby = new Jooby();

          jooby.use(ConfigFactory.empty()
              .withValue("application.env", ConfigValueFactory.fromAnyRef("prod"))
              .withValue("application.secret", ConfigValueFactory.fromAnyRef("234")));

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
        .expect(classInfo)
        .expect(ssl)
        .expect(charset)
        .expect(locale)
        .expect(zoneId)
        .expect(timeZone)
        .expect(dateTimeFormatter)
        .expect(numberFormat)
        .expect(decimalFormat)
        .expect(renderers)
        .expect(session)
        .expect(routes)
        .expect(routeHandler)
        .expect(params)
        .expect(requestScope)
        .expect(webSockets)
        .expect(tmpdir)
        .expect(err)
        .run(unit -> {

          Jooby jooby = new Jooby();

          jooby.start();

        }, boot);
  }

  @Test
  public void cookieSession() throws Exception {

    new MockUnit(Binder.class)
        .expect(guice)
        .expect(shutdown)
        .expect(config)
        .expect(env)
        .expect(classInfo)
        .expect(ssl)
        .expect(charset)
        .expect(locale)
        .expect(zoneId)
        .expect(timeZone)
        .expect(dateTimeFormatter)
        .expect(numberFormat)
        .expect(decimalFormat)
        .expect(renderers)
        .expect(unit -> {
          Binder binder = unit.get(Binder.class);

          AnnotatedBindingBuilder<SessionManager> smABB = unit.mock(AnnotatedBindingBuilder.class);
          expect(smABB.to(CookieSessionManager.class)).andReturn(smABB);
          smABB.asEagerSingleton();

          expect(binder.bind(SessionManager.class)).andReturn(smABB);

          AnnotatedBindingBuilder<Session.Definition> sdABB = unit
              .mock(AnnotatedBindingBuilder.class);
          expect(sdABB.toProvider(isA(com.google.inject.Provider.class))).andReturn(sdABB);
          sdABB.asEagerSingleton();

          expect(binder.bind(Session.Definition.class)).andReturn(sdABB);
        })
        .expect(routes)
        .expect(routeHandler)
        .expect(params)
        .expect(requestScope)
        .expect(webSockets)
        .expect(tmpdir)
        .expect(err)
        .run(unit -> {

          Jooby jooby = new Jooby();

          jooby.use(ConfigFactory.empty()
              .withValue("application.secret", ConfigValueFactory.fromAnyRef("234")));

          jooby.cookieSession();

          jooby.start();

        }, boot);
  }

  @Test
  public void cookieSessionShouldFailWhenApplicationSecretIsnotPresent() throws Throwable {

    Jooby jooby = new Jooby();

    jooby.cookieSession();

    jooby.start();
  }

  @Test
  public void onStartStopCallback() throws Exception {

    new MockUnit(Binder.class, CheckedRunnable.class)
        .expect(guice)
        .expect(shutdown)
        .expect(config)
        .expect(env)
        .expect(classInfo)
        .expect(ssl)
        .expect(charset)
        .expect(locale)
        .expect(zoneId)
        .expect(timeZone)
        .expect(dateTimeFormatter)
        .expect(numberFormat)
        .expect(decimalFormat)
        .expect(renderers)
        .expect(session)
        .expect(routes)
        .expect(routeHandler)
        .expect(params)
        .expect(requestScope)
        .expect(webSockets)
        .expect(tmpdir)
        .expect(err)
        .expect(unit -> {
          unit.get(CheckedRunnable.class).run();
          unit.get(CheckedRunnable.class).run();
        })
        .run(unit -> {

          Jooby app = new Jooby()
              .onStart(unit.get(CheckedRunnable.class))
              .onStop(unit.get(CheckedRunnable.class));
          app.start();
          app.stop();

        }, boot);
  }

  @Test(expected = IllegalStateException.class)
  public void appDidnStart() throws Exception {
    new Jooby().require(Object.class);
  }

  @Test
  public void onStopCallbackLogError() throws Exception {

    new MockUnit(Binder.class, CheckedRunnable.class)
        .expect(guice)
        .expect(shutdown)
        .expect(config)
        .expect(env)
        .expect(classInfo)
        .expect(ssl)
        .expect(charset)
        .expect(locale)
        .expect(zoneId)
        .expect(timeZone)
        .expect(dateTimeFormatter)
        .expect(numberFormat)
        .expect(decimalFormat)
        .expect(renderers)
        .expect(session)
        .expect(routes)
        .expect(routeHandler)
        .expect(params)
        .expect(requestScope)
        .expect(webSockets)
        .expect(tmpdir)
        .expect(err)
        .expect(unit -> {
          unit.get(CheckedRunnable.class).run();
          unit.get(CheckedRunnable.class).run();
          expectLastCall().andThrow(new IllegalStateException("intentional err"));
        })
        .run(unit -> {

          Jooby app = new Jooby()
              .onStart(unit.get(CheckedRunnable.class))
              .onStop(unit.get(CheckedRunnable.class));
          app.start();
          app.stop();

        }, boot);
  }

  @Test
  public void defaultsWithCallback() throws Exception {

    Jooby jooby = new Jooby();
    assertNotNull(Jooby.exportRoutes(jooby));
  }

  @Test
  public void customEnv() throws Exception {

    new MockUnit(Binder.class, Env.Builder.class)
        .expect(guice)
        .expect(shutdown)
        .expect(config)
        .expect(unit -> {
          Env env = unit.mock(Env.class);
          expect(env.name()).andReturn("dev").times(2);
          expect(env.startTasks()).andReturn(Collections.emptyList());
          expect(env.stopTasks()).andReturn(Collections.emptyList());

          Env.Builder builder = unit.get(Env.Builder.class);
          expect(builder.build(isA(Config.class), isA(Jooby.class), isA(Locale.class)))
              .andReturn(env);

          unit.mockStatic(UrlEscapers.class);
          unit.mockStatic(HtmlEscapers.class);
          Escaper escaper = unit.mock(Escaper.class);

          expect(UrlEscapers.urlFragmentEscaper()).andReturn(escaper);
          expect(UrlEscapers.urlFormParameterEscaper()).andReturn(escaper);
          expect(UrlEscapers.urlPathSegmentEscaper()).andReturn(escaper);
          expect(HtmlEscapers.htmlEscaper()).andReturn(escaper);

          expect(env.xss(eq("urlFragment"), unit.capture(Function.class))).andReturn(env);
          expect(env.xss(eq("formParam"), unit.capture(Function.class))).andReturn(env);
          expect(env.xss(eq("pathSegment"), unit.capture(Function.class))).andReturn(env);
          expect(env.xss(eq("html"), unit.capture(Function.class))).andReturn(env);

          Binder binder = unit.get(Binder.class);

          AnnotatedBindingBuilder<Env> binding = unit.mock(AnnotatedBindingBuilder.class);
          binding.toInstance(env);

          expect(binder.bind(Env.class)).andReturn(binding);
        })
        .expect(classInfo)
        .expect(ssl)
        .expect(charset)
        .expect(locale)
        .expect(zoneId)
        .expect(timeZone)
        .expect(dateTimeFormatter)
        .expect(numberFormat)
        .expect(decimalFormat)
        .expect(renderers)
        .expect(session)
        .expect(routes)
        .expect(routeHandler)
        .expect(params)
        .expect(requestScope)
        .expect(webSockets)
        .expect(tmpdir)
        .expect(err)
        .run(unit -> {

          Jooby jooby = new Jooby();

          jooby.env(unit.get(Env.Builder.class));

          jooby.start();

        }, boot);
  }

  @Test
  public void exportRoutes() {
    Jooby app = new Jooby();
    app.get("/export", () -> "OK");
    List<Route.Definition> routes = Jooby.exportRoutes(app);
    assertEquals(1, routes.size());
    assertEquals("/export", routes.get(0).pattern());
    assertEquals("GET", routes.get(0).method());
  }

  @Test
  public void exportRoutesFailure() {
    Jooby app = new Jooby();
    // generate an error on bootstrap
    app.use(ConfigFactory.empty().withValue("application.lang", ConfigValueFactory.fromAnyRef("")));

    app.get("/export", () -> "OK");
    List<Route.Definition> routes = Jooby.exportRoutes(app);
    assertEquals(0, routes.size());
  }

  @Test
  public void customLang() throws Exception {

    new MockUnit(Binder.class)
        .expect(guice)
        .expect(shutdown)
        .expect(config)
        .expect(env)
        .expect(classInfo)
        .expect(ssl)
        .expect(charset)
        .expect(locale)
        .expect(zoneId)
        .expect(timeZone)
        .expect(dateTimeFormatter)
        .expect(numberFormat)
        .expect(decimalFormat)
        .expect(renderers)
        .expect(session)
        .expect(routes)
        .expect(routeHandler)
        .expect(params)
        .expect(requestScope)
        .expect(webSockets)
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
              server.join();
              server.stop();
              expectLastCall().andThrow(new Exception());

              ScopedBindingBuilder serverScope = unit.mock(ScopedBindingBuilder.class);
              serverScope.in(Singleton.class);
              expectLastCall().times(0, 1);

              AnnotatedBindingBuilder<Server> serverBinding = unit
                  .mock(AnnotatedBindingBuilder.class);
              expect(serverBinding.to(isA(Class.class))).andReturn(serverScope).times(0, 1);

              Binder binder = unit.get(Binder.class);
              expect(binder.bind(Server.class)).andReturn(serverBinding).times(0, 1);

              unit.constructor(AppPrinter.class)
                  .args(Set.class, Set.class, Config.class)
                  .build(isA(Set.class), isA(Set.class), isA(Config.class));

              ConfigOrigin configOrigin = unit.mock(ConfigOrigin.class);
              expect(configOrigin.description()).andReturn("test.conf, mock.conf");

              Config config = unit.mock(Config.class);
              expect(config.getString("application.env")).andReturn("dev");
              expect(config.hasPath("server.join")).andReturn(true);
              expect(config.getBoolean("server.join")).andReturn(true);
              expect(config.origin()).andReturn(configOrigin);

              Injector injector = unit.mock(Injector.class);
              expect(injector.getInstance(Server.class)).andReturn(server).times(1, 2);
              expect(injector.getInstance(Config.class)).andReturn(config);
              expect(injector.getInstance(Route.KEY)).andReturn(Collections.emptySet());
              expect(injector.getInstance(WebSocket.KEY)).andReturn(Collections.emptySet());

              unit.mockStatic(Guice.class);
              expect(Guice.createInjector(eq(Stage.DEVELOPMENT), unit.capture(Module.class)))
                  .andReturn(
                      injector);

              unit.mockStatic(OptionalBinder.class);

              TypeConverters tc = unit.mockConstructor(TypeConverters.class);
              tc.configure(binder);
            })
        .expect(shutdown)
        .expect(config)
        .expect(env)
        .expect(classInfo)
        .expect(ssl)
        .expect(charset)
        .expect(locale)
        .expect(zoneId)
        .expect(timeZone)
        .expect(dateTimeFormatter)
        .expect(numberFormat)
        .expect(decimalFormat)
        .expect(renderers)
        .expect(session)
        .expect(routes)
        .expect(routeHandler)
        .expect(params)
        .expect(requestScope)
        .expect(webSockets)
        .expect(tmpdir)
        .expect(err)
        .run(unit -> {

          Jooby jooby = new Jooby();

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
        .expect(classInfo)
        .expect(ssl)
        .expect(charset)
        .expect(locale)
        .expect(zoneId)
        .expect(timeZone)
        .expect(dateTimeFormatter)
        .expect(numberFormat)
        .expect(decimalFormat)
        .expect(renderers)
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
        .expect(routeHandler)
        .expect(params)
        .expect(requestScope)
        .expect(webSockets)
        .expect(tmpdir)
        .expect(err)
        .run(unit -> {

          Jooby jooby = new Jooby();

          Route.Definition first = jooby.use("/filter", unit.get(Route.Filter.class));
          assertNotNull(first);
          assertEquals("/filter", first.pattern());
          assertEquals("*", first.method());
          assertEquals("/anonymous", first.name());
          assertEquals(MediaType.ALL, first.consumes());
          assertEquals(MediaType.ALL, first.produces());

          expected.add(first);

          Route.Definition second = jooby.use("GET", "*", unit.get(Route.Filter.class));
          assertNotNull(second);
          assertEquals("/**", second.pattern());
          assertEquals("GET", second.method());
          assertEquals("/anonymous", second.name());
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
        .expect(classInfo)
        .expect(ssl)
        .expect(charset)
        .expect(locale)
        .expect(zoneId)
        .expect(timeZone)
        .expect(dateTimeFormatter)
        .expect(numberFormat)
        .expect(decimalFormat)
        .expect(renderers)
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
        .expect(routeHandler)
        .expect(params)
        .expect(requestScope)
        .expect(webSockets)
        .expect(tmpdir)
        .expect(err)
        .run(unit -> {

          Jooby jooby = new Jooby();

          Route.Definition first = jooby.use("/first", unit.get(Route.Handler.class));
          assertNotNull(first);
          assertEquals("/first", first.pattern());
          assertEquals("*", first.method());
          assertEquals("/anonymous", first.name());
          assertEquals(MediaType.ALL, first.consumes());
          assertEquals(MediaType.ALL, first.produces());

          expected.add(first);

          Route.Definition second = jooby.use("GET", "*", unit.get(Route.Handler.class));
          assertNotNull(second);
          assertEquals("/**", second.pattern());
          assertEquals("GET", second.method());
          assertEquals("/anonymous", second.name());
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
  public void postHandlers() throws Exception {

    List<Route.Definition> expected = new LinkedList<>();

    new MockUnit(Binder.class, Route.Handler.class, Route.OneArgHandler.class,
        Route.ZeroArgHandler.class, Route.Filter.class)
            .expect(guice)
            .expect(shutdown)
            .expect(config)
            .expect(env)
            .expect(classInfo)
            .expect(ssl)
            .expect(charset)
            .expect(locale)
            .expect(zoneId)
            .expect(timeZone)
            .expect(dateTimeFormatter)
            .expect(numberFormat)
            .expect(decimalFormat)
            .expect(renderers)
            .expect(session)
            .expect(unit -> {
              Multibinder<Route.Definition> multibinder = unit.mock(Multibinder.class);

              Binder binder = unit.get(Binder.class);

              expect(Multibinder.newSetBinder(binder, Route.Definition.class))
                  .andReturn(multibinder);

              LinkedBindingBuilder<Route.Definition> binding = unit
                  .mock(LinkedBindingBuilder.class);
              expect(multibinder.addBinding()).andReturn(binding).times(4);

              binding.toInstance(unit.capture(Route.Definition.class));
              binding.toInstance(unit.capture(Route.Definition.class));
              binding.toInstance(unit.capture(Route.Definition.class));
              binding.toInstance(unit.capture(Route.Definition.class));
            })
            .expect(routeHandler)
            .expect(params)
            .expect(requestScope)
            .expect(webSockets)
            .expect(tmpdir)
            .expect(err)
            .run(unit -> {

              Jooby jooby = new Jooby();

              Route.Definition first = jooby.post("/first", unit.get(Route.Handler.class));
              assertNotNull(first);
              assertEquals("/first", first.pattern());
              assertEquals("POST", first.method());
              assertEquals("/anonymous", first.name());
              assertEquals(MediaType.ALL, first.consumes());
              assertEquals(MediaType.ALL, first.produces());

              expected.add(first);

              Route.Definition second = jooby.post("/second", unit.get(Route.OneArgHandler.class));
              assertNotNull(second);
              assertEquals("/second", second.pattern());
              assertEquals("POST", second.method());
              assertEquals("/anonymous", second.name());
              assertEquals(MediaType.ALL, second.consumes());
              assertEquals(MediaType.ALL, second.produces());

              expected.add(second);

              Route.Definition third = jooby.post("/third", unit.get(Route.ZeroArgHandler.class));
              assertNotNull(third);
              assertEquals("/third", third.pattern());
              assertEquals("POST", third.method());
              assertEquals("/anonymous", third.name());
              assertEquals(MediaType.ALL, third.consumes());
              assertEquals(MediaType.ALL, third.produces());

              expected.add(third);

              Route.Definition fourth = jooby.post("/fourth", unit.get(Route.Filter.class));
              assertNotNull(fourth);
              assertEquals("/fourth", fourth.pattern());
              assertEquals("POST", fourth.method());
              assertEquals("/anonymous", fourth.name());
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
            .expect(classInfo)
            .expect(ssl)
            .expect(charset)
            .expect(locale)
            .expect(zoneId)
            .expect(timeZone)
            .expect(dateTimeFormatter)
            .expect(numberFormat)
            .expect(decimalFormat)
            .expect(renderers)
            .expect(session)
            .expect(unit -> {
              Multibinder<Route.Definition> multibinder = unit.mock(Multibinder.class);

              Binder binder = unit.get(Binder.class);

              expect(Multibinder.newSetBinder(binder, Route.Definition.class))
                  .andReturn(multibinder);

              LinkedBindingBuilder<Route.Definition> binding = unit
                  .mock(LinkedBindingBuilder.class);
              expect(multibinder.addBinding()).andReturn(binding).times(4);

              binding.toInstance(unit.capture(Route.Definition.class));
              binding.toInstance(unit.capture(Route.Definition.class));
              binding.toInstance(unit.capture(Route.Definition.class));
              binding.toInstance(unit.capture(Route.Definition.class));
            })
            .expect(routeHandler)
            .expect(params)
            .expect(requestScope)
            .expect(webSockets)
            .expect(tmpdir)
            .expect(err)
            .run(unit -> {

              Jooby jooby = new Jooby();

              Route.Definition first = jooby.head("/first", unit.get(Route.Handler.class));
              assertNotNull(first);
              assertEquals("/first", first.pattern());
              assertEquals("HEAD", first.method());
              assertEquals("/anonymous", first.name());
              assertEquals(MediaType.ALL, first.consumes());
              assertEquals(MediaType.ALL, first.produces());

              expected.add(first);

              Route.Definition second = jooby.head("/second", unit.get(Route.OneArgHandler.class));
              assertNotNull(second);
              assertEquals("/second", second.pattern());
              assertEquals("HEAD", second.method());
              assertEquals("/anonymous", second.name());
              assertEquals(MediaType.ALL, second.consumes());
              assertEquals(MediaType.ALL, second.produces());

              expected.add(second);

              Route.Definition third = jooby.head("/third", unit.get(Route.ZeroArgHandler.class));
              assertNotNull(third);
              assertEquals("/third", third.pattern());
              assertEquals("HEAD", third.method());
              assertEquals("/anonymous", third.name());
              assertEquals(MediaType.ALL, third.consumes());
              assertEquals(MediaType.ALL, third.produces());

              expected.add(third);

              Route.Definition fourth = jooby.head("/fourth", unit.get(Route.Filter.class));
              assertNotNull(fourth);
              assertEquals("/fourth", fourth.pattern());
              assertEquals("HEAD", fourth.method());
              assertEquals("/anonymous", fourth.name());
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
            .expect(classInfo)
            .expect(ssl)
            .expect(charset)
            .expect(locale)
            .expect(zoneId)
            .expect(timeZone)
            .expect(dateTimeFormatter)
            .expect(numberFormat)
            .expect(decimalFormat)
            .expect(renderers)
            .expect(session)
            .expect(unit -> {
              Multibinder<Route.Definition> multibinder = unit.mock(Multibinder.class);

              Binder binder = unit.get(Binder.class);

              expect(Multibinder.newSetBinder(binder, Route.Definition.class))
                  .andReturn(multibinder);

              LinkedBindingBuilder<Route.Definition> binding = unit
                  .mock(LinkedBindingBuilder.class);
              expect(multibinder.addBinding()).andReturn(binding).times(4);

              binding.toInstance(unit.capture(Route.Definition.class));
              binding.toInstance(unit.capture(Route.Definition.class));
              binding.toInstance(unit.capture(Route.Definition.class));
              binding.toInstance(unit.capture(Route.Definition.class));
            })
            .expect(routeHandler)
            .expect(params)
            .expect(requestScope)
            .expect(webSockets)
            .expect(tmpdir)
            .expect(err)
            .run(unit -> {

              Jooby jooby = new Jooby();

              Route.Definition first = jooby.options("/first", unit.get(Route.Handler.class));
              assertNotNull(first);
              assertEquals("/first", first.pattern());
              assertEquals("OPTIONS", first.method());
              assertEquals("/anonymous", first.name());
              assertEquals(MediaType.ALL, first.consumes());
              assertEquals(MediaType.ALL, first.produces());

              expected.add(first);

              Route.Definition second = jooby.options("/second",
                  unit.get(Route.OneArgHandler.class));
              assertNotNull(second);
              assertEquals("/second", second.pattern());
              assertEquals("OPTIONS", second.method());
              assertEquals("/anonymous", second.name());
              assertEquals(MediaType.ALL, second.consumes());
              assertEquals(MediaType.ALL, second.produces());

              expected.add(second);

              Route.Definition third = jooby.options("/third",
                  unit.get(Route.ZeroArgHandler.class));
              assertNotNull(third);
              assertEquals("/third", third.pattern());
              assertEquals("OPTIONS", third.method());
              assertEquals("/anonymous", third.name());
              assertEquals(MediaType.ALL, third.consumes());
              assertEquals(MediaType.ALL, third.produces());

              expected.add(third);

              Route.Definition fourth = jooby.options("/fourth", unit.get(Route.Filter.class));
              assertNotNull(fourth);
              assertEquals("/fourth", fourth.pattern());
              assertEquals("OPTIONS", fourth.method());
              assertEquals("/anonymous", fourth.name());
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
            .expect(classInfo)
            .expect(ssl)
            .expect(charset)
            .expect(locale)
            .expect(zoneId)
            .expect(timeZone)
            .expect(dateTimeFormatter)
            .expect(numberFormat)
            .expect(decimalFormat)
            .expect(renderers)
            .expect(session)
            .expect(unit -> {
              Multibinder<Route.Definition> multibinder = unit.mock(Multibinder.class);

              Binder binder = unit.get(Binder.class);

              expect(Multibinder.newSetBinder(binder, Route.Definition.class))
                  .andReturn(multibinder);

              LinkedBindingBuilder<Route.Definition> binding = unit
                  .mock(LinkedBindingBuilder.class);
              expect(multibinder.addBinding()).andReturn(binding).times(4);

              binding.toInstance(unit.capture(Route.Definition.class));
              binding.toInstance(unit.capture(Route.Definition.class));
              binding.toInstance(unit.capture(Route.Definition.class));
              binding.toInstance(unit.capture(Route.Definition.class));
            })
            .expect(routeHandler)
            .expect(params)
            .expect(requestScope)
            .expect(webSockets)
            .expect(tmpdir)
            .expect(err)
            .run(unit -> {

              Jooby jooby = new Jooby();

              Route.Definition first = jooby.put("/first", unit.get(Route.Handler.class));
              assertNotNull(first);
              assertEquals("/first", first.pattern());
              assertEquals("PUT", first.method());
              assertEquals("/anonymous", first.name());
              assertEquals(MediaType.ALL, first.consumes());
              assertEquals(MediaType.ALL, first.produces());

              expected.add(first);

              Route.Definition second = jooby.put("/second", unit.get(Route.OneArgHandler.class));
              assertNotNull(second);
              assertEquals("/second", second.pattern());
              assertEquals("PUT", second.method());
              assertEquals("/anonymous", second.name());
              assertEquals(MediaType.ALL, second.consumes());
              assertEquals(MediaType.ALL, second.produces());

              expected.add(second);

              Route.Definition third = jooby.put("/third", unit.get(Route.ZeroArgHandler.class));
              assertNotNull(third);
              assertEquals("/third", third.pattern());
              assertEquals("PUT", third.method());
              assertEquals("/anonymous", third.name());
              assertEquals(MediaType.ALL, third.consumes());
              assertEquals(MediaType.ALL, third.produces());

              expected.add(third);

              Route.Definition fourth = jooby.put("/fourth", unit.get(Route.Filter.class));
              assertNotNull(fourth);
              assertEquals("/fourth", fourth.pattern());
              assertEquals("PUT", fourth.method());
              assertEquals("/anonymous", fourth.name());
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
            .expect(classInfo)
            .expect(ssl)
            .expect(charset)
            .expect(locale)
            .expect(zoneId)
            .expect(timeZone)
            .expect(dateTimeFormatter)
            .expect(numberFormat)
            .expect(decimalFormat)
            .expect(renderers)
            .expect(session)
            .expect(unit -> {
              Multibinder<Route.Definition> multibinder = unit.mock(Multibinder.class);

              Binder binder = unit.get(Binder.class);

              expect(Multibinder.newSetBinder(binder, Route.Definition.class))
                  .andReturn(multibinder);

              LinkedBindingBuilder<Route.Definition> binding = unit
                  .mock(LinkedBindingBuilder.class);
              expect(multibinder.addBinding()).andReturn(binding).times(4);

              binding.toInstance(unit.capture(Route.Definition.class));
              binding.toInstance(unit.capture(Route.Definition.class));
              binding.toInstance(unit.capture(Route.Definition.class));
              binding.toInstance(unit.capture(Route.Definition.class));
            })
            .expect(routeHandler)
            .expect(params)
            .expect(requestScope)
            .expect(webSockets)
            .expect(tmpdir)
            .expect(err)
            .run(unit -> {

              Jooby jooby = new Jooby();

              Route.Definition first = jooby.patch("/first", unit.get(Route.Handler.class));
              assertNotNull(first);
              assertEquals("/first", first.pattern());
              assertEquals("PATCH", first.method());
              assertEquals("/anonymous", first.name());
              assertEquals(MediaType.ALL, first.consumes());
              assertEquals(MediaType.ALL, first.produces());

              expected.add(first);

              Route.Definition second = jooby.patch("/second", unit.get(Route.OneArgHandler.class));
              assertNotNull(second);
              assertEquals("/second", second.pattern());
              assertEquals("PATCH", second.method());
              assertEquals("/anonymous", second.name());
              assertEquals(MediaType.ALL, second.consumes());
              assertEquals(MediaType.ALL, second.produces());

              expected.add(second);

              Route.Definition third = jooby.patch("/third", unit.get(Route.ZeroArgHandler.class));
              assertNotNull(third);
              assertEquals("/third", third.pattern());
              assertEquals("PATCH", third.method());
              assertEquals("/anonymous", third.name());
              assertEquals(MediaType.ALL, third.consumes());
              assertEquals(MediaType.ALL, third.produces());

              expected.add(third);

              Route.Definition fourth = jooby.patch("/fourth", unit.get(Route.Filter.class));
              assertNotNull(fourth);
              assertEquals("/fourth", fourth.pattern());
              assertEquals("PATCH", fourth.method());
              assertEquals("/anonymous", fourth.name());
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
            .expect(classInfo)
            .expect(ssl)
            .expect(charset)
            .expect(locale)
            .expect(zoneId)
            .expect(timeZone)
            .expect(dateTimeFormatter)
            .expect(numberFormat)
            .expect(decimalFormat)
            .expect(renderers)
            .expect(session)
            .expect(unit -> {
              Multibinder<Route.Definition> multibinder = unit.mock(Multibinder.class);

              Binder binder = unit.get(Binder.class);

              expect(Multibinder.newSetBinder(binder, Route.Definition.class))
                  .andReturn(multibinder);

              LinkedBindingBuilder<Route.Definition> binding = unit
                  .mock(LinkedBindingBuilder.class);
              expect(multibinder.addBinding()).andReturn(binding).times(4);

              binding.toInstance(unit.capture(Route.Definition.class));
              binding.toInstance(unit.capture(Route.Definition.class));
              binding.toInstance(unit.capture(Route.Definition.class));
              binding.toInstance(unit.capture(Route.Definition.class));
            })
            .expect(routeHandler)
            .expect(params)
            .expect(requestScope)
            .expect(webSockets)
            .expect(tmpdir)
            .expect(err)
            .run(unit -> {

              Jooby jooby = new Jooby();

              Route.Definition first = jooby.delete("/first", unit.get(Route.Handler.class));
              assertNotNull(first);
              assertEquals("/first", first.pattern());
              assertEquals("DELETE", first.method());
              assertEquals("/anonymous", first.name());
              assertEquals(MediaType.ALL, first.consumes());
              assertEquals(MediaType.ALL, first.produces());

              expected.add(first);

              Route.Definition second = jooby.delete("/second",
                  unit.get(Route.OneArgHandler.class));
              assertNotNull(second);
              assertEquals("/second", second.pattern());
              assertEquals("DELETE", second.method());
              assertEquals("/anonymous", second.name());
              assertEquals(MediaType.ALL, second.consumes());
              assertEquals(MediaType.ALL, second.produces());

              expected.add(second);

              Route.Definition third = jooby.delete("/third", unit.get(Route.ZeroArgHandler.class));
              assertNotNull(third);
              assertEquals("/third", third.pattern());
              assertEquals("DELETE", third.method());
              assertEquals("/anonymous", third.name());
              assertEquals(MediaType.ALL, third.consumes());
              assertEquals(MediaType.ALL, third.produces());

              expected.add(third);

              Route.Definition fourth = jooby.delete("/fourth", unit.get(Route.Filter.class));
              assertNotNull(fourth);
              assertEquals("/fourth", fourth.pattern());
              assertEquals("DELETE", fourth.method());
              assertEquals("/anonymous", fourth.name());
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
            .expect(classInfo)
            .expect(ssl)
            .expect(charset)
            .expect(locale)
            .expect(zoneId)
            .expect(timeZone)
            .expect(dateTimeFormatter)
            .expect(numberFormat)
            .expect(decimalFormat)
            .expect(renderers)
            .expect(session)
            .expect(unit -> {
              Multibinder<Route.Definition> multibinder = unit.mock(Multibinder.class);

              Binder binder = unit.get(Binder.class);

              expect(Multibinder.newSetBinder(binder, Route.Definition.class))
                  .andReturn(multibinder);

              LinkedBindingBuilder<Route.Definition> binding = unit
                  .mock(LinkedBindingBuilder.class);
              expect(multibinder.addBinding()).andReturn(binding).times(4);

              binding.toInstance(unit.capture(Route.Definition.class));
              binding.toInstance(unit.capture(Route.Definition.class));
              binding.toInstance(unit.capture(Route.Definition.class));
              binding.toInstance(unit.capture(Route.Definition.class));
            })
            .expect(routeHandler)
            .expect(params)
            .expect(requestScope)
            .expect(webSockets)
            .expect(tmpdir)
            .expect(err)
            .run(unit -> {

              Jooby jooby = new Jooby();

              Route.Definition first = jooby.connect("/first", unit.get(Route.Handler.class));
              assertNotNull(first);
              assertEquals("/first", first.pattern());
              assertEquals("CONNECT", first.method());
              assertEquals("/anonymous", first.name());
              assertEquals(MediaType.ALL, first.consumes());
              assertEquals(MediaType.ALL, first.produces());

              expected.add(first);

              Route.Definition second = jooby.connect("/second",
                  unit.get(Route.OneArgHandler.class));
              assertNotNull(second);
              assertEquals("/second", second.pattern());
              assertEquals("CONNECT", second.method());
              assertEquals("/anonymous", second.name());
              assertEquals(MediaType.ALL, second.consumes());
              assertEquals(MediaType.ALL, second.produces());

              expected.add(second);

              Route.Definition third = jooby.connect("/third",
                  unit.get(Route.ZeroArgHandler.class));
              assertNotNull(third);
              assertEquals("/third", third.pattern());
              assertEquals("CONNECT", third.method());
              assertEquals("/anonymous", third.name());
              assertEquals(MediaType.ALL, third.consumes());
              assertEquals(MediaType.ALL, third.produces());

              expected.add(third);

              Route.Definition fourth = jooby.connect("/fourth", unit.get(Route.Filter.class));
              assertNotNull(fourth);
              assertEquals("/fourth", fourth.pattern());
              assertEquals("CONNECT", fourth.method());
              assertEquals("/anonymous", fourth.name());
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
            .expect(classInfo)
            .expect(ssl)
            .expect(charset)
            .expect(locale)
            .expect(zoneId)
            .expect(timeZone)
            .expect(dateTimeFormatter)
            .expect(numberFormat)
            .expect(decimalFormat)
            .expect(renderers)
            .expect(session)
            .expect(unit -> {
              Multibinder<Route.Definition> multibinder = unit.mock(Multibinder.class);

              Binder binder = unit.get(Binder.class);

              expect(Multibinder.newSetBinder(binder, Route.Definition.class))
                  .andReturn(multibinder);

              LinkedBindingBuilder<Route.Definition> binding = unit
                  .mock(LinkedBindingBuilder.class);
              expect(multibinder.addBinding()).andReturn(binding).times(4);

              binding.toInstance(unit.capture(Route.Definition.class));
              binding.toInstance(unit.capture(Route.Definition.class));
              binding.toInstance(unit.capture(Route.Definition.class));
              binding.toInstance(unit.capture(Route.Definition.class));
            })
            .expect(routeHandler)
            .expect(params)
            .expect(requestScope)
            .expect(webSockets)
            .expect(tmpdir)
            .expect(err)
            .run(unit -> {

              Jooby jooby = new Jooby();

              Route.Definition first = jooby.trace("/first", unit.get(Route.Handler.class));
              assertNotNull(first);
              assertEquals("/first", first.pattern());
              assertEquals("TRACE", first.method());
              assertEquals("/anonymous", first.name());
              assertEquals(MediaType.ALL, first.consumes());
              assertEquals(MediaType.ALL, first.produces());

              expected.add(first);

              Route.Definition second = jooby.trace("/second", unit.get(Route.OneArgHandler.class));
              assertNotNull(second);
              assertEquals("/second", second.pattern());
              assertEquals("TRACE", second.method());
              assertEquals("/anonymous", second.name());
              assertEquals(MediaType.ALL, second.consumes());
              assertEquals(MediaType.ALL, second.produces());

              expected.add(second);

              Route.Definition third = jooby.trace("/third", unit.get(Route.ZeroArgHandler.class));
              assertNotNull(third);
              assertEquals("/third", third.pattern());
              assertEquals("TRACE", third.method());
              assertEquals("/anonymous", third.name());
              assertEquals(MediaType.ALL, third.consumes());
              assertEquals(MediaType.ALL, third.produces());

              expected.add(third);

              Route.Definition fourth = jooby.trace("/fourth", unit.get(Route.Filter.class));
              assertNotNull(fourth);
              assertEquals("/fourth", fourth.pattern());
              assertEquals("TRACE", fourth.method());
              assertEquals("/anonymous", fourth.name());
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

    String path = "/org/jooby/JoobyTest.js";
    new MockUnit(Binder.class, Request.class, Response.class, Route.Chain.class)
        .expect(guice)
        .expect(shutdown)
        .expect(config)
        .expect(env)
        .expect(classInfo)
        .expect(ssl)
        .expect(charset)
        .expect(locale)
        .expect(zoneId)
        .expect(timeZone)
        .expect(dateTimeFormatter)
        .expect(numberFormat)
        .expect(decimalFormat)
        .expect(unit -> {
          Multibinder<Renderer> multibinder = unit.mock(Multibinder.class);

          Binder binder = unit.get(Binder.class);

          unit.mockStatic(Multibinder.class);
          expect(Multibinder.newSetBinder(binder, Renderer.class)).andReturn(multibinder);

          LinkedBindingBuilder<Renderer> customFormatter = unit
              .mock(LinkedBindingBuilder.class);
          customFormatter.toInstance(BuiltinRenderer.asset);

          LinkedBindingBuilder<Renderer> formatByteArray = unit.mock(LinkedBindingBuilder.class);
          formatByteArray.toInstance(BuiltinRenderer.bytes);

          LinkedBindingBuilder<Renderer> formatByteBuffer = unit.mock(LinkedBindingBuilder.class);
          formatByteBuffer.toInstance(BuiltinRenderer.byteBuffer);

          LinkedBindingBuilder<Renderer> file = unit.mock(LinkedBindingBuilder.class);
          file.toInstance(BuiltinRenderer.file);

          LinkedBindingBuilder<Renderer> formatStream = unit.mock(LinkedBindingBuilder.class);
          formatStream.toInstance(BuiltinRenderer.stream);

          LinkedBindingBuilder<Renderer> reader = unit.mock(LinkedBindingBuilder.class);
          reader.toInstance(BuiltinRenderer.reader);

          LinkedBindingBuilder<Renderer> charBuffer = unit.mock(LinkedBindingBuilder.class);
          charBuffer.toInstance(BuiltinRenderer.charBuffer);

          LinkedBindingBuilder<Renderer> fchannel = unit.mock(LinkedBindingBuilder.class);
          fchannel.toInstance(BuiltinRenderer.fileChannel);

          LinkedBindingBuilder<Renderer> err = unit.mock(LinkedBindingBuilder.class);
          err.toInstance(isA(DefaulErrRenderer.class));

          LinkedBindingBuilder<Renderer> formatAny = unit.mock(LinkedBindingBuilder.class);
          formatAny.toInstance(BuiltinRenderer.text);

          expect(multibinder.addBinding()).andReturn(customFormatter);
          expect(multibinder.addBinding()).andReturn(formatByteArray);
          expect(multibinder.addBinding()).andReturn(formatByteBuffer);
          expect(multibinder.addBinding()).andReturn(file);
          expect(multibinder.addBinding()).andReturn(charBuffer);
          expect(multibinder.addBinding()).andReturn(formatStream);
          expect(multibinder.addBinding()).andReturn(reader);
          expect(multibinder.addBinding()).andReturn(fchannel);
          expect(multibinder.addBinding()).andReturn(err);
          expect(multibinder.addBinding()).andReturn(formatAny);
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
        .expect(routeHandler)
        .expect(params)
        .expect(requestScope)
        .expect(webSockets)
        .expect(tmpdir)
        .expect(err)
        .expect(unit -> {
          Mutant ifModifiedSince = unit.mock(Mutant.class);
          expect(ifModifiedSince.toOptional(Long.class)).andReturn(Optional.empty());

          Mutant ifnm = unit.mock(Mutant.class);
          expect(ifnm.toOptional()).andReturn(Optional.empty());

          Request req = unit.get(Request.class);
          expect(req.path()).andReturn(path).times(2);
          expect(req.header("If-Modified-Since")).andReturn(ifModifiedSince);
          expect(req.header("If-None-Match")).andReturn(ifnm);

          Response rsp = unit.get(Response.class);
          expect(rsp.header(eq("Last-Modified"), unit.capture(java.util.Date.class)))
              .andReturn(rsp);
          expect(rsp.header(eq("ETag"), isA(String.class))).andReturn(rsp);
          rsp.send(isA(Asset.class));

          Route.Chain chain = unit.get(Route.Chain.class);
          chain.next(req, rsp);
        })
        .run(unit -> {
          Jooby jooby = new Jooby();

          Route.Definition assets = jooby.assets("/org/jooby/**");
          expected.add(assets);

          Route.Definition dir = jooby.assets("/dir/**");
          expected.add(dir);

          jooby.start();

          Optional<Route> route = assets.matches("GET", "/org/jooby/JoobyTest.js",
              MediaType.all, MediaType.ALL);
          assertNotNull(route);
          assertTrue(route.isPresent());

          ((RouteImpl) route.get()).handle(unit.get(Request.class), unit.get(Response.class),
              unit.get(Route.Chain.class));

        }, boot, unit -> {
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
        .expect(classInfo)
        .expect(ssl)
        .expect(charset)
        .expect(locale)
        .expect(zoneId)
        .expect(timeZone)
        .expect(dateTimeFormatter)
        .expect(numberFormat)
        .expect(decimalFormat)
        .expect(renderers)
        .expect(session)
        .expect(unit -> {
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

          expect(binder.bind(SingletonTestRoute.class)).andReturn(null);

          expect(binder.bind(GuiceSingletonTestRoute.class)).andReturn(null);

          expect(binder.bind(ProtoTestRoute.class)).andReturn(null);
        })
        .expect(routeHandler)
        .expect(params)
        .expect(requestScope)
        .expect(webSockets)
        .expect(tmpdir)
        .expect(err)
        .run(unit -> {

          Jooby jooby = new Jooby();
          jooby.use(SingletonTestRoute.class);
          jooby.use(GuiceSingletonTestRoute.class);
          jooby.use(ProtoTestRoute.class);
          jooby.start();

        },
            boot,
            unit -> {
              // assert routes
              List<Route.Definition> defs = unit.captured(Route.Definition.class);
              assertEquals(5, defs.size());

              assertEquals("GET", defs.get(0).method());
              assertEquals("/singleton", defs.get(0).pattern());
              assertEquals("/SingletonTestRoute.m1", defs.get(0).name());

              assertEquals("POST", defs.get(1).method());
              assertEquals("/singleton", defs.get(1).pattern());
              assertEquals("/SingletonTestRoute.m1", defs.get(1).name());

              assertEquals("GET", defs.get(2).method());
              assertEquals("/singleton", defs.get(2).pattern());
              assertEquals("/GuiceSingletonTestRoute.m1", defs.get(2).name());

              assertEquals("POST", defs.get(3).method());
              assertEquals("/singleton", defs.get(3).pattern());
              assertEquals("/GuiceSingletonTestRoute.m1", defs.get(3).name());

              assertEquals("GET", defs.get(4).method());
              assertEquals("/proto", defs.get(4).pattern());
              assertEquals("/ProtoTestRoute.m1", defs.get(4).name());
            });
  }

  @Test
  public void globHead() throws Exception {
    new MockUnit(Request.class, Response.class)
        .run(unit -> {
          Jooby jooby = new Jooby();

          Route.Definition head = jooby.head();
          assertNotNull(head);
          assertEquals("/**", head.pattern());
          assertEquals("HEAD", head.method());
        });
  }

  @Test
  public void globOptions() throws Exception {
    new MockUnit(Request.class, Response.class)
        .run(unit -> {
          Jooby jooby = new Jooby();

          Route.Definition options = jooby.options();
          assertNotNull(options);
          assertEquals("/**", options.pattern());
          assertEquals("OPTIONS", options.method());
        });
  }

  @Test
  public void globTrace() throws Exception {
    new MockUnit(Request.class, Response.class)
        .run(unit -> {
          Jooby jooby = new Jooby();

          Route.Definition trace = jooby.trace();
          assertNotNull(trace);
          assertEquals("/**", trace.pattern());
          assertEquals("TRACE", trace.method());
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
        .expect(classInfo)
        .expect(ssl)
        .expect(charset)
        .expect(locale)
        .expect(zoneId)
        .expect(timeZone)
        .expect(dateTimeFormatter)
        .expect(numberFormat)
        .expect(decimalFormat)
        .expect(renderers)
        .expect(session)
        .expect(routes)
        .expect(routeHandler)
        .expect(params)
        .expect(requestScope)
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
        .expect(classInfo)
        .expect(ssl)
        .expect(charset)
        .expect(locale)
        .expect(zoneId)
        .expect(timeZone)
        .expect(dateTimeFormatter)
        .expect(numberFormat)
        .expect(decimalFormat)
        .expect(renderers)
        .expect(
            unit -> {
              Binder binder = unit.get(Binder.class);

              AnnotatedBindingBuilder<SessionManager> smABB = unit
                  .mock(AnnotatedBindingBuilder.class);
              expect(smABB.to(ServerSessionManager.class)).andReturn(smABB);
              smABB.asEagerSingleton();

              ScopedBindingBuilder ssSBB = unit.mock(ScopedBindingBuilder.class);
              ssSBB.asEagerSingleton();

              AnnotatedBindingBuilder<Store> ssABB = unit.mock(AnnotatedBindingBuilder.class);
              expect(ssABB.to(unit.get(Session.Store.class).getClass())).andReturn(ssSBB);

              expect(binder.bind(SessionManager.class)).andReturn(smABB);
              expect(binder.bind(Session.Store.class)).andReturn(ssABB);

              AnnotatedBindingBuilder<Session.Definition> sdABB = unit
                  .mock(AnnotatedBindingBuilder.class);
              expect(sdABB.toProvider(unit.capture(com.google.inject.Provider.class)))
                  .andReturn(sdABB);
              sdABB.asEagerSingleton();

              expect(binder.bind(Session.Definition.class)).andReturn(sdABB);
            })
        .expect(routes)
        .expect(routeHandler)
        .expect(params)
        .expect(requestScope)
        .expect(webSockets)
        .expect(tmpdir)
        .expect(err)
        .run(unit -> {

          Jooby jooby = new Jooby();
          jooby.session(unit.get(Store.class).getClass());

          jooby.start();

        }, boot, unit -> {
          Definition def = (Definition) unit.captured(com.google.inject.Provider.class)
              .iterator().next().get();
          assertEquals(unit.get(Store.class).getClass(), def.store());
        });
  }

  @Test
  public void renderer() throws Exception {

    new MockUnit(Renderer.class, Binder.class)
        .expect(guice)
        .expect(shutdown)
        .expect(config)
        .expect(env)
        .expect(classInfo)
        .expect(ssl)
        .expect(charset)
        .expect(locale)
        .expect(zoneId)
        .expect(timeZone)
        .expect(dateTimeFormatter)
        .expect(numberFormat)
        .expect(decimalFormat)
        .expect(session)
        .expect(unit -> {
          Multibinder<Renderer> multibinder = unit.mock(Multibinder.class);

          Binder binder = unit.get(Binder.class);

          unit.mockStatic(Multibinder.class);
          expect(Multibinder.newSetBinder(binder, Renderer.class)).andReturn(multibinder);

          LinkedBindingBuilder<Renderer> customFormatter = unit
              .mock(LinkedBindingBuilder.class);
          customFormatter.toInstance(unit.get(Renderer.class));

          LinkedBindingBuilder<Renderer> formatAsset = unit.mock(LinkedBindingBuilder.class);
          formatAsset.toInstance(BuiltinRenderer.asset);

          LinkedBindingBuilder<Renderer> formatByteArray = unit.mock(LinkedBindingBuilder.class);
          formatByteArray.toInstance(BuiltinRenderer.bytes);

          LinkedBindingBuilder<Renderer> formatByteBuffer = unit.mock(LinkedBindingBuilder.class);
          formatByteBuffer.toInstance(BuiltinRenderer.byteBuffer);

          LinkedBindingBuilder<Renderer> file = unit.mock(LinkedBindingBuilder.class);
          file.toInstance(BuiltinRenderer.file);

          LinkedBindingBuilder<Renderer> formatStream = unit.mock(LinkedBindingBuilder.class);
          formatStream.toInstance(BuiltinRenderer.stream);

          LinkedBindingBuilder<Renderer> reader = unit.mock(LinkedBindingBuilder.class);
          reader.toInstance(BuiltinRenderer.reader);

          LinkedBindingBuilder<Renderer> charBuffer = unit.mock(LinkedBindingBuilder.class);
          charBuffer.toInstance(BuiltinRenderer.charBuffer);

          LinkedBindingBuilder<Renderer> fchannel = unit.mock(LinkedBindingBuilder.class);
          fchannel.toInstance(BuiltinRenderer.fileChannel);

          LinkedBindingBuilder<Renderer> err = unit.mock(LinkedBindingBuilder.class);
          err.toInstance(isA(DefaulErrRenderer.class));

          LinkedBindingBuilder<Renderer> formatAny = unit.mock(LinkedBindingBuilder.class);
          formatAny.toInstance(BuiltinRenderer.text);

          expect(multibinder.addBinding()).andReturn(formatAsset);
          expect(multibinder.addBinding()).andReturn(formatByteArray);
          expect(multibinder.addBinding()).andReturn(formatByteBuffer);
          expect(multibinder.addBinding()).andReturn(file);
          expect(multibinder.addBinding()).andReturn(charBuffer);
          expect(multibinder.addBinding()).andReturn(formatStream);
          expect(multibinder.addBinding()).andReturn(reader);
          expect(multibinder.addBinding()).andReturn(fchannel);
          expect(multibinder.addBinding()).andReturn(customFormatter);
          expect(multibinder.addBinding()).andReturn(err);
          expect(multibinder.addBinding()).andReturn(formatAny);
        })
        .expect(routes)
        .expect(routeHandler)
        .expect(params)
        .expect(requestScope)
        .expect(webSockets)
        .expect(tmpdir)
        .expect(err)
        .run(unit -> {

          Jooby jooby = new Jooby();
          jooby.renderer(unit.get(Renderer.class));

          jooby.start();

        }, boot);
  }

  @Test
  @SuppressWarnings("rawtypes")
  public void useParser() throws Exception {

    new MockUnit(Parser.class, Binder.class)
        .expect(guice)
        .expect(shutdown)
        .expect(config)
        .expect(env)
        .expect(classInfo)
        .expect(ssl)
        .expect(charset)
        .expect(locale)
        .expect(zoneId)
        .expect(timeZone)
        .expect(dateTimeFormatter)
        .expect(numberFormat)
        .expect(decimalFormat)
        .expect(renderers)
        .expect(unit -> {
          Binder binder = unit.get(Binder.class);

          AnnotatedBindingBuilder<ParserExecutor> parambinding = unit
              .mock(AnnotatedBindingBuilder.class);
          parambinding.in(Singleton.class);

          expect(binder.bind(ParserExecutor.class)).andReturn(parambinding);

          Multibinder<Parser> multibinder = unit.mock(Multibinder.class, true);

          LinkedBindingBuilder<Parser> customParser = unit.mock(LinkedBindingBuilder.class);
          customParser.toInstance(unit.get(Parser.class));

          for (Parser parser : BuiltinParser.values()) {
            LinkedBindingBuilder<Parser> converterBinding = unit.mock(LinkedBindingBuilder.class);
            converterBinding.toInstance(parser);
            expect(multibinder.addBinding()).andReturn(converterBinding);
          }

          expect(multibinder.addBinding()).andReturn(customParser);

          Class[] parserClasses = {
              DateParser.class,
              LocalDateParser.class,
              LocaleParser.class,
              StaticMethodParser.class,
              StaticMethodParser.class,
              StaticMethodParser.class,
              StringConstructorParser.class,
              BeanParser.class
          };

          for (Class<? extends Parser> converter : parserClasses) {
            LinkedBindingBuilder<Parser> converterBinding = unit.mock(LinkedBindingBuilder.class);
            converterBinding.toInstance(isA(converter));
            expect(multibinder.addBinding()).andReturn(converterBinding);
          }

          expect(Multibinder.newSetBinder(binder, Parser.class)).andReturn(multibinder);
        })
        .expect(session)
        .expect(routes)
        .expect(routeHandler)
        .expect(requestScope)
        .expect(webSockets)
        .expect(tmpdir)
        .expect(err)
        .run(unit -> {

          Jooby jooby = new Jooby();

          jooby.parser(unit.get(Parser.class));

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
        .expect(classInfo)
        .expect(ssl)
        .expect(charset)
        .expect(locale)
        .expect(zoneId)
        .expect(timeZone)
        .expect(dateTimeFormatter)
        .expect(numberFormat)
        .expect(decimalFormat)
        .expect(renderers)
        .expect(session)
        .expect(routes)
        .expect(routeHandler)
        .expect(params)
        .expect(requestScope)
        .expect(webSockets)
        .expect(tmpdir)
        .expect(err)
        .expect(unit -> {
          Binder binder = unit.get(Binder.class);
          Jooby.Module module = unit.get(Jooby.Module.class);

          Config config = ConfigFactory.empty();

          expect(module.config()).andReturn(config).times(2);

          module.configure(isA(Env.class), isA(Config.class), eq(binder));
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
        .expect(classInfo)
        .expect(ssl)
        .expect(charset)
        .expect(locale)
        .expect(zoneId)
        .expect(timeZone)
        .expect(dateTimeFormatter)
        .expect(numberFormat)
        .expect(decimalFormat)
        .expect(renderers)
        .expect(session)
        .expect(routes)
        .expect(params)
        .expect(requestScope)
        .expect(webSockets)
        .expect(tmpdir)
        .expect(err)
        .expect(unit -> {
          Binder binder = unit.get(Binder.class);
          Jooby.Module module = unit.get(Jooby.Module.class);

          Config config = ConfigFactory.empty();

          expect(module.config()).andReturn(config).times(2);

          module.configure(isA(Env.class), isA(Config.class), eq(binder));
          expectLastCall().andThrow(new NullPointerException());
        })
        .run(unit -> {

          Jooby jooby = new Jooby();

          jooby.use(unit.get(Jooby.Module.class));

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
        .expect(classInfo)
        .expect(ssl)
        .expect(charset)
        .expect(locale)
        .expect(zoneId)
        .expect(timeZone)
        .expect(dateTimeFormatter)
        .expect(numberFormat)
        .expect(decimalFormat)
        .expect(renderers)
        .expect(session)
        .expect(routes)
        .expect(routeHandler)
        .expect(params)
        .expect(requestScope)
        .expect(webSockets)
        .expect(tmpdir)
        .expect(err)
        .expect(unit -> {
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

          jooby.use(ConfigFactory.parseResources(getClass(), "JoobyTest.conf"));

          jooby.start();

        }, boot);
  }

  @Test
  public void customConf() throws Exception {

    new MockUnit(Binder.class)
        .expect(guice)
        .expect(shutdown)
        .expect(config)
        .expect(env)
        .expect(classInfo)
        .expect(ssl)
        .expect(charset)
        .expect(locale)
        .expect(zoneId)
        .expect(timeZone)
        .expect(dateTimeFormatter)
        .expect(numberFormat)
        .expect(decimalFormat)
        .expect(renderers)
        .expect(session)
        .expect(routes)
        .expect(routeHandler)
        .expect(params)
        .expect(requestScope)
        .expect(webSockets)
        .expect(tmpdir)
        .expect(err)
        .run(unit -> {

          Jooby jooby = new Jooby();

          jooby.conf("JoobyTest.conf");

          jooby.start();

        }, boot);
  }

  @Test
  public void customConfFile() throws Exception {

    new MockUnit(Binder.class)
        .expect(guice)
        .expect(shutdown)
        .expect(config)
        .expect(env)
        .expect(classInfo)
        .expect(ssl)
        .expect(charset)
        .expect(locale)
        .expect(zoneId)
        .expect(timeZone)
        .expect(dateTimeFormatter)
        .expect(numberFormat)
        .expect(decimalFormat)
        .expect(renderers)
        .expect(session)
        .expect(routes)
        .expect(routeHandler)
        .expect(params)
        .expect(requestScope)
        .expect(webSockets)
        .expect(tmpdir)
        .expect(err)
        .run(unit -> {

          Jooby jooby = new Jooby();

          jooby.conf(new File("JoobyTest.conf"));

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
        .expect(ssl)
        .expect(classInfo)
        .expect(charset)
        .expect(locale)
        .expect(zoneId)
        .expect(timeZone)
        .expect(dateTimeFormatter)
        .expect(numberFormat)
        .expect(decimalFormat)
        .expect(renderers)
        .expect(session)
        .expect(routes)
        .expect(routeHandler)
        .expect(params)
        .expect(requestScope)
        .expect(webSockets)
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
        .expect(ssl)
        .expect(classInfo)
        .expect(charset)
        .expect(locale)
        .expect(zoneId)
        .expect(timeZone)
        .expect(dateTimeFormatter)
        .expect(numberFormat)
        .expect(decimalFormat)
        .expect(renderers)
        .expect(session)
        .expect(routes)
        .expect(routeHandler)
        .expect(params)
        .expect(requestScope)
        .expect(webSockets)
        .expect(tmpdir)
        .expect(unit -> {
          Binder binder = unit.get(Binder.class);

          LinkedBindingBuilder<Err.Handler> ehlbb = unit.mock(LinkedBindingBuilder.class);
          ehlbb.toInstance(unit.get(Err.Handler.class));

          LinkedBindingBuilder<Err.Handler> dehlbb = unit.mock(LinkedBindingBuilder.class);
          dehlbb.toInstance(isA(Err.DefHandler.class));

          Multibinder<Err.Handler> multibinder = unit.mock(Multibinder.class);
          expect(Multibinder.newSetBinder(binder, Err.Handler.class)).andReturn(multibinder);

          expect(multibinder.addBinding()).andReturn(ehlbb);
          expect(multibinder.addBinding()).andReturn(dehlbb);
        })
        .run(unit -> {

          Jooby jooby = new Jooby();

          jooby.err(unit.get(Err.Handler.class));

          jooby.start();

        }, boot);
  }
}
