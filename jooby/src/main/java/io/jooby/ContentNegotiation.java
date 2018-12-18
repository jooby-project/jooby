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
          int score = type.score();
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
