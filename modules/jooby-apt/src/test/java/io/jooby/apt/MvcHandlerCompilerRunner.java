package io.jooby.apt;

import com.google.common.truth.Truth;
import com.google.testing.compile.JavaFileObjects;
import com.google.testing.compile.JavaSourcesSubjectFactory;
import io.jooby.Route;
import io.jooby.SneakyThrows;
import io.jooby.internal.apt.HandlerCompiler;

import javax.inject.Provider;
import javax.tools.JavaFileObject;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MvcHandlerCompilerRunner {
  private final TestMvcProcessor processor;
  private final Object instance;

  public MvcHandlerCompilerRunner(Object instance) throws Exception {
    this.instance = instance;
    this.processor = new TestMvcProcessor();
    Truth.assert_()
        .about(JavaSourcesSubjectFactory.javaSources())
        .that(sources(instance.getClass().getSimpleName() + ".java"))
        .processedWith(processor)
        .compilesWithoutError();
  }

  public MvcHandlerCompilerRunner compile(String path,
      SneakyThrows.Consumer<Route.Handler> consumer) throws Exception {
    return compile("GET", path, consumer);
  }

  public MvcHandlerCompilerRunner debug(String path,
      SneakyThrows.Consumer<Route.Handler> consumer) throws Exception {
    return compile("GET", path, true, consumer);
  }

  public MvcHandlerCompilerRunner compile(String method, String path,
      SneakyThrows.Consumer<Route.Handler> consumer) throws Exception {
    return compile(method, path, false, consumer);
  }

  public MvcHandlerCompilerRunner debug(String method, String path,
      SneakyThrows.Consumer<Route.Handler> consumer) throws Exception {
    return compile(method, path, true, consumer);
  }

  private MvcHandlerCompilerRunner compile(String method, String path, boolean debug,
      SneakyThrows.Consumer<Route.Handler> consumer) throws Exception {
    String key = method.toUpperCase() + path;
    HandlerCompiler compiler = processor.compilerFor(key);
    assertNotNull("Compiler not found for: " + key, compiler);
    if (debug) {
      System.out.println(compiler);
    }
    String handlerName = compiler.getGeneratedClass();
    Class<? extends Route.Handler> handleClass = compileClass(handlerName, compiler.compile());
    Constructor<? extends Route.Handler> constructor = handleClass
        .getDeclaredConstructor(Provider.class);
    Provider provider = () -> instance;
    Route.Handler handler = constructor.newInstance(provider);
    consumer.accept(handler);
    return this;
  }

  private Class<? extends Route.Handler> compileClass(String className, byte[] bytes)
      throws Exception {
    return (Class<? extends Route.Handler>) new ClassLoader(getClass().getClassLoader()) {
      @Override protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (className.equals(name)) {
          return defineClass(name, bytes, 0, bytes.length);
        }
        return super.findClass(name);
      }
    }.loadClass(className);
  }

  private static List<JavaFileObject> sources(String... names) throws MalformedURLException {
    Path basedir = basedir().resolve("src").resolve("test").resolve("java").resolve("source");
    List<JavaFileObject> sources = new ArrayList<>();
    for (String name : names) {
      Path path = basedir.resolve(name);
      assertTrue(path.toString(), Files.exists(path));
      sources.add(JavaFileObjects.forResource(path.toUri().toURL()));
    }
    return sources;
  }

  private static Path basedir() {
    Path path = Paths.get(System.getProperty("user.dir"));
    if (path.getFileName().toString().equals("jooby-apt")) {
      return path;
    }
    return path.resolve("modules").resolve("jooby-apt");
  }
}
