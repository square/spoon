package com.squareup.spoon.sample.tests;

import android.app.Instrumentation;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.Button;
import android.widget.TextView;
import com.squareup.spoon.sample.R;
import com.squareup.spoon.sample.SampleActivity;

public class SampleActivityTest extends ActivityInstrumentationTestCase2<SampleActivity> {
  public SampleActivityTest() {
    super(SampleActivity.class);
  }

  private Instrumentation instrumentation;
  private SampleActivity activity;

  @Override protected void setUp() throws Exception {
    super.setUp();
    instrumentation = getInstrumentation();
    activity = getActivity();
  }

  public void testClickButton_displaysHello() throws InterruptedException {
    final Button button = (Button) activity.findViewById(R.id.click_me);
    TextView textView = (TextView) activity.findViewById(R.id.say_hello);

    // Check initial state.
    assertEquals("", textView.getText());
    assertEquals(activity.getString(R.string.click_me), button.getText());
    Thread.sleep(1000);

    // Click the button to change state.
    activity.runOnUiThread(new Runnable() {
      @Override public void run() {
        button.performClick();
      }
    });
    instrumentation.waitForIdleSync();
    Thread.sleep(1000);

    // Check new state.
    assertEquals(activity.getString(R.string.hello), textView.getText());
  }
}
