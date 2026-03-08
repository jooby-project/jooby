/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.gradle;

import cz.habarta.typescript.generator.DateMapping;
import cz.habarta.typescript.generator.EnumMapping;
import cz.habarta.typescript.generator.JsonLibrary;
import io.jooby.trpc.TrpcGenerator;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Generate a tRpc script file from a jooby application.
 *
 * Usage: https://jooby.io/modules/trpc
 *
 * @author edgar
 * @since 4.0.17
 */
public class TrpcTask extends BaseTask {

  private Map<String, String> customTypeMappings;

  private Map<String, String> customTypeNaming;

  private JsonLibrary jsonLibrary = JsonLibrary.jackson2;

  private DateMapping mapDate = DateMapping.asString;

  private EnumMapping mapEnum = EnumMapping.asInlineUnion;

  private File outputDir;

  private String outputFile = "trpc.d.ts";

  private List<String> importDeclarations;

  /**
   * Creates a tRPC task.
   */
  public TrpcTask() {}

  /**
   * Generate tRPC files from Jooby application.
   *
   * @throws Throwable If something goes wrong.
   */
  @TaskAction
  public void generate() throws Throwable {
    var projects = getProjects();
    var classLoader = createClassLoader(projects);

    // Default to the compiled classes directory if the user hasn't overridden outputDir
    var outDir = outputDir != null ? outputDir.toPath() : classes(getProject(), false);

    var generator = new TrpcGenerator();
    generator.setClassLoader(classLoader);
    generator.setOutputDir(outDir);
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

    getLogger().info("Generating: " + outDir.resolve(outputFile));

    generator.generate();
  }

  @Input
  @Optional
  public Map<String, String> getCustomTypeMappings() {
    return customTypeMappings;
  }

  public void setCustomTypeMappings(Map<String, String> customTypeMappings) {
    this.customTypeMappings = customTypeMappings;
  }

  @Input
  @Optional
  public Map<String, String> getCustomTypeNaming() {
    return customTypeNaming;
  }

  public void setCustomTypeNaming(Map<String, String> customTypeNaming) {
    this.customTypeNaming = customTypeNaming;
  }

  @Input
  @Optional
  public JsonLibrary getJsonLibrary() {
    return jsonLibrary;
  }

  public void setJsonLibrary(JsonLibrary jsonLibrary) {
    this.jsonLibrary = jsonLibrary;
  }

  @Input
  @Optional
  public DateMapping getMapDate() {
    return mapDate;
  }

  public void setMapDate(DateMapping mapDate) {
    this.mapDate = mapDate;
  }

  @Input
  @Optional
  public EnumMapping getMapEnum() {
    return mapEnum;
  }

  public void setMapEnum(EnumMapping mapEnum) {
    this.mapEnum = mapEnum;
  }

  @Input
  @Optional
  public File getOutputDir() {
    return outputDir;
  }

  public void setOutputDir(File outputDir) {
    this.outputDir = outputDir;
  }

  @Input
  @Optional
  public String getOutputFile() {
    return outputFile;
  }

  public void setOutputFile(String outputFile) {
    this.outputFile = outputFile;
  }

  @Input
  @Optional
  public List<String> getImportDeclarations() {
    return importDeclarations;
  }

  public void setImportDeclarations(List<String> importDeclarations) {
    this.importDeclarations = importDeclarations;
  }
}
