package org.jooby.assets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;

public class SvgSymbolTest {

  @Test
  public void defaults() throws Exception {
    String svgname = "symbols";
    String cssname = "symbols";

    Path output = Paths.get("target", "symbols1");
    new SvgSymbol()
        .set("input", "svg")
        .set("output", Paths.get("..").resolve(output).resolve(svgname).toString())
        .run(ConfigFactory.empty());

    assertTrue(Files.exists(output.resolve(svgname + ".svg")));
    assertTrue(Files.exists(output.resolve(cssname + ".css")));
    assertEquals(".approved {\n" +
        "  width: 32px;\n" +
        "  height: 32px;\n" +
        "}\n" +
        ".rejected {\n" +
        "  width: 18px;\n" +
        "  height: 18px;\n" +
        "}", Files.readAllLines(output.resolve(cssname + ".css")).stream()
            .collect(Collectors.joining("\n")));

    Document svg = Jsoup.parse(output.resolve(svgname + ".svg").toFile(), "UTF-8");
    assertNotNull(svg.select("symbol#approved").first());
    assertEquals("0 0 32 32", svg.select("symbol#approved").first().attr("viewbox"));
    assertNotNull(svg.select("symbol#rejected").first());
    assertEquals("0 0 18 18", svg.select("symbol#rejected").first().attr("viewbox"));
  }

  @Test
  public void defaultsNoOutput() throws Exception {
    String svgname = "sprite";
    String cssname = "sprite";

    Path output = Paths.get("public");
    new SvgSymbol()
        .set("input", "svg")
        .run(ConfigFactory.empty());

    assertTrue(Files.exists(output.resolve(svgname + ".svg")));
    assertTrue(Files.exists(output.resolve(cssname + ".css")));
    assertEquals(".approved {\n" +
        "  width: 32px;\n" +
        "  height: 32px;\n" +
        "}\n" +
        ".rejected {\n" +
        "  width: 18px;\n" +
        "  height: 18px;\n" +
        "}", Files.readAllLines(output.resolve(cssname + ".css")).stream()
            .collect(Collectors.joining("\n")));

    Document svg = Jsoup.parse(output.resolve(svgname + ".svg").toFile(), "UTF-8");
    assertNotNull(svg.select("symbol#approved").first());
    assertEquals("0 0 32 32", svg.select("symbol#approved").first().attr("viewbox"));
    assertNotNull(svg.select("symbol#rejected").first());
    assertEquals("0 0 18 18", svg.select("symbol#rejected").first().attr("viewbox"));
  }

  @Test
  public void customOutput() throws Exception {

    Path output = Paths.get("target", "symbols2");

    Path svgoutput = output.resolve("img").resolve("sprite.svg");
    Path cssoutput = output.resolve("css").resolve("sprite.css");
    new SvgSymbol()
        .set("input", "svg")
        .set("css.output", Paths.get("..").resolve(cssoutput).toString())
        .set("svg.output", Paths.get("..").resolve(svgoutput).toString())
        .run(ConfigFactory.empty());

    assertTrue(Files.exists(svgoutput));
    assertTrue(Files.exists(cssoutput));
    assertEquals(".approved {\n" +
        "  width: 32px;\n" +
        "  height: 32px;\n" +
        "}\n" +
        ".rejected {\n" +
        "  width: 18px;\n" +
        "  height: 18px;\n" +
        "}", Files.readAllLines(cssoutput).stream()
            .collect(Collectors.joining("\n")));

    Document svg = Jsoup.parse(svgoutput.toFile(), "UTF-8");
    assertNotNull(svg.select("symbol#approved").first());
    assertEquals("0 0 32 32", svg.select("symbol#approved").first().attr("viewbox"));
    assertNotNull(svg.select("symbol#rejected").first());
    assertEquals("0 0 18 18", svg.select("symbol#rejected").first().attr("viewbox"));
  }

  @Test
  public void mergWithCssClassPrefix() throws Exception {
    String svgname = "symbols";
    String cssname = "symbols";

    Path output = Paths.get("target", "symbols3");
    new SvgSymbol()
        .set("input", "svg")
        .set("css.prefix", "svg")
        .set("output", Paths.get("..").resolve(output).resolve(svgname).toString())
        .run(ConfigFactory.empty());

    assertTrue(Files.exists(output.resolve(svgname + ".svg")));
    assertTrue(Files.exists(output.resolve(cssname + ".css")));
    assertEquals("svg.approved {\n" +
        "  width: 32px;\n" +
        "  height: 32px;\n" +
        "}\n" +
        "svg.rejected {\n" +
        "  width: 18px;\n" +
        "  height: 18px;\n" +
        "}", Files.readAllLines(output.resolve(cssname + ".css")).stream()
            .collect(Collectors.joining("\n")));

    Document svg = Jsoup.parse(output.resolve(svgname + ".svg").toFile(), "UTF-8");
    assertNotNull(svg.select("symbol#approved").first());
    assertEquals("0 0 32 32", svg.select("symbol#approved").first().attr("viewbox"));
    assertNotNull(svg.select("symbol#rejected").first());
    assertEquals("0 0 18 18", svg.select("symbol#rejected").first().attr("viewbox"));
  }

  @Test
  public void mergeWithIdSuffix() throws Exception {
    String svgname = "symbols";
    String cssname = "symbols";

    Path output = Paths.get("target", "symbols4");
    new SvgSymbol()
        .set("input", "svg")
        .set("id.suffix", "-icon")
        .set("output", Paths.get("..").resolve(output).resolve(svgname).toString())
        .run(ConfigFactory.empty());

    assertTrue(Files.exists(output.resolve(svgname + ".svg")));
    assertTrue(Files.exists(output.resolve(cssname + ".css")));
    assertEquals(".approved-icon {\n" +
        "  width: 32px;\n" +
        "  height: 32px;\n" +
        "}\n" +
        ".rejected-icon {\n" +
        "  width: 18px;\n" +
        "  height: 18px;\n" +
        "}", Files.readAllLines(output.resolve(cssname + ".css")).stream()
            .collect(Collectors.joining("\n")));

    Document svg = Jsoup.parse(output.resolve(svgname + ".svg").toFile(), "UTF-8");
    assertNotNull(svg.select("symbol#approved-icon").first());
    assertEquals("0 0 32 32", svg.select("symbol#approved-icon").first().attr("viewbox"));
    assertNotNull(svg.select("symbol#rejected-icon").first());
    assertEquals("0 0 18 18", svg.select("symbol#rejected-icon").first().attr("viewbox"));
  }

  @Test
  public void mergeWithIdPrefix() throws Exception {
    String svgname = "symbols";
    String cssname = "symbols";

    Path output = Paths.get("target", "symbols5");
    new SvgSymbol()
        .set("input", "svg")
        .set("id.prefix", "icon-")
        .set("output", Paths.get("..").resolve(output).resolve(svgname).toString())
        .run(ConfigFactory.empty());

    assertTrue(Files.exists(output.resolve(svgname + ".svg")));
    assertTrue(Files.exists(output.resolve(cssname + ".css")));
    assertEquals(".icon-approved {\n" +
        "  width: 32px;\n" +
        "  height: 32px;\n" +
        "}\n" +
        ".icon-rejected {\n" +
        "  width: 18px;\n" +
        "  height: 18px;\n" +
        "}", Files.readAllLines(output.resolve(cssname + ".css")).stream()
            .collect(Collectors.joining("\n")));

    Document svg = Jsoup.parse(output.resolve(svgname + ".svg").toFile(), "UTF-8");
    assertNotNull(svg.select("symbol#icon-approved").first());
    assertEquals("0 0 32 32", svg.select("symbol#icon-approved").first().attr("viewbox"));
    assertNotNull(svg.select("symbol#icon-rejected").first());
    assertEquals("0 0 18 18", svg.select("symbol#icon-rejected").first().attr("viewbox"));
  }

}
