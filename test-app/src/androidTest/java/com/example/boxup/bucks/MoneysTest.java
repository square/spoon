package com.example.boxup.bucks;

import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

public final class MoneysTest {
  @Test public void dollarsAndCentsAsCents() {
    assertThat(Moneys.formatCents(340)).isEqualTo("$3.40");
    assertThat(Moneys.formatCents(100000042)).isEqualTo("$1,000,000.42");
  }

  @Test public void dollarsOnlyAsCents() {
    assertThat(Moneys.formatCents(300)).isEqualTo("$3");
    assertThat(Moneys.formatCents(100000000)).isEqualTo("$1,000,000");
  }

  @Test public void dollarsAndCentsAsDollars() {
    assertThat(Moneys.formatDollars(3.4d)).isEqualTo("$3.40");
    assertThat(Moneys.formatDollars(3.42d)).isEqualTo("$3.42");
    assertThat(Moneys.formatDollars(1000000.42d)).isEqualTo("$1,000,000.42");
  }

  @Test public void dollarsOnlyAsDollars() {
    assertThat(Moneys.formatDollars(3d)).isEqualTo("$3");
    assertThat(Moneys.formatDollars(1000000d)).isEqualTo("$1,000,000");
  }
}
