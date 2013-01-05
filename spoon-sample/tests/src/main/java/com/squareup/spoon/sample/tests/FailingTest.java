package com.squareup.spoon.sample.tests;

import android.test.InstrumentationTestCase;

public class FailingTest extends InstrumentationTestCase {
  public void testAVeryLongNameBecauseIWantToSeeThePageWordWrapAndAlwaysBeFailingForFunAndProfit() {
    fail("Explicitly testing assertion failures!");
  }

  public void testAnotherLongNameBecauseItIsHumorousAndTestingThingsLikeThisIsImportant() {
    throw new RuntimeException("Explicitly testing unexpected exceptions!");
  }
}
