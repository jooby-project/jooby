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

import org.jooby.MediaType;
import org.jooby.MediaType.Matcher;
import org.jooby.Parser;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.TypeLiteral;

class JacksonParser implements Parser {

  private ObjectMapper mapper;

  private Matcher matcher;

  public JacksonParser(final ObjectMapper mapper, final MediaType type) {
    this.mapper = mapper;
    this.matcher = MediaType.matcher(type);
  }

  @Override
  public Object parse(final TypeLiteral<?> type, final Context ctx) throws Exception {
    MediaType ctype = ctx.type();
    if (ctype.isAny()) {
      // */*
      return ctx.next();
    }

    JavaType javaType = mapper.constructType(type.getType());
    if (matcher.matches(ctype) && mapper.canDeserialize(javaType)) {
      return ctx.body(body -> mapper.readValue(body.bytes(), javaType))
          .param(values -> mapper.readValue(values.iterator().next(), javaType));
    }
    return ctx.next();
  }

  @Override
  public String toString() {
    return "json";
  }

}
