package com.squareup.spoon;

import com.google.common.collect.ImmutableSet;
import org.hamcrest.CustomTypeSafeMatcher;
import org.jacoco.core.tools.ExecFileLoader;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class SpoonCoverageMergerTest {

  @Test
  public void shouldMergeCoverageFiles() throws Exception {
    ExecFileLoader execFileLoader = mock(ExecFileLoader.class);
    Set<String> serials = ImmutableSet.of("10.0.0.1:1234", "10.0.0.2:1234");
    File spoonOutputDirectory = new File("/output");

    SpoonCoverageMerger spoonCoverageMerger = new SpoonCoverageMerger(execFileLoader);

    spoonCoverageMerger.mergeCoverageFiles(serials, spoonOutputDirectory);

    ArgumentCaptor<File> captor = ArgumentCaptor.forClass(File.class);
    verify(execFileLoader, times(2)).load(captor.capture());
    assertThat(captor.getAllValues().get(0), coverageFileOf("10.0.0.1:1234"));
    assertThat(captor.getAllValues().get(1), coverageFileOf("10.0.0.2:1234"));

    verify(execFileLoader, times(1)).save(argThat(hasPath("/output/coverage/merged-coverage.ec")), eq(false));
  }

  private CustomTypeSafeMatcher<File> hasPath(final String path) {
    return new CustomTypeSafeMatcher<File>("") {
      @Override
      protected boolean matchesSafely(File file) {
        return file.getPath().equals(path);
      }
    };
  }

  private CustomTypeSafeMatcher<File> coverageFileOf(final String serial) {
    return new CustomTypeSafeMatcher<File>("") {
      @Override
      protected boolean matchesSafely(File file) {
        assertThat(file.getPath(), is("/output/coverage/" + SpoonUtils.sanitizeSerial(serial) + "/coverage.ec"));
        return true;
      }
    };
  }
}