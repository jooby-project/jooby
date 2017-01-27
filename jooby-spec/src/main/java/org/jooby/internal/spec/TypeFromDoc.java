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
package org.jooby.internal.spec;

import java.lang.reflect.Type;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.javaparser.ast.Node;
import com.google.inject.util.Types;

public class TypeFromDoc {

  private static final Pattern TYPE = Pattern.compile("\\{@link\\s+([^\\}]+)\\}");


  public static Optional<Type> parse(final Node node, final Context ctx, final String text) {
    Matcher matcher = TYPE.matcher(text);
    Type type = null;
    while (matcher.find()) {
      String link = matcher.group(1).trim();
      String stype = link.split("\\s+")[0];
      Optional<Type> resolvedType = ctx.resolveType(node, stype);
      if (resolvedType.isPresent()) {
        Type ittype = resolvedType.get();
        if (type != null) {
          type = Types.newParameterizedType(type, ittype);
        } else {
          type = ittype;
        }
      }
    }
    return Optional.ofNullable(type);
  }
}
