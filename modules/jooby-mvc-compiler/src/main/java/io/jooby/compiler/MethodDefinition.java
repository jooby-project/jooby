package io.jooby.compiler;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

public class MethodDefinition {

  private final TypeElement owner;
  private final ExecutableElement executable;
  private final String httpMethod;

  public MethodDefinition(TypeElement owner, ExecutableElement executable, String httpMethod) {
    this.owner = owner;
    this.httpMethod = httpMethod;
    this.executable = executable;
  }

  public String getHandlerInternalName() {
    return getHandlerName().replace(".", "/");
  }

  @Override public String toString() {
    return executable.toString();
  }

  public String getHandlerName() {
    return getOwner() + "$" + httpMethod.toUpperCase() + "$" + executable.getSimpleName();
  }

  public String getOwner() {
    return owner.getQualifiedName().toString();
  }
}
