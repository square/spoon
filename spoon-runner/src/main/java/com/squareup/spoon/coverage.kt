@file:JvmName("SpoonCoverageMerger")
package com.squareup.spoon

import com.squareup.spoon.SpoonDeviceRunner.COVERAGE_DIR
import com.squareup.spoon.SpoonDeviceRunner.COVERAGE_FILE
import org.jacoco.core.tools.ExecFileLoader
import java.io.File
import java.io.IOException

@Throws(IOException::class)
internal fun mergeCoverageFiles(serials: Set<String>, outputDirectory: File) {
  val execFileLoader = ExecFileLoader()
  serials.map(SpoonUtils::sanitizeSerial).forEach { serial ->
    execFileLoader.load(File(outputDirectory, "$COVERAGE_DIR/$serial/$COVERAGE_FILE"))
  }
  execFileLoader.save(File(outputDirectory, "$COVERAGE_DIR/merged-coverage.ec"), false)
}

/**
 * Merges all coverage files inside a folder into a single coverage file
 */
@Throws(IOException::class)
internal fun mergeAllCoverageFiles(covReportsFolder: File) {
  val covFiles = covReportsFolder.listFiles()
  if (covFiles == null || covFiles.isEmpty())
    throw RuntimeException("No coverage file in path")
  SpoonLogger.logInfo("Merging code coverage files in folder %s", covReportsFolder.absolutePath);
  val execFileLoader = ExecFileLoader()
  covFiles.forEach { covReport ->
      execFileLoader.load(covReport)
  }

  val mergedCovFile = File(covReportsFolder, COVERAGE_FILE)
  if (!mergedCovFile.exists())
    mergedCovFile.createNewFile()
  execFileLoader.save(mergedCovFile, false)
  SpoonLogger.logInfo("Merged code coverage file saved in %s", mergedCovFile.absolutePath);
}
