package io.jooby.apt;

import com.google.common.truth.Truth;
import com.google.testing.compile.JavaFileObjects;
import com.google.testing.compile.JavaSourcesSubjectFactory;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.MvcFactory;
import io.jooby.SneakyThrows;
import io.jooby.internal.converter.ReflectiveBeanConverter;

import javax.inject.Provider;
import javax.tools.JavaFileObject;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
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

  public MvcModuleCompilerRunner debugModule(SneakyThrows.Consumer<Jooby> consumer) throws Exception {
    return module(true, consumer);
  }

  private MvcModuleCompilerRunner module(boolean debug, SneakyThrows.Consumer<Jooby> consumer) throws Exception {
    Class clazz = instance.getClass();
    ClassLoader classLoader = processor.getModuleClassLoader(debug);
    String factoryName = clazz.getName() + "$Module";
    Class<? extends MvcFactory> factoryClass = (Class<? extends MvcFactory>) classLoader.loadClass(factoryName);
    Constructor<? extends MvcFactory> constructor = factoryClass.getDeclaredConstructor();
    MvcFactory factory = constructor.newInstance();
    Provider provider = () -> instance;
    Extension extension = factory.create(provider);
    Jooby application = new Jooby();

    application.converter(new ReflectiveBeanConverter());

    application.install(extension);

    Path services = Paths
        .get(classLoader.getResource("META-INF/services/" + MvcFactory.class.getName()).toURI());
    assertTrue(Files.exists(services));

    List<String> clsLst = new ArrayList<String>() {{
        for (Class c = clazz; c != null && c != Object.class; c = c.getSuperclass()) {
          add(c.getName() + "$Module");
        }
      }};
    assertEquals(String.join(System.lineSeparator(), clsLst), new String(Files.readAllBytes(services), StandardCharsets.UTF_8).trim());

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

