/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.mvc;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public abstract class MvcAnnotationParserBase implements MvcAnnotationParser {

  public final List<MvcAnnotation> parse(Method method) {
    List<MvcAnnotation> result = new ArrayList<>();

    for (Class<? extends Annotation> m : httpMethods()) {
      Annotation annotation = method.getAnnotation(m);
      if (annotation != null) {
        result.add(create(method, annotation));
      }
    }

    return result;
  }

  protected abstract MvcAnnotation create(Method method, Annotation annotation);

  protected abstract List<Class<? extends Annotation>> httpMethods();

  protected String[] merge(String[] parent, String[] path) {
    if (parent == null) {
      if (path == null) {
        return new String[]{"/"};
      }
      return path;
    }
    if (path == null) {
      return parent;
    }
    String[] result = new String[parent.length * path.length];
    int k = 0;

    for (String base : parent) {
      for (String element : path) {
        result[k] = base + "/" + element;
        k += 1;
      }
    }
    return result;
  }
}
