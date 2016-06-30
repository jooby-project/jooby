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
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.plugin.MojoFailureException;
import org.jooby.hotreload.AppModule;

public class RunForkedApp implements Command {

  private File basedir;

  private ExternalCommand cmd;

  private String mainClass;

  public RunForkedApp(final File basedir, final String debug, final List<String> vmArgs,
      final Set<File> cp,
      final String mId, final String mainClass, final Set<File> appcp, final String includes,
      final String excludes) throws MojoFailureException {
    this.basedir = basedir;
    List<String> args = new ArrayList<String>();
    args.addAll(vmArgs(debug, vmArgs));
    args.add("-cp");
    args.add(
        cp.stream().map(File::getAbsolutePath).collect(Collectors.joining(File.pathSeparator)));
    args.add(AppModule.class.getName());
    args.add(mId);
    args.add(mainClass);
    args.add(appcp.stream().map(File::getAbsolutePath).collect(Collectors.joining(":", "deps=", ""))
        .trim());
    if (includes != null) {
      args.add("includes=" + includes);
    }
    if (excludes != null) {
      args.add("excludes=" + excludes);
    }
    args.add(
        "props=" + dumpSysProps(basedir.toPath().resolve("target")
            .resolve("sys.properties")));

    cmd = new ExternalCommand(mainClass, "java", args);
    cmd.setWorkdir(basedir);
    this.mainClass = mainClass;
  }

  @Override
  public void stop() throws InterruptedException {
    cmd.stop();
  }

  @Override
  public void execute() throws Exception {
    cmd.execute();
  }

  private Path dumpSysProps(final Path path) throws MojoFailureException {
    try {
      FileOutputStream output = new FileOutputStream(path.toFile());
      Properties properties = System.getProperties();
      properties.store(output, "system properties");
      return path;
    } catch (IOException ex) {
      throw new MojoFailureException("Can't dump system properties to: " + path, ex);
    }
  }

  @Override
  public File getWorkdir() {
    return cmd.getWorkdir();
  }

  @Override
  public void setWorkdir(final File workdir) {
  }

  @Override
  public String debug() {
    return cmd.debug();
  }

  private List<String> vmArgs(final String debug, final List<String> vmArgs) {
    List<String> results = new ArrayList<String>();
    if (vmArgs != null) {
      results.addAll(vmArgs);
    }
    if (!"false".equals(debug)) {
      // true, number, debug line
      if ("true".equals(debug)) {
        // default debug
        results.add("-agentlib:jdwp=transport=dt_socket,address=8000,server=y,suspend=n");
      } else {
        try {
          int port = Integer.parseInt(debug);
          results.add("-agentlib:jdwp=transport=dt_socket,address=" + port + ",server=y,suspend=n");
        } catch (NumberFormatException ex) {
          // assume it is a debug line
          results.add(debug);
        }
      }
    }
    // logback
    File[] logbackFiles = {localFile("conf", "logback-test.xml"),
        localFile("conf", "logback.xml") };
    for (File logback : logbackFiles) {
      if (logback.exists()) {
        results.add("-Dlogback.configurationFile=" + logback.getAbsolutePath());
        break;
      }
    }
    // dcevm? OFF
    // String altjvm = null;
    // for (String boot : System.getProperty("sun.boot.library.path", "").split(File.pathSeparator))
    // {
    // File dcevm = new File(boot, "dcevm");
    // if (dcevm.exists()) {
    // altjvm = dcevm.getName();
    // }
    // }
    // if (altjvm == null) {
    // getLog().error("dcevm not found, please install it: https://github.com/dcevm/dcevm");
    // } else {
    // results.add("-XXaltjvm=" + altjvm);
    // }
    return results;
  }

  private File localFile(final String... paths) {
    File result = basedir;
    for (String path : paths) {
      result = new File(result, path);
    }
    return result;
  }

  @Override
  public String toString() {
    return mainClass;
  }
}
