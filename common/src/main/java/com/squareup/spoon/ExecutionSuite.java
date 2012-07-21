package com.squareup.spoon;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import com.squareup.spoon.model.Device;
import com.squareup.spoon.model.RunConfig;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

public class ExecutionSuite implements Runnable {
  private final Logger logger;
  private final String sdkPath;
  private final RunConfig config;
  private final Collection<Device> devices;

  public ExecutionSuite(String sdkPath, RunConfig config, Collection<Device> devices, boolean includeAll) {
    this.logger = Logger.getLogger("Spoon");
    this.sdkPath = sdkPath;
    this.config = config;
    this.devices = devices;

    if (includeAll) {
      devices.addAll(findAllDevices(sdkPath));
    }
  }

  @Override public void run() {
    final int targetCount = devices.size();
    if (targetCount == 0) {
      logger.info("No devices.");
      return;
    }

    // TODO read this from config.test's AndroidManifest.xml using a Zip stream.
    final String testPackage = "com.squareup.spoon.sample.tests";

    logger.info("Executing instrumentation on " + targetCount + " devices.");

    deletePath(config.output);

    final CountDownLatch done = new CountDownLatch(targetCount);
    try {
      for (final Device device : devices) {
        new Thread(new Runnable() {
          @Override public void run() {
            try {
              ExecutionTarget target = new ExecutionTarget(sdkPath, config, testPackage, device);
              ExecutionResult result = target.call();
              //TODO aggregate result
            } catch (InterruptedException e) {
              e.printStackTrace();
            } catch (IOException e) {
              e.printStackTrace();
            } finally {
              done.countDown();
            }
          }
        }).start();
      }

      done.await();

      // TODO assemble final index.html
    } catch (Exception e) {
      e.printStackTrace();
      // TODO record exception
    }
  }

  private static void deletePath(File path) {
    if (path.isDirectory()) {
      for (File childPath : path.listFiles()) {
        deletePath(childPath);
      }
    }
    path.delete();
  }

  private static Collection<Device> findAllDevices(String sdkPath) {
    List<Device> devices = new ArrayList<Device>();
    AndroidDebugBridge adb = AdbHelper.init(sdkPath);
    for (IDevice realDevice : adb.getDevices()) {
      Device device = new Device();
      device.serial = realDevice.getSerialNumber();
      devices.add(device);
    }
    AndroidDebugBridge.terminate();
    return devices;
  }
}
