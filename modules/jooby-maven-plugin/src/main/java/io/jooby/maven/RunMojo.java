/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.maven;

import edu.emory.mathcs.backport.java.util.Collections;
import io.jooby.run.JoobyRun;
import io.jooby.run.JoobyRunOptions;
import org.apache.maven.Maven;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

import static org.apache.maven.plugins.annotations.LifecyclePhase.PROCESS_CLASSES;
import static org.apache.maven.plugins.annotations.ResolutionScope.COMPILE_PLUS_RUNTIME;

/**
 * Maven plugin for jooby run.
 *
 * @author edgar
 * @since 2.0.0
 */
@Mojo(name = "run", threadSafe = true, requiresDependencyResolution = COMPILE_PLUS_RUNTIME, aggregator = true)
@Execute(phase = PROCESS_CLASSES)
public class RunMojo extends BaseMojo {

  static {
    /** Turn off shutdown hook on Server. */
    System.setProperty("jooby.useShutdownHook", "false");
  }

  /**
   * List of file extensions that trigger an application restart. Default is: <code>conf</code>,
   * <code>properties</code> and <code>class</code>.
   */
  @Parameter
  private List<String> restartExtensions;

  /**
   * List of file extensions that trigger a compilation request. Default is: <code>java</code> and
   * <code>kt</code>.
   */
  @Parameter
  private List<String> compileExtensions;

  /**
   * Application port.
   */
  @Parameter
  private int port = JoobyRunOptions.DEFAULT_PORT;

  @Override protected void doExecute(List<MavenProject> projects, String mainClass) throws Exception {
    try {
      Maven maven = getMaven();
      JoobyRunOptions options = createOptions(mainClass);
      JoobyRun joobyRun = new JoobyRun(options);

      Runtime.getRuntime().addShutdownHook(new Thread(joobyRun::shutdown));

      BiConsumer<String, Path> onFileChanged = (event, path) -> {
        if (options.isCompileExtension(path)) {
          MavenExecutionResult result = maven.execute(mavenRequest("process-classes"));
          // Success?
          if (result.hasExceptions()) {
            getLog().debug("Compilation error found: " + path);
          } else {
            getLog().debug("Restarting application on file change: " + path);
            joobyRun.restart();
          }
        } else if (options.isRestartExtension(path)) {
          getLog().debug("Restarting application on file change: " + path);
          joobyRun.restart();
        } else {
          getLog().debug("Ignoring file change: " + path);
        }
      };

      for (MavenProject project : projects) {
        getLog().debug("Adding project: " + project.getArtifactId());

        // main resources + conf, etc..
        resources(project)
            .forEach(file -> joobyRun.addResource(file, onFileChanged));

        // target/classes
        bin(project).forEach(joobyRun::addResource);

        Set<Path> src = sourceDirectories(project);
        if (src.isEmpty()) {
          getLog().debug("Compiler is off in favor of Eclipse compiler.");
          bin(project).forEach(path -> joobyRun.addResource(path, onFileChanged));
        } else {
          src.forEach(path -> joobyRun.addResource(path, onFileChanged));
        }

        jars(project).forEach(joobyRun::addResource);
      }

      // Block current thread.
      joobyRun.start();
    } catch (MojoExecutionException | MojoFailureException x) {
      throw x;
    } catch (Throwable x) {
      throw new MojoFailureException("jooby-run resulted in exception", x);
    }
  }

  private JoobyRunOptions createOptions(String mainClass) {
    JoobyRunOptions options = new JoobyRunOptions();
    options.setMainClass(mainClass);
    if (compileExtensions != null) {
      options.setCompileExtensions(compileExtensions);
    }
    options.setPort(port);
    options.setProjectName(session.getCurrentProject().getArtifactId());
    if (restartExtensions != null) {
      options.setRestartExtensions(restartExtensions);
    }
    return options;
  }

  /**
   * List of file extensions that trigger a compilation request. Compilation is done via Maven or
   * Gradle. Default is: <code>java</code> and <code>kt</code>.
   *
   * @return Compile extensions.
   */
  public List<String> getCompileExtensions() {
    return compileExtensions;
  }

  /**
   * Set compile extensions. Extension is expected to be specify without <code>.</code> (dot).
   *
   * @param compileExtensions Compile extensions.
   */
  public void setCompileExtensions(List<String> compileExtensions) {
    this.compileExtensions = compileExtensions;
  }

  /**
   * List of file extensions that trigger an application restart. Default is: <code>conf</code>,
   * <code>properties</code> and <code>class</code>.
   *
   * @return Restart extensions.
   */
  public List<String> getRestartExtensions() {
    return restartExtensions;
  }

  /**
   * Set restart extensions. Extension is expected to be specify without <code>.</code> (dot).
   *
   * @param restartExtensions Restart extensions.
   */
  public void setRestartExtensions(List<String> restartExtensions) {
    this.restartExtensions = restartExtensions;
  }

  /**
   * Execute maven goal.
   *
   * @param goal Goal to execute.
   * @return Request.
   */
  private MavenExecutionRequest mavenRequest(String goal) {
    return DefaultMavenExecutionRequest.copy(session.getRequest())
        .setGoals(Collections.singletonList(goal));
  }

  private Set<Path> sourceDirectories(final MavenProject project) {
    Path eclipse = project.getBasedir().toPath().resolve(".classpath");
    if (Files.exists(eclipse)) {
      // let eclipse to do the incremental compilation
      return Collections.emptySet();
    }
    return Collections.singleton(Paths.get(project.getBuild().getSourceDirectory()));
  }

  @Override protected String mojoName() {
    return "run";
  }

  //  private boolean requiredDependency(Set<Artifact> artifacts,
  //      org.eclipse.aether.artifact.Artifact artifact) {
  //    return artifacts.stream().anyMatch(
  //        it -> it.getGroupId().equals(artifact.getGroupId())
  //            && it.getArtifactId().equals(artifact.getArtifactId())
  //            && it.getVersion().equals(artifact.getVersion()));
  //  }
  //
  //  private Collection<org.eclipse.aether.artifact.Artifact> resolveDependencies(Artifact artifact,
  //      Predicate<org.eclipse.aether.artifact.Artifact> predicate) {
  //    return resolveDependencies(new DefaultArtifact(artifact.getGroupId(), artifact.getArtifactId(),
  //        artifact.getClassifier(), artifact.getType(), artifact.getVersion()), predicate);
  //  }
  //
  //  private Set<org.eclipse.aether.artifact.Artifact> resolveDependencies(
  //      org.eclipse.aether.artifact.Artifact artifact,
  //      Predicate<org.eclipse.aether.artifact.Artifact> predicate) {
  //    CollectRequest collectRequest = new CollectRequest()
  //        .setRoot(new Dependency(artifact, null));
  //
  //    DependencyRequest request = new DependencyRequest(collectRequest, null);
  //
  //    DependencyResult result;
  //
  //    try {
  //      result = repoSystem.resolveDependencies(repoSession, request);
  //    } catch (DependencyResolutionException dre) {
  //      result = dre.getResult();
  //    }
  //
  //    if (result == null) {
  //      return Collections.emptySet();
  //    }
  //
  //    return result.getArtifactResults().stream()
  //        // Assume all dependencies has been resolved by maven. We added for ignore optional deps
  //        .filter(it -> !it.isMissing())
  //        .map(ArtifactResult::getArtifact)
  //        .filter(it -> it != null && it.getExtension().equals("jar"))
  //        .filter(predicate)
  //        .collect(toSet());
  //  }
}

