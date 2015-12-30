Change Log
==========

Version 1.2.1 *(2015-11-16)*
----------------------------

* Fix: no more hardcoded 30s timeout
* New: api to load list of attached devices

Version 1.2.0 *(2015-08-26)*
----------------------------

* New: attach arbitrary files to your test output

Version 1.1.10 *(2015-07-05)*
----------------------------

* Fix: hang after executing tests.
* New: ability to set arbitrary -e test runner args.

Version 1.1.9 *(2015-04-02)*
----------------------------

* Fix: adb logs

Version 1.1.8 *(2015-03-21)*
----------------------------

* Fix: timeout while deploying apk to the device

Version 1.1.7 *(2015-03-18)*
----------------------------

* Fix: one more concurrent-adb issue.

Version 1.1.6 *(2015-03-16)*
----------------------------

* Fix: a different approach to fixing the adb connection issues.

Version 1.1.5 *(2015-03-16)*
----------------------------

* Fix: stacktrace in stdout when running on multiple devices.
* Fix: disconnect from adb after running tests.

Version 1.1.4 *(2015-03-16)*
----------------------------

* Allow specifying test class/method names in Spoon.screenshot call.

Version 1.1.3 *(2015-03-13)*
----------------------------

* Fix screenshots on Lollipop.
* Allow additional testRunListener.
* Support junit4 test methods that do not start with "test".
* Support junit4 tests that do not extend InstrumentationTestCase.
* Support sequential running of tests.

Version 1.1.2 *(2014-11-07)*
----------------------------

 * Return file path from screenshot method.
 * Expose 'failIfNoDeviceConnected' parameter in Maven plugin.
 * Fix: Correct occasional CSS problems on the TV output.
 * Fix: Force Google fonts to load over HTTPS.
 * Fix: Update to latest 'ddmlib' to resolve conflicts with the Android Gradle plugin.


Version 1.1.1 *(2014-02-11)*
----------------------------

 * Use emulator name instead of serial number in output HTML.
 * Update to latest Maven Android plugin.


Version 1.1.0 *(2013-11-24)*
----------------------------

 * Add preliminary TV display output which cycles through tests and screenshots.
 * Old APKs are no longer uninstalled.
 * All strings are sanitized for use on the filesystem.
 * Support exceptions whose header has no message.
 * `--no-animations` argument disables GIF generation.
 * `--size` argument allows specifying which test size to run. Default is to run all tests.
 * `--adb-timeout` argument controls maximum time per test. Default is 10 minutes.
 * `--fail-if-no-device-connected` argument causes failure indication when no devices are found.
   Default is to succeed.


Version 1.0.5 *(2013-06-05)*
----------------------------

 * Generate JUnit-compatible XML reports for each device.
 * Add timeout for stalled tests and flaky devices.
 * Add `spoon:open` Maven command to open the output web page.


Version 1.0.4 *(2013-05-23)*
----------------------------

 * Support for GIFs of tests in multiple orientations.
 * Fix: Prevent Java from showing a window while running tests on some OSs.
 * Fix: Prevent screenshots from being listed out of order on some OSs.


Version 1.0.3 *(2013-04-04)*
----------------------------

 * Display OS properties on the top of device page.
 * Fix: Prevent exception when `ANDROID_SDK` not set.


Version 1.0.2 *(2013-03-14)*
----------------------------

 * Devices without names are properly sorted.
 * Fix: App and instrumentation APK now resolves using Aether.


Version 1.0.1 *(2013-02-26)*
----------------------------

 * Improve classpath detection inside Maven plugin.
 * Screenshot tags are now logged and displayed as tooltips.
 * Fix: Generating output on Windows no longer throws exception.
 * Fix: Screenshots in base test classes no longer throws exception.
 * Fix: Lack of `ANDROID_SDK` environment variable no longer throws inadvertent exception.
 * Fix: Device run failure is now correctly indicated in output.


Version 1.0.0 *(2012-02-13)*
----------------------------

Initial release.
