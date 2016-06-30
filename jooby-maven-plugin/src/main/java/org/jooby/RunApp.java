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
package org.jooby;

import java.io.File;
import java.util.Set;

import org.jooby.hotreload.AppModule;

import edu.emory.mathcs.backport.java.util.Arrays;
import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;

public class RunApp implements Command {

  private String mId;
  private String mainClass;
  private File[] cp;
  private CountDownLatch latch;
  private String includes;
  private String excludes;

  public RunApp(final String mId, final String mainClass, final Set<File> cp, final String includes,
      final String excludes) {
    this.mId = mId;
    this.mainClass = mainClass;
    this.cp = cp.toArray(new File[cp.size()]);
    this.includes = includes;
    this.excludes = excludes;
    latch = new CountDownLatch(1);

  }

  @Override
  public void stop() throws InterruptedException {
    latch.countDown();
  }

  @Override
  public void execute() throws Exception {
    AppModule module = new AppModule(mId, mainClass, cp);
    if (includes != null) {
      module.includes(includes);
    }
    if (excludes != null) {
      module.excludes(excludes);
    }
    module.run();
    latch.await();
  }

  @Override
  public File getWorkdir() {
    return new File(System.getProperty("user.dir"));
  }

  @Override
  public void setWorkdir(final File workdir) {
    // NOOP
  }

  @Override
  public String debug() {
    return mId + "; deps = " + Arrays.toString(cp);
  }

  @Override
  public String toString() {
    return mainClass;
  }
}
