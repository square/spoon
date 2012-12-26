// Copyright 2012 Square, Inc.
package com.squareup.spoon;

import java.io.File;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

/**
 * Goal which invokes Spoon. By default the output will be placed in a {@code spoon-output/} folder
 * in your project's build directory.
 *
 * @goal run
 * @phase integration-test
 */
@SuppressWarnings({ "JavaDoc", "UnusedDeclaration" }) // Non-standard Javadoc used by Maven.
public class SpoonMojo extends AbstractMojo {
  /**
   * -Dmaven.test.skip is commonly used with Maven to skip tests. We honor it too.
   *
   * @parameter expression="${maven.test.skip}" default-value=false
   * @readonly
   */
  private boolean mavenTestSkip;

  /**
   * -DskipTests is commonly used with Maven to skip tests. We honor it too.
   *
   * @parameter expression="${skipTests}" default-value=false
   * @readonly
   */
  private boolean mavenSkipTests;

  /**
   * Location of the file.
   *
   * @parameter expression="${project.build.directory}/spoon-output/"
   * @required
   */
  private File outputDirectory;

  /**
   * A title for the output website.
   *
   * @parameter default-value="Spoon Test Run"
   */
  private String title;

  /**
   * The location of the Android SDK.
   *
   * @parameter expression="${env.ANDROID_HOME}"
   * @required
   */
  private String androidSdk;

  /**
   * The location of the Android SDK.
   *
   * @parameter
   */
  private boolean debug;

  /**
   * Maven project.
   *
   * @parameter expression="${project}"
   * @required
   * @readonly
   */
  private MavenProject project;

  public void execute() throws MojoExecutionException {
    Log log = getLog();

    if (mavenTestSkip || mavenSkipTests) {
      log.info("Skipping Spoon execution.");
      return;
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

    log.debug("Output directory: " + outputDirectory.getAbsolutePath());
    log.debug("Spoon title: " + title);
    log.debug("Debug: " + Boolean.toString(debug));

    new ExecutionSuite(title, androidSdk, app, instrumentation, outputDirectory, debug).run();
  }
}
