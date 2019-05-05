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
package io.jooby.maven;

import edu.emory.mathcs.backport.java.util.Collections;
import io.jooby.run.HotSwap;
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

@Mojo(name = "run", threadSafe = true, requiresDependencyResolution = ResolutionScope.TEST)
@Execute(phase = LifecyclePhase.TEST)
public class JoobyRun extends AbstractMojo {

  static {
    System.setProperty("jooby.useShutdownHook", "false");
  }

  private static final String APP_CLASS = "application.class";

  @Parameter(defaultValue = "${application.class}")
  private String mainClass;

  @Parameter(defaultValue = "${application.mode}")
  private String executionMode;

  @Parameter(defaultValue = "conf,properties,class")
  private List<String> restartExtensions;

  @Parameter(defaultValue = "java,kt")
  private List<String> compileExtensions;

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
            .orElseThrow(() -> new MojoExecutionException("Application class not found"));
      }
      if (executionMode == null) {
        executionMode = "DEFAULT";
      }
      getLog().debug("Found `" + APP_CLASS + "`: " + mainClass);

      HotSwap hotSwap = new HotSwap(session.getCurrentProject().getArtifactId(), mainClass,
          executionMode);

      Runtime.getRuntime().addShutdownHook(new Thread(hotSwap::shutdown));

      BiConsumer<String, Path> onFileChanged = (event, path) -> {
        if (isCompileExtension(path)) {
          MavenExecutionResult result = maven.execute(mavenRequest("process-classes"));
          // Success?
          if (result.hasExceptions()) {
            getLog().debug("Compilation error found: " + path);
          } else {
            getLog().debug("Restarting application on file change: " + path);
            hotSwap.restart();
          }
        } else if (isRestartExtension(path)) {
          getLog().debug("Restarting application on file change: " + path);
          hotSwap.restart();
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
            .forEach(file -> hotSwap.addResource(file, onFileChanged));
        // conf directory
        Path conf = project.getBasedir().toPath().resolve("conf");
        hotSwap.addResource(conf, onFileChanged);

        // target/classes
        hotSwap.addResource(Paths.get(project.getBuild().getOutputDirectory()));

        Set<Path> src = sourceDirectories(project);
        if (src.isEmpty()) {
          getLog().debug("Compiler is off in favor of Eclipse compiler.");
          binDirectories(project).forEach(path -> hotSwap.addResource(path, onFileChanged));
        } else {
          src.forEach(path -> hotSwap.addResource(path, onFileChanged));
        }

        artifacts(project).forEach(hotSwap::addResource);
      }

      // Block current thread.
      hotSwap.start();
    } catch (MojoExecutionException | MojoFailureException x) {
      throw x;
    } catch (Exception x) {
      throw new MojoFailureException("jooby-run resulted in exception", x);
    }
  }

  public List<String> getCompileExtensions() {
    return compileExtensions;
  }

  public void setCompileExtensions(List<String> compileExtensions) {
    this.compileExtensions = compileExtensions;
  }

  public List<String> getRestartExtensions() {
    return restartExtensions;
  }

  public void setRestartExtensions(List<String> restartExtensions) {
    this.restartExtensions = restartExtensions;
  }

  public Maven getMaven() {
    return maven;
  }

  public void setMaven(Maven maven) {
    this.maven = maven;
  }

  public MavenSession getSession() {
    return session;
  }

  public void setSession(MavenSession session) {
    this.session = session;
  }

  public ProjectDependenciesResolver getDependenciesResolver() {
    return dependenciesResolver;
  }

  public void setDependenciesResolver(
      ProjectDependenciesResolver dependenciesResolver) {
    this.dependenciesResolver = dependenciesResolver;
  }

  public String getMainClass() {
    return mainClass;
  }

  public void setMainClass(String mainClass) {
    this.mainClass = mainClass;
  }

  private boolean isCompileExtension(Path path) {
    return containsExtension(compileExtensions, path);
  }

  private boolean isRestartExtension(Path path) {
    return containsExtension(restartExtensions, path);
  }

  private boolean containsExtension(List<String> extensions, Path path) {
    String filename = path.getFileName().toString();
    return extensions.stream().anyMatch(ext -> filename.endsWith("." + ext));
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

