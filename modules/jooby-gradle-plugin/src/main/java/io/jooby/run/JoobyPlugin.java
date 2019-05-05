package io.jooby.run;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;

import java.util.HashMap;
import java.util.Map;

public class JoobyPlugin implements Plugin<Project> {
  @Override public void apply(Project project) {
    Map<String, Object> options = new HashMap<>();
    options.put(Task.TASK_TYPE, JoobyRun.class);
    options.put(Task.TASK_DEPENDS_ON, "classes");
    options.put(Task.TASK_NAME, "joobyRun");
    options.put(Task.TASK_DESCRIPTION, "Run, debug and hot reload applications");
    options.put(Task.TASK_GROUP, "jooby");
    project.getTasks().create(options);
  }
}
