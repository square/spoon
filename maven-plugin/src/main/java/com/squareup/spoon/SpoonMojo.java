// Copyright 2012 Square, Inc.
package com.squareup.spoon;

import java.io.File;
import java.util.Iterator;
import java.util.Set;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;

import static com.squareup.spoon.Main.DEFAULT_TITLE;
import static com.squareup.spoon.Main.OUTPUT_DIRECTORY_NAME;
import static org.apache.maven.plugins.annotations.LifecyclePhase.INTEGRATION_TEST;

/**
 * Goal which invokes Spoon. By default the output will be placed in a {@code spoon-output/} folder
 * in your project's build directory.
 */
@SuppressWarnings("UnusedDeclaration") // Used reflectively by Maven.
@Mojo(name = "run", defaultPhase = INTEGRATION_TEST, threadSafe = true)
public class SpoonMojo extends AbstractMojo {
  /** {@code -Dmaven.test.skip} is commonly used with Maven to skip tests. We honor it too. */
  @Parameter(property = "maven.test.skip", defaultValue = "false")
  private boolean mavenTestSkip;

  /** {@code -DskipTests} is commonly used with Maven to skip tests. We honor it too. */
  @Parameter(property = "skipTests", defaultValue = "false")
  private boolean mavenSkipTests;

  /** Location of the output directory. */
  @Parameter(property = "project.build.directory", required = true)
  private File outputDirectory;

  /** A title for the output website. */
  @Parameter(defaultValue = DEFAULT_TITLE)
  private String title;

  /** The location of the Android SDK. */
  @Parameter(property = "env.ANDROID_HOME", required = true)
  private String androidSdk;

  /** The location of the Android SDK. */
  @Parameter
  private boolean debug;

  /** Maven project. */
  @Parameter(property = "project", required = true, readonly = true)
  private MavenProject project;

  @Parameter(property = "localRepository", readonly = true, required = true)
  private ArtifactRepository local;

  @Component
  private RepositorySystem repositorySystem;

  public void execute() throws MojoExecutionException {
    Log log = getLog();

    if (mavenTestSkip || mavenSkipTests) {
      log.info("Skipping Spoon execution.");
      return;
    }

    File sdkFile = new File(androidSdk);
    if (!sdkFile.exists()) {
      throw new MojoExecutionException(
          "Could not find Android SDK. Ensure ANDROID_HOME environment variable is set.");
    }
    log.debug("Android SDK: " + sdkFile.getAbsolutePath());

    File instrumentation = getInstrumentationApk();
    log.debug("Instrumentation APK: " + instrumentation);

    File app = getApplicationApk();
    log.debug("Application APK: " + app.getAbsolutePath());

    String classpath = getSpoonClasspath();
    log.debug("Classpath: " + classpath);

    File output = new File(outputDirectory, OUTPUT_DIRECTORY_NAME);
    log.debug("Output directory: " + output.getAbsolutePath());

    log.debug("Spoon title: " + title);
    log.debug("Debug: " + Boolean.toString(debug));

    new ExecutionSuite(title, androidSdk, app, instrumentation, output, debug, classpath).run();
  }

  private File getInstrumentationApk() throws MojoExecutionException {
    Artifact instrumentationArtifact = project.getArtifact();
    if (!"apk".equals(instrumentationArtifact.getType())) {
      throw new MojoExecutionException("Spoon can only be invoked on a module with type 'apk'.");
    }
    return instrumentationArtifact.getFile();
  }

  private File getApplicationApk() throws MojoExecutionException {
    for (Artifact dependency : project.getDependencyArtifacts()) {
      if ("apk".equals(dependency.getType())) {
        return dependency.getFile();
      }
    }
    throw new MojoExecutionException(
        "Could not find application. Ensure 'apk' dependency on it exists.");
  }

  private String getSpoonClasspath() throws MojoExecutionException {
    Artifact spoonPlugin = findArtifact("com.squareup.spoon", "spoon-maven-plugin",
        project.getPluginArtifacts());
    Set<Artifact> spoonPluginDeps = getDependenciesForArtifact(spoonPlugin);
    Artifact spoon = findArtifact("com.squareup.spoon", "spoon", spoonPluginDeps);
    Set<Artifact> spoonDeps = getDependenciesForArtifact(spoon);
    return createClasspath(spoonDeps);
  }

  private static Artifact findArtifact(String groupId, String artifactId, Set<Artifact> artifacts)
      throws MojoExecutionException {
    for (Artifact artifact : artifacts) {
      if (groupId.equals(artifact.getGroupId()) && artifactId.equals(artifact.getArtifactId())) {
        return artifact;
      }
    }
    throw new MojoExecutionException("Could not find " + groupId + ":" + artifactId + " artifact.");
  }

  private Set<Artifact> getDependenciesForArtifact(Artifact artifact) {
    ArtifactResolutionRequest arr = new ArtifactResolutionRequest()
        .setArtifact(artifact)
        .setResolveTransitively(true)
        .setLocalRepository(local);
    return repositorySystem.resolve(arr).getArtifacts();
  }

  private String createClasspath(Set<Artifact> selfWithDeps) {
    StringBuilder builder = new StringBuilder();
    Iterator<Artifact> i = selfWithDeps.iterator();
    if (i.hasNext()) {
      builder.append(getLocalPathToArtifact(i.next()));
      while (i.hasNext()) {
        builder.append(File.pathSeparator).append(getLocalPathToArtifact(i.next()));
      }
    }
    return builder.toString();
  }

  private String getLocalPathToArtifact(Artifact artifact) {
    return new File(local.getBasedir(), local.pathOf(artifact)).getAbsolutePath();
  }
}
