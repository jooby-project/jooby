/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.adoc;

import io.jooby.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AndFileFilter;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Attributes;
import org.asciidoctor.Options;
import org.asciidoctor.Placement;
import org.asciidoctor.SafeMode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DocGenerator {
  public static void main(String[] args) throws Exception {
    List<String> options = Arrays.asList(args);
    generate(basedir(), options.contains("publish"), options.contains("v1"));
  }

  public static void generate(Path basedir, boolean publish, boolean v1) throws Exception {
    String version = version();

    Path asciidoc = basedir.resolve("asciidoc");

    Path outdir = asciidoc.resolve("site");
    if (!Files.exists(outdir)) {
      Files.createDirectories(outdir);
    }

    /** Wipe out directory: */
    FileUtils.cleanDirectory(outdir.toFile());

    /** Copy /images and /js: */
    copyFile(outdir,
        // images
        basedir.resolve("images"),
        // js
        basedir.resolve("js")
    );

    Asciidoctor asciidoctor = Asciidoctor.Factory.create();

    asciidoctor.convertFile(asciidoc.resolve("index.adoc").toFile(),
        createOptions(asciidoc, outdir, version, null));
    Stream.of("usage", "modules").forEach(SneakyThrows.throwingConsumer(name -> {
      Path modules = outdir.resolve(name);
      Files.createDirectories(modules);
      Files.walk(asciidoc.resolve(name)).filter(Files::isRegularFile).forEach(module -> {
        processModule(asciidoctor, asciidoc, module, outdir, name, version);
      });
    }));

    // post process
    Files.walk(outdir).filter(it -> it.getFileName().toString().endsWith("index.html"))
        .forEach(SneakyThrows.throwingConsumer(it -> {
          Files.write(it, document(it).getBytes(StandardCharsets.UTF_8));
        }));

    // LICENSE
    Files.copy(basedir.getParent().resolve("LICENSE"), outdir.resolve("LICENSE.txt"),
        StandardCopyOption.REPLACE_EXISTING);

    if (v1) {
      v1doc(basedir, outdir);
    }

    if (publish) {
      Path website = basedir.resolve("target")// Paths.get(System.getProperty("java.io.tmpdir"))
          .resolve(Long.toHexString(UUID.randomUUID().getMostSignificantBits()));
      Files.createDirectories(website);
      Git git = new Git("jooby-project", "jooby.io", website);
      git.clone();

      /** Clean: */
      FileUtils.deleteDirectory(website.resolve("images").toFile());
      FileUtils.deleteDirectory(website.resolve("js").toFile());
      FileUtils.deleteQuietly(website.resolve("index.html").toFile());

      FileUtils.copyDirectory(outdir.toFile(), website.toFile());
      git.commit(version);
    }
  }

  private static void v1doc(Path basedir, Path output) throws Exception {
    Path v1source = basedir.resolve("v1");
    if (!Files.exists(v1source)) {
      Files.createDirectories(v1source);

      Git git = new Git("jooby-project", "jooby", v1source);
      git.clone("--single-branch", "--branch", "gh-pages");
    }
    Path v1target = output.resolve("v1");
    FileUtils.copyDirectory(v1source.toFile(), v1target.toFile());

    Collection<File> files = FileUtils.listFiles(v1target.toFile(), new String[]{"html"}, true);
    for (File index : files) {
      String content = FileUtils.readFileToString(index, "UTF-8")
          .replace("http://jooby.org", "https://jooby.org")
          .replace("href=\"/resources", "href=\"/v1/resources")
          .replace("src=\"/resources", "src=\"/v1/resources")
          .replace("src=\"http://ajax.", "src=\"https://ajax.")
          // remove/replace redirection
          .replace("<meta http-equiv=\"refresh\" content=\"0; URL=https://jooby.io\" />", "");
      Document doc = Jsoup.parse(content);
      doc.select("a").forEach(a -> {
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

  private static void processModule(Asciidoctor asciidoctor, Path basedir, Path module, Path outdir,
      String name, String version) {
    try {
      String moduleName = module.getFileName().toString().replace(".adoc", "");

      String title = moduleName.replace("-", " ");
      if (name.equals("modules")) {
        title += " module";
      }
      Options options = createOptions(basedir, outdir, version, title);

      asciidoctor.convertFile(module.toFile(), options);

      Path output = outdir.resolve(moduleName + ".html").toAbsolutePath();
      Path indexlike = output.getParent().resolve(name);
      if (name.equals("modules")) {
        indexlike = indexlike.resolve(moduleName);
      }
      indexlike = indexlike.resolve("index.html");
      Files.createDirectories(indexlike.getParent());
      Files.move(output, indexlike);
      String content = new String(Files.readAllBytes(indexlike), StandardCharsets.UTF_8)
          .replace("js/", "../../js/")
          .replace("images/", "../../images/");
      Files.write(indexlike, content.getBytes(StandardCharsets.UTF_8));
    } catch (IOException x) {
      throw new IllegalStateException(x);
    }
  }

  private static Options createOptions(Path basedir, Path outdir, String version, String title)
      throws IOException {
    Attributes attributes = new Attributes();

    attributes.setAttribute("love", "&#9825;");
    attributes.setAttribute("docinfo", "shared");
    attributes.setTitle(title == null ? "jooby: do more! more easily!!" : "jooby: " + title);
    attributes.setTableOfContents(Placement.LEFT);
    attributes.setAttribute("toclevels", "3");
    attributes.setAnchors(true);
    attributes.setAttribute("sectlinks", "");
    attributes.setSectionNumbers(true);
    attributes.setAttribute("sectnumlevels", "3");
    attributes.setLinkAttrs(true);
    attributes.setNoFooter(true);
    attributes.setAttribute("idprefix", "");
    attributes.setAttribute("idseparator", "-");
    attributes.setIcons("font");
    attributes.setAttribute("description", "The modular micro web framework for Java");
    attributes.setAttribute("keywords",
        "Java, Modern, Micro, Web, Framework, Reactive, Lightweight, Microservices");
    attributes.setImagesDir("images");
    attributes.setSourceHighlighter("highlightjs");
    attributes.setAttribute("highlightjsdir", "js");
    attributes.setAttribute("highlightjs-theme", "agate");
    attributes.setAttribute("favicon", "images/favicon96.png");

    // versions:
    Document pom = Jsoup
        .parse(DocGenerator.basedir().getParent().resolve("pom.xml").toFile(), "UTF-8");
    pom.select("properties > *").stream()
        .forEach(tag -> attributes.setAttribute(toJavaName(tag.tagName()), tag.text().trim()));

    attributes.setAttribute("joobyVersion", version);

    Options options = new Options();
    options.setBackend("html");

    options.setAttributes(attributes);
    options.setBaseDir(basedir.toAbsolutePath().toString());
    options.setDocType("book");
    options.setToDir(outdir.getFileName().toString());
    options.setMkDirs(true);
    options.setDestinationDir("site");
    options.setSafe(SafeMode.UNSAFE);
    return options;
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

  private static String document(Path index) {
    try {
      Document doc = Jsoup.parse(index.toFile(), "UTF-8");
      tocItems(doc);
      languageTab(doc);
      clipboard(doc);
      externalLink(doc);
      Document.OutputSettings settings = new Document.OutputSettings();
      settings.prettyPrint(false);
      settings.indentAmount(0);
      settings.outline(false);
      return doc.outputSettings(settings).toString();
    } catch (IOException x) {
      throw new IllegalStateException(x);
    }
  }

  private static void externalLink(Document doc) {
    for (Element a : doc.select("a")) {
      String href = a.attr("href");
      if (href.startsWith("http://") || href.startsWith("https://")) {
        a.attr("target", "_blank");
      }
    }
  }

  private static void languageTab(Document doc) {
    for (Element primary : doc.select(".listingblock.primary")) {
      Element secondary = primary.nextElementSibling();
      String secondaryTitle = secondary.selectFirst(".title").text().trim();
      Element primaryContent = primary.selectFirst(".content");
      Element secondaryContent = secondary.selectFirst(".content");
      secondary.remove();
      secondaryContent.remove();

      Element title = primary.selectFirst(".title");

      Element tabs = doc.createElement("div").attr("class", "switch");
      Element tab1 = tabs.appendElement("div");
      tab1.attr("class", "switch--item selected");
      if (secondaryTitle.equalsIgnoreCase("Kotlin")) {
        tab1.text("Java");
      } else {
        tab1.text(title.text());
      }

      if (title.text().trim().equalsIgnoreCase(tab1.text().trim())) {
        title.remove();
      }

      Element tab2 = tabs.appendElement("div");
      tab2.attr("class", "switch--item");
      tab2.text(secondaryTitle);
      tabs.appendTo(primary);
      primaryContent.appendTo(primary);
      secondaryContent.appendTo(primary);
      secondaryContent.addClass("hidden");
    }
  }

  private static void tocItems(Document doc) {
    tocItems(doc, 2);
    tocItems(doc, 3);
    tocItems(doc, 4);
  }

  private static void tocItems(Document doc, int level) {
    doc.select("h" + level).forEach(h -> {
      if (!h.hasClass("discrete")) {
        String id = h.attr("id");
        LinkedHashSet<String> name = new LinkedHashSet<>();
        int parent = level - 1;
        Element p = h.parents().select("h" + parent).first();
        if (p != null && !p.hasClass("discrete")) {
          String parentId = p.attr("id");
          if (parentId != null && parentId.length() > 0) {
            name.add(parentId);
          }
        }
        name.add(id.replaceAll("([a-zA-Z-]+)-\\d+", "$1"));
        String newId = name.stream().collect(Collectors.joining("-"));
        if (!id.equals(newId)) {
          h.attr("id", newId);
          doc.select("a").forEach(a -> {
            if (a.attr("href").equals("#" + id) && a.attr("class").length() > 0) {
              a.attr("href", "#" + newId);
            }
          });
        }
      }
    });
  }

  private static void clipboard(Document doc) {
    doc.select("code").removeAttr("data-lang");
    for (Element pre : doc.select("pre.highlight")) {
      Element button = pre.appendElement("button");
      button.addClass("clipboard");
      button.text("Copy");
      if (pre.childNodeSize() == 1 && (pre.childNode(0) instanceof TextNode)) {
        Element div = pre.appendElement("div");
        div.html(pre.html());
        pre.html("");
      }
    }
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
      String version = doc.selectFirst("version").text().trim();
      return version;
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
