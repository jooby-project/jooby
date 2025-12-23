/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.adoc;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.zeroturnaround.exec.ProcessExecutor;

public class Git {

  private final String branch;

  private String repo;

  private Path dir;

  public Git(final String owner, final String project, final String branch, final Path dir) {
    this.repo = "git@github.com:" + owner + "/" + project + ".git";
    this.dir = dir;
    this.branch = branch;
  }

  public Git(final String owner, final String project, final Path dir) {
    this(owner, project, "master", dir);
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

  public String currentBranch() throws Exception {
    var out = new ByteArrayOutputStream();
    execute(List.of("git", "branch", "--show-current"), out);
    return out.toString(StandardCharsets.UTF_8).trim();
  }

  public void commit(String comment) throws Exception {
    execute(Arrays.asList("git", "add", "."));
    execute(Arrays.asList("git", "commit", "-m", "'" + comment + "'"));
    execute(Arrays.asList("git", "push", "origin", branch));
  }

  private void execute(final List<String> args) throws Exception {
    execute(args, System.out);
  }

  private void execute(final List<String> args, OutputStream out) throws Exception {
    System.out.println(args.stream().collect(Collectors.joining(" ")));
    int exit =
        new ProcessExecutor()
            .command(args.toArray(new String[0]))
            .redirectOutput(out)
            .directory(dir.toFile())
            .execute()
            .getExitValue();
    if (exit != 0) {
      throw new IllegalStateException("Execution of " + args + " resulted in exit code: " + exit);
    }
  }
}
