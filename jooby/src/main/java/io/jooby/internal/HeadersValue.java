package io.jooby.internal;

import io.jooby.Context;
import io.jooby.ValueNode;
import java.util.Map;
import java.util.TreeMap;

public class HeadersValue extends HashValue {

  public HeadersValue(final Context ctx) {
    super(ctx);
  }

  @Override
  protected Map<String, ValueNode> hash() {
    if (hash == EMPTY) {
      hash = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    }
    return hash;
  }
}
