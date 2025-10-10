/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.adoc;

import static io.jooby.SneakyThrows.throwingConsumer;
import static io.jooby.SneakyThrows.throwingFunction;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Attributes;
import org.asciidoctor.Options;
import org.asciidoctor.OptionsBuilder;
import org.asciidoctor.Placement;
import org.asciidoctor.SafeMode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;

public class DocGenerator {
  public static void main(String[] args) throws Exception {
    List<String> options = Arrays.asList(args);
    generate(basedir(), options.contains("publish"), options.contains("v1"), true);
  }

  public static void generate(Path basedir, boolean publish, boolean v1, boolean doAscii)
      throws Exception {
    String version = version();

    Path asciidoc = basedir.resolve("asciidoc");

    /*
      Tree dir. The .adoc file became a directory
      modules/hikari.adoc => modules/hikari/index.html
     */
    String[] treeDirs = {"modules", "packaging", "usage", "migration"};

    int adocCount =
        Stream.of(treeDirs)
            .map(throwingFunction(dir -> countAdoc(asciidoc.resolve(dir))))
            .reduce(1, Integer::sum);
    int steps = 6 + (doAscii ? adocCount : 0);

    ProgressBarBuilder pbb =
        new ProgressBarBuilder()
            .setStyle(ProgressBarStyle.UNICODE_BLOCK)
            .setInitialMax(steps)
            .setTaskName("Building Site");

    try (var pb = pbb.build()) {

      Path outdir = asciidoc.resolve("site");
      if (!Files.exists(outdir)) {
        Files.createDirectories(outdir);
      }
      pb.step();

      /* Wipe out directory: */
      FileUtils.cleanDirectory(outdir.toFile());
      pb.step();

      /* Copy /images and /js: */
      copyFile(
          outdir,
          // images
          basedir.resolve("images"),
          // js
          basedir.resolve("js"));
      pb.step();

      if (doAscii) {
        Asciidoctor asciidoctor = Asciidoctor.Factory.create();

        asciidoctor.convertFile(
            asciidoc.resolve("index.adoc").toFile(),
            createOptions(asciidoc, outdir, version, null, asciidoc.resolve("index.adoc")));
        var index = outdir.resolve("index.html");
        Files.writeString(index, hljs(Files.readString(index)));
        pb.step();

        Stream.of(treeDirs)
            .forEach(
                throwingConsumer(
                    name -> {
                      Path modules = outdir.resolve(name);
                      Files.createDirectories(modules);
                      Files.walk(asciidoc.resolve(name))
                          .filter(Files::isRegularFile)
                          .forEach(
                              module -> {
                                processModule(asciidoctor, asciidoc, module, outdir, name, version);
                                pb.step();
                              });
                    }));
      }

      // LICENSE
      Files.copy(
          basedir.getParent().resolve("LICENSE"),
          outdir.resolve("LICENSE.txt"),
          StandardCopyOption.REPLACE_EXISTING);
      pb.step();

      if (v1) {
        v1doc(basedir, outdir);
      }
      pb.step();

      if (publish) {
        Path website =
            basedir
                .resolve("target") // Paths.get(System.getProperty("java.io.tmpdir"))
                .resolve(Long.toHexString(UUID.randomUUID().getMostSignificantBits()));
        Files.createDirectories(website);
        Git git = new Git("jooby-project", "jooby.io", website);
        git.clone();

        /* Clean: */
        FileUtils.deleteDirectory(website.resolve("images").toFile());
        FileUtils.deleteDirectory(website.resolve("js").toFile());
        FileUtils.deleteQuietly(website.resolve("index.html").toFile());

        FileUtils.copyDirectory(outdir.toFile(), website.toFile());
        git.commit(version);
      }
      pb.step();
    }
  }

  private static int countAdoc(Path basedir) throws IOException {
    try (Stream<Path> tree = Files.walk(basedir)) {
      return (int)
          tree.filter(Files::isRegularFile).filter(it -> it.toString().endsWith(".adoc")).count();
    }
  }

  private static void v1doc(Path basedir, Path output) throws Exception {
    Path v1source = basedir.resolve("v1");
    FileUtils.cleanDirectory(v1source.toFile());
    Files.createDirectories(v1source);

    Git git = new Git("jooby-project", "jooby", v1source);
    git.clone("--single-branch", "--branch", "gh-pages");
    Path v1target = output.resolve("v1");
    FileUtils.copyDirectory(v1source.toFile(), v1target.toFile());

    Collection<File> files = FileUtils.listFiles(v1target.toFile(), new String[] {"html"}, true);
    for (File index : files) {
      String content =
          FileUtils.readFileToString(index, "UTF-8")
              .replace("http://jooby.org", "https://jooby.org")
              .replace("href=\"/resources", "href=\"/v1/resources")
              .replace("src=\"/resources", "src=\"/v1/resources")
              .replace("href=\"https://jooby.org/resources", "href=\"/v1/resources")
              .replace("src=\"https://jooby.org/resources", "src=\"/v1/resources")
              .replace("href=\"resources", "href=\"/v1/resources")
              .replace("src=\"resources", "src=\"/v1/resources")
              .replace("src=\"http://ajax.", "src=\"https://ajax.")
              // remove/replace redirection
              .replace("<meta http-equiv=\"refresh\" content=\"0; URL=https://jooby.io\" />", "");
      Document doc = Jsoup.parse(content);
      doc.select("a")
          .forEach(
              a -> {
                String href = a.attr("href");
                if (!href.startsWith("http") && !href.startsWith("#")) {
                  href = "/v1" + href;
                  a.attr("href", href);
                }
              });
      FileUtils.writeStringToFile(index, doc.toString(), "UTF-8");
    }
    FileUtils.deleteQuietly(v1target.resolve(".git").toFile());
    FileUtils.deleteQuietly(v1target.resolve(".gitignore").toFile());
    FileUtils.deleteQuietly(v1target.resolve("CNAME").toFile());
  }

  private static void processModule(
      Asciidoctor asciidoctor,
      Path basedir,
      Path module,
      Path outdir,
      String name,
      String version) {
    try {
      String moduleName = module.getFileName().toString().replace(".adoc", "");

      String title = moduleName.replace("-", " ");
      if (name.equals("modules")
          && !moduleName.equals("modules")
          && !moduleName.equals("packaging")) {
        title += " module";
      }
      Options options = createOptions(basedir, outdir, version, title, module);

      asciidoctor.convertFile(module.toFile(), options);

      Path output = outdir.resolve(moduleName + ".html").toAbsolutePath();
      Path indexlike = output.getParent().resolve(name);
      if ((name.equals("modules") || name.equals("migration"))
          && !moduleName.equals("modules")
          && !moduleName.equals("packaging")) {
        indexlike = indexlike.resolve(moduleName);
      }
      indexlike = indexlike.resolve("index.html");
      Files.createDirectories(indexlike.getParent());
      Files.move(output, indexlike);
      String content = hljs(Files.readString(indexlike)
              .replace("js/", "../../js/")
              .replace("images/", "../../images/"));
      Files.writeString(indexlike, content);
    } catch (IOException x) {
      throw new IllegalStateException(x);
    }
  }

  private static String hljs(String content) {
    return content.replace(".highlightBlock", ".highlightElement")
        .replace("hljs.initHighlighting.called = true", "hljs.configure({ignoreUnescapedHTML: true});hljs.initHighlighting.called = true");
  }

  private static Options createOptions(Path basedir, Path outdir, String version, String title, Path docfile)
      throws IOException {
    var attributes = Attributes.builder();

    attributes.attribute("docfile", docfile.toString());
    attributes.attribute("love", "&#9825;");
    attributes.attribute("docinfo", "shared");
    attributes.title(title == null ? "jooby: do more! more easily!!" : "jooby: " + title);
    attributes.tableOfContents(Placement.LEFT);
    attributes.attribute("toclevels", "3");
    attributes.setAnchors(true);
    attributes.attribute("sectlinks", "");
    attributes.sectionNumbers(true);
    attributes.attribute("sectnumlevels", "3");
    attributes.linkAttrs(true);
    attributes.noFooter(true);
    attributes.attribute("idprefix", "");
    attributes.attribute("idseparator", "-");
    attributes.icons("font");
    attributes.attribute("description", "The modular micro web framework for Java");
    attributes.attribute(
        "keywords", "Java, Modern, Micro, Web, Framework, Reactive, Lightweight, Microservices");
    attributes.imagesDir("images");
    attributes.sourceHighlighter("highlightjs");
    attributes.attribute("highlightjsdir", "js");
    // agate, tom-one-dark, tomorrow-night-bright, tokyo-night-dark
    attributes.attribute("highlightjs-theme", "agate");
    attributes.attribute("favicon", "images/favicon96.png");

    // versions:
    Document pom =
        Jsoup.parse(DocGenerator.basedir().getParent().resolve("pom.xml").toFile(), "UTF-8");
    pom.select("properties > *").forEach(tag -> {
          var tagName = tag.tagName();
          var value = tag.text().trim();
          Stream.of(tagName, tagName.replaceAll("[.-]", "_"), tagName.replaceAll("[.-]", "-"), toJavaName(tagName))
                  .forEach(key -> attributes.attribute(key, value));
        });

    attributes.attribute("joobyVersion", version);
    attributes.attribute("date", DateTimeFormatter.ISO_INSTANT.format(Instant.now()));

    OptionsBuilder options = Options.builder();
    options.backend("html");

    options.attributes(attributes.build());
    options.baseDir(basedir.toAbsolutePath().toFile());
    options.docType("book");
    options.toDir(outdir.toFile());
    options.mkDirs(true);
    options.safe(SafeMode.UNSAFE);
    return options.build();
  }

  private static String toJavaName(String tagName) {
    StringBuilder name = new StringBuilder();
    name.append(tagName.charAt(0));
    boolean up = false;
    for (int i = 1; i < tagName.length(); i++) {
      char ch = tagName.charAt(i);
      if (Character.isJavaIdentifierPart(ch)) {
        if (up) {
          name.append(Character.toUpperCase(ch));
          up = false;
        } else {
          name.append(ch);
        }
      } else {
        up = true;
      }
    }
    return name.toString();
  }

  public static Path basedir() {
    Path basedir = Paths.get(System.getProperty("user.dir"));
    if (!basedir.toString().endsWith("docs")) {
      // maven exec vs main method from IDE
      basedir = basedir.resolve("docs");
    }
    return basedir;
  }

  public static String version() {
    try {
      Document doc = Jsoup.parse(basedir().getParent().resolve("pom.xml").toFile(), "utf-8");
      return doc.selectFirst("version").text().trim();
    } catch (IOException x) {
      throw new IllegalStateException(x);
    }
  }

  private static void copyFile(Path out, Path... dirs) throws IOException {
    for (Path dir : dirs) {
      FileUtils.copyDirectory(dir.toFile(), out.resolve(dir.getFileName().toString()).toFile());
    }
  }
}
