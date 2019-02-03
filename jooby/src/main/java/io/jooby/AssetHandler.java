/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import javax.annotation.Nonnull;
import java.util.Date;
import java.util.List;

public class AssetHandler implements Route.Handler, Route.Aware {
  private final AssetSource[] sources;

  private boolean etag = true;

  private boolean lastModified = true;

  private long maxAge = -1;

  private String filekey;

  public AssetHandler(AssetSource... sources) {
    this.sources = sources;
  }

  @Nonnull @Override public Object apply(@Nonnull Context ctx) throws Exception {
    String filepath = ctx.pathMap().get(filekey);
    Asset asset = resolve(filepath);
    if (asset == null) {
      ctx.sendStatusCode(StatusCode.NOT_FOUND);
      return ctx;
    }

    // handle If-None-Match
    if (this.etag) {
      String ifnm = ctx.header("If-None-Match").value((String) null);
      if (ifnm != null && ifnm.equals(asset.etag())) {
        ctx.sendStatusCode(StatusCode.NOT_MODIFIED);
        asset.release();
        return ctx;
      } else {
        ctx.setHeader("ETag", asset.etag());
      }
    }

    // Handle If-Modified-Since
    if (this.lastModified) {
      long lastModified = asset.lastModified();
      if (lastModified > 0) {
        long ifms = ctx.header("If-Modified-Since").longValue(-1);
        if (lastModified <= ifms) {
          ctx.sendStatusCode(StatusCode.NOT_MODIFIED);
          asset.release();
          return ctx;
        }
        ctx.setHeader("Last-Modified", new Date(lastModified));
      }
    }

    // cache max-age
    if (maxAge > 0) {
      ctx.setHeader("Cache-Control", "max-age=" + maxAge);
    }

    long length = asset.length();
    if (length != -1) {
      ctx.setContentLength(length);
    }
    ctx.setContentType(asset.type());
    return ctx.sendStream(asset.content());
  }

  public AssetHandler etag(boolean etag) {
    this.etag = etag;
    return this;
  }

  public AssetHandler lastModified(boolean lastModified) {
    this.lastModified = lastModified;
    return this;
  }

  public AssetHandler maxAge(long maxAge) {
    this.maxAge = maxAge;
    return this;
  }

  private Asset resolve(String filepath) {
    for (AssetSource source : sources) {
      Asset asset = source.resolve(filepath);
      if (asset != null) {
        return asset;
      }
    }
    return null;
  }

  @Override public void setRoute(Route route) {
    List<String> keys = route.pathKeys();
    this.filekey = keys.size() == 0 ? "*" : keys.get(0);

    // NOTE: It send an inputstream we don't need a renderer
    route.returnType(Context.class);
  }
}
