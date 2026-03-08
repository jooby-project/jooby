/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.maven;

import static org.apache.maven.plugins.annotations.LifecyclePhase.PROCESS_CLASSES;
import static org.apache.maven.plugins.annotations.ResolutionScope.COMPILE_PLUS_RUNTIME;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import cz.habarta.typescript.generator.DateMapping;
import cz.habarta.typescript.generator.EnumMapping;
import cz.habarta.typescript.generator.JsonLibrary;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.trpc.TrpcGenerator;

/**
 * Generate a tRpc script file from a jooby application.
 *
 * <p>Usage: https://jooby.io/modules/trpc
 *
 * @author edgar
 * @since 4.0.17
 */
@Mojo(
    name = "tRPC",
    threadSafe = true,
    requiresDependencyResolution = COMPILE_PLUS_RUNTIME,
    aggregator = true,
    defaultPhase = PROCESS_CLASSES)
public class TrpcMojo extends BaseMojo {

  /** Custom mapping overrides translating Java types to raw TypeScript strings. */
  @Parameter private Map<String, String> customTypeMappings;

  @Parameter private Map<String, String> customTypeNaming;

  /** The target JSON library used to parse field annotations. Defaults to jackson2. */
  @Parameter(defaultValue = "jackson2", property = "jooby.trpc.jsonLibrary")
  private JsonLibrary jsonLibrary = JsonLibrary.jackson2;

  @Parameter(defaultValue = "asString", property = "jooby.trpc.mapDate")
  private DateMapping mapDate = DateMapping.asString;

  @Parameter(defaultValue = "asInlineUnion", property = "jooby.trpc.mapEnum")
  private EnumMapping mapEnum = EnumMapping.asInlineUnion;

  @Parameter(defaultValue = "${project.build.outputDirectory}", property = "jooby.trpc.outputDir")
  private File outputDir;

  @Parameter(defaultValue = "trpc.d.ts", property = "jooby.trpc.outputFile")
  private String outputFile;

  @Parameter private List<String> importDeclarations;

  @Override
  protected void doExecute(@NonNull List<MavenProject> projects, @NonNull String mainClass)
      throws Exception {
    var classLoader = createClassLoader(projects);

    var generator = new TrpcGenerator();
    generator.setClassLoader(classLoader);
    generator.setOutputDir(outputDir.toPath());
    generator.setOutputFile(outputFile);

    if (customTypeMappings != null) {
      generator.setCustomTypeMappings(customTypeMappings);
    }
    generator.setJsonLibrary(jsonLibrary);
    generator.setMapDate(mapDate);
    generator.setMapEnum(mapEnum);
    if (importDeclarations != null) {
      generator.setImportDeclarations(importDeclarations);
    }
    if (customTypeNaming != null) {
      generator.setCustomTypeNaming(customTypeNaming);
    }

    getLog().info("Generating: " + outputDir.toPath().resolve(outputFile));

    generator.generate();
  }

  public String getOutputFile() {
    return outputFile;
  }

  public void setOutputFile(String outputFile) {
    this.outputFile = outputFile;
  }

  public File getOutputDir() {
    return outputDir;
  }

  public void setOutputDir(File outputDir) {
    this.outputDir = outputDir;
  }

  public Map<String, String> getCustomTypeMappings() {
    return customTypeMappings;
  }

  public void setCustomTypeMappings(Map<String, String> customTypeMappings) {
    this.customTypeMappings = customTypeMappings;
  }

  public Map<String, String> getCustomTypeNaming() {
    return customTypeNaming;
  }

  public void setCustomTypeNaming(Map<String, String> customTypeNaming) {
    this.customTypeNaming = customTypeNaming;
  }

  public JsonLibrary getJsonLibrary() {
    return jsonLibrary;
  }

  public DateMapping getMapDate() {
    return mapDate;
  }

  public EnumMapping getMapEnum() {
    return mapEnum;
  }

  public List<String> getImportDeclarations() {
    return importDeclarations;
  }

  public void setImportDeclarations(List<String> importDeclarations) {
    this.importDeclarations = importDeclarations;
  }
}
