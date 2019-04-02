/**
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *    Copyright 2014 Edgar Espina
 */
package io.jooby.adoc;

import org.apache.commons.io.FileUtils;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Attributes;
import org.asciidoctor.Options;
import org.asciidoctor.Placement;
import org.asciidoctor.SafeMode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DocGenerator {
  public static void main(String[] args) throws Exception {
    generate(basedir(), args.length > 0 && "publish".equals(args[0]));
  }

  public static void generate(Path basedir, boolean publish) throws Exception {
    Path asciidoc = basedir.resolve("asciidoc");
    Asciidoctor asciidoctor = Asciidoctor.Factory.create();

    Attributes attributes = new Attributes();

    attributes.setAttribute("joobyVersion", version());
    attributes.setAttribute("love", "&#9825;");

    attributes.setAttribute("docinfo", "shared");
    attributes.setTitle("jooby: do more! more easily!!");
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

    Path outdir = asciidoc.resolve("site");
    if (!Files.exists(outdir)) {
      Files.createDirectories(outdir);
    }

    Options options = new Options();
    options.setBackend("html");

    options.setAttributes(attributes);
    options.setBaseDir(asciidoc.toAbsolutePath().toString());
    options.setDocType("book");
    options.setToDir(outdir.getFileName().toString());
    options.setMkDirs(true);
    options.setDestinationDir("site");
    options.setSafe(SafeMode.SAFE);

    /** Wipe out directory: */
    FileUtils.cleanDirectory(outdir.toFile());

    /** Copy /images and /js: */
    copyFile(outdir,
        // images
        basedir.resolve("images"),
        // js
        basedir.resolve("js")
    );

    asciidoctor.convertFile(asciidoc.resolve("index.adoc").toFile(), options);

    Path index = outdir.resolve("index.html");

    // adjust toc
    Files.write(index, document(index).getBytes(StandardCharsets.UTF_8));

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
      git.commit("Sync documentation");
    }
  }

  private static String document(Path index) throws IOException {
    Document doc = Jsoup.parse(index.toFile(), "UTF-8");
    tocItems(doc);
    languageTab(doc);
    clipboard(doc);
    Document.OutputSettings settings = new Document.OutputSettings();
    settings.prettyPrint(false);
    settings.indentAmount(0);
    settings.outline(false);
    return doc.outputSettings(settings).toString();
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
    int l = 3;
    while (l > 1) {
      Elements items = doc.body().select(".sectlevel" + l);
      for (Element it : items) {
        String parent = it.parent().select("a").first().attr("href").replace("#", "");
        for (Element e : it.select("li>a")) {
          String key = e.attr("href").replace("#", "");
          String newkey = Stream.of((parent + "-" + key).split("-"))
              .distinct()
              .collect(Collectors.joining("-"));
          Element title = doc.select("#" + key).first();

          title.select("a").forEach(a -> a.attr("href", "#" + newkey));
          title.attr("id", newkey);
          e.attr("href", "#" + newkey);
        }

      }
      l -= 1;
    }
    Document.OutputSettings settings = doc.outputSettings();
    settings.indentAmount(2);
    settings.prettyPrint(true);
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
      return Jsoup.parse(basedir().getParent().resolve("pom.xml").toFile(), "utf-8")
          .selectFirst("version")
          .text()
          .trim();
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
