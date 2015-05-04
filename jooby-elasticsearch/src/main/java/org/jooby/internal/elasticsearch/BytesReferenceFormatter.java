package org.jooby.internal.elasticsearch;

import java.util.List;

import org.elasticsearch.common.bytes.BytesReference;
import org.jooby.BodyFormatter;
import org.jooby.MediaType;

public class BytesReferenceFormatter implements BodyFormatter {

  @Override
  public List<MediaType> types() {
    return MediaType.ALL;
  }

  @Override
  public boolean canFormat(final Class<?> type) {
    return BytesReference.class.isAssignableFrom(type);
  }

  @Override
  public void format(final Object body, final Context ctx) throws Exception {
    ctx.bytes(out -> ((BytesReference) body).writeTo(out));
  }

}
