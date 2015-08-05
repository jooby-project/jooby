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
package org.jooby.internal.sass;

import org.jooby.Err;
import org.jooby.Status;
import org.w3c.css.sac.InputSource;

import com.vaadin.sass.internal.ScssStylesheet;
import com.vaadin.sass.internal.resolver.AbstractResolver;

/**
 * Force Sass to fail and avoid silently failures.
 *
 * @author edgar
 */
@SuppressWarnings("serial")
public class FileNotFoundResolver extends AbstractResolver {

  private String identifier;

  @Override
  protected InputSource resolveNormalized(final String identifier) {
    if (this.identifier == null) {
      this.identifier = identifier;
    }
    return null;
  }

  public void validate(final ScssStylesheet scss) {
    if (identifier != null || scss == null) {
      throw new Err(Status.NOT_FOUND, identifier);
    }
  }

}
