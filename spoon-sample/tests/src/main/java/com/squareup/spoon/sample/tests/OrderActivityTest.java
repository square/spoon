package com.squareup.spoon.sample.tests;

import android.app.Instrumentation;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.Button;
import android.widget.CheckedTextView;
import com.jayway.android.robotium.solo.Solo;
import com.squareup.spoon.Screenshot;
import com.squareup.spoon.sample.OrderActivity;

public class OrderActivityTest extends ActivityInstrumentationTestCase2<OrderActivity> {
  public OrderActivityTest() {
    super(OrderActivity.class);
  }

  private Instrumentation instrumentation;
  private OrderActivity activity;
  private Solo solo;

  @Override protected void setUp() throws Exception {
    super.setUp();
    instrumentation = getInstrumentation();
    activity = getActivity();
    solo = new Solo(instrumentation, activity);
  }

  public void testMakeASandwich_ItTastesGood() {
    Button nextButton = (Button) solo.getText("Next");

    Screenshot.snap(activity, "initial_state");

    assertFalse(nextButton.isEnabled());
    solo.clickOnText("Sandwich");
    Screenshot.snap(activity, "selected_sandwich");
    assertTrue(nextButton.isEnabled());

    solo.clickOnView(nextButton);
    Screenshot.snap(activity, "bread");

    assertFalse(nextButton.isEnabled());
    solo.clickOnText("Wheat");
    Screenshot.snap(activity, "selected_bread");
    assertTrue(nextButton.isEnabled());

    solo.clickOnView(nextButton);
    Screenshot.snap(activity, "meats");

    assertTrue(nextButton.isEnabled());
    solo.clickOnText("Turkey");
    solo.clickOnText("Roast Beef");
    Screenshot.snap(activity, "selected_meats");

    solo.clickOnView(nextButton);
    Screenshot.snap(activity, "veggies");

    assertTrue(nextButton.isEnabled());
    solo.clickOnText("Lettuce");
    solo.clickOnText("Tomato");
    solo.clickOnText("Cucumbers");
    Screenshot.snap(activity, "selected_veggies");

    solo.clickOnView(nextButton);
    Screenshot.snap(activity, "cheeses");

    assertTrue(nextButton.isEnabled());
    solo.clickOnText("American");
    Screenshot.snap(activity, "selected_cheeses");

    solo.clickOnView(nextButton);
    Screenshot.snap(activity, "toasted");

    CheckedTextView toastedNo = (CheckedTextView) solo.getText("No");
    assertTrue("No selected by default", toastedNo.isChecked());
    assertTrue(nextButton.isEnabled());

    solo.clickOnView(nextButton);
    Screenshot.snap(activity, "your_info");

    assertFalse(nextButton.isEnabled());
    solo.typeText(0, "Trent Sondag");
    solo.typeText(1, "bearfight@example.com");
    Screenshot.snap(activity, "completed_your_info");
    assertTrue(nextButton.isEnabled());

    solo.clickOnView(nextButton);
    Screenshot.snap(activity, "review");
  }

  public void testMakeSomeSalad_ItIsHealthy() {
    Button nextButton = (Button) solo.getText("Next");

    Screenshot.snap(activity, "initial_state");

    assertFalse(nextButton.isEnabled());
    solo.clickOnText("Salad");
    Screenshot.snap(activity, "selected_salad");
    assertTrue(nextButton.isEnabled());

    solo.clickOnView(nextButton);
    Screenshot.snap(activity, "salad_type");

    assertFalse(nextButton.isEnabled());
    solo.clickOnText("Caesar");
    Screenshot.snap(activity, "selected_salad_type");
    assertTrue(nextButton.isEnabled());

    solo.clickOnView(nextButton);
    Screenshot.snap(activity, "dressing");

    CheckedTextView dressingNone = (CheckedTextView) solo.getText("No dressing");
    assertTrue(dressingNone.isChecked());
    assertTrue(nextButton.isEnabled());

    solo.clickOnView(nextButton);
    Screenshot.snap(activity, "your_info");

    assertFalse(nextButton.isEnabled());
    solo.typeText(0, "Trent Sondag");
    solo.typeText(1, "bearfight@example.com");
    Screenshot.snap(activity, "completed_your_info");
    assertTrue(nextButton.isEnabled());

    solo.clickOnView(nextButton);
    Screenshot.snap(activity, "review");
  }
}
