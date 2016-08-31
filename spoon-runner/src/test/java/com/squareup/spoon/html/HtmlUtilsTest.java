package com.squareup.spoon.html;

import java.io.File;
import java.util.List;
import org.junit.Test;
import com.squareup.spoon.html.HtmlUtils.ExceptionInfo;
import com.squareup.spoon.misc.StackTrace;

import static com.squareup.spoon.html.HtmlUtils.createRelativeUri;
import static com.squareup.spoon.html.HtmlUtils.prettifyImageName;
import static com.squareup.spoon.html.HtmlUtils.prettifyMethodName;
import static com.squareup.spoon.html.HtmlUtils.processStackTrace;
import static com.squareup.spoon.html.HtmlUtils.humanReadableDuration;
import static org.fest.assertions.api.Assertions.assertThat;

public class HtmlUtilsTest {
  @Test public void prettifyImageNameExamples() {
    // Simple cases.
    assertThat(prettifyImageName("012344567_click-the-button.png")).isEqualTo("Click The Button");
    assertThat(prettifyImageName("987245978_click_the_button.png")).isEqualTo("Click The Button");
  }

  @Test public void prettifyTestNameExamples() {
    // Simple cases.
    assertThat(prettifyMethodName("testClickTheButton")).isEqualTo("Click The Button");
    assertThat(prettifyMethodName("testClickTheButton_ShowsTheName")) //
        .isEqualTo("Click The Button, Shows The Name");
    assertThat(prettifyMethodName("testClickTheButton_ShowsTheName_HidesTheInput")) //
        .isEqualTo("Click The Button, Shows The Name, Hides The Input");

    // Over-eager underscore users.
    assertThat(prettifyMethodName("testOne__Two_Three")).isEqualTo("One, Two, Three");
    assertThat(prettifyMethodName("testOne_Two____Three")).isEqualTo("One, Two, Three");
    assertThat(prettifyMethodName("test__One_Two_Three")).isEqualTo("One, Two, Three");
    assertThat(prettifyMethodName("testOne_Two_Three__")).isEqualTo("One, Two, Three");

    // Harder, edge cases.
    assertThat(prettifyMethodName("testURLConnection")).isEqualTo("URL Connection");
    assertThat(prettifyMethodName("testAHardCase")).isEqualTo("A Hard Case");
    assertThat(prettifyMethodName("testOneATwo")).isEqualTo("One A Two");
  }

  @Test public void prettifyMethodNameWorksRegardlessOfTestPrefix() {
    assertThat(prettifyMethodName("oneTwoThree_Four")).isEqualTo("One Two Three, Four");
    assertThat(prettifyMethodName("thisIsAHardCase")).isEqualTo("This Is A Hard Case");
  }

  @Test public void relativeUriCreation() {
    File file = new File("/path/to/image/this/that/whatever.png");
    File output = new File("/path/to");
    assertThat(createRelativeUri(file, output)).isEqualTo("image/this/that/whatever.png");
  }

  @Test(expected = IllegalArgumentException.class)
  public void relativeUriCreationFailsIfSame() {
    File output = new File("/path/to");
    createRelativeUri(output, output);
  }

  @Test public void humanReadableDurationCases() {
    assertThat(humanReadableDuration(0)).isEqualTo("0 seconds");
    assertThat(humanReadableDuration(1)).isEqualTo("1 second");
    assertThat(humanReadableDuration(2)).isEqualTo("2 seconds");
    assertThat(humanReadableDuration(59)).isEqualTo("59 seconds");
    assertThat(humanReadableDuration(60)).isEqualTo("1 minute");
    assertThat(humanReadableDuration(61)).isEqualTo("1 minute, 1 second");
    assertThat(humanReadableDuration(62)).isEqualTo("1 minute, 2 seconds");
    assertThat(humanReadableDuration(122)).isEqualTo("2 minutes, 2 seconds");
    assertThat(humanReadableDuration(3661)).isEqualTo("61 minutes, 1 second");
  }

  /**
   * This test is similar to {@link StackTraceTest#nestedCustomExceptionUnexpectedFormat}.
   *
   * The intent of this test is to check that unexpected format exceptions still print something
   * useful to the user in the test results.
   */
  @Test public void processStackTraceUnexpectedFormat() {
    // This exception does not match the expected stack trace format
    StackTrace exception = StackTrace.from(""
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
            + "at com.capitalone.mobile.wallet.testing.AssertionErrorCollector.assertTrue(AssertionErrorCollector.java:34)\n");
    ExceptionInfo exceptionInfo = processStackTrace(exception);
    // This is one of the rare cases where newline characters need to be converted to <br/>.
    // Usually newline characters are stripped out by the parsing code in
    // {@link StackTrace#from(String)}, but it doesn't happen for unexpected format exceptions.
    assertThat(exceptionInfo.title).isEqualTo(""
            + "        **** 2 Assertion Errors Found ****: <br/>"
            + "        --------- Failed Assertion # 1 --------<br/>"
            + "junit.framework.AssertionFailedError: 1st expected failure");
    List<String> lines = exceptionInfo.body;
    assertThat(lines).isNotNull();
    assertThat(lines.size()).isEqualTo(4);
    assertThat(lines.get(0)).isEqualTo(""
            + "&nbsp;&nbsp;&nbsp;&nbsp;at junit.framework.Assert.fail(Assert.java:50)");
    assertThat(lines.get(1)).isEqualTo(""
            + "&nbsp;&nbsp;&nbsp;&nbsp;at junit.framework.Assert.assertTrue(Assert.java:20)");
    assertThat(lines.get(2)).isEqualTo(""
            + "&nbsp;&nbsp;&nbsp;&nbsp;at com.capitalone.mobile.wallet.testing.AssertionErrorCollector.assertTrue(AssertionErrorCollector.java:34)");
    // The final line here is "Caused by: ".  This is because the remaining parts of the stack trace
    // are interpreted as a "Caused by: " exception.  This behavior isn't all that desirable, so we
    // don't assert it here. :-)
  }

  @Test public void processStackTraceHtmlEscapeAngleBrackets() {
    StackTrace exception = StackTrace.from(""
            + "java.fake.Exception: Expected <SUCCESS> but was <FAILED>!\n"
            + " at android.fake.FakeClass.fakeMethod(FakeClass.java:1)\n"
            + " at android.fake.FakeClass.fakeMethod(FakeClass.java:2)\n"
            + "Caused by: java.lang.IllegalArgumentException: Inner exception <FAILED>\n"
            + " at android.fake.FakeClass.fakeMethod(FakeClass.java:3)\n");
    ExceptionInfo exceptionInfo = processStackTrace(exception);
    assertThat(exceptionInfo.title).isEqualTo(""
            + "java.fake.Exception: Expected &lt;SUCCESS&gt; but was &lt;FAILED&gt;!");
    List<String> lines = exceptionInfo.body;
    assertThat(lines).isNotNull();
    assertThat(lines.size()).isEqualTo(3);
    assertThat(lines.get(0)).isEqualTo(""
            + "&nbsp;&nbsp;&nbsp;&nbsp;at android.fake.FakeClass.fakeMethod(FakeClass.java:1)");
    assertThat(lines.get(1)).isEqualTo(""
            + "&nbsp;&nbsp;&nbsp;&nbsp;at android.fake.FakeClass.fakeMethod(FakeClass.java:2)");
    assertThat(lines.get(2)).isEqualTo(""
            + "Caused by: java.lang.IllegalArgumentException: Inner exception &lt;FAILED&gt;");
    // The stack trace for the "Caused by" exception does not get printed
  }
}