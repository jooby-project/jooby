package org.jooby.internal.mvc;

import java.lang.reflect.Parameter;
import java.util.Optional;

import javax.inject.Named;

import org.jooby.mvc.Header;

public interface ParamNameProvider {

  ParamNameProvider JAVA_8 = new ParamNameProvider() {

    @Override
    public String name(final int index, final Parameter parameter) {
      return parameter.isNamePresent() ? parameter.getName() : null;
    }
  };

  ParamNameProvider NAMED = new ParamNameProvider() {
    @Override
    public String name(final int index, final Parameter parameter) {
      return Optional
          .ofNullable(parameter.getAnnotation(Named.class))
          .map(Named::value)
          .orElseGet(
              () -> Optional.ofNullable(parameter.getAnnotation(Header.class))
                  .map(h -> {
                    String name = h.value();
                    return name.length() > 0 ? name : null;
                  })
                  .orElse(null)
          );
    }
  };

  String name(int index, Parameter parameter);

}
