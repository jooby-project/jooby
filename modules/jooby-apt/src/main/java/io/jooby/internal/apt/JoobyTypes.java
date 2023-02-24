/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt;

import org.objectweb.asm.Type;

public interface JoobyTypes {
  Type Reified = Type.getType("Lio/jooby/Reified;");

  Type MvcFactory = Type.getType("Lio/jooby/MvcFactory;");

  Type StatusCode = Type.getType("Lio/jooby/StatusCode;");
  Type Dispatch = Type.getType("Lio/jooby/annotations.Dispatch;");

  Type Context = Type.getType("Lio/jooby/Context;");
  Type MediaType = Type.getType("Lio/jooby/MediaType;");
  Type FileUpload = Type.getType("Lio/jooby/FileUpload;");
  Type QueryString = Type.getType("Lio/jooby/QueryString;");

  Type Formdata = Type.getType("Lio/jooby/Formdata;");
  Type FlashMap = Type.getType("Lio/jooby/FlashMap;");

  Type Session = Type.getType("Lio/jooby/Session;");

  Type Route = Type.getType("Lio/jooby/Route;");

  Type Value = Type.getType("Lio/jooby/Value;");
  Type ValueNode = Type.getType("Lio/jooby/ValueNode;");
  Type Body = Type.getType("Lio/jooby/Body;");

  Type ParamSource = Type.getType("Lio/jooby/ParamSource;");

  Type Provider = Type.getType("Ljakarta/inject/Provider;");
}
