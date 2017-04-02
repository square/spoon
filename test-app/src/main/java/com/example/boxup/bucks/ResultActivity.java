package com.example.boxup.bucks;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public final class ResultActivity extends AppCompatActivity {
  private static final String KEY_SUCCESS = "success";
  private static final String KEY_AMOUNT = "amount";
  private static final String KEY_RECIPIENT = "recipient";

  public static Intent createSuccessIntent(Context context, long amount, String recipient) {
    Intent intent = new Intent(context, ResultActivity.class);
    intent.putExtra(KEY_SUCCESS, true);
    intent.putExtra(KEY_AMOUNT, amount);
    intent.putExtra(KEY_RECIPIENT, recipient);
    return intent;
  }

  public static Intent createFailureIntent(Context context) {
    Intent intent = new Intent(context, ResultActivity.class);
    intent.putExtra(KEY_SUCCESS, false);
    return intent;
  }

  @BindView(R.id.image) ImageView imageView;
  @BindView(R.id.message) TextView messageView;

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (!getIntent().hasExtra(KEY_SUCCESS)) {
      throw new IllegalStateException("Missing success extra");
    }
    boolean success = getIntent().getBooleanExtra(KEY_SUCCESS, true);

    setContentView(R.layout.result);
    ButterKnife.bind(this);

    if (success) {
      if (!getIntent().hasExtra(KEY_AMOUNT)) {
        throw new IllegalStateException("Missing amount extra");
      }
      if (!getIntent().hasExtra(KEY_RECIPIENT)) {
        throw new IllegalStateException("Missing recipient extra");
      }
      long amount = getIntent().getLongExtra(KEY_AMOUNT, -1);
      String recipient = getIntent().getStringExtra(KEY_RECIPIENT);

      imageView.setImageResource(R.drawable.ic_check_circle_white_24dp);
      messageView.setText(getString(R.string.send_success, Moneys.formatCents(amount), recipient));
    } else {
      imageView.setImageResource(R.drawable.ic_error_white_24dp);
      messageView.setText(R.string.send_error);
    }
  }

  @OnClick(R.id.action_ok) void okClicked() {
    finish();
  }
}
