/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jte;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.resolve.DirectoryCodeResolver;
import io.jooby.Environment;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.MediaType;
import io.jooby.ServiceRegistry;
import io.jooby.internal.jte.JteModelEncoder;

/**
 * Jte templates: https://jooby.io/modules/jte
 *
 * <pre>
 * </pre>
 *
 * @author edgar
 * @since 3.0.0
 */
public class JteModule implements Extension {

  private Path sourceDirectory;

  private Path classDirectory;

  private TemplateEngine templateEngine;

  /**
   * Creates a new instance. Compiled templates in production mode are expected to be found in the
   * provided class directory.
   *
   * <p>See
   * https://github.com/casid/jte/blob/main/DOCUMENTATION.md#using-a-directory-on-your-server-recommended
   *
   * @param sourceDirectory Where templates are located.
   * @param classDirectory Where compiled templates are located. Only for production mode.
   */
  public JteModule(@NonNull Path sourceDirectory, @NonNull Path classDirectory) {
    this.sourceDirectory = requireNonNull(sourceDirectory, "Source directory is required.");
    this.classDirectory = requireNonNull(classDirectory, "Class directory is required.");
  }

  /**
   * Creates a new instances. Compiled templates in production mode are expected to be found inside
   * classpath.
   *
   * <p>See
   * https://github.com/casid/jte/blob/main/DOCUMENTATION.md#using-the-application-class-loader-since-120
   *
   * @param sourceDirectory Where templates are located.
   */
  public JteModule(@NonNull Path sourceDirectory) {
    this.sourceDirectory = requireNonNull(sourceDirectory, "Source directory is required.");
  }

  /**
   * Creates a new instances.
   *
   * @param templateEngine Attach this module to provided template engine.
   */
  public JteModule(@NonNull TemplateEngine templateEngine) {
    this.templateEngine = requireNonNull(templateEngine, "Template engine is required.");
  }

  @Override
  public void install(@NonNull Jooby application) {
    if (templateEngine == null) {
      this.templateEngine = create(application.getEnvironment(), sourceDirectory, classDirectory);
    }

    ServiceRegistry services = application.getServices();
    services.put(TemplateEngine.class, templateEngine);
    // model and view
    application.encoder(MediaType.html, new JteTemplateEngine(templateEngine));
    // jte models
    application.encoder(new JteModelEncoder());
  }

  /**
   * Creates a template engine.
   *
   * @param environment Jooby environment.
   * @param sourceDirectory The source directory is required for development. Development
   *     environment is when {@link Environment#isActive(String, String...)} returns true for <code>
   *     dev</code> or <code>test</code>.
   * @param classDirectory Class directory is optional. When <code>null</code> we set this to <code>
   *     source-directory/jte-classes</code>. Now, in prod when <code>null</code> jte classes must
   *     be contained within the jar. Otherwise, you need to deploy this directory along with your
   *     application. See
   *     https://github.com/casid/jte/blob/main/DOCUMENTATION.md#precompiling-templates
   * @return
   */
  public static TemplateEngine create(
      @NonNull Environment environment,
      @NonNull Path sourceDirectory,
      @Nullable Path classDirectory) {
    boolean dev = environment.isActive("dev", "test");
    if (dev) {
      requireNonNull(sourceDirectory, "Source directory is required.");
      Path requiredClassDirectory =
          Optional.ofNullable(classDirectory)
              .orElseGet(() -> sourceDirectory.resolve("jte-classes"));
      TemplateEngine engine =
          TemplateEngine.create(
              new DirectoryCodeResolver(sourceDirectory),
              requiredClassDirectory,
              ContentType.Html,
              environment.getClassLoader());
      // Helps jte to use correct classpath while running from jooby run (maven or gradle)
      Optional.ofNullable(System.getProperty("jooby.run.classpath"))
          .map(it -> it.split(File.pathSeparator))
          .map(Stream::of)
          .map(Stream::toList)
          .ifPresent(engine::setClassPath);
      return engine;
    } else {
      return classDirectory == null
          ? TemplateEngine.createPrecompiled(ContentType.Html)
          : TemplateEngine.createPrecompiled(classDirectory, ContentType.Html);
    }
  }
}
