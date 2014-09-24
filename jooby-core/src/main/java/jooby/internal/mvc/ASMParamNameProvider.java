package jooby.internal.mvc;

import static java.util.Objects.requireNonNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;

import org.objectweb.asm.Type;

public class ASMParamNameProvider implements ParamNameProvider {

  private Map<String, String[]> params;

  public ASMParamNameProvider(final Map<String, String[]> params) {
    this.params = requireNonNull(params, "The params are required.");
  }

  @Override
  public String name(final int index, final Parameter parameter) {
    String descriptor = descriptor(parameter.getDeclaringExecutable());
    String[] names = this.params.get(descriptor);
    return names[index];
  }

  @SuppressWarnings("rawtypes")
  private static String descriptor(final Executable exec) {
    if (exec instanceof Method) {
      return exec.getName() + Type.getMethodDescriptor((Method) exec);
    } else if (exec instanceof Constructor) {
      return exec.getName() + Type.getConstructorDescriptor((Constructor) exec);
    }
    throw new IllegalArgumentException(exec.getClass().getName());
  }

}
