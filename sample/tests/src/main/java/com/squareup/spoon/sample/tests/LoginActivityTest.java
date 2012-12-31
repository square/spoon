package com.squareup.spoon.sample.tests;

import android.app.Instrumentation;
import android.content.IntentFilter;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.Button;
import android.widget.EditText;
import com.squareup.spoon.Screenshot;
import com.squareup.spoon.sample.LoginActivity;
import com.squareup.spoon.sample.R;

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

  public void testEmptyFormShowsBothErrors() {
    Screenshot.snap(activity, "initial_state");

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

    Screenshot.snap(activity, "login_clicked");

    // Verify errors were shown for both input fields.
    String required = activity.getString(R.string.required);
    assertEquals(required, username.getError().toString());
    assertEquals(required, password.getError().toString());
  }

  public void testBlankPasswordShowsError() {
    Screenshot.snap(activity, "initial_state");

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
    Screenshot.snap(activity, "username_entered");

    // Click the "login" button.
    instrumentation.runOnMainSync(new Runnable() {
      @Override public void run() {
        login.performClick();
      }
    });
    Screenshot.snap(activity, "login_clicked");

    // Verify error was shown only for password field.
    assertNull(username.getError());
    assertEquals(activity.getString(R.string.required), password.getError().toString());
  }

  public void testBlankUsernameShowsError() {
    Screenshot.snap(activity, "initial_state");

    // Make sure the initial state does not show any errors.
    assertNull(username.getError());
    assertNull(password.getError());

    // Type a value into the password field.
    instrumentation.runOnMainSync(new Runnable() {
      @Override public void run() {
        password.setText("secretpassword");
      }
    });
    Screenshot.snap(activity, "password_entered");

    // Click the "login" button.
    instrumentation.runOnMainSync(new Runnable() {
      @Override public void run() {
        login.performClick();
      }
    });
    instrumentation.waitForIdleSync();
    Screenshot.snap(activity, "login_clicked");

    // Verify error was shown only for username field.
    assertEquals(activity.getString(R.string.required), username.getError().toString());
    assertNull(password.getError());
  }

  public void testPasswordTooShortShowsError() {
    Screenshot.snap(activity, "initial_state");

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
    Screenshot.snap(activity, "values_entered");

    // Click the "login" button.
    instrumentation.runOnMainSync(new Runnable() {
      @Override public void run() {
        login.performClick();
      }
    });
    instrumentation.waitForIdleSync();
    Screenshot.snap(activity, "login_clicked");

    // Verify error was shown only for username field.
    assertNull(username.getError());
    assertEquals(activity.getString(R.string.password_length), password.getError().toString());
  }

  public void testValidValuesStartsNewActivity() {
    IntentFilter filter = new IntentFilter();
    ActivityMonitor monitor = instrumentation.addMonitor(filter, null, false);

    Screenshot.snap(activity, "initial_state");

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
    Screenshot.snap(activity, "values_entered");

    // Click the "login" button.
    instrumentation.runOnMainSync(new Runnable() {
      @Override public void run() {
        login.performClick();
      }
    });
    instrumentation.waitForIdleSync();

    // Verify error was shown only for username field.
    assertEquals(1, monitor.getHits());

    Screenshot.snap(monitor.getLastActivity(), "next_activity_shown");
  }
}
