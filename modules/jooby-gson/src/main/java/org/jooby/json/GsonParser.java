/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby.json;

import static java.util.Objects.requireNonNull;

import org.jooby.MediaType;
import org.jooby.Parser;

import com.google.gson.Gson;
import com.google.inject.TypeLiteral;

class GsonParser implements Parser {

  private MediaType type;

  private Gson gson;

  public GsonParser(final MediaType type, final Gson gson) {
    this.type = requireNonNull(type, "Media type is required.");
    this.gson = requireNonNull(gson, "Gson is required.");
  }

  @Override
  public Object parse(final TypeLiteral<?> type, final Context ctx) throws Throwable {
    MediaType ctype = ctx.type();
    if (ctype.isAny()) {
      // */*
      return ctx.next();
    }

    if (ctype.matches(this.type)) {
      return ctx
          .ifbody(body -> gson.fromJson(body.text(), type.getType()))
          .ifparam(values -> gson.fromJson(values.first(), type.getType()));
    }
    return ctx.next();
  }

  @Override
  public String toString() {
    return "gson";
  }
}
