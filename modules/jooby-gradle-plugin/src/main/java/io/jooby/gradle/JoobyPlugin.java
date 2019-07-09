/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.gradle;

import io.jooby.run.JoobyRunOptions;
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
    project.getExtensions().create("joobyRun", JoobyRunOptions.class);

    Map<String, Object> options = new HashMap<>();
    options.put(Task.TASK_TYPE, RunTask.class);
    options.put(Task.TASK_DEPENDS_ON, "classes");
    options.put(Task.TASK_NAME, "joobyRun");
    options.put(Task.TASK_DESCRIPTION, "Run, debug and hot reload applications");
    options.put(Task.TASK_GROUP, "jooby");
    project.getTasks().create(options);
  }
}
