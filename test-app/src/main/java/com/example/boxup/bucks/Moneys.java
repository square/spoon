package com.example.boxup.bucks;

import android.support.annotation.MainThread;
import java.text.DecimalFormat;

final class Moneys {
  private static final DecimalFormat DOLLARS_ONLY = new DecimalFormat("$#,###");
  private static final DecimalFormat DOLLARS_AND_CENTS = new DecimalFormat("$#,###.00");

  @MainThread // Not thread-safe.
  static String formatDollars(double dollars) {
    return dollars % 1 == 0d
        ? DOLLARS_ONLY.format(dollars)
        : DOLLARS_AND_CENTS.format(dollars);
  }

  @MainThread // Not thread-safe.
  static String formatCents(long cents) {
    return cents % 100 == 0
        ? DOLLARS_ONLY.format(cents / 100)
        : DOLLARS_AND_CENTS.format(cents / 100d);
  }

  private Moneys() {
  }
}
