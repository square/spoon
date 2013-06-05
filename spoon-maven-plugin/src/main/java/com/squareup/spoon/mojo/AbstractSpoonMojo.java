package com.squareup.spoon.mojo;

import java.io.File;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;

abstract class AbstractSpoonMojo extends AbstractMojo {
  /** Location of the output directory. */
  @Parameter(defaultValue = "${project.build.directory}/spoon-output")
  protected File outputDirectory;
}
