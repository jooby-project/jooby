package io.jooby.adoc;

import static java.util.function.Predicate.not;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.UUID;
import org.asciidoctor.extension.Postprocessor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Document.OutputSettings;
import org.jsoup.nodes.Element;

public class DocPostprocessor extends Postprocessor {

  @Override
  public String process(org.asciidoctor.ast.Document document, String output) {
    try {
      Document doc = Jsoup.parse(output, "UTF-8");

      headerIds(doc);
      languageTab(doc);
      clipboard(doc);
      externalLink(doc);

      OutputSettings settings = new OutputSettings();
      settings.prettyPrint(false);
      settings.indentAmount(0);
      settings.outline(false);
      return doc.outputSettings(settings).outerHtml();
    } catch (RuntimeException x) {
      x.addSuppressed(new IOException((String) document.getAttribute("docfile")));
      throw x;
    }
  }

  private static void externalLink(Document doc) {
    doc.select("a[href^=http://], a[href^=https://]")
        .forEach(a -> a.attr("target", "_blank"));
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
      tab1.attr("class", "switch--item option-1 selected");
      if (secondaryTitle.equalsIgnoreCase("Kotlin")) {
        tab1.text("Java");
      } else {
        tab1.text(title.text());
      }

      if (title.text().trim().equalsIgnoreCase(tab1.text().trim())) {
        title.remove();
      }

      Element tab2 = tabs.appendElement("div");
      tab2.attr("class", "switch--item option-2");
      tab2.text(secondaryTitle);
      tabs.appendTo(primary);
      primaryContent.addClass("option-1");
      primaryContent.appendTo(primary);
      secondaryContent.appendTo(primary);
      secondaryContent.addClass("hidden").addClass("option-2");
    }
  }

  private static void headerIds(Document doc) {
    headerIds(doc, 2);
    headerIds(doc, 3);
    headerIds(doc, 4);
    headerIds(doc, 5);
  }

  private static void headerIds(Document doc, int level) {
    doc.select("h" + level).stream()
        .filter(not(DocPostprocessor::isDiscrete))
        .forEach(h -> {
          String id = h.attr("id");
          LinkedHashSet<String> name = new LinkedHashSet<>();
          int parent = level - 1;
          Element p = h.parents().select("h" + parent).first();
          if (p != null && !isDiscrete(p)) {
            String parentId = p.attr("id");
            if (!parentId.isEmpty()) {
              name.add(parentId);
            }
          }
          name.add(id.replaceAll("([a-zA-Z0-9-]+)-\\d+$", "$1"));
          String newId = String.join("-", name);
          if (!id.equals(newId)) {
            h.attr("id", newId);
            h.select("a").stream()
                .filter(a -> a.attr("href").equals("#" + id) && !a.attr("class").isEmpty())
                .forEach(a -> a.attr("href", "#" + newId));
          }
        });
  }

  private static boolean isDiscrete(Element e) {
    return e.hasClass("discrete");
  }

  private static void clipboard(Document doc) {
    for (Element code : doc.select("code.hljs")) {
      String id = "x" + Long.toHexString(UUID.randomUUID().getMostSignificantBits());
      code.attr("id", id);
      Element button = code.parent().appendElement("button");
      button.addClass("clipboard");
      button.attr("data-clipboard-target", "#" + id);
      Element img = button.appendElement("img");
      img.attr("src", "/images/clippy.svg");
      img.attr("class", "clippy");
      img.attr("width", "13");
      img.attr("alt", "Copy to clipboard");
    }
  }
}
