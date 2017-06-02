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
package org.jooby.internal.hbm;

import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.hibernate.boot.archive.scan.spi.ScanEnvironment;

public class ScanEnvImpl implements ScanEnvironment {

  private List<URL> packages;

  public ScanEnvImpl(final List<URL> packages)  {
    this.packages = packages;
  }

  @Override
  public URL getRootUrl() {
    return null;
  }

  @Override
  public List<URL> getNonRootUrls() {
    return packages;
  }

  @Override
  public List<String> getExplicitlyListedClassNames() {
    return Collections.emptyList();
  }

  @Override
  public List<String> getExplicitlyListedMappingFiles() {
    return Collections.emptyList();
  }

}
