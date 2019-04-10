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
package io.jooby.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Allow access to field or entire form from MVC route method.
 *
 * <pre>{@code
 *  public String formField(&#64;FormParm String name) {
 *    ...
 *  }
 *
 *  public String form(&#64;FormParam MyForm form) {
 *    ...
 *  }
 * }</pre>
 *
 * HTTP request must be encoded as {@link io.jooby.MediaType#FORM_URLENCODED} or
 * {@link io.jooby.MediaType#MULTIPART_FORMDATA}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface FormParam {

  /**
   * Parameter name.
   *
   * @return Parameter name.
   */
  String value() default "";
}
