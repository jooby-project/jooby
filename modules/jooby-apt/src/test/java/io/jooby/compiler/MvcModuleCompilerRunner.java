package io.jooby.compiler;

import com.google.common.truth.Truth;
import com.google.testing.compile.JavaFileObjects;
import com.google.testing.compile.JavaSourcesSubjectFactory;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.Route;
import io.jooby.SneakyThrows;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.util.ASMifier;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.TraceClassVisitor;

import javax.inject.Provider;
import javax.tools.JavaFileObject;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
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

public class MvcModuleCompilerRunner {
  private final MvcProcessor processor;
  private final Object instance;

  public MvcModuleCompilerRunner(Object instance) throws Exception {
    this.instance = instance;
    this.processor = new MvcProcessor();
    Truth.assert_()
        .about(JavaSourcesSubjectFactory.javaSources())
        .that(sources(instance.getClass().getSimpleName() + ".java"))
        .processedWith(processor)
        .compilesWithoutError();
  }

  public MvcModuleCompilerRunner module(SneakyThrows.Consumer<Jooby> consumer) throws Exception {
    return module(false, consumer);
  }

  public MvcModuleCompilerRunner module(boolean debug, SneakyThrows.Consumer<Jooby> consumer) throws Exception {
    Class clazz = instance.getClass();
    ClassLoader classLoader = processor.getModuleClassLoader(debug);
    String moduleName = clazz.getName() + "$Module";
    Class<? extends Extension> handleClass = (Class<? extends Extension>) classLoader.loadClass(moduleName);
    Constructor<? extends Extension> constructor = handleClass
        .getDeclaredConstructor(Provider.class);
    Provider provider = () -> instance;
    Extension extension = constructor.newInstance(provider);
    Jooby application = new Jooby();
    application.install(extension);
    consumer.accept(application);
    return this;
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

