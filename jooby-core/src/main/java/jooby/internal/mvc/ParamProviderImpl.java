package jooby.internal.mvc;

import static java.util.Objects.requireNonNull;

import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.thoughtworks.paranamer.BytecodeReadingParanamer;

public class ParamProviderImpl implements ParamProvider {

  private ParamNameProvider provider;

  public ParamProviderImpl(final ParamNameProvider provider) {
    this.provider = requireNonNull(provider, "Parameter name provider is required.");
  }

  @Override
  public List<Param> parameters(final Executable exec) {
    Parameter[] parameters = exec.getParameters();
    if (parameters.length == 0) {
      return Collections.emptyList();
    }
    // ASM
    ParamNameProvider asm = new ParamNameProvider() {
      String[] names;

      @Override
      public String name(final int index, final Parameter parameter) {
        return names()[index];
      }

      private String[] names() {
        if (names == null) {
          names = new BytecodeReadingParanamer().lookupParameterNames(exec);
        }
        return names;
      }
    };
    ParamNameProvider provider = new ChainParamNameProvider(this.provider, asm);
    Builder<Param> builder = ImmutableList.builder();
    for (int i = 0; i < parameters.length; i++) {
      Parameter parameter = parameters[i];
      builder.add(new Param(provider.name(i, parameter), parameter));
    }
    return builder.build();
  }

}
