@file:JvmName("Main")
package com.squareup.spoon

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.mainBody

fun main(vararg args: String) {
  mainBody("spoon-runner") {
    CliArgs(ArgParser(args)).run {
      val builder = SpoonRunner.Builder()
      builder.setApplicationApk(mainApk)
      builder.setInstrumentationApk(testApk)
      sdk?.let(builder::setAndroidSdk)
      title?.let(builder::setTitle)
      builder.setInstrumentationArgs(instrumentationArgs);
      className?.let(builder::setClassName)
      methodName?.let(builder::setMethodName)
      size?.let(builder::setTestSize)
      output?.let(builder::setOutputDirectory)
      builder.setAllowNoDevices(allowNoDevices)
      builder.setSequential(sequential)
      initScript?.let(builder::setInitScript)
      builder.setGrantAll(grantAll)
      builder.setNoAnimations(disableGif)
      adbTimeout?.let(builder::setAdbTimeout)
      serials.forEach { builder.addDevice(it) }
      skipSerials.forEach { builder.addDevice(it) }
      builder.setShard(shard)
      builder.setDebug(debug)
      builder.setCodeCoverage(coverage)

      if (!builder.build().run() && !alwaysZero) {
        System.exit(1)
      }
    }
  }
}
