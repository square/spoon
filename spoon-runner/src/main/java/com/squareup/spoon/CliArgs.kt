package com.squareup.spoon

import com.android.ddmlib.testrunner.IRemoteAndroidTestRunner.TestSize
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import java.io.File
import java.time.Duration

internal class CliArgs(parser: ArgParser) {
  val mainApk by parser.positional("MAIN_APK", "Main APK", transform = ::File)

  val testApk by parser.positional("TEST_APK", "Test APK", transform = ::File)

  val title by parser.storing("Execution title").default(null)

  val instrumentationArgs by parser.option<List<String>>("-e", "--es",
      help = "Instrumentation runner arguments.", argNames = listOf("KEY", "VALUE")) { arguments }
      .default(emptyList())

  val className by parser.storing("--class-name", help = "Fully-qualified test class to run")
      .default(null)

  val methodName by parser.storing("--method-name", help = "Method name inside --class-name to run")
      .default(null)

  val size by parser.mapping(
      "--small" to TestSize.SMALL,
      "--medium" to TestSize.MEDIUM,
      "--large" to TestSize.LARGE,
      help = "Test size to run")
      .default(null)

  val output by parser.storing(
      "Output path. Defaults to spoon-output/ in the working directory if unset",
      transform = ::File).default(null)

  val sdk by parser.storing("Android SDK path. Defaults to ANDROID_HOME if unset.",
      transform = ::File).default(null)

  val alwaysZero by parser.flagging("--always-zero",
      help = "Always use 0 for the exit code regardless of execution failure")

  val allowNoDevices by parser.flagging("--allow-no-devices",
      help = "Do not fail if zero devices connected")

  val sequential by parser.flagging("Execute tests sequentially (one device at a time)")

  val initScript by parser.storing("--init-script",
      help = "Script file executed between each devices", transform = ::File).default(null)

  val grantAll by parser.flagging("--grant-all",
      help = "Grant all runtime permissions during installation on M+")

  val disableGif by parser.flagging("--disable-gif", help = "Disable GIF generation")

  val adbTimeout by parser.storing("--adb-timeout",
      help = "Maximum execution time per test. Parsed by java.time.Duration.",
      transform = Duration::parse).default(null)

  val serials by parser.adding("--serial",
      help = "Device serials to use. If empty all devices will be used.")

  val skipSerials by parser.adding("--skip-serial", help = "Device serials to skip")

  val shard by parser.flagging("Shard tests across all devices")

  val debug by parser.flagging("Enable debug logging")

  val coverage by parser.flagging("Enable code coverage")

  init {
    parser.force()
  }
}
