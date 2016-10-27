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

import static com.eclipsesource.v8.utils.V8ObjectUtils.toV8Array;
import static com.eclipsesource.v8.utils.V8ObjectUtils.toV8Object;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Function;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;
import com.typesafe.config.Config;

import javaslang.control.Try;

/**
 * <h1>svg-sprites</h1>
 * <p>
 * An {@link AssetAggregator} that creates SVG sprites with PNG fallbacks at needed sizes via
 * <a href="https://github.com/drdk/dr-svg-sprites">dr-svg-sprites</a>.
 * </p>
 *
 * <h2>usage</h2>
 *
 * <pre>
 * assets {
 *   fileset {
 *     sprite: svg-sprites
 *     home: home.scss
 *   }
 *
 *   svg-sprites {
 *     spriteElementPath: "images/svg-source",
 *     spritePath: "css"
 *   }
 * }
 * </pre>
 *
 * <p>
 * The <code>spriteElementPath</code> contains all the <code>*.svg</code> files you want to process.
 * The <code>spritePath</code> indicates where to save the sprite, here you will find the following
 * generated files: <code>css/sprite.css</code>, <code>css/sprite.svg</code> and
 * <code>css/sprite.png</code>.
 * </p>
 *
 * <h2>options</h2>
 *
 * <pre>
 * assets {
 *   fileset {
 *     sprite: svg-sprites
 *     home: home.scss
 *   }
 *
 *   svg-sprites {
 *     spriteElementPath: "images/svg-source",
 *     spritePath: "css",
 *     layout: "vertical",
 *     sizes: {
 *       large: 24,
 *       small: 16
 *     },
 *     refSize: "large"
 *   }
 * }
 * </pre>
 *
 * <p>
 * Please refer to <a href="https://github.com/drdk/dr-svg-sprites">dr-svg-sprites</a> for more
 * details.
 * </p>
 *
 * @author edgar
 *
 */
public class SvgSprites extends AssetAggregator {

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  @Override
  public SvgSprites set(final Config options) {
    super.set(options);
    return this;
  }

  @Override
  public SvgSprites set(final String name, final Object value) {
    super.set(name, value);
    return this;
  }

  @Override
  public List<String> fileset() {
    return Arrays.asList(cssPath());
  }

  @Override
  public void run(final Config conf) throws Exception {
    File spriteElementPath = resolve(get("spriteElementPath").toString());
    if (!spriteElementPath.exists()) {
      throw new FileNotFoundException(spriteElementPath.toString());
    }

    File workdir = new File(Try.of(() -> conf.getString("application.tmpdir"))
        .getOrElse(System.getProperty("java.io.tmpdir")));

    File spritePath = resolve(spritePath());
    File cssPath = resolve(cssPath());
    String sha1 = new File(spritePath()).getName()
        .replace(".svg", "-" + sha1(spriteElementPath, spritePath, cssPath) + ".sha1");
    File uptodate = workdir.toPath().resolve("svg-sprites").resolve(sha1).toFile();

    if (uptodate.exists()) {
      log.info("svg-sprites is up-to-date: {}", uptodate);
      return;
    }

    Nodejs.run(workdir, node -> {
      node.overwrite(conf.hasPath("_overwrite") ? conf.getBoolean("_overwrite") : false)
          .exec("dr-svg-sprites", v8 -> {
            Map<String, Object> options = options();
            // rewrite paths
            options.put("spritePath", spritePath.toString());
            options.put("cssPath", cssPath.toString());
            options.put("spriteElementPath", spriteElementPath.toString());

            log.debug("svg-sprites options {}  ", options.entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining("\n  ", "{\n  ", "\n}")));

            v8.add("$options", toV8Object(v8, options));

            /**
             * Hook sv2png and remove panthomjs dependency.
             */
            v8.add("svg2png", new V8Function(v8, (receiver, params) -> {
              String svgPath = params.get(0).toString();
              String pngPath = params.get(1).toString();
              Float w = new Float(params.getDouble(2));
              Float h = new Float(params.getDouble(3));
              V8Function callback = (V8Function) params.get(4);
              Try.run(() -> {
                try (FileReader in = new FileReader(svgPath);
                    OutputStream out = new FileOutputStream(pngPath)) {
                  PNGTranscoder transcoder = new PNGTranscoder();
                  transcoder.addTranscodingHint(PNGTranscoder.KEY_WIDTH, w);
                  transcoder.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, h);
                  transcoder.transcode(new TranscoderInput(in), new TranscoderOutput(out));
                }
              })
                  .onSuccess(v -> callback.call(null, null))
                  .onFailure(x -> {
                    log.debug("png-fallback resulted in exception", x);
                    callback.call(null, toV8Array(v8, Arrays.asList(x.getMessage())));
                  });
              return V8.UNDEFINED;
            }));
          });
    });
    log.debug("creating sha1: {}", uptodate);
    uptodate.getParentFile().mkdirs();
    // clean old/previous *.sha1 files
    try (Stream<Path> sha1files = Files.walk(uptodate.getParentFile().toPath())
        .filter(it -> it.toString().endsWith(".sha1"))) {
      sha1files.forEach(it -> Try.run(() -> Files.delete(it)));
    }
    Files.createFile(uptodate.toPath());
    uptodate.deleteOnExit();
  }

  private String sha1(final File dir, final File sprite, final File css) throws IOException {
    try (Stream<Path> stream = Files.walk(dir.toPath())) {
      Hasher sha1 = Hashing.sha1().newHasher();
      stream.filter(p -> !Files.isDirectory(p))
          .forEach(p -> Try.run(() -> sha1.putBytes(Files.readAllBytes(p))));
      if (sprite.exists()) {
        sha1.putBytes(Files.readAllBytes(sprite.toPath()));
      }
      if (css.exists()) {
        sha1.putBytes(Files.readAllBytes(css.toPath()));
      }
      return BaseEncoding.base16().encode(sha1.hash().asBytes()).toLowerCase();
    }
  }

  private File resolve(final String path) {
    // basedir is set to public from super class
    return new File(get("basedir").toString(), path);
  }

  public String cssPath() {
    try {
      return nameFor("cssPath", ".css");
    } catch (IllegalArgumentException x) {
      return spritePath().replace(".svg", ".css");
    }
  }

  public String spritePath() {
    return nameFor("spritePath", ".svg");
  }

  private String nameFor(final String property, final String ext) {
    String spritePath = get(property);
    if (spritePath == null) {
      throw new IllegalArgumentException(
          "Required option 'svg-sprites." + property + "' not present");
    }
    if (spritePath.endsWith(ext)) {
      return spritePath;
    } else {
      return spritePath + "/" + prefix("prefix") + prefix("name") + "sprite" + ext;
    }
  }

  private String prefix(final String name) {
    return Optional.ofNullable(get(name))
        .map(it -> it + "-")
        .orElse("");
  }

}
