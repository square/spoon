// Copyright 2012 Square, Inc.
package com.squareup.spoon.mojo;

import com.google.common.base.Strings;
import com.squareup.spoon.SpoonRunner;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;

import java.io.File;
import java.util.List;

import static com.squareup.spoon.SpoonRunner.DEFAULT_OUTPUT_DIRECTORY;
import static org.apache.maven.plugins.annotations.LifecyclePhase.INTEGRATION_TEST;

/**
 * Goal which invokes Spoon. By default the output will be placed in a {@code spoon-output/} folder
 * in your project's build directory.
 */
@SuppressWarnings("UnusedDeclaration") // Used reflectively by Maven.
@Mojo(name = "run", defaultPhase = INTEGRATION_TEST, threadSafe = false)
public class RunMojo extends AbstractSpoonMojo {
  private static final String SPOON_GROUP_ID = "com.squareup.spoon";
  private static final String SPOON_PLUGIN_ARTIFACT_ID = "spoon-maven-plugin";
  private static final String SPOON_RUNNER_ARTIFACT_ID = "spoon-runner";
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

  /** If true then build will fail when no device to test on is attached. */
  @Parameter
  private boolean failIfNoDeviceConnected;

  /** Execute tests sequentially (one device at a time) */
  @Parameter
  private boolean sequential;

  /** Whether debug logging is enabled. */
  @Parameter
  private boolean debug;

  @Parameter(property = "project.build.directory", required = true, readonly = true)
  private File buildDirectory;

  @Parameter(property = "project", required = true, readonly = true)
  private MavenProject project;

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

  @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
  private RepositorySystemSession repoSession;

  @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true)
  private List<RemoteRepository> remoteRepositories;

  @Override public void execute() throws MojoExecutionException {
    Log log = getLog();

    if (mavenTestSkip || mavenSkipTests || skip) {
      log.debug("maven.test.skip = " + mavenTestSkip);
      log.debug("skipTests = " + mavenSkipTests);
      log.debug("skip = " + skip);
      log.info("Skipping Spoon execution.");
      return;
    }

    if (androidSdk == null) {
      throw new MojoExecutionException(
          "Could not find Android SDK. Ensure ANDROID_HOME environment variable is set.");
    }
    File sdkFile = new File(androidSdk);
    if (!sdkFile.exists()) {
      throw new MojoExecutionException(
          String.format("Could not find Android SDK at: %s", androidSdk));
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
        .setFailIfNoDeviceConnected(failIfNoDeviceConnected)
        .setSequential(sequential)
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
    return aetherArtifact(instrumentationArtifact).getFile();
  }

  private File getApplicationApk() throws MojoExecutionException {
    for (Artifact dependency : project.getDependencyArtifacts()) {
      if ("apk".equals(dependency.getType())) {
        return aetherArtifact(dependency).getFile();
      }
    }
    throw new MojoExecutionException(
        "Could not find application. Ensure 'apk' dependency on it exists.");
  }

  private org.eclipse.aether.artifact.Artifact aetherArtifact(Artifact dep)
      throws MojoExecutionException {
    return resolveArtifact(dep.getGroupId(), dep.getArtifactId(), "apk", dep.getVersion());
  }

  private String getSpoonClasspath() throws MojoExecutionException {
    Log log = getLog();

    String spoonVersion = findMyVersion();
    log.debug("[getSpoonClasspath] Plugin version: " + spoonVersion);

    org.eclipse.aether.artifact.Artifact spoonRunner =
        resolveArtifact(SPOON_GROUP_ID, SPOON_RUNNER_ARTIFACT_ID, "jar", spoonVersion);
    log.debug("[getSpoonClasspath] Runner artifact: " + spoonRunner);

    return createClasspath(spoonRunner);
  }

  private String findMyVersion() throws MojoExecutionException {
    for (Artifact artifact : project.getPluginArtifacts()) {
      if (SPOON_GROUP_ID.equals(artifact.getGroupId()) //
          && SPOON_PLUGIN_ARTIFACT_ID.equals(artifact.getArtifactId())) {
        return artifact.getVersion();
      }
    }
    throw new MojoExecutionException("Could not find reference to Spoon plugin artifact.");
  }

  private org.eclipse.aether.artifact.Artifact resolveArtifact(String groupId, String artifactId,
      String extension, String version) throws MojoExecutionException {
    ArtifactRequest request = new ArtifactRequest();
    request.setArtifact(new DefaultArtifact(groupId, artifactId, extension, version));
    request.setRepositories(remoteRepositories);

    try {
      ArtifactResult artifactResult = repositorySystem.resolveArtifact(repoSession, request);
      return artifactResult.getArtifact();
    } catch (ArtifactResolutionException e) {
      throw new MojoExecutionException("Unable to resolve runner from repository.", e);
    }
  }

  private String createClasspath(org.eclipse.aether.artifact.Artifact artifact)
      throws MojoExecutionException {
    // Request a collection of all dependencies for the artifact.
    DependencyRequest dependencyRequest = new DependencyRequest();
    CollectRequest collectRequest = new CollectRequest();
    collectRequest.setRoot(new Dependency(artifact, ""));
    for (RemoteRepository remoteRepository : remoteRepositories) {
      collectRequest.addRepository(remoteRepository);
    }
    CollectResult result;
    try {
      result = repositorySystem.collectDependencies(repoSession, collectRequest);
    } catch (DependencyCollectionException e) {
      throw new MojoExecutionException("Unable to resolve runner dependencies.", e);
    }

    final Log log = getLog();
    final StringBuilder builder = new StringBuilder();

    // Walk the tree of all dependencies to add to the classpath.
    result.getRoot().accept(new DependencyVisitor() {
      @Override public boolean visitEnter(DependencyNode node) {
        log.debug("Visiting: " + node);

        // Resolve the dependency node artifact into a real, local artifact.
        org.eclipse.aether.artifact.Artifact resolvedArtifact;
        try {
          org.eclipse.aether.artifact.Artifact nodeArtifact = node.getDependency().getArtifact();
          resolvedArtifact =
              resolveArtifact(nodeArtifact.getGroupId(), nodeArtifact.getArtifactId(), "jar",
                  nodeArtifact.getVersion());
        } catch (MojoExecutionException e) {
          throw new RuntimeException(e);
        }

        // Add the artifact's path to our classpath.
        if (builder.length() > 0) {
          builder.append(File.pathSeparator);
        }
        builder.append(resolvedArtifact.getFile().getAbsolutePath());

        return true;
      }

      @Override public boolean visitLeave(DependencyNode node) {
        return true;
      }
    });

    return builder.toString();
  }
}
