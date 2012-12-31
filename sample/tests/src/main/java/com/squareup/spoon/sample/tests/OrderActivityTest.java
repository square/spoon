package com.squareup.spoon.sample.tests;

import android.app.Instrumentation;
import android.test.ActivityInstrumentationTestCase2;
import com.squareup.spoon.Screenshot;
import com.squareup.spoon.sample.OrderActivity;
import java.util.Random;

public class OrderActivityTest extends ActivityInstrumentationTestCase2<OrderActivity> {
  public OrderActivityTest() {
    super(OrderActivity.class);
  }

  private Instrumentation instrumentation;
  private OrderActivity activity;

  @Override protected void setUp() throws Exception {
    super.setUp();
    instrumentation = getInstrumentation();
    activity = getActivity();
  }

  // TODO

  public void testAlwaysFailingForFun() {
    Screenshot.snap(activity, "initial_state");
    fail("Whoops!");
  }

  private static final Random RANDOM = new Random();
  private static void randomlyFail() {
    assertTrue(RANDOM.nextInt(3) > 0);
  }
}
