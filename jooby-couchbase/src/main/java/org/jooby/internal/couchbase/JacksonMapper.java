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
package org.jooby.internal.couchbase;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.jooby.couchbase.N1Q;

import com.couchbase.client.deps.com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.couchbase.client.deps.com.fasterxml.jackson.core.JsonParser;
import com.couchbase.client.deps.com.fasterxml.jackson.core.JsonProcessingException;
import com.couchbase.client.deps.com.fasterxml.jackson.databind.DeserializationContext;
import com.couchbase.client.deps.com.fasterxml.jackson.databind.DeserializationFeature;
import com.couchbase.client.deps.com.fasterxml.jackson.databind.JsonDeserializer;
import com.couchbase.client.deps.com.fasterxml.jackson.databind.JsonNode;
import com.couchbase.client.deps.com.fasterxml.jackson.databind.ObjectMapper;
import com.couchbase.client.deps.com.fasterxml.jackson.databind.SerializationFeature;
import com.couchbase.client.deps.com.fasterxml.jackson.databind.module.SimpleModule;
import com.couchbase.client.java.document.EntityDocument;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.repository.mapping.EntityConverter;
import com.couchbase.client.java.transcoder.JacksonTransformers;

import javaslang.control.Try;

@SuppressWarnings({"rawtypes", "unchecked" })
public class JacksonMapper implements EntityConverter<JsonDocument> {

  static final ObjectMapper MAPPER;

  static {
    MAPPER = JacksonTransformers.MAPPER
        .copy()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);

    /** Fields ONLY, ignore transient field. */
    MAPPER.setVisibility(
        MAPPER.getSerializationConfig().getDefaultVisibilityChecker()
            .withFieldVisibility(Visibility.ANY)
            .withGetterVisibility(Visibility.NONE));

    SimpleModule module = new SimpleModule("couchbase-hack");
    // hack couchbase query response where long are reported as double
    module.addDeserializer(Date.class, new JsonDeserializer<Date>() {
      @Override
      public Date deserialize(final JsonParser p, final DeserializationContext ctxt)
          throws IOException, JsonProcessingException {
        Number number = p.getNumberValue();
        return new Date(number.longValue());
      }
    });
    MAPPER.registerModule(module);
  }

  private ObjectMapper mapper;

  private JacksonMapper(final ObjectMapper mapper) {
    this.mapper = mapper;
  }

  public JacksonMapper() {
    this(MAPPER);
  }

  @Override
  public JsonDocument fromEntity(final EntityDocument<Object> source) {
    Object bean = source.content();
    Map json = mapper.convertValue(bean, HashMap.class);
    json.put(N1Q.CLASS, bean.getClass().getName());
    return JsonDocument.create(source.id(), JsonObject.from(json));
  }

  @Override
  public <T> EntityDocument<T> toEntity(final JsonDocument source, final Class<T> clazz) {
    JsonObject json = source.content();
    // favor embedded type over provided type
    Class<T> type = type(json.getString(N1Q.CLASS), clazz);
    T value = mapper.convertValue(json, type);
    return EntityDocument.create(source.id(), value);
  }

  public <T> T fromBytes(final byte[] bytes) throws IOException {
    JsonNode json = mapper.readTree(bytes);
    return (T) mapper.treeToValue(json, type(json.get(N1Q.CLASS).textValue(), null));
  }

  private Class type(final String cname, final Class deftype) {
    return Try.of(() -> getClass().getClassLoader().loadClass(cname)).getOrElse(deftype);
  }

}
