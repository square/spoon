package com.example.boxup.bucks;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public final class AmountActivity extends AppCompatActivity {
  private static final String KEY_AMOUNT = "amount";

  private Vibrator vibrator;
  private String amount = "";

  @BindView(R.id.amount) TextView amountView;
  @BindView(R.id.action_send) View sendView;

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.amount);
    ButterKnife.bind(this);
    vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
    if (savedInstanceState != null && savedInstanceState.containsKey(KEY_AMOUNT)) {
      amount = savedInstanceState.getString(KEY_AMOUNT);
    }
    renderAmount();
  }

  @Override protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putString(KEY_AMOUNT, amount);
  }

  @OnClick({
      R.id.keypad_1, R.id.keypad_2, R.id.keypad_3, R.id.keypad_4, R.id.keypad_5,
      R.id.keypad_6, R.id.keypad_7, R.id.keypad_8, R.id.keypad_9, R.id.keypad_0
  })
  void numberClicked(TextView number) {
    int dot = amount.indexOf('.');
    if (dot == -1 || dot >= amount.length() - 2) {
      amount += number.getText();
      renderAmount();
    } else {
      bbzzzzzztt();
    }
  }

  @OnClick(R.id.keypad_dot) void onDotClicked() {
    if (parsedAmount() < 1d || amount.indexOf('.') != -1) {
      bbzzzzzztt();
    } else {
      amount += '.';
      renderAmount();
    }
  }

  @OnClick(R.id.keypad_back) void onKeypadClicked() {
    if (amount.isEmpty()) {
      bbzzzzzztt();
    } else {
      amount = amount.substring(0, amount.length() - 1);
      renderAmount();
    }
  }

  @OnClick(R.id.action_send) void actionSend() {
    DialogFragment newFragment = new ConfirmDialog();
    newFragment.show(getSupportFragmentManager(), "dialog");
  }

  private void sendAfterConfirm() {
    int amountCents = (int) (parsedAmount() * 100);
    startActivity(RecipientActivity.createIntent(AmountActivity.this, amountCents));

    amount = "";
    renderAmount();
  }

  private void bbzzzzzztt() {
    vibrator.vibrate(new long[] { 10, 10, 10 }, -1);
  }

  private void renderAmount() {
    double amountDouble = parsedAmount();
    String rendered = Moneys.formatDollars(amountDouble);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      TransitionManager.beginDelayedTransition((ViewGroup) amountView.getParent());
    }
    amountView.setText(rendered);
    sendView.setEnabled(amountDouble > 1d);
  }

  private double parsedAmount() {
    return amount.isEmpty() ? 0d : Double.parseDouble(amount);
  }

  public static class ConfirmDialog extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

      return new AlertDialog.Builder(getActivity())
        .setTitle(R.string.confirm_title)
        .setPositiveButton(R.string.ok,
          new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
              ((AmountActivity) getActivity()).sendAfterConfirm();
              dismiss();
            }
          }
        )
        .setNegativeButton(R.string.cancel,
          new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
              dismiss();
            }
          }
        )
        .create();
    }
  }
}
