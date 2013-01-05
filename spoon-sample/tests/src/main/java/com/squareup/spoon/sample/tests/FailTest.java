package com.squareup.spoon.sample.tests;

import android.test.InstrumentationTestCase;

public class FailTest extends InstrumentationTestCase {
  public void testAVeryLongNameJustBecauseIWantToSeeThePageWordWrapAndAlwaysBeFailingForFunAnd() {
    fail("Explicitly testing Stack Traces!");
  }
}
