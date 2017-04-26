package com.example.boxup.bucks;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

public final class EdgeCases {
  @Test @Ignore("Message!") public void ignored() {
    fail();
  }

  @Test public void assumptionFailure() {
    assumeTrue(false);
  }
}
