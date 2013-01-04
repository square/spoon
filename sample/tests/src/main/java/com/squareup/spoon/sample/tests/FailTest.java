package com.squareup.spoon.sample.tests;

import android.test.ActivityInstrumentationTestCase2;
import com.squareup.spoon.Screenshot;
import com.squareup.spoon.sample.LoginActivity;

public class FailTest extends ActivityInstrumentationTestCase2<LoginActivity> {
  public FailTest() {
    super(LoginActivity.class);
  }

  public void
  testThisIsAVeryLongNameJustBecauseIWantToSeeThePageWordWrapAndAlwaysBeFailingForFunAndProfit() {
    Screenshot.snap(getActivity(), "initial_state");
    fail("Explicitly testing Stack Traces!");
  }
}
