/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright 2014 Edgar Espina
 */
package io.jooby.adoc;

import org.zeroturnaround.exec.ProcessExecutor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Git {

  private String repo;

  private Path dir;

  public Git(final String owner, final String project, final Path dir) {
    this.repo = "git@github.com:" + owner + "/" + project + ".git";
    this.dir = dir;
  }

  public void clone(final String... args) throws Exception {
    List<String> cmd = new ArrayList<>();
    cmd.add("git");
    cmd.add("clone");
    if (args.length > 0) {
      cmd.addAll(Arrays.asList(args));
    }
    cmd.add(repo);
    cmd.add(".");
    execute(cmd);
  }

  public void commit(String comment) throws Exception {
    execute(Arrays.asList("git", "add", "."));
    execute(Arrays.asList("git", "commit", "-m", "'" + comment + "'"));
    execute(Arrays.asList("git", "push", "origin", "master"));
  }

  private void execute(final List<String> args) throws Exception {
    System.out.println(args.stream().collect(Collectors.joining(" ")));
    int exit = new ProcessExecutor()
        .command(args.toArray(new String[args.size()]))
        .redirectOutput(System.out)
        .directory(dir.toFile())
        .execute()
        .getExitValue();
    if (exit != 0) {
      throw new IllegalStateException("Execution of " + args + " resulted in exit code: " + exit);
    }
  }
}
