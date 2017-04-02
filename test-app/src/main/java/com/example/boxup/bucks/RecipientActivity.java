package com.example.boxup.bucks;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.EditText;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnTextChanged;

public final class RecipientActivity extends AppCompatActivity {
  @VisibleForTesting static boolean SENDING_FAIL = false;

  private static final String KEY_AMOUNT_CENTS = "amount";

  public static Intent createIntent(Context context, long amount) {
    Intent intent = new Intent(context, RecipientActivity.class);
    intent.putExtra(KEY_AMOUNT_CENTS, amount);
    return intent;
  }

  @BindView(R.id.toolbar) Toolbar toolbar;
  @BindView(R.id.recipient) EditText recipientView;

  private long amount;
  private MenuItem sendMenuItem;

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (!getIntent().hasExtra(KEY_AMOUNT_CENTS)) {
      throw new IllegalStateException("Missing amount extra");
    }
    amount = getIntent().getLongExtra(KEY_AMOUNT_CENTS, -1);
    if (amount < 100) {
      throw new IllegalArgumentException("Invalid amount: " + amount);
    }

    setContentView(R.layout.recipeint);
    ButterKnife.bind(this);

    toolbar.setTitle(getString(R.string.send_amount, Moneys.formatCents(amount)));
    toolbar.inflateMenu(R.menu.recipient);
    sendMenuItem = toolbar.getMenu().findItem(R.id.recipient_send);
    sendMenuItem.setEnabled(false);
    toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
      @Override public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
          case R.id.recipient_send:
            onSendClicked();
            return true;
          default:
            throw new IllegalStateException("Unknown menu item: " + item);
        }
      }
    });
  }

  @OnTextChanged(R.id.recipient) void toChanged(CharSequence text) {
    sendMenuItem.setEnabled(text.length() > 0);
  }

  private void onSendClicked() {
    if (SENDING_FAIL) {
      startActivity(ResultActivity.createFailureIntent(this));
    } else {
      String recipient = recipientView.getText().toString();
      startActivity(ResultActivity.createSuccessIntent(this, amount, recipient));
    }
    finish();
  }
}
