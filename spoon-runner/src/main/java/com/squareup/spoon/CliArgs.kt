package com.squareup.spoon

import com.android.ddmlib.testrunner.IRemoteAndroidTestRunner.TestSize
import com.xenomachina.argparser.ArgParser
import java.io.File
import java.time.Duration

internal class CliArgs(parser: ArgParser) {
  /* A transform that coerces the normal String type to be nullable. */
  private val nullableString: String.() -> String? = { this }

  val mainApk by parser.positional("main-apk", help = "Main APKs", transform = ::File)

  val testApk by parser.positional("test-apk", help = "Test APK", transform = ::File)

  val title by parser.storing("Execution title", nullableString).default(null)

  val instrumentationArgs by parser.adding("-e", "--es",
      help = "Instrumentation runner arguments. Format: key=value")

  val className by parser.storing("--class-name", help = "Fully-qualified test class to run",
      transform = nullableString).default(null)

  val methodName by parser.storing("--method-name", help = "Method name inside --class-name to run",
      transform = nullableString).default(null)

  val size by parser.mapping<TestSize?>(
      "--small" to TestSize.SMALL,
      "--medium" to TestSize.MEDIUM,
      "--large" to TestSize.LARGE,
      help = "Test size to run")
      .default(null)

  val output by parser.storing<File?>(
      "Output path. Defaults to spoon-output/ in the working directory if unset", ::File).default(
      null)

  val sdk by parser.storing<File?>("Android SDK path. Defaults to ANDROID_HOME if unset.",
      ::File).default(null)

  val failOnFailure by parser.flagging("--fail-on-failure", help = "Non-zero exit code on failure")

  val failIfNoDevices by parser.flagging("--fail-if-no-devices",
      help = "Fail if no devices connected")

  val sequential by parser.flagging("Execute tests sequentially (one device at a time)")

  val initScript by parser.storing<File?>("--init-script",
      help = "Script file executed between each devices", transform = ::File).default(null)

  val grantAll by parser.flagging("--grant-all",
      help = "Grant all runtime permissions during installation on M+")

  val disableGif by parser.flagging("--disable-gif", help = "Disable GIF generation")

  val adbTimeout by parser.storing<Duration?>("--adb-timeout",
      help = "Maximum execution time per test. Parsed by java.time.Duration.",
      transform = Duration::parse).default(null)

  val serials by parser.adding("--serial",
      help = "Device serials to use. If empty all devices will be used.")

  val skipSerials by parser.adding("--skip-serial", help = "Device serials to skip")

  val shard by parser.flagging("Shard tests across all devices")

  val debug by parser.flagging("Enable debug logging")

  val coverage by parser.flagging("Enable code coverage")
}
