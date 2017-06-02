/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby.assets;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.typesafe.config.Config;

import javaslang.Lazy;
import javaslang.Tuple;
import javaslang.Tuple2;

/**
 * <h1>svg-symbol</h1>
 * <p>
 * SVG <code>symbol</code> for icons: merge svg files from a folder and generates a
 * <code>sprite.svg</code> and <code>sprite.css</code> files.
 * </p>
 *
 * <h2>usage</h2>
 *
 * <pre>{@code
 * assets {
 *   fileset {
 *     sprite: svg-symbol
 *   }
 *
 *   svg-symbol {
 *     input: "images/svg"
 *   }
 * }
 * }</pre>
 *
 * <p>
 * Previous example looks for <code>*.svg</code> files inside the <code>images/svg</code> folder and
 * generate a <code>sprite.svg</code> and <code>sprite.css</code> files.
 * </p>
 *
 * <p>
 * You can display the svg icons using id reference:
 * </p>
 *
 * <pre>{@code
 * <svg>
 *   <use xlink:href="#approved" />
 * </svg>
 * }</pre>
 *
 * <p>
 * This technique is described here:
 * <a href="https://css-tricks.com/svg-symbol-good-choice-icons">SVG symbol a Good Choice for
 * Icons</a>
 * </p>
 *
 * <h2>options</h2>
 *
 * <h3>output</h3>
 * <p>
 * Defines where to write the <code>svg</code> and <code>css</code> files. Default value is:
 * <code>sprite</code>.
 * </p>
 *
 * <pre>{@code
 * svg-symbol {
 *   output: "folder/symbols"
 * }
 * }</pre>
 *
 * <p>
 * There are two more specific output options: <code>svg.output</code> and <code>css.output</code>
 * if any of these options are present the <code>output</code> option is ignored:
 * </p>
 *
 * <pre>{@code
 * svg-symbol {
 *   css {
 *     output: "css/sprite.css"
 *   },
 *   svg {
 *     output: "img/sprite.svg"
 *   }
 * }
 * }</pre>
 *
 * <h3>id prefix and suffix</h3>
 * <p>
 * ID is generated from <code>svg file names</code>. These options prepend or append something to
 * the generated id.
 * </p>
 *
 * <pre>{@code
 * svg-symbol {
 *   output: "sprite"
 *   id {
 *     prefix: "icon-"
 *   }
 * }
 * }</pre>
 *
 * <p>
 * Generates IDs like: <code>icon-approved</code>, while:
 * </p>
 *
 * <pre>{@code
 * svg-symbol {
 *   output: "sprite"
 *   id {
 *     suffix: "-icon"
 *   }
 * }
 * }</pre>
 *
 * <p>
 * Generates IDs like: <code>approved-icon</code>
 * </p>
 *
 * <h3>css prefix</h3>
 * <p>
 * Prepend a string to a generated css class. Here is the css class for <code>approved.svg</code>:
 * </p>
 *
 * <pre>{@code
 * .approved {
 *   width: 18px;
 *   height: 18px;
 * }
 * }</pre>
 *
 * <p>
 * If we set a <code>svg</code> css prefix:
 *
 * <pre>{@code
 * {
 *   svg-symbol: {
 *     css {
 *       prefix: "svg"
 *     }
 *   }
 * }
 * }</pre>
 *
 * <p>
 * The generated css class will be:
 * <pre>{@code
 * svg.approved {
 *   width: 18px;
 *   height: 18px;
 * }
 * }</pre>
 *
 * <p>
 * This option is useful for generating more specific css class selectors.
 * </p>
 *
 * @author edgar
 */
public class SvgSymbol extends AssetAggregator {

  /** #3. */
  private static final int _3 = 3;

  /** Handle svg dimension: 0 0 20 20. */
  static final Pattern SIZE = Pattern.compile("(\\d+(\\.\\d+)?)(\\w+)");

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  /**
   * Creates a new {@link SvgSymbol}.
   */
  public SvgSymbol() {
    set("output", "sprite");

    set("id.prefix", "");
    set("id.suffix", "");

    set("css.prefix", "");
    set("css.round", true);

    set("svg.xmlns", "http://www.w3.org/2000/svg");
  }

  @Override
  public List<String> fileset() {
    return ImmutableList.of(cssPath());
  }

  @Override
  public void run(final Config conf) throws Exception {
    String input = get("input");
    if (input == null) {
      throw new IllegalArgumentException("Required option 'svg-symbol.input' not present");
    }
    Path basedir = Paths.get(get("basedir").toString());
    Path dir = basedir.resolve(input);
    Iterator<Path> files = Files.walk(dir).filter(Files::isRegularFile)
        .filter(it -> it.toString().endsWith(".svg"))
        .sorted()
        .iterator();

    List<CharSequence> cssout = new ArrayList<>();

    Element svg = new Element(Tag.valueOf("svg"), "");
    attrs("svg", "output").forEach((n, v) -> svg.attr(n, v.toString()));

    while (files.hasNext()) {
      Path file = files.next();
      log.debug("{}", file);
      String id = get("id.prefix") + file.getFileName().toString().replace(".svg", "")
          + get("id.suffix");

      Tuple2<Element, Element> rewrite = symbol(file, id);
      svg.appendChild(rewrite._2);

      cssout.add(css(id, rewrite._1));
    }

    write(basedir.resolve(svgPath()), ImmutableList.of(svg.outerHtml()));
    write(basedir.resolve(cssPath()), cssout);
  }

  /**
   * Read an object path and optionally filter some child paths.
   *
   * @param path Path to read.
   * @param without Properties to filter.
   * @return Properties.
   */
  private Map<String, Object> attrs(final String path, final String... without) {
    Map<String, Object> attrs = new LinkedHashMap<>(get(path));
    Arrays.asList(without).forEach(attrs::remove);
    return attrs;
  }

  /**
   * Read a svg file and return the svg element (original) and a new symbol element (created from
   * original).
   *
   * @param file Svg file.
   * @param id ID to use.
   * @return Svg element (original) and a symbol element (converted).
   * @throws IOException If something goes wrong.
   */
  private Tuple2<Element, Element> symbol(final Path file, final String id) throws IOException {
    Element svg = Jsoup.parse(file.toFile(), "UTF-8").select("svg").first();
    Element symbol = new Element(Tag.valueOf("symbol"), "")
        .attr("id", id)
        .attr("viewBox", svg.attr("viewBox"));
    new ArrayList<>(svg.childNodes()).forEach(symbol::appendChild);
    return Tuple.of(svg, symbol);
  }

  /**
   * Generate a CSS rule, it reads the width and height attributes of the svg element or fallback to
   * viewBox attribute.
   *
   * @param id ID to use.
   * @param svg Svg element to convert.
   * @return A css rule.
   */
  private CharSequence css(final String id, final Element svg) {
    Lazy<Tuple2<Tuple2<Number, String>, Tuple2<Number, String>>> viewBox = Lazy.of(() -> {
      String vbox = svg.attr("viewBox");
      String[] dimension = vbox.split("\\s+");
      return Tuple.of(parse(dimension[2]), parse(dimension[_3]));
    });
    Tuple2<Number, String> w = Optional.ofNullable(Strings.emptyToNull(svg.attr("width")))
        .map(this::parse)
        .orElseGet(() -> viewBox.get()._1);
    Tuple2<Number, String> h = Optional.ofNullable(Strings.emptyToNull(svg.attr("height")))
        .map(this::parse)
        .orElseGet(() -> viewBox.get()._2);
    StringBuilder css = new StringBuilder();
    css.append(get("css.prefix").toString()).append(".").append(id)
        .append(" {\n  width: ").append(w._1).append(w._2).append(";\n")
        .append("  height: ").append(h._1).append(h._2).append(";\n}");
    return css;
  }

  /**
   * Parse a css size unit value, like 10px or 18.919px and optionally round the value to the
   * closest integer.
   *
   * @param value Value to parse.
   * @return A tuple with a number and unit(px, em, etc...)
   */
  private Tuple2<Number, String> parse(final String value) {
    Matcher matcher = SIZE.matcher(value);
    if (matcher.find()) {
      String number = matcher.group(1);
      String unit = matcher.group(_3);
      boolean round = get("css.round");
      Number num = Double.parseDouble(number);
      return Tuple.of(round ? Math.round(num.doubleValue()) : num, unit);
    }
    return null;
  }

  /**
   * Write content to file.
   *
   * @param path Target file.
   * @param sequence File content.
   * @throws IOException If something goes wrong.
   */
  private void write(final Path path, final List<CharSequence> sequence) throws IOException {
    log.debug("writing: {}", path.normalize().toAbsolutePath());
    path.toFile().getParentFile().mkdirs();
    Files.write(path, sequence);
  }

  /**
   * @return Generate a css path from <code>css.output</code> or fallback to <code>output</code>
   */
  private String cssPath() {
    String css = Optional.<String> ofNullable(get("css.output")).orElse(get("output"));
    return css.endsWith(".css") ? css : css + ".css";
  }

  /**
   * @return Generate a css path from <code>svg.output</code> or fallback to <code>output</code>
   */
  private String svgPath() {
    String svg = Optional.<String> ofNullable(get("svg.output")).orElse(get("output"));
    return svg.endsWith(".svg") ? svg : svg + ".svg";
  }

}
