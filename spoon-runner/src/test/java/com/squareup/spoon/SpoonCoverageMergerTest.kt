package com.squareup.spoon

import com.squareup.spoon.SpoonUtils.sanitizeSerial
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.lang.String.format

class SpoonCoverageMergerTest {
  @JvmField @Rule val testFolder = TemporaryFolder()

  @Test fun shouldMergeCoverageFiles() {
    val outputDirectory = testFolder.newFolder("output")
    val serialId1 = "10.0.0.1:1234"
    val serialId2 = "10.0.0.2:1234"
    createTemporaryCoverageFiles(serialId1, serialId2)

    mergeCoverageFiles(setOf(serialId1, serialId2), outputDirectory)
    val mergedCoverageFile = File(outputDirectory, "/coverage/merged-coverage.ec")
    assertTrue(mergedCoverageFile.exists())
  }

  @Test fun shouldMergeMultiInstrumentedCoverageFiles() {
    val outputDirectory = testFolder.newFolder("coverage")
    val test1 = "class1_test1"
    val test2 = "class1_test2"
    val test3 = "class2_test1"
    createTemporaryMultiTestsCoverageFiles(test1, test2, test3)

    mergeAllCoverageFiles(outputDirectory)
    val mergedCoverageFile = File(outputDirectory, "coverage.ec")
    assertTrue(mergedCoverageFile.exists())
  }

  private fun createTemporaryCoverageFiles(serialId1: String, serialId2: String) {
    testFolder.newFolder("output", "coverage", sanitizeSerial(serialId1))
    testFolder.newFolder("output", "coverage", sanitizeSerial(serialId2))
    testFolder.newFile(format("output/coverage/%s/coverage.ec", sanitizeSerial(serialId1)))
    testFolder.newFile(format("output/coverage/%s/coverage.ec", sanitizeSerial(serialId2)))
  }

  private fun createTemporaryMultiTestsCoverageFiles(test1: String, test2: String, test3: String) {
    testFolder.newFile(format("coverage/%s-coverage.ec", test1))
    testFolder.newFile(format("coverage/%s-coverage.ec", test2))
    testFolder.newFile(format("coverage/%s-coverage.ec", test3))
  }
}
