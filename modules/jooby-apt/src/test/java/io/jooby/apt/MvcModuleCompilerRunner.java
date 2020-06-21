package io.jooby.apt;

import com.google.common.truth.Truth;
import com.google.testing.compile.JavaFileObjects;
import com.google.testing.compile.JavaSourcesSubjectFactory;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.MvcFactory;
import io.jooby.SneakyThrows;
import io.jooby.internal.converter.ReflectiveBeanConverter;
import org.objectweb.asm.util.ASMifier;

import javax.inject.Provider;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.Assert.assertTrue;

public class MvcModuleCompilerRunner {
  private final TestMvcProcessor processor;
  private final Object instance;
  private final List<String> examples = new ArrayList<>();

  public MvcModuleCompilerRunner(Object instance) throws Exception {
    this(instance, false);
  }

  public MvcModuleCompilerRunner(Object instance, boolean debug) throws Exception {
    this.instance = instance;
    this.processor = new TestMvcProcessor();
    Truth.assert_()
        .about(JavaSourcesSubjectFactory.javaSources())
        .that(sources(sourceNames(instance.getClass())))
        .withCompilerOptions("-Adebug=" + debug)
        .processedWith(processor)
        .compilesWithoutError();
  }

  private String[] sourceNames(Class input) {
    List<String> result = new ArrayList<>();
    while (input != Object.class) {
      result.add(input.getName());
      input = input.getSuperclass();
    }
    return result.toArray(new String[result.size()]);
  }

  public MvcModuleCompilerRunner module(SneakyThrows.Consumer<Jooby> consumer) throws Exception {
    return module(false, consumer);
  }

  public MvcModuleCompilerRunner debugModule(SneakyThrows.Consumer<Jooby> consumer)
      throws Exception {
    for (String example : examples) {
      printExample(example);
    }
    return module(true, consumer);
  }

  private void printExample(String example) throws IOException {
    System.out.println("*************************************************************************");
    System.out.println("******************************** Example ********************************");
    ASMifier.main(new String[]{example});
    System.out.println("*************************************************************************");
  }

  private MvcModuleCompilerRunner module(boolean debug, SneakyThrows.Consumer<Jooby> consumer)
      throws Exception {
    Class clazz = instance.getClass();
    ClassLoader classLoader = processor.getModuleClassLoader(debug);
    String factoryName = clazz.getName() + "$Module";
    Class<? extends MvcFactory> factoryClass = (Class<? extends MvcFactory>) classLoader
        .loadClass(factoryName);
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

    consumer.accept(application);
    return this;
  }

  private static List<JavaFileObject> sources(String... names) throws MalformedURLException {
    Path basedir = basedir().resolve("src").resolve("test").resolve("java");
    List<JavaFileObject> sources = new ArrayList<>();
    for (String name : names) {
      String[] segments = name.split("\\.");
      Path path = Stream.of(segments)
          .limit(segments.length - 1)
          .reduce(basedir, Path::resolve, Path::resolve);
      path = path.resolve(segments[segments.length - 1] + ".java");
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

  public MvcModuleCompilerRunner example(Class example) {
    examples.add(example.getName());
    return this;
  }
}

