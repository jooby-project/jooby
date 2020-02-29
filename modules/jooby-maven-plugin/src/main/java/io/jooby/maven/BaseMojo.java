/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.maven;

import edu.emory.mathcs.backport.java.util.Collections;
import org.apache.maven.Maven;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DefaultDependencyResolutionRequest;
import org.apache.maven.project.DependencyResolutionException;
import org.apache.maven.project.DependencyResolutionRequest;
import org.apache.maven.project.DependencyResolutionResult;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.eclipse.aether.graph.Dependency;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public abstract class BaseMojo extends AbstractMojo {
  protected static final String APP_CLASS = "application.class";

  /** Startup class (the one with the main method. */
  @Parameter(defaultValue = "${application.class}")
  private String mainClass;

  @Parameter(defaultValue = "${session}", required = true, readonly = true)
  protected MavenSession session;

  @Component
  private Maven maven;

  @Component
  private ProjectDependenciesResolver dependenciesResolver;

  @Override public void execute() throws MojoExecutionException, MojoFailureException {
    try {
      List<MavenProject> projects = getProjects();
      if (mainClass == null) {
        mainClass = projects.stream()
            .filter(it -> it.getProperties().containsKey(APP_CLASS))
            .findFirst()
            .map(it -> it.getProperties().getProperty(APP_CLASS))
            .orElseThrow(() -> new MojoExecutionException(
                "Application class not found. Did you forget to set `application.class`?"));
      }
      getLog().debug("Found `" + APP_CLASS + "`: " + mainClass);
      doExecute(projects, mainClass);
    } catch (MojoExecutionException | MojoFailureException x) {
      throw x;
    } catch (Throwable x) {
      throw new MojoFailureException(
          "execution of " + mojoName() + " resulted in exception", x);
    }
  }

  protected String mojoName() {
    return getClass().getSimpleName().replace("Mojo", "").toLowerCase();
  }

  protected abstract void doExecute(List<MavenProject> projects, String mainClass) throws Exception;

  /**
   * Multiple projects for multimodule project. Otherwise single project.
   *
   * @return Multiple projects for multimodule project. Otherwise single project.
   */
  protected List<MavenProject> getProjects() {
    return session.getAllProjects().stream()
        .filter(it -> !it.getPackaging().equals("pom"))
        .collect(Collectors.toList());
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

  protected Set<Path> jars(MavenProject project) throws DependencyResolutionException {
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
          .map(org.eclipse.aether.artifact.Artifact::getFile)
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

  protected Set<Path> resources(MavenProject project) {
    // main/resources, etc..
    List<Resource> resourceList = project.getResources();
    List<Path> paths = resourceList.stream()
        .map(Resource::getDirectory)
        .map(Paths::get)
        .collect(Collectors.toList());
    // conf directory
    Path conf = project.getBasedir().toPath().resolve("conf");
    paths.add(conf);
    return paths.stream()
        .filter(Files::exists)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  protected Set<Path> bin(final MavenProject project) {
    return Collections.singleton(Paths.get(project.getBuild().getOutputDirectory()));
  }

  protected ClassLoader createClassLoader(List<MavenProject> projects)
      throws MalformedURLException, DependencyResolutionException {
    return toClassLoader(classpath(projects), getClass().getClassLoader());
  }

  private List<URL> classpath(List<MavenProject> projects)
      throws MalformedURLException, DependencyResolutionException {
    List<URL> classpath = new ArrayList<>();
    for (MavenProject project : projects) {
      Set<Path> cp = classpath(project);
      for (Path path : cp) {
        classpath.add(path.toUri().toURL());
      }
    }
    return classpath.stream()
        .distinct()
        .collect(Collectors.toList());
  }

  @SuppressWarnings("unchecked")
  private Set<Path> classpath(MavenProject project)
      throws DependencyResolutionException {
    Set<Path> paths = new LinkedHashSet<>();
    resources(project).forEach(paths::add);
    bin(project).forEach(paths::add);
    jars(project).forEach(paths::add);
    return paths;
  }

  private static URLClassLoader toClassLoader(final List<URL> cp, final ClassLoader parent) {
    return new URLClassLoader(cp.toArray(new URL[cp.size()]), parent) {
      @Override
      public String toString() {
        return cp.toString();
      }
    };
  }

}
