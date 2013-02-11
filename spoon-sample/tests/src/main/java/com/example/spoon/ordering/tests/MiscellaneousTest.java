package com.example.spoon.ordering.tests;

import android.test.InstrumentationTestCase;

public class MiscellaneousTest extends InstrumentationTestCase {
  public void testAVeryLongNameBecauseIWantToSeeThePageWordWrapAndAlwaysBeFailingForFunAndProfit() {
    fail("Explicitly testing assertion failures!");
  }

  public void testAnotherLongNameBecauseItIsHumorousAndTestingThingsLikeThisIsImportant() {
    try {
      throw new RuntimeException("Messages can have...\n...newlines.");
    } catch (RuntimeException e) {
      throw new IllegalStateException("Explicitly testing unexpected, nested exceptions.", e);
    }
  }
}
