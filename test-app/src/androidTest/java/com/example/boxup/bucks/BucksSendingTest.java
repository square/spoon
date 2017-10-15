package com.example.boxup.bucks;

import android.support.test.rule.ActivityTestRule;
import com.squareup.spoon.SpoonRule;
import org.junit.Rule;
import org.junit.Test;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

public final class BucksSendingTest {
  @Rule public final SpoonRule spoon = new SpoonRule();
  @Rule public final ActivityTestRule<AmountActivity> amountActivityRule =
      new ActivityTestRule<>(AmountActivity.class);

  @Test public void sendTenDollars() {
    spoon.screenshot(amountActivityRule.getActivity(), "amount_empty");

    onView(withText("1")).perform(click());
    onView(withText("0")).perform(click());
    spoon.screenshot(amountActivityRule.getActivity(), "amount_ten");

    onView(withText("Send")).perform(click());
    spoon.screenshot(amountActivityRule.getActivity(), "send_clicked");
  }

  @Test
  public void sendNothing() {
    spoon.screenshot(amountActivityRule.getActivity(), "amount_empty");

    onView(withText("Send")).perform(click());
    spoon.screenshot(amountActivityRule.getActivity(), "send_clicked");
  }
}
