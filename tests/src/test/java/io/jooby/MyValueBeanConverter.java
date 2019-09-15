package io.jooby;

import javax.annotation.Nonnull;

public class MyValueBeanConverter implements BeanConverter {
  @Override public boolean supports(@Nonnull Class type) {
    return MyValue.class == type;
  }

  @Override public Object convert(@Nonnull Value value, @Nonnull Class type) {
    MyValue result = new MyValue();
    result.setString(value.get("string").value());
    return result;
  }
}
