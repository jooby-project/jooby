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

import static org.twdata.maven.mojoexecutor.MojoExecutor.artifactId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.configuration;
import static org.twdata.maven.mojoexecutor.MojoExecutor.element;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;
import static org.twdata.maven.mojoexecutor.MojoExecutor.goal;
import static org.twdata.maven.mojoexecutor.MojoExecutor.groupId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.name;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;
import static org.twdata.maven.mojoexecutor.MojoExecutor.version;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

@Mojo(name = "run", threadSafe = true, requiresDependencyResolution = ResolutionScope.TEST)
@Execute(phase = LifecyclePhase.TEST_COMPILE)
public class JoobyRun extends AbstractMojo {

  @Component
  private MavenProject mavenProject;

  @Component
  private MavenSession mavenSession;

  @Component
  private BuildPluginManager pluginManager;

  @Parameter(defaultValue = "${exec-maven-plugin.version}")
  private String execVersion;

  @Parameter(property = "skip", defaultValue = "false")
  private boolean skip;

  /**
   * Whether to interrupt/join and possibly stop the daemon threads upon quitting. <br/>
   * If this is <code>false</code>, maven does nothing about the daemon threads. When maven has no
   * more work to do,
   * the VM will normally terminate any remaining daemon threads.
   * <p>
   * In certain cases (in particular if maven is embedded), you might need to keep this enabled to
   * make sure threads are properly cleaned up to ensure they don't interfere with subsequent
   * activity. In that case, see {@link #daemonThreadJoinTimeout} and
   * {@link #stopUnresponsiveDaemonThreads} for further tuning.
   * </p>
   *
   * @since 1.1-beta-1
   */
  @Parameter(property = "jooby.cleanupDaemonThreads", defaultValue = "true")
  private boolean cleanupDaemonThreads;

  /**
   * This defines the number of milliseconds to wait for daemon threads to quit following their
   * interruption.<br/>
   * This is only taken into account if {@link #cleanupDaemonThreads} is <code>true</code>. A value
   * &lt;=0 means to
   * not timeout (i.e. wait indefinitely for threads to finish). Following a timeout, a warning will
   * be logged.
   * <p>
   * Note: properly coded threads <i>should</i> terminate upon interruption but some threads may
   * prove problematic: as the VM does interrupt daemon threads, some code may not have been written
   * to handle interruption properly. For example java.util.Timer is known to not handle
   * interruptions in JDK &lt;= 1.6. So it is not possible for us to infinitely wait by default
   * otherwise maven could hang. A sensible default value has been chosen, but this default value
   * <i>may change</i> in the future based on user feedback.
   * </p>
   */
  @Parameter(property = "jooby.daemonThreadJoinTimeout", defaultValue = "15000")
  private long daemonThreadJoinTimeout;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    List<Resource> resources = mavenProject.getResources();
    Set<String> cp = new LinkedHashSet<String>();
    for (Resource resource : resources) {
      cp.add(resource.getDirectory());
    }

    getLog().info("CP: " + cp);


    executeMojo(
        plugin(
            groupId("org.codehaus.mojo"),
            artifactId("exec-maven-plugin"),
            version(execVersion)
        ),
        goal("exec"),
        configuration(
            element(name("executable"), "java"),
            element("arguments",
                element("argument", "-classpath"),
                element("classpath")
            ),
            element(name("mainClass"), "${application.main}"),
            element(name("killAfter"), "-1"),
            element(name("arguments"), "${jooby.arguments}"),
            element(name("skip"), Boolean.toString(skip)),
            element(name("cleanupDaemonThreads"), Boolean.toString(cleanupDaemonThreads)),
            element(name("daemonThreadJoinTimeout"), Long.toString(daemonThreadJoinTimeout))
//            element("additionalClasspathElements", additionalClasspathElements)
        ),
        executionEnvironment(
            mavenProject,
            mavenSession,
            pluginManager
        ));

  }

}
