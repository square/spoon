// Copyright 2013 Square, Inc.
package com.squareup.spoon.mojo;

import com.squareup.spoon.html.HtmlRenderer;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;

/** Goal which opens the Spoon output in the default browser. */
@SuppressWarnings("UnusedDeclaration") // Used reflectively by Maven.
@Mojo(name = "open", threadSafe = true)
public class OpenMojo extends AbstractSpoonMojo {
  @Override public void execute() throws MojoExecutionException, MojoFailureException {
    Log log = getLog();

    if (!Desktop.isDesktopSupported()) {
      log.error("Unable to open Spoon output web page: Desktop API not supported.");
      return;
    }

    File file = new File(outputDirectory, HtmlRenderer.INDEX_FILENAME);
    try {
      Desktop.getDesktop().browse(file.toURI());
    } catch (IOException e) {
      log.error("Unable to open Spoon output web page:", e);
    }
  }
}
