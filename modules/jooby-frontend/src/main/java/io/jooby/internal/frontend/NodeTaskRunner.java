package io.jooby.internal.frontend;

import com.github.eirslett.maven.plugins.frontend.lib.TaskRunnerException;

import java.util.Map;

public interface NodeTaskRunner {
  void execute(String args, Map<String,String> environment) throws TaskRunnerException;
}
