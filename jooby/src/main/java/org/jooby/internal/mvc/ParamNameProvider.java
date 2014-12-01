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
package org.jooby.internal.mvc;

import java.lang.reflect.Parameter;

import javax.inject.Named;

import org.jooby.mvc.Header;

import com.google.common.base.Strings;

public interface ParamNameProvider {

  ParamNameProvider NAMED = new ParamNameProvider() {
    @Override
    public String name(final int index, final Parameter parameter) {
      Named named = parameter.getAnnotation(Named.class);
      if (named == null) {
        com.google.inject.name.Named gnamed = parameter
            .getAnnotation(com.google.inject.name.Named.class);
        if (gnamed == null) {
          Header header = parameter.getAnnotation(Header.class);
          if (header == null) {
            return null;
          }
          return Strings.emptyToNull(header.value());
        }
        return Strings.emptyToNull(gnamed.value());
      }
      return Strings.emptyToNull(named.value());
    }
  };

  String name(int index, Parameter parameter);

}
