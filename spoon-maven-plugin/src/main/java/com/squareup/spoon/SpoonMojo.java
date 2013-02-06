// Copyright 2012 Square, Inc.
package com.squareup.spoon;

import com.google.common.base.Strings;
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
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.repository.RepositorySystem;

import static com.squareup.spoon.SpoonRunner.DEFAULT_OUTPUT_DIRECTORY;
import static org.apache.maven.plugins.annotations.LifecyclePhase.INTEGRATION_TEST;

/**
 * Goal which invokes Spoon. By default the output will be placed in a {@code spoon-output/} folder
 * in your project's build directory.
 */
@SuppressWarnings("UnusedDeclaration") // Used reflectively by Maven.
@Mojo(name = "run", defaultPhase = INTEGRATION_TEST, threadSafe = false)
public class SpoonMojo extends AbstractMojo {
  private static final String SPOON_GROUP_ID = "com.squareup.spoon";
  private static final String SPOON_PLUGIN_ARTIFACT_ID = "spoon-maven-plugin";
  private static final String SPOON_ARTIFACT_ID = "spoon-runner";
  private static final String ARTIFACT_TYPE = "zip";
  private static final String ARTIFACT_CLASSIFIER = "spoon-output";

  /** {@code -Dmaven.test.skip} is commonly used with Maven to skip tests. We honor it too. */
  @Parameter(property = "maven.test.skip", defaultValue = "false", readonly = true)
  private boolean mavenTestSkip;

  /** {@code -DskipTests} is commonly used with Maven to skip tests. We honor it too. */
  @Parameter(property = "skipTests", defaultValue = "false", readonly = true)
  private boolean mavenSkipTests;

  /** Configuration option to skip execution. */
  @Parameter
  private boolean skip;

  /** Location of the output directory. */
  @Parameter(defaultValue = "${project.build.directory}/spoon-output")
  private File outputDirectory;

  /** A title for the output website. */
  @Parameter(defaultValue = "${project.name}")
  private String title;

  /** The location of the Android SDK. */
  @Parameter(defaultValue = "${env.ANDROID_HOME}")
  private String androidSdk;

  /** Attaches output artifact as zip when {@code true}. */
  @Parameter
  private boolean attachArtifact;

  /** If true then any test failures will cause the plugin to error. */
  @Parameter
  private boolean failOnFailure;

  /** Whether debug logging is enabled. */
  @Parameter
  private boolean debug;

  @Parameter(property = "project.build.directory", required = true, readonly = true)
  private File buildDirectory;

  @Parameter(property = "project", required = true, readonly = true)
  private MavenProject project;

  @Parameter(property = "localRepository", readonly = true, required = true)
  private ArtifactRepository local;

  /** Run only a specific test. */
  @Parameter(defaultValue = "${spoon.test.class}")
  private String className;

  /** Run only a specific test method.  Must be specified with {@link #className}. */
  @Parameter(defaultValue = "${spoon.test.method}")
  private String methodName;

  @Component
  private MavenProjectHelper projectHelper;

  @Component
  private RepositorySystem repositorySystem;

  public void execute() throws MojoExecutionException {
    Log log = getLog();

    if (mavenTestSkip || mavenSkipTests || skip) {
      log.debug("maven.test.skip = " + mavenTestSkip);
      log.debug("skipTests = " + mavenSkipTests);
      log.debug("skip = " + skip);
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

    if (!Strings.isNullOrEmpty(className)) {
      log.debug("Class name: " + className);
      if (!Strings.isNullOrEmpty(methodName)) {
        log.debug("Method name: " + methodName);
      }
    }

    String classpath = getSpoonClasspath();
    log.debug("Classpath: " + classpath);

    log.debug("Output directory: " + outputDirectory.getAbsolutePath());

    log.debug("Spoon title: " + title);
    log.debug("Debug: " + Boolean.toString(debug));

    boolean success = new SpoonRunner.Builder() //
        .setTitle(title)
        .setApplicationApk(app)
        .setInstrumentationApk(instrumentation)
        .setOutputDirectory(outputDirectory)
        .setAndroidSdk(sdkFile)
        .setDebug(debug)
        .setClasspath(classpath)
        .setClassName(className)
        .setMethodName(methodName)
        .useAllAttachedDevices()
        .build()
        .run();

    if (!success && failOnFailure) {
      throw new MojoExecutionException("Spoon returned non-zero exit code.");
    }

    if (attachArtifact) {
      File outputZip = new File(buildDirectory, DEFAULT_OUTPUT_DIRECTORY + ".zip");
      ZipUtil.zip(outputZip, outputDirectory);
      projectHelper.attachArtifact(project, ARTIFACT_TYPE, ARTIFACT_CLASSIFIER, outputZip);
    }
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
    Artifact spoonPlugin =
        findArtifact(SPOON_GROUP_ID, SPOON_PLUGIN_ARTIFACT_ID, project.getPluginArtifacts());
    Set<Artifact> spoonPluginDeps = getDependenciesForArtifact(spoonPlugin);
    Artifact spoon = findArtifact(SPOON_GROUP_ID, SPOON_ARTIFACT_ID, spoonPluginDeps);
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
    ArtifactResolutionRequest arr = new ArtifactResolutionRequest().setArtifact(artifact)
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
