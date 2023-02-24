/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

public class MethodDescriptorTest {

  @Test
  public void checkReifiedDescriptor() throws Exception {
    assertMethodDescriptors(MethodDescriptor.Reified.class);
  }

  @Test
  public void checkRouteDescriptor() throws Exception {
    assertMethodDescriptors(MethodDescriptor.Route.class);
  }

  @Test
  public void checkValueDescriptor() throws Exception {
    assertMethodDescriptors(MethodDescriptor.Value.class);
  }

  @Test
  public void checkValueNodeDescriptor() throws Exception {
    assertMethodDescriptors(MethodDescriptor.ValueNode.class);
  }

  @Test
  public void checkFileUploadDescriptor() throws Exception {
    assertMethodDescriptors(MethodDescriptor.FileUpload.class);
  }

  @Test
  public void checkContextDescriptor() throws Exception {
    assertMethodDescriptors(MethodDescriptor.Context.class);
  }

  @Test
  public void checkBodyDescriptor() throws Exception {
    System.out.println(byte[].class.getName());
    assertMethodDescriptors(MethodDescriptor.Body.class);
  }

  private void assertMethodDescriptors(Class source) throws Exception {
    for (Method method : source.getDeclaredMethods()) {
      if (method.getReturnType().equals(MethodDescriptor.class)) {
        // must be static:
        assertMethodDescriptor((MethodDescriptor) method.invoke(null, new Object[0]));
      }
    }
  }

  private void assertMethodDescriptor(MethodDescriptor descriptor) throws Exception {
    String classname = descriptor.getDeclaringType().getClassName();
    Class owner = getClass().getClassLoader().loadClass(classname);
    org.objectweb.asm.Type[] argumentTypes = descriptor.getArgumentTypes();
    Class[] args = new Class[argumentTypes.length];
    for (int i = 0; i < argumentTypes.length; i++) {
      try {
        args[i] = getClass().getClassLoader().loadClass(argumentTypes[i].getClassName());
      } catch (ClassNotFoundException x) {
        args[i] =
            Class.forName(
                argumentTypes[i].getDescriptor().replace("/", "."),
                false,
                getClass().getClassLoader());
      }
    }
    Method method = owner.getMethod(descriptor.getName(), args);
    assertEquals(
        Type.getMethodDescriptor(method), descriptor.getDescriptor(), "on " + descriptor.getName());
    assertEquals(
        Type.getType(method.getReturnType()).getClassName(),
        descriptor.getReturnType().getClassName());
  }
}
