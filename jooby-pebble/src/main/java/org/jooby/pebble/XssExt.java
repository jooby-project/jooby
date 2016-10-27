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
package org.jooby.pebble;

import java.util.List;
import java.util.Map;

import org.jooby.Env;

import com.google.common.collect.ImmutableMap;
import com.mitchellbosecke.pebble.extension.AbstractExtension;
import com.mitchellbosecke.pebble.extension.Function;

public class XssExt extends AbstractExtension {

  private Map<String, Function> xss;

  public XssExt(final Env env) {
    this.xss = ImmutableMap.of("xss", new Function() {

      @Override
      public List<String> getArgumentNames() {
        return null;
      }

      @Override
      public Object execute(final Map<String, Object> args) {
        args.remove("_context");
        args.remove("_self");
        Object[] values = args.values().toArray(new Object[args.size()]);
        String[] xss = new String[values.length - 1];
        System.arraycopy(values, 1, xss, 0, values.length - 1);
        return env.xss(xss).apply(values[0].toString());
      }
    });
  }

  @Override
  public Map<String, Function> getFunctions() {
    return xss;
  }
}
