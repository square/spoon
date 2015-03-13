package com.example.spoon.ordering;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;
import static android.view.View.OnClickListener;

public class LoginActivity extends Activity {
  private static final String TAG = "LoginActivity";

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_login);

    TextView title = (TextView) findViewById(android.R.id.title);
    title.setTypeface(Typeface.createFromAsset(getAssets(), "Roboto-Thin.ttf"));

    final EditText username = (EditText) findViewById(R.id.username);
    final EditText password = (EditText) findViewById(R.id.password);

    findViewById(R.id.login).setOnClickListener(new OnClickListener() {
      @Override public void onClick(View v) {
        boolean hasError = false;

        if (TextUtils.isEmpty(username.getText())) {
          Log.e(TAG, "Username was blank.");
          username.setError(getString(R.string.required));
          hasError = true;
        } else {
          username.setError(null);
        }

        Editable pass = password.getText();
        if (TextUtils.isEmpty(pass)) {
          Log.e(TAG, "Password was blank.");
          password.setError(getString(R.string.required));
          hasError = true;
        } else if (pass.length() < 8) {
          Log.e(TAG, "Password too short.");
          password.setError(getString(R.string.password_length));
          hasError = true;
        } else {
          password.setError(null);
        }

        if (!hasError) {
          Intent intent = new Intent(LoginActivity.this, OrderActivity.class);
          intent.setFlags(FLAG_ACTIVITY_CLEAR_TOP);
          startActivity(intent);
          finish();
        }
      }
    });
  }
}
