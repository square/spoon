package com.example.spoon.ordering.tests;

import android.test.ActivityInstrumentationTestCase2;
import android.widget.Button;
import android.widget.CheckedTextView;
import com.robotium.solo.Solo;
import com.squareup.spoon.Spoon;
import com.example.spoon.ordering.OrderActivity;

import static org.fest.assertions.api.ANDROID.assertThat;

public class OrderActivityTest extends ActivityInstrumentationTestCase2<OrderActivity> {
  public OrderActivityTest() {
    super(OrderActivity.class);
  }

  private OrderActivity activity;
  private Solo solo;

  @Override protected void setUp() throws Exception {
    super.setUp();
    activity = getActivity();
    solo = new Solo(getInstrumentation(), activity);
  }

  public void testMakeASandwich_ItTastesGood() {
    Button nextButton = (Button) solo.getText("Next");

    Spoon.screenshot(activity, "initial_state");

    assertThat(nextButton).isDisabled();
    solo.clickOnText("Sandwich");
    Spoon.screenshot(activity, "selected_sandwich");
    assertThat(nextButton).isEnabled();

    solo.clickOnView(nextButton);
    Spoon.screenshot(activity, "bread");

    assertThat(nextButton).isDisabled();
    solo.clickOnText("Wheat");
    Spoon.screenshot(activity, "selected_bread");
    assertThat(nextButton).isEnabled();

    solo.clickOnView(nextButton);
    Spoon.screenshot(activity, "meats");

    assertThat(nextButton).isEnabled();
    solo.clickOnText("Turkey");
    solo.clickOnText("Roast Beef");
    Spoon.screenshot(activity, "selected_meats");

    solo.clickOnView(nextButton);
    Spoon.screenshot(activity, "veggies");

    assertThat(nextButton).isEnabled();
    solo.clickOnText("Lettuce");
    solo.clickOnText("Tomato");
    solo.clickOnText("Cucumbers");
    Spoon.screenshot(activity, "selected_veggies");

    solo.clickOnView(nextButton);
    Spoon.screenshot(activity, "cheeses");

    assertThat(nextButton).isEnabled();
    solo.clickOnText("American");
    Spoon.screenshot(activity, "selected_cheeses");

    solo.clickOnView(nextButton);
    Spoon.screenshot(activity, "toasted");

    CheckedTextView toastedNo = (CheckedTextView) solo.getText("No");
    assertThat(toastedNo).isChecked();
    assertThat(nextButton).isEnabled();

    solo.clickOnView(nextButton);
    Spoon.screenshot(activity, "your_info");

    assertThat(nextButton).isDisabled();
    solo.typeText(0, "Trent Sondag");
    solo.typeText(1, "bearfight@example.com");
    Spoon.screenshot(activity, "completed_your_info");
    assertThat(nextButton).isEnabled();

    solo.clickOnView(nextButton);
    Spoon.screenshot(activity, "review");
  }

  public void testMakeSomeSalad_ItIsHealthy() {
    Button nextButton = (Button) solo.getText("Next");

    Spoon.screenshot(activity, "initial_state");

    assertThat(nextButton).isDisabled();
    solo.clickOnText("Salad");
    Spoon.screenshot(activity, "selected_salad");
    assertThat(nextButton).isEnabled();

    solo.clickOnView(nextButton);
    Spoon.screenshot(activity, "salad_type");

    assertThat(nextButton).isDisabled();
    solo.clickOnText("Caesar");
    Spoon.screenshot(activity, "selected_salad_type");
    assertThat(nextButton).isEnabled();

    solo.clickOnView(nextButton);
    Spoon.screenshot(activity, "dressing");

    CheckedTextView dressingNone = (CheckedTextView) solo.getText("No dressing");
    assertThat(dressingNone).isChecked();
    assertThat(nextButton).isEnabled();

    solo.clickOnView(nextButton);
    Spoon.screenshot(activity, "your_info");

    assertThat(nextButton).isDisabled();
    solo.typeText(0, "Trent Sondag");
    solo.typeText(1, "bearfight@example.com");
    Spoon.screenshot(activity, "completed_your_info");
    assertThat(nextButton).isEnabled();

    solo.clickOnView(nextButton);
    Spoon.screenshot(activity, "review");
  }
}
