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
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.DefaultDependencyResolutionRequest;
import org.apache.maven.project.DependencyResolutionException;
import org.apache.maven.project.DependencyResolutionRequest;
import org.apache.maven.project.DependencyResolutionResult;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Maven plugin for jooby run.
 *
 * @author edgar
 * @since 2.0.0
 */
@Mojo(name = "run", threadSafe = true, requiresDependencyResolution = ResolutionScope.TEST, aggregator = true)
@Execute(phase = LifecyclePhase.TEST_COMPILE)
public class RunMojo extends AbstractMojo {

  static {
    /** Turn off shutdown hook on Server. */
    System.setProperty("jooby.useShutdownHook", "false");
  }

  private static final String APP_CLASS = "application.class";

  /** Startup class (the one with the main method. */
  @Parameter(defaultValue = "${application.class}")
  private String mainClass;

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

  @Parameter(defaultValue = "${session}", required = true, readonly = true)
  private MavenSession session;

  @Component
  private ProjectDependenciesResolver dependenciesResolver;

  @Component
  private Maven maven;

  @Override public void execute() throws MojoExecutionException, MojoFailureException {
    try {
      List<MavenProject> projects = projects();

      if (mainClass == null) {
        mainClass = projects.stream()
            .filter(it -> it.getProperties().containsKey(APP_CLASS))
            .findFirst()
            .map(it -> it.getProperties().getProperty(APP_CLASS))
            .orElseThrow(() -> new MojoExecutionException(
                "Application class not found. Did you forget to set `application.class`?"));
      }
      getLog().debug("Found `" + APP_CLASS + "`: " + mainClass);

      JoobyRunOptions options = createOptions();
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

        // main/resources, etc..
        List<Resource> resourceList = project.getResources();
        resourceList.stream()
            .map(Resource::getDirectory)
            .map(Paths::get)
            .forEach(file -> joobyRun.addResource(file, onFileChanged));
        // conf directory
        Path conf = project.getBasedir().toPath().resolve("conf");
        joobyRun.addResource(conf, onFileChanged);

        // target/classes
        joobyRun.addResource(Paths.get(project.getBuild().getOutputDirectory()));

        Set<Path> src = sourceDirectories(project);
        if (src.isEmpty()) {
          getLog().debug("Compiler is off in favor of Eclipse compiler.");
          binDirectories(project).forEach(path -> joobyRun.addResource(path, onFileChanged));
        } else {
          src.forEach(path -> joobyRun.addResource(path, onFileChanged));
        }

        artifacts(project).forEach(joobyRun::addResource);
      }

      // Block current thread.
      joobyRun.start();
    } catch (MojoExecutionException | MojoFailureException x) {
      throw x;
    } catch (Throwable x) {
      throw new MojoFailureException("jooby-run resulted in exception", x);
    }
  }

  private JoobyRunOptions createOptions() {
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
   * Maven reference (available while running the plugin).
   *
   * @return Maven reference (available while running the plugin).
   */
  public Maven getMaven() {
    return maven;
  }

  /**
   * Set maven instance (available while running the plugin).
   *
   * @param maven Maven instance Maven reference (available while running the plugin).
   */
  public void setMaven(Maven maven) {
    this.maven = maven;
  }

  /**
   * Maven session reference (available while running the plugin).
   *
   * @return Maven reference (available while running the plugin).
   */
  public MavenSession getSession() {
    return session;
  }

  /**
   * Set maven session instance (available while running the plugin).
   *
   * @param session Maven session instance Maven reference (available while running the plugin).
   */
  public void setSession(MavenSession session) {
    this.session = session;
  }

  /**
   * Project dependencies resolver (available while running the plugin).
   *
   * @return Project dependencies resolver (available while running the plugin).
   */
  public ProjectDependenciesResolver getDependenciesResolver() {
    return dependenciesResolver;
  }

  /**
   * Set project dependencies resolver.
   *
   * @param dependenciesResolver Project dependencies resolver.
   */
  public void setDependenciesResolver(
      ProjectDependenciesResolver dependenciesResolver) {
    this.dependenciesResolver = dependenciesResolver;
  }

  /**
   * Application main class.
   *
   * @return Application main class.
   */
  public String getMainClass() {
    return mainClass;
  }

  /**
   * Set application main class.
   *
   * @param mainClass Application main class.
   */
  public void setMainClass(String mainClass) {
    this.mainClass = mainClass;
  }

  private Set<Path> artifacts(MavenProject project) throws DependencyResolutionException {
    Set<org.apache.maven.artifact.Artifact> artifacts = project.getArtifacts();
    if (artifacts.isEmpty()) {
      DependencyResolutionRequest request = new DefaultDependencyResolutionRequest();
      request.setMavenProject(project);
      request.setRepositorySession(session.getRepositorySession());
      DependencyResolutionResult result = dependenciesResolver.resolve(request);
      return result.getDependencies().stream()
          .filter(it -> !it.isOptional())
          .map(Dependency::getArtifact)
          .filter(it -> it.getExtension().equals("jar"))
          .map(Artifact::getFile)
          .map(File::toPath)
          .collect(Collectors.toCollection(LinkedHashSet::new));
    } else {
      return artifacts.stream()
          .map(org.apache.maven.artifact.Artifact::getFile)
          .filter(it -> it.toString().endsWith(".jar"))
          .map(File::toPath)
          .collect(Collectors.toCollection(LinkedHashSet::new));
    }
  }

  /**
   * Multiple projects for multimodule project. Otherwise single project.
   *
   * @return Multiple projects for multimodule project. Otherwise single project.
   */
  private List<MavenProject> projects() {
    return session.getAllProjects().stream()
        .filter(it -> !it.getPackaging().equals("pom"))
        .collect(Collectors.toList());
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

  private Set<Path> binDirectories(final MavenProject project) {
    return Collections.singleton(Paths.get(project.getBuild().getOutputDirectory()));
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

