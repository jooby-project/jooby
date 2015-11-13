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
package org.jooby.internal;

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

public class RegexRouteMatcher implements RouteMatcher {

  private final Matcher matcher;

  private final List<String> varNames;

  private final Map<Object, String> vars = new HashMap<>();

  private final String path;

  public RegexRouteMatcher(final String path, final Matcher matcher,
      final List<String> varNames) {
    this.path = requireNonNull(path, "A path is required.");
    this.matcher = requireNonNull(matcher, "A matcher is required.");
    this.varNames = requireNonNull(varNames, "The varNames are required.");
  }

  @Override
  public String path() {
    return path;
  }

  @Override
  public boolean matches() {
    boolean matches = matcher.matches();
    if (matches) {
      int varCount = varNames.size();
      int groupCount = matcher.groupCount();
      for (int idx = 0; idx < groupCount; idx++) {
        String var = matcher.group(idx + 1);
        if (var.startsWith("/")) {
          var = var.substring(1);
        }
        // idx indices
        vars.put(idx, var);
        // named vars
        if (idx < varCount) {
          vars.put(varNames.get(idx), matcher.group("v" + idx));
        }
      }
    }
    return matches;
  }

  @Override
  public Map<Object, String> vars() {
    return vars;
  }

}
