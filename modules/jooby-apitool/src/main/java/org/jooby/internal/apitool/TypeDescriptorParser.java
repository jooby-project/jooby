package org.jooby.internal.apitool;

import com.google.common.collect.ImmutableList;
import com.google.inject.util.Types;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

class TypeDescriptorParser extends SignatureVisitor {
  private List<List<Type>> stack = new ArrayList<>();
  private boolean returns;
  private ClassLoader loader;
  private List<java.lang.reflect.Type> result;
  private int index;

  private TypeDescriptorParser(final ClassLoader loader) {
    super(Opcodes.ASM5);
    this.loader = loader;
  }

  public static Type parse(ClassLoader loader, String desc) {
    TypeDescriptorParser builder = new TypeDescriptorParser(loader);
    new SignatureReader(desc).accept(builder);
    return builder.getType();
  }

  private java.lang.reflect.Type getType() {
    java.lang.reflect.Type type = result.get(0);
    stack.forEach(List::clear);
    stack.clear();
    return type;
  }

  private java.lang.reflect.Type primitive(final org.objectweb.asm.Type type) {
    switch (type.getSort()) {
      case org.objectweb.asm.Type.BOOLEAN:
        return boolean.class;
      case org.objectweb.asm.Type.CHAR:
        return char.class;
      case org.objectweb.asm.Type.BYTE:
        return byte.class;
      case org.objectweb.asm.Type.SHORT:
        return short.class;
      case org.objectweb.asm.Type.INT:
        return int.class;
      case org.objectweb.asm.Type.LONG:
        return long.class;
      case org.objectweb.asm.Type.FLOAT:
        return float.class;
    }
    return double.class;
  }

  @Override
  public void visitBaseType(final char descriptor) {
    if (returns) {
      List<java.lang.reflect.Type> types = new LinkedList<>();
      types.add(primitive(org.objectweb.asm.Type.getType(Character.toString(descriptor))));
      stack.add(types);
      index += 1;
      visitEnd();
    }
  }

  @Override
  public void visitClassType(final String name) {
    if (returns) {
      List<java.lang.reflect.Type> types;
      if (index < stack.size()) {
        types = stack.get(index);
      } else {
        types = new LinkedList<>();
        stack.add(types);
      }
      java.lang.reflect.Type type = BytecodeRouteParser.loadType(loader, name);
      types.add(type);
      index += 1;
    }
  }

  @Override
  public void visitEnd() {
    if (returns) {
      List<java.lang.reflect.Type> types = stack.get(index - 1);
      if (result == null) {
        result = types;
      } else if (result != types) {
        result = ImmutableList.of(Types.newParameterizedType(types.get(0),
            result.toArray(new java.lang.reflect.Type[0])));
      }
      index -= 1;
    }
  }

  @Override
  public SignatureVisitor visitReturnType() {
    returns = true;
    return this;
  }

}
