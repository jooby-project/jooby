package org.jooby.internal.reqparam;

import org.jooby.ParamConverter;
import org.jooby.Upload;

import com.google.inject.TypeLiteral;

public class UploadParamConverter implements ParamConverter {

  @Override
  public Object convert(final TypeLiteral<?> toType, final Object[] values, final Chain chain)
      throws Exception {
    if (Upload.class == toType.getRawType()) {
      return values[0];
    } else {
      return chain.convert(toType, values);
    }
  }

}
