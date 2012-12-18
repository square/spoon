// Copyright 2012 Square, Inc.
package com.squareup.spoon;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;

/**
 * Goal which invokes Spoon. By default the output will be placed in a {@code spoon/} folder in your
 * project's build directory.
 *
 * @goal spoon
 */
@SuppressWarnings({ "JavaDoc", "UnusedDeclaration" }) // Non-standard Javadoc used by Maven.
public class SpoonMojo extends AbstractMojo {

  /**
   * The .apk file of the app to test.
   *
   * @parameter
   * @required
   */
  private File appApk;

  /**
   * The .apk file of the instrumentation runner.
   *
   * @parameter
   * @required
   */
  private File instrumentationApk;

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

  public void execute() throws MojoExecutionException {
    if (!new File(androidSdk).exists()) {
      getLog().error("Could not find Android SDK, make sure the ANDROID_HOME environment variable "
         + "is set.");
      return;
    }

    if (!appApk.exists()) {
      getLog().error("Could not find app APK file. Ensure " + appApk.getAbsolutePath()
          + " exists!");
      return;
    }

    if (!instrumentationApk.exists()) {
      getLog().error("Could not find instrumentation APK file. Ensure "
          + instrumentationApk.getAbsolutePath() + " exists!");
      return;
    }

    new ExecutionSuite(title, androidSdk, appApk, instrumentationApk, outputDirectory, debug).run();
  }
}
