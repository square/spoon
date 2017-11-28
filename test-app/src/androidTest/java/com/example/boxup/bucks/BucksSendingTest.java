package com.example.boxup.bucks;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.uiautomator.UiDevice;

import com.squareup.spoon.SpoonRule;

import org.junit.Rule;
import org.junit.Test;

import java.io.File;

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
    File fullscreenshot = takeFullScreenshot();
    spoon.screenshot(InstrumentationRegistry.getTargetContext(), "full_device_screenshot",fullscreenshot);
    fullscreenshot.delete();
  }

  private File takeFullScreenshot() {
    Context context = InstrumentationRegistry.getTargetContext().getApplicationContext();
    File file = getFile(System.currentTimeMillis() + "_full_screenshot.png",
            context.getPackageName() + "_automator", context);
    if (UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).takeScreenshot(file)) {
      return file;
    } else {
      return null;
    }
  }

  private File getFile(String filename,String dirname,Context context) {
    File file = null;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      // Use external storage.
      file = new File(new File(Environment.getExternalStorageDirectory(), dirname), filename);
    } else {
      // Use internal storage.
      file = new File(context.getDir(dirname, Context.MODE_WORLD_READABLE), filename);
    }

    if (!file.getParentFile().exists()) {
      file.getParentFile().mkdirs();
    }
    return file;
  }

  @Test
  public void sendNothing() {
    spoon.screenshot(amountActivityRule.getActivity(), "amount_empty");

    onView(withText("Send")).perform(click());
    spoon.screenshot(amountActivityRule.getActivity(), "send_clicked");
  }
}
