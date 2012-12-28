// Copyright 2012 Square, Inc.
package com.squareup.spoon;

import java.io.File;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

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

  public void execute() throws MojoExecutionException {
    Log log = getLog();

    if (mavenTestSkip || mavenSkipTests) {
      log.info("Skipping Spoon execution.");
      return;
    }

    Artifact self = null;
    for (Artifact artifact : project.getPluginArtifacts()) {
      if ("com.squareup".equals(artifact.getGroupId()) //
          && "maven-spoon-plugin".equals(artifact.getArtifactId())) {
        self = artifact;
        break;
      }
    }
    if (self == null) {
      throw new MojoExecutionException("Could not find representation of this plugin in project.");
    }

    boolean hasError = false;

    File sdkFile = new File(androidSdk);
    if (!sdkFile.exists()) {
      log.error("Could not find Android SDK. Ensure ANDROID_HOME environment variable is set.");
      hasError = true;
    }
    log.debug("Android SDK: " + sdkFile.getAbsolutePath());

    Artifact instrumentationArtifact = project.getArtifact();
    File instrumentation = instrumentationArtifact.getFile();
    if (!"apk".equals(instrumentationArtifact.getType())) {
      log.error("Spoon can only be invoked on a module with type 'apk'.");
      hasError = true;
    }
    log.debug("Instrumentation APK: " + instrumentation);

    File app = null;
    for (Artifact dependency : project.getDependencyArtifacts()) {
      if ("apk".equals(dependency.getType())) {
        if (app != null) {
          log.error("Multiple APK dependencies detected. Only one is supported.");
          break;
        }
        app = dependency.getFile();
        log.debug("Application APK: " + app.getAbsolutePath());
      }
    }
    if (app == null) {
      log.error("Could not find application. Ensure 'apk' dependency on it exists.");
      hasError = true;
    }

    if (hasError) {
      throw new MojoExecutionException("Unable to invoke Spoon. See console for details.");
    }

    String classpath = System.getProperty("java.class.path"); // TODO infer from artifact.
    File output = new File(outputDirectory, OUTPUT_DIRECTORY_NAME);

    log.debug("Output directory: " + output.getAbsolutePath());
    log.debug("Spoon title: " + title);
    log.debug("Debug: " + Boolean.toString(debug));
    log.debug("Classpath: " + classpath);

    new ExecutionSuite(title, androidSdk, app, instrumentation, output, debug, classpath).run();
  }
}
