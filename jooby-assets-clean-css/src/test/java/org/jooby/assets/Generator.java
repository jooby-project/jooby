package org.jooby.assets;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Generator {

  private static final String NODE = "/usr/local/bin/node";

  private static final String NPM = new File(System.getProperty("user.home")).toPath()
      .resolve(".node").resolve("lib").resolve("node_modules").resolve("npm").resolve("bin")
      .resolve("npm-cli.js").toString();

  private static final File workDir = new File(System.getProperty("user.dir"), "target");

  static File log = new File(workDir, "log.log");

  public static void main(final String[] args) throws Exception {
    Path cleancss = install("clean-css").resolve("lib");

    Files.copy(localFile("java-fs.js"), cleancss.resolve("java-fs.js"));
    Files.copy(localFile("nashorn.js"), cleancss.resolve("nashorn.js"));

    Path output = browserify(cleancss, "nashorn.js", "--s", "CleanCSS", "-o", "output.js");

    String content = com.google.common.io.Files.toString(output.toFile(), StandardCharsets.UTF_8);

    String fs = idx(content, "fs");
    String javafs = idx(content, "./java-fs").replace("./", "");
    System.out.println(javafs);

    content = content.replace(fs, javafs).replace("'fs'", "'java-fs'");

    com.google.common.io.Files.write(content, output.toFile(), StandardCharsets.UTF_8);
  }

  private static String idx(final String input, final String name) {
    Pattern fsPattern = Pattern.compile("\"" + name + "\":\\d+");
    Matcher matcher = fsPattern.matcher(input);
    matcher.find();
    return matcher.group();
  }

  public static Path browserify(final Path workdir, final Object... args) throws Exception {
    Path browserify = module("browserify").resolve("bin").resolve("cmd.js");
    File output = new File(workDir, args[args.length - 1].toString());
    args[args.length - 1] = output;
    String[] arguments = new String[args.length + 2];
    arguments[0] = NODE;
    arguments[1] = browserify.toString();
    StringBuilder print = new StringBuilder("browserify");
    for (int i = 0; i < args.length; i++) {
      arguments[i + 2] = args[i].toString();
      print.append(" ").append(args[i]);
    }
    System.out.println(print);

    ProcessBuilder pb = new ProcessBuilder(arguments)
        .directory(workdir.toFile());

    pb.redirectErrorStream(true);
    pb.redirectOutput(log);

    Process p = pb.start();

    p.waitFor();

    Files.readAllLines(log.toPath()).forEach(System.out::println);

    return output.toPath();
  }

  public static Path install(final String name) throws Exception {
    ProcessBuilder pb = new ProcessBuilder(NODE, NPM, "install", "--prefix",
        workDir.getAbsolutePath(), name);

    pb.redirectErrorStream(true);
    pb.redirectOutput(log);

    Process p = pb.start();

    p.waitFor();

    Files.readAllLines(log.toPath()).forEach(System.out::println);

    return workDir.toPath().resolve("node_modules").resolve(name);
  }

  public static Path localFile(final String name) {
    return new File(System.getProperty("user.dir")).toPath().resolve("src").resolve("test")
        .resolve("resources").resolve(name);
  }

  public static Path module(final String name) {
    return new File(System.getProperty("user.home")).toPath().resolve("node_modules").resolve(name);
  }
}
