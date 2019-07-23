package io.jooby.compiler;

import com.google.common.truth.Truth;
import com.google.testing.compile.JavaFileObjects;
import com.google.testing.compile.JavaSourcesSubjectFactory;
import io.jooby.Route;
import io.jooby.SneakyThrows;
import org.objectweb.asm.Type;

import javax.inject.Provider;
import javax.tools.JavaFileObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MvcHandlerCompilerRunner {
  private final MvcProcessor processor;
  private final Object instance;

  public MvcHandlerCompilerRunner(Object instance) throws Exception {
    this.instance = instance;
    this.processor = new MvcProcessor();
    Truth.assert_()
        .about(JavaSourcesSubjectFactory.javaSources())
        .that(sources(instance.getClass().getSimpleName() + ".java"))
        .processedWith(processor)
        .compilesWithoutError();
  }

  public MvcHandlerCompilerRunner compile(String executableName, Class[] args,
      SneakyThrows.Consumer<Route.Handler> consumer) throws Exception {
    return compile("GET", executableName, args, false, consumer);
  }

  public MvcHandlerCompilerRunner compile(String httpMethod, String executableName, Class[] args,
      SneakyThrows.Consumer<Route.Handler> consumer) throws Exception {
    return compile(httpMethod, executableName, args, false, consumer);
  }

  public MvcHandlerCompilerRunner compile(String executableName, Class[] args, boolean debug,
      SneakyThrows.Consumer<Route.Handler> consumer) throws Exception {
    return compile("GET", executableName, args, debug, consumer);
  }

  public MvcHandlerCompilerRunner compile(String httpMethod, String executableName, Class[] args, boolean debug,
      SneakyThrows.Consumer<Route.Handler> consumer) throws Exception {
    Class clazz = instance.getClass();
    Method method = clazz.getMethod(executableName, args);
    String key = clazz.getName() + "." + executableName + Type.getMethodDescriptor(method);
//    key = key.replace("[B", "Lbyte[];");
    MvcHandlerCompiler compiler = processor.compilerFor(key);
    assertNotNull("Compiler not found for: " + method, compiler);
    if (debug) {
      System.out.println(compiler);
    }
    String handlerName = clazz.getName() + "$" + httpMethod + "$" + executableName;
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
    if (path.getFileName().toString().equals("jooby-mvc-compiler")) {
      return path;
    }
    return path.resolve("modules").resolve("jooby-mvc-compiler");
  }
}
