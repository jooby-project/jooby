/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;

import java.util.HashMap;
import java.util.Map;

/**
 * Configure Gradle plugins for Jooby.
 *
 * @author edgar
 * @since 2.0.0
 */
public class JoobyPlugin implements Plugin<Project> {

  @Override public void apply(Project project) {
    openAPI(project);

    joobyRun(project);
  }

  private void joobyRun(Project project) {
    Map<String, Object> options = new HashMap<>();
    options.put(Task.TASK_TYPE, RunTask.class);
    options.put(Task.TASK_DEPENDS_ON, "classes");
    options.put(Task.TASK_NAME, "joobyRun");
    options.put(Task.TASK_DESCRIPTION, "Run, debug and reload applications");
    options.put(Task.TASK_GROUP, "jooby");
    project.getTasks().create(options);
  }

  private void openAPI(Project project) {
    Map<String, Object> openAPIOptions = new HashMap<>();
    openAPIOptions.put(Task.TASK_TYPE, OpenAPITask.class);
    openAPIOptions.put(Task.TASK_DEPENDS_ON, "classes");
    openAPIOptions.put(Task.TASK_NAME, "openAPI");
    openAPIOptions
        .put(Task.TASK_DESCRIPTION, "OpenAPI Generator");
    openAPIOptions.put(Task.TASK_GROUP, "jooby");

    project.getTasks().create(openAPIOptions);
  }
}
