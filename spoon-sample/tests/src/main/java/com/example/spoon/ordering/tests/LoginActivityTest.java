package com.example.spoon.ordering.tests;

import android.app.Instrumentation;
import android.content.IntentFilter;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.Button;
import android.widget.EditText;
import com.squareup.spoon.Spoon;
import com.example.spoon.ordering.LoginActivity;
import com.example.spoon.ordering.R;
import java.util.Random;

import static android.app.Instrumentation.ActivityMonitor;

public class LoginActivityTest extends ActivityInstrumentationTestCase2<LoginActivity> {
  public LoginActivityTest() {
    super(LoginActivity.class);
  }

  private Instrumentation instrumentation;
  private LoginActivity activity;

  private EditText username;
  private EditText password;
  private Button login;

  @Override protected void setUp() throws Exception {
    super.setUp();
    instrumentation = getInstrumentation();
    activity = getActivity();

    username = (EditText) activity.findViewById(R.id.username);
    password = (EditText) activity.findViewById(R.id.password);
    login = (Button) activity.findViewById(R.id.login);
  }

  public void testEmptyForm_ShowsBothErrors() {
    Spoon.screenshot(activity, "initial_state");

    // Make sure the initial state does not show any errors.
    assertNull(username.getError());
    assertNull(password.getError());

    // Click the "login" button.
    instrumentation.runOnMainSync(new Runnable() {
      @Override public void run() {
        login.performClick();
      }
    });
    instrumentation.waitForIdleSync();

    Spoon.screenshot(activity, "login_clicked");

    // Verify errors were shown for both input fields.
    String required = activity.getString(R.string.required);
    assertEquals(required, username.getError().toString());
    assertEquals(required, password.getError().toString());
  }

  public void testBlankPassword_ShowsError() {
    Spoon.screenshot(activity, "initial_state");

    // Make sure the initial state does not show any errors.
    assertNull(username.getError());
    assertNull(password.getError());

    // Type a value into the username field.
    instrumentation.runOnMainSync(new Runnable() {
      @Override public void run() {
        username.setText("jake");
      }
    });
    instrumentation.waitForIdleSync();
    Spoon.screenshot(activity, "username_entered");

    // Click the "login" button.
    instrumentation.runOnMainSync(new Runnable() {
      @Override public void run() {
        login.performClick();
      }
    });
    Spoon.screenshot(activity, "login_clicked");

    // Verify error was shown only for password field.
    assertNull(username.getError());
    assertEquals(activity.getString(R.string.required), password.getError().toString());
  }

  public void testBlankUsername_ShowsError() {
    Spoon.screenshot(activity, "initial_state");

    // Make sure the initial state does not show any errors.
    assertNull(username.getError());
    assertNull(password.getError());

    // Type a value into the password field.
    instrumentation.runOnMainSync(new Runnable() {
      @Override public void run() {
        password.setText("secretpassword");
      }
    });
    Spoon.screenshot(activity, "password_entered");

    // Click the "login" button.
    instrumentation.runOnMainSync(new Runnable() {
      @Override public void run() {
        login.performClick();
      }
    });
    instrumentation.waitForIdleSync();
    Spoon.screenshot(activity, "login_clicked");

    // Verify error was shown only for username field.
    assertEquals(activity.getString(R.string.required), username.getError().toString());
    assertNull(password.getError());
  }

  public void testPasswordTooShort_ShowsError() {
    Spoon.screenshot(activity, "initial_state");

    // Make sure the initial state does not show any errors.
    assertNull(username.getError());
    assertNull(password.getError());

    // Type a value into the username and password field.
    instrumentation.runOnMainSync(new Runnable() {
      @Override public void run() {
        username.setText("jake");
        password.setText("secret");
      }
    });
    Spoon.screenshot(activity, "values_entered");

    // Click the "login" button.
    instrumentation.runOnMainSync(new Runnable() {
      @Override public void run() {
        login.performClick();
      }
    });
    instrumentation.waitForIdleSync();
    Spoon.screenshot(activity, "login_clicked");

    // Verify error was shown only for username field.
    assertNull(username.getError());
    assertEquals(activity.getString(R.string.password_length), password.getError().toString());
  }

  public void testValidValues_StartsNewActivity() {
    IntentFilter filter = new IntentFilter();
    ActivityMonitor monitor = instrumentation.addMonitor(filter, null, false);

    Spoon.screenshot(activity, "initial_state");

    // Make sure the initial state does not show any errors.
    assertNull(username.getError());
    assertNull(password.getError());

    // Type a value into the username and password field.
    instrumentation.runOnMainSync(new Runnable() {
      @Override public void run() {
        username.setText("jake");
        password.setText("secretpassword");
      }
    });
    Spoon.screenshot(activity, "values_entered");

    // Click the "login" button.
    instrumentation.runOnMainSync(new Runnable() {
      @Override public void run() {
        login.performClick();
      }
    });
    instrumentation.waitForIdleSync();

    // Verify new activity was shown.
    assertEquals(1, monitor.getHits());
    Spoon.screenshot(monitor.getLastActivity(), "next_activity_shown");

    // For fun (and to make the output more interesting), randomly fail!
    assertTrue(new Random().nextInt(4) != 0);
  }
}
