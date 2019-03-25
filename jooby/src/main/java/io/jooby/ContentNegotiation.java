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
package io.jooby;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.jooby.MediaType.ALL;

public class ContentNegotiation {

  Map<MediaType, Throwing.Supplier<Object>> options = new LinkedHashMap<>();

  Throwing.Supplier<Object> fallback;

  public ContentNegotiation accept(String contentType, Throwing.Supplier<Object> supplier) {
    return accept(MediaType.valueOf(contentType), supplier);
  }

  public ContentNegotiation accept(MediaType contentType, Throwing.Supplier<Object> supplier) {
    options.put(contentType, supplier);
    return this;
  }

  public ContentNegotiation accept(Throwing.Supplier<Object> fallback) {
    this.fallback = fallback;
    return this;
  }

  public Object render(String accept) {
    List<MediaType> types = MediaType.parse(accept);
    int maxScore = Integer.MIN_VALUE;
    Throwing.Supplier<Object> result = fallback;
    for (Map.Entry<MediaType, Throwing.Supplier<Object>> entry : options.entrySet()) {
      MediaType contentType = entry.getKey();
      for (MediaType type : types) {
        if (contentType.matches(type)) {
          int score = type.getScore();
          if (score > maxScore) {
            maxScore = score;
            result = entry.getValue();
          }
        }
      }
    }
    if (result == null) {
      throw new Err(StatusCode.NOT_ACCEPTABLE, "Not Acceptable: " + accept);
    }
    return result.get();
  }

  public Object render(Context ctx) {
    return render(ctx.header(Context.ACCEPT).value(ALL));
  }

}
