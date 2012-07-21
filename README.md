Spoon
=====

Command line app which aids in the deployment, execution, and aggregation of
instrumentation tests across a large number of devices with varying
configurations.


Goals
-----

 * Easily run and test your application on a lot of device configurations.
 * Aggregate in a single web page which is easy to navigate and determine
   failures.
 * Powerful yet simple so that we can support both out-of-the-box executions
   on a single machine to running as a Jenkins plugin that distributes the
   work across multiple slaves.
 * __Open source, modular, reusable__


Features
--------

 * Automatically create and tear down AVDs as needed.
 * Ships with 50 preset templates for the most common devices.
 * Custom device templates.
 * Default configurations on which templates are applied (e.g., setting
   locale to 'fr' so all devices run in French).
 * Operate by convention but allow overriding via command-line args.


Overview
--------

 * Written in Java.
 * Common library, command-line app, Jenkins plugin.
 * YML configuration files.
 * Requires `android` and `adb` be in the system path.
