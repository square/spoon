@file:JvmName("Main")
package com.squareup.spoon

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.mainBody

fun main(vararg args: String) {
  val cli = mainBody("spoon-runner") {
    CliArgs(ArgParser(args))
  }

  val runner = SpoonRunner.Builder().apply {
    setTestApk(cli.testApk)
    cli.otherApks.forEach { addOtherApk(it) }
    cli.sdk?.let(this::setAndroidSdk)
    cli.title?.let(this::setTitle)
    setInstrumentationArgs(cli.instrumentationArgs);
    cli.className?.let(this::setClassName)
    cli.methodName?.let(this::setMethodName)
    cli.size?.let(this::setTestSize)
    cli.output?.let(this::setOutputDirectory)
    setAllowNoDevices(cli.allowNoDevices)
    setSequential(cli.sequential)
    cli.initScript?.let(this::setInitScript)
    setGrantAll(cli.grantAll)
    setNoAnimations(cli.disableGif)
    cli.adbTimeout?.let(this::setAdbTimeout)
    cli.serials.forEach { addDevice(it) }
    cli.skipSerials.forEach { skipDevice(it) }
    setShard(cli.shard)
    setDebug(cli.debug)
    setCodeCoverage(cli.coverage)
    setSingleInstrumentationCall(cli.singleInstrumentationCall)
    setClearAppDataBeforeEachTest(cli.clearAppDataBeforeEachTest)
  }.build()

  if (!runner.run() && !cli.alwaysZero) {
    System.exit(1)
  }
}
