package com.squareup.spoon

import com.google.common.truth.Truth.assertThat
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.SystemExitException
import java.io.File;
import org.junit.Test


class CliArgsTest {

  @Test
  fun parserInstrumentationArgsTest() {

    val args = arrayOf(
        "app-androidTest.apk",
        "app.apk",
        "-e",
        "package",
        "com.sample.testsuites",
        "--output",
        "outputs/spoon"
    )
    val parser = ArgParser(args)
    val cliArgs = CliArgs(parser)

    assertThat(cliArgs.testApk.name).isEqualTo("app-androidTest.apk")
    assertThat(cliArgs.otherApks[0].name).isEqualTo("app.apk")
    assertThat(cliArgs.instrumentationArgs?.get("package"))
        .isEqualTo("com.sample.testsuites")
    assertThat(cliArgs.output?.path).isEqualTo("outputs" + File.separator + "spoon")
  }

  @Test
  fun parserNullInstrumentationArgsTest() {
    val args = arrayOf(
        "app-androidTest.apk",
        "app.apk",
        "--output",
        "outputs/spoon"
    )
    val parser = ArgParser(args)
    val cliArgs = CliArgs(parser)

    assertThat(cliArgs.testApk.name).isEqualTo("app-androidTest.apk")
    assertThat(cliArgs.otherApks[0].name).isEqualTo("app.apk")
    assertThat(cliArgs.output?.path).isEqualTo("outputs" + File.separator + "spoon")
    assertThat(cliArgs.instrumentationArgs).isNull()
  }

  @Test
  fun parserInstrumentationArgsValidatorConflictDataTest() {
    // conflicting instrumentation args package and class, exception expected
    val args1 = arrayOf(
        "app-androidTest.apk",
        "app.apk",
        "-e",
        "package",
        "com.sample.testsuites",
        "-e",
        "class",
        "com.sample.testsuites.SomeClass"
    )
    // conflicting instrumentation args package and --class-name, exception expected
    val args2 = arrayOf(
        "app-androidTest.apk",
        "app.apk",
        "-e",
        "package",
        "com.sample.testsuites",
        "--class-name",
        "com.sample.testsuites.SomeClass"
    )
    // conflicting instrumentation args package and --method-name, exception expected
    val args3 = arrayOf(
        "app-androidTest.apk",
        "app.apk",
        "-e",
        "package",
        "com.sample.testsuites",
        "--method-name",
        "com.sample.testsuites.SomeClass#someMethod"
    )
    var args = listOf(args1, args2, args3)
    args
        .map { ArgParser(it) }
        .forEach {
          try {
            val cliArgs = CliArgs(it)
            assertThat(cliArgs).isEqualTo("this assertion shouldn't be executed, exception is expected")
          } catch (e: SystemExitException) {
            assertThat(e).hasMessage("Ambiguous arguments: cannot provide both test package and test class(es)")
          }
        }
  }

  @Test
  fun parserInstrumentationArgsValidatorValidDataTest() {
    // Package argument only, conflicts are not expected.
    val packageOnlyArgs = arrayOf(
        "app-androidTest.apk",
        "app.apk",
        "-e",
        "package",
        "com.sample.testsuites"
    )
    CliArgs(ArgParser(packageOnlyArgs))
    // Instrumentation class argument only, conflicts are not expected.
    val classOnlyArgs = arrayOf(
        "app-androidTest.apk",
        "app.apk",
        "-e",
        "class",
        "com.sample.testsuites.SomeClass"
    )
    CliArgs(ArgParser(classOnlyArgs))
    // Spoon --class-name argument only, conflicts are not expected.
    val classNameOnlyArgs = arrayOf(
        "app-androidTest.apk",
        "app.apk",
        "--class-name",
        "com.sample.testsuites.SomeClass"
    )
    CliArgs(ArgParser(classNameOnlyArgs))
    // Spoon --method-name argument only, conflicts are not expected.
    val methodNameOnlyArgs = arrayOf(
        "app-androidTest.apk",
        "app.apk",
        "--method-name",
        "com.sample.testsuites.SomeClass#someMethod"
    )
    CliArgs(ArgParser(methodNameOnlyArgs))
  }
}
