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
