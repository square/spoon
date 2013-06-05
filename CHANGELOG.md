Change Log
==========

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
