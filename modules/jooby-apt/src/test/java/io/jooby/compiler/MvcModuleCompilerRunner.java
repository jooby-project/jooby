package io.jooby.compiler;

import com.google.common.truth.Truth;
import com.google.testing.compile.JavaFileObjects;
import com.google.testing.compile.JavaSourcesSubjectFactory;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.MvcModule;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MvcModuleCompilerRunner {
  private final TestMvcProcessor processor;
  private final Object instance;

  public MvcModuleCompilerRunner(Object instance) throws Exception {
    this.instance = instance;
    this.processor = new TestMvcProcessor();
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
    String factoryName = clazz.getName() + "$Factory";
    Class<? extends MvcModule> factoryClass = (Class<? extends MvcModule>) classLoader.loadClass(factoryName);
    Constructor<? extends MvcModule> constructor = factoryClass.getDeclaredConstructor();
    MvcModule factory = constructor.newInstance();
    Provider provider = () -> instance;
    Extension extension = factory.create(provider);
    Jooby application = new Jooby();
    application.install(extension);

    Path services = Paths
        .get(classLoader.getResource("META-INF/services/" + MvcModule.class.getName()).toURI());
    assertTrue(Files.exists(services));
    assertEquals(factoryName, new String(Files.readAllBytes(services), StandardCharsets.UTF_8).trim());

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

