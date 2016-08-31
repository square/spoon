package com.squareup.spoon.misc;

import java.util.ArrayDeque;
import java.util.Deque;
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

  /**
   * The intent of this test is to check that unexpected format stack traces don't cause any
   * parsing exceptions.
   */
  @Test public void unexpectedFormatException() {
    String exception = "" +
            "junit.framework.AssertionFailedError:\n";

    // This exception does not match the expected stack trace format
    String message = ""
            + "        **** 2 Assertion Errors Found ****\n"
            + "\n"
            + "        --------- Failed Assertion # 1 --------\n"
            + "junit.framework.AssertionFailedError: 1st expected failure\n"
            + "at junit.framework.Assert.fail(Assert.java:50)\n"
            + "at junit.framework.Assert.assertTrue(Assert.java:20)\n"
            + "at com.capitalone.mobile.wallet.testing.AssertionErrorCollector.assertTrue(AssertionErrorCollector.java:34)\n"
            + "\n"
            + "        --------- Failed Assertion # 2 --------\n"
            + "junit.framework.AssertionFailedError: 2nd expected failure\n"
            + "at junit.framework.Assert.fail(Assert.java:50)\n"
            + "at junit.framework.Assert.assertTrue(Assert.java:20)\n"
            + "at com.capitalone.mobile.wallet.testing.AssertionErrorCollector.assertTrue(AssertionErrorCollector.java:34)\n";

    StackTrace actual = StackTrace.from(exception + message);
    assertThat(actual.getClassName()).isEqualTo("junit.framework.AssertionFailedError");

    // Due to the unexpected exception format, only the first part of the stack trace gets parsed
    // into the exception message
    assertThat(actual.getMessage()).isEqualTo(""
            + "        **** 2 Assertion Errors Found ****\n"
            + "\n"
            + "        --------- Failed Assertion # 1 --------\n"
            + "junit.framework.AssertionFailedError: 1st expected failure");
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

  @Test public void mockitoException() {
    String exception = "org.mockito.exceptions.misusing.InvalidUseOfMatchersException:\n"
        + "Invalid use of argument matchers!\n"
        + "2 matchers expected, 1 recorded:\n"
        + "-> at com.autoscout24.SomeTest.someTest(SomeTest.java:46)\n"
        + "\n"
        + "This exception may occur if matchers are combined with raw values:\n"
        + "//incorrect:\n"
        + "someMethod(anyObject(), \"raw String\");\n"
        + "When using matchers, all arguments have to be provided by matchers.\n"
        + "For example:\n"
        + "//correct:\n"
        + "someMethod(anyObject(), eq(\"String by matcher\"));\n"
        + "\n"
        + "For more info see javadoc for Matchers class.\n"
        + "\n"
        + "at com.autoscout24.SomeTest.someTest(SomeTest.java:46)\n"
        + "at java.lang.reflect.Method.invokeNative(Native Method)\n"
        + "at android.test.InstrumentationTestCase.runMethod(InstrumentationTestCase.java:214)\n"
        + "at android.test.InstrumentationTestCase.runTest(InstrumentationTestCase.java:199)\n"
        + "at android.test.AndroidTestRunner.runTest(AndroidTestRunner.java:191)\n"
        + "at android.test.AndroidTestRunner.runTest(AndroidTestRunner.java:176)\n"
        + "at android.test.InstrumentationTestRunner.onStart(InstrumentationTestRunner.java:554)\n"
        + "at android.app.Instrumentation$InstrumentationThread.run(Instrumentation.java:1701)";

    StackTrace actual = StackTrace.from(exception);

    String expected = ""
        + "Invalid use of argument matchers!\n"
        + "2 matchers expected, 1 recorded:\n"
        + "-> at com.autoscout24.SomeTest.someTest(SomeTest.java:46)\n"
        + "\n"
        + "This exception may occur if matchers are combined with raw values:\n"
        + "//incorrect:\n"
        + "someMethod(anyObject(), \"raw String\");\n"
        + "When using matchers, all arguments have to be provided by matchers.\n"
        + "For example:\n"
        + "//correct:\n"
        + "someMethod(anyObject(), eq(\"String by matcher\"));\n"
        + "\n"
        + "For more info see javadoc for Matchers class.";
    assertThat(actual.getMessage()).isEqualTo(expected);
  }

  @Test public void toStringFormat() {
    Deque<StackTrace.Element> elements = new ArrayDeque<StackTrace.Element>();

    StackTrace onlyClass = new StackTrace("java.lang.NullPointerException", null, elements, null);
    assertThat(onlyClass.toString()).isEqualTo("java.lang.NullPointerException");

    StackTrace onlyMessage = new StackTrace(null, "Hello, World!", elements, null);
    assertThat(onlyMessage.toString()).isEqualTo("Hello, World!");

    StackTrace both = new StackTrace("java.lang.NullPointerException", "Hi, Mom!", elements, null);
    assertThat(both.toString()).isEqualTo("java.lang.NullPointerException: Hi, Mom!");
  }
}
