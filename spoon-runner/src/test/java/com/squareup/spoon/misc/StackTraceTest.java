package com.squareup.spoon.misc;

import java.util.Collections;
import java.util.List;
import org.junit.Test;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class StackTraceTest {
  @Test public void recursiveCause() {
    Exception spied = spy(new IllegalArgumentException("To understand recursion..."));
    when(spied.getCause()).thenReturn(spied);

    StackTrace actual = StackTrace.from(spied);
    assertThat(actual.getCause()).isNull();
  }

  @Test public void nullCause() {
    Exception t = new IllegalStateException("Null cause.", null);
    StackTrace actual = StackTrace.from(t);
    assertThat(actual.getClassName()).isEqualTo("java.lang.IllegalStateException");
    assertThat(actual.getMessage()).isEqualTo("Null cause.");
    assertThat(actual.getCause()).isNull();
  }

  @Test public void stringException() {
    String exception = ""
        + "java.lang.RuntimeException: Explicitly testing unexpected exceptions!\n"
        + "at com.example.spoon.ordering.tests.MiscellaneousTest.testAnotherLongNameBecauseItIsHumorousAndTestingThingsLikeThisIsImportant(MiscellaneousTest.java:11)\n"
        + "at java.lang.reflect.Method.invokeNative(Native Method)\n"
        + "at android.test.InstrumentationTestCase.runMethod(InstrumentationTestCase.java:214)\n"
        + "at android.test.InstrumentationTestCase.runTest(InstrumentationTestCase.java:199)\n"
        + "at android.test.AndroidTestRunner.runTest(AndroidTestRunner.java:190)\n"
        + "at android.test.AndroidTestRunner.runTest(AndroidTestRunner.java:175)\n"
        + "at android.test.InstrumentationTestRunner.onStart(InstrumentationTestRunner.java:555)\n"
        + "at com.example.spoon.ordering.tests.SpoonInstrumentationTestRunner.onStart(SpoonInstrumentationTestRunner.java:36)\n"
        + "at android.app.Instrumentation$InstrumentationThread.run(Instrumentation.java:1661)";

    StackTrace actual = StackTrace.from(exception);
    assertThat(actual.getClassName()).isEqualTo("java.lang.RuntimeException");
    assertThat(actual.getMessage()).isEqualTo("Explicitly testing unexpected exceptions!");
    assertThat(actual.getCause()).isNull();
    assertThat(actual.getElements()).hasSize(9);

    StackTrace.Element elementOne = actual.getElements().get(0);
    assertThat(elementOne.getClassName()) //
        .isEqualTo("com.example.spoon.ordering.tests.MiscellaneousTest");
    assertThat(elementOne.getMethodName()) //
        .isEqualTo("testAnotherLongNameBecauseItIsHumorousAndTestingThingsLikeThisIsImportant");
    assertThat(elementOne.getFileName()).isEqualTo("MiscellaneousTest.java");
    assertThat(elementOne.getLine()).isEqualTo(11);
    assertThat(elementOne.isNative()).isFalse();

    StackTrace.Element elementTwo = actual.getElements().get(1);
    assertThat(elementTwo.getClassName()).isEqualTo("java.lang.reflect.Method");
    assertThat(elementTwo.getMethodName()).isEqualTo("invokeNative");
    assertThat(elementTwo.getFileName()).isNull();
    assertThat(elementTwo.getLine()).isZero();
    assertThat(elementTwo.isNative()).isTrue();
  }

  @Test public void noMessage() {
    String exception = ""
        + "java.lang.NullPointerException\n"
        + "at com.example.spoon.ordering.tests.MiscellaneousTest.test(MiscellaneousTest.java:11)";

    StackTrace actual = StackTrace.from(exception);
    assertThat(actual.getClassName()).isEqualTo("java.lang.NullPointerException");
    assertThat(actual.getMessage()).isNull();
    assertThat(actual.getCause()).isNull();
    assertThat(actual.getElements()).hasSize(1);
  }

  @Test public void nestedStringException() {
    String exception = ""
        + "java.lang.NullPointerException: Hello to the world.\n"
        + "at com.example.spoon.ordering.tests.MiscellaneousTest.test(MiscellaneousTest.java:11)\n"
        + "Caused by: java.lang.IllegalArgumentException: Inner exception.\n"
        + "at com.example.spoon.ordering.tests.Other.otherTest(Other.java:12)\n"
        + "at com.example.spoon.ordering.tests.Other.things(Other.java:22)";

    StackTrace actual = StackTrace.from(exception);
    assertThat(actual.getClassName()).isEqualTo("java.lang.NullPointerException");
    assertThat(actual.getMessage()).isEqualTo("Hello to the world.");
    assertThat(actual.getCause()).isNotNull();
    assertThat(actual.getElements()).hasSize(1);

    StackTrace inner = actual.getCause();
    assertThat(inner.getClassName()).isEqualTo("java.lang.IllegalArgumentException");
    assertThat(inner.getMessage()).isEqualTo("Inner exception.");
    assertThat(inner.getCause()).isNull();
    assertThat(inner.getElements()).hasSize(2);
  }

  @Test public void multiLineException() {
    String exception = ""
        + "java.lang.AssertionError: Expected:\n"
        + "<4>\n"
        + "but was:\n"
        + "<8>\n"
        + "at com.example.spoon.ordering.tests.MiscellaneousTest.test(MiscellaneousTest.java:11)";

    StackTrace actual = StackTrace.from(exception);
    assertThat(actual.getClassName()).isEqualTo("java.lang.AssertionError");
    assertThat(actual.getMessage()).isEqualTo("Expected:\n<4>\nbut was:\n<8>");
    assertThat(actual.getCause()).isNull();
    assertThat(actual.getElements()).hasSize(1);
  }

  @Test public void nestedMultiLineException() {
    String exception = ""
        + "java.lang.NullPointerException: Hello to the world.\n"
        + "at com.example.spoon.ordering.tests.MiscellaneousTest.test(MiscellaneousTest.java:11)\n"
        + "Caused by: java.lang.AssertionError: Expected:\n"
        + "<4>\n"
        + "but was:\n"
        + "<8>\n"
        + "at com.example.spoon.ordering.tests.Other.otherTest(Other.java:12)\n"
        + "at com.example.spoon.ordering.tests.Other.things(Other.java:22)";

    StackTrace actual = StackTrace.from(exception);
    assertThat(actual.getClassName()).isEqualTo("java.lang.NullPointerException");
    assertThat(actual.getMessage()).isEqualTo("Hello to the world.");
    assertThat(actual.getCause()).isNotNull();
    assertThat(actual.getElements()).hasSize(1);

    StackTrace inner = actual.getCause();
    assertThat(inner.getClassName()).isEqualTo("java.lang.AssertionError");
    assertThat(inner.getMessage()).isEqualTo("Expected:\n" + "<4>\n" + "but was:\n" + "<8>");
    assertThat(inner.getCause()).isNull();
    assertThat(inner.getElements()).hasSize(2);
  }

  @Test public void nestedExceptionWithMore() {
    String exception = ""
        + "java.lang.NullPointerException: Hello to the world.\n"
        + "at com.example.spoon.ordering.tests.MiscellaneousTest.test(MiscellaneousTest.java:11)\n"
        + "Caused by: java.lang.AssertionError: Broken\n"
        + "at com.example.spoon.ordering.tests.Other.otherTest(Other.java:12)\n"
        + "... 12 more";

    StackTrace actual = StackTrace.from(exception);
    assertThat(actual.getClassName()).isEqualTo("java.lang.NullPointerException");
    assertThat(actual.getMessage()).isEqualTo("Hello to the world.");
    assertThat(actual.getCause()).isNotNull();
    assertThat(actual.getElements()).hasSize(1);

    StackTrace inner = actual.getCause();
    assertThat(inner.getClassName()).isEqualTo("java.lang.AssertionError");
    assertThat(inner.getMessage()).isEqualTo("Broken");
    assertThat(inner.getCause()).isNull();
    assertThat(inner.getElements()).hasSize(1);
  }

  @Test public void toStringFormat() {
    List<StackTrace.Element> elements = Collections.emptyList();

    StackTrace onlyClass = new StackTrace("java.lang.NullPointerException", null, elements, null);
    assertThat(onlyClass.toString()).isEqualTo("java.lang.NullPointerException");

    StackTrace onlyMessage = new StackTrace(null, "Hello, World!", elements, null);
    assertThat(onlyMessage.toString()).isEqualTo("Hello, World!");

    StackTrace both = new StackTrace("java.lang.NullPointerException", "Hi, Mom!", elements, null);
    assertThat(both.toString()).isEqualTo("java.lang.NullPointerException: Hi, Mom!");
  }
}
