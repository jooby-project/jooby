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

import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStream;
import java.util.Arrays;

import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.jooby.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Function;
import com.typesafe.config.Config;

import javaslang.control.Try;

public class SvgSprites extends AssetInOutProcessor {

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  public SvgSprites() {
    set("cssPngPrefix", ".ie");
    set("cssSvgPrefix", "");
    set("layout", "vertical");
  }

  @Override
  public boolean matches(final MediaType type) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public String process(final String filename, final String source, final Config conf)
      throws Exception {
    Nodejs.run(node -> {
      node.overwrite(conf.hasPath("_overwrite") ? conf.getBoolean("_overwrite") : false)
          .exec("dr-svg-sprites", v8 -> {

            v8.add("$options", toV8Object(v8, options()));

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
    return source;
  }

}
