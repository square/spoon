package com.squareup.spoon.sample.tests;

import android.app.Instrumentation;
import android.test.ActivityInstrumentationTestCase2;
import com.squareup.spoon.sample.OrderActivity;

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
}
