package jooby.internal.mvc;

import java.lang.reflect.Parameter;

import javax.inject.Named;

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
      Named named = parameter.getAnnotation(Named.class);
      return named == null ? null : named.value();
    }
  };

  String name(int index, Parameter parameter);

}
