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

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jooby.MediaType;

import com.google.common.base.Splitter;
import com.google.common.io.ByteStreams;
import com.typesafe.config.Config;

import io.bit3.jsass.CompilationException;
import io.bit3.jsass.Compiler;
import io.bit3.jsass.Options;
import io.bit3.jsass.Output;
import io.bit3.jsass.OutputStyle;
import io.bit3.jsass.context.StringContext;
import io.bit3.jsass.importer.Import;
import io.bit3.jsass.importer.Importer;
import javaslang.control.Try;

/**
 * <h1>sass</h1>
 * <p>
 * <a href="http://sass-lang.com/">sass-lang</a> implementation from
 * <a href="https://github.com/bit3/jsass">Java sass compiler</a>. Sass is the most mature, stable,
 * and powerful professional grade CSS extension language in the world.
 * </p>
 * <p>
 * <a href="https://github.com/bit3/jsass">Java sass compiler</a> Feature complete java sass
 * compiler using libsass.
 * </p>
 *
 * <h2>usage</h2>
 *
 * <pre>
 * assets {
 *   fileset {
 *     home: home.scss
 *   }
 *
 *   pipeline {
 *     dev: [sass]
 *     dist: [sass]
 *   }
 * }
 * </pre>
 *
 * <h2>options</h2>
 * <pre>
 * assets {
 *   ...
 *   sass {
 *     syntax: scss
 *     dev {
 *       sourceMap: inline
 *     }
 *     dist {
 *       style: compressed
 *     }
 *   }
 * }
 * </pre>
 *
 * @author edgar
 * @since 0.11.0
 */
public class Sass extends AssetProcessor {

  static enum FileResolver implements Function<String, URI> {
    FS {

      @Override
      public URI apply(final String it) {
        File file = new File(it);
        return file.exists() ? file.toURI() : null;
      }

    },

    CLASSPATH {
      @Override
      public URI apply(final String it) {
        URL resource = Sass.class.getResource(it);
        return resource == null ? null : Try.of(resource::toURI).get();
      }

    };

  }

  static class SassImporter implements Importer {

    private String ext;

    private Function<String, URI> resolver;

    public SassImporter(final String ext, final Function<String, URI> resolver) {
      this.ext = ext;
      this.resolver = resolver;
    }

    @Override
    public Collection<Import> apply(final String url, final Import previous) {
      String fname = nameWithExtension(url);

      List<String> segments = Splitter.on('/').trimResults().omitEmptyStrings()
          .splitToList(previous.getAbsoluteUri().toString());

      String relative = segments.subList(0, segments.size() - 1).stream()
          .collect(Collectors.joining("/", "/", ""));

      return Arrays.asList(relative + fname, fname)
          .stream()
          .map(resolver::apply)
          .filter(it -> it != null)
          .findFirst()
          .map(it -> {
            String content = Try.of(() -> {
              try (InputStream in = it.toURL().openStream()) {
                return new String(ByteStreams.toByteArray(in), StandardCharsets.UTF_8);
              }
            }).get();
            return Arrays.asList(new Import(it, it, content));
          })
          .orElse(null);
    }

    private String nameWithExtension(final String name) {
      String filename = name;
      if (!filename.endsWith(ext)) {
        filename += "." + ext;
      }
      if (filename.charAt(0) != '/') {
        return "/" + filename;
      }
      return filename;
    }

  }

  static final Pattern LOCATION = Pattern.compile("\"(.+?)\":\\s+(\\d+)");

  static final Function<String, URI> FS = it -> {
    File file = new File(it);
    return file.exists() ? file.toURI() : null;
  };

  static final Function<String, URI> CP = it -> {
    URL resource = Sass.class.getResource(it);
    return resource == null ? null : Try.of(resource::toURI).get();
  };

  public Sass() {
    set("syntax", "scss");
    set("style", "nested");
    set("importer", "classpath");
    set("indent", "  ");
    set("linefeed", "\n");
    set("omitSourceMapUrl", false);
    set("precision", 8);
    set("sourceComments", false);
  }

  @Override
  public boolean matches(final MediaType type) {
    return MediaType.css.matches(type);
  }

  @Override
  public String process(final String filename, final String source, final Config conf)
      throws Exception {
    String syntax = get("syntax");
    FileResolver resolver = FileResolver.valueOf(get("importer").toString().toUpperCase());
    OutputStyle style = OutputStyle.valueOf(get("style").toString().toUpperCase());

    Options options = new Options();
    options.setIsIndentedSyntaxSrc("sass".equals(syntax));
    options.getImporters().add(new SassImporter(syntax, resolver));
    options.setOutputStyle(style);
    options.setIndent(get("indent"));
    options.setLinefeed(get("linefeed"));
    options.setOmitSourceMapUrl(get("omitSourceMapUrl"));
    options.setPrecision(get("precision"));
    options.setSourceComments(get("sourceComments"));

    String sourcemap = get("sourcemap");
    if ("inline".equals(sourcemap)) {
      options.setSourceMapEmbed(true);
    } else if ("file".equals(sourcemap)) {
      options.setSourceMapFile(URI.create(filename + ".map"));
    }
    try {
      URI input = URI.create(filename);
      StringContext ctx = new StringContext(source, input, null, options);
      Output output = new Compiler().compile(ctx);
      return filename.endsWith(".map") ? output.getSourceMap() : output.getCss();
    } catch (CompilationException x) {
      Matcher matcher = LOCATION.matcher(x.getErrorJson());
      Map<String, Integer> location = new HashMap<>();
      while (matcher.find()) {
        location.put(matcher.group(1), Integer.parseInt(matcher.group(2)));
      }
      int line = location.getOrDefault("line", -1);
      int column = location.getOrDefault("column", -1);
      AssetException aex = new AssetException(name(),
          new AssetProblem(Optional.ofNullable(x.getErrorFile()).orElse(filename), line, column,
              x.getErrorText(), null));
      throw aex;
    }
  }

}
