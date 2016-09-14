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
package org.jooby.internal.ftl;

import java.util.List;
import java.util.stream.Collectors;

import org.jooby.Env;

import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;
import javaslang.control.Try;

public class XssDirective implements TemplateMethodModelEx {

  private Env env;

  public XssDirective(final Env env) {
    this.env = env;
  }

  @SuppressWarnings({"rawtypes", "unchecked" })
  @Override
  public Object exec(final List arguments) throws TemplateModelException {
    List<String> args = (List<String>) arguments.stream()
        .map(it -> Try.of(() -> ((TemplateScalarModel) it).getAsString()).get())
        .collect(Collectors.toList());
    String[] xss = args.subList(1, args.size())
        .toArray(new String[arguments.size() - 1]);
    return env.xss(xss).apply(args.get(0));
  }

}
