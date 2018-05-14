package com.squareup.spoon.internal.thirdparty.axmlparser;

import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.Assert.*;

/**
 * Contains 4 test to ensure that the StringBlock class properly parses
 * both UTF-8 and UTF-16 encoded strings within the compiled MANIFEST.MF
 * within the APK. Two test APKs are included in the test resources for
 * testing both encoding models.
 */
public class StringBlockTests {
    /**
     * Path to test APKs in the test/resources directory. The loadResource call
     * only seems to work when these tests are run on the command line via
     * ./gradlew tests. When the test are run through the Intellij IDE, the call
     * to getResource fails. So as a workaround, when the call fails, a fallback
     * strategy is used to load the APK files which avoids calling getResource.
     */
    private static final String APK_MANIFEST_UTF_8 = "//manifestUtf8.apk";
    private static final String APK_MANIFEST_UTF_16 = "//manifestUtf16.apk";

    /**
     * Used in fallback method to load the test APKs when getResource() fails.
     */
    private static final String PATH_TO_PROJECT = "third-party/axmlParser";
    private static final String PATH_TO_RESOURCES = "src/test/resources";

    /** Strings that should be string pool in the test file APK_MANIFEST_UTF_8. */
    private static final String[] EXPECTED_STRINGS_UTF_8 = new String[]{
            "label",
            "name",
            "targetPackage",
            "handleProfiling",
            "functionalTest",
            "minSdkVersion",
            "targetSdkVersion",
            "application",
            "android.test.runner",
            "uses-library",
            "Intents",
            "android",
            "edu.vanderbilt.demoapp",
            "android.support.test.runner.AndroidJUnitRunner",
            "Tests for edu.vanderbilt.demoapp",
            "instrumentation",
            "uses-sdk",
            "edu.vanderbilt.demoapp.test",
            "package",
            "manifest",
            "http://schemas.android.com/apk/res/android"
    };

    /** Strings that should be string pool in the test file APK_MANIFEST_UTF_16 . */
    private static final String[] EXPECTED_STRINGS_UTF_16 = new String[]{
            "minSdkVersion",
            "targetSdkVersion",
            "name",
            "functionalTest",
            "handleProfiling",
            "label",
            "targetPackage",
            "debuggable",
            "android",
            "http://schemas.android.com/apk/res/android",
            "",
            "package",
            "platformBuildVersionCode",
            "platformBuildVersionName",
            "manifest",
            "edu.vanderbilt.demoapp.test",
            "25",
            "7.1.1",
            "uses-sdk",
            "instrumentation",
            "android.support.test.runner.AndroidJUnitRunner",
            "Tests for edu.vanderbilt.demoapp",
            "edu.vanderbilt.demoapp",
            "application",
            "Intents",
            "uses-library",
            "android.test.runner"
    };

    /**
     * Tests UTF-8 APK parsing using same code as spoon-runner's
     * SpoonInstrumentationInfo@parserFromFile.
     *
     * @throws Exception Any test exception.
     */
    @Test
    public void testParseApk_Utf_8() throws Exception {
        testParseApk(APK_MANIFEST_UTF_8);
    }

    /**
     * Tests UTF-16 APK parsing using same code as spoon-runner's
     * SpoonInstrumentationInfo@parserFromFile.
     *
     * @throws Exception Any test exception.
     */
    @Test
    public void testParseApk_Utf_16() throws Exception {
        testParseApk(APK_MANIFEST_UTF_16);
    }

    /**
     * Tests UTF-8 APK string pool contents against expected
     * static UTF-8 string list.
     *
     * @throws Exception Any test exception.
     */
    @Test
    public void testApkForExpectedStringsInStringPool_Utf8() throws Exception {
        testApkForExpectedStringsInStringPool(APK_MANIFEST_UTF_8, EXPECTED_STRINGS_UTF_8);
    }

    /**
     * Tests UTF-16 APK string pool contents against expected
     * static UTF-16 string list.
     *
     * @throws Exception Any test exception.
     */
    @Test
    public void testApkForExpectedStringsInStringPool_Utf16() throws Exception {
        testApkForExpectedStringsInStringPool(APK_MANIFEST_UTF_16, EXPECTED_STRINGS_UTF_16);
    }

    /**
     * Helper that constructs File object for test APK files located
     * in test/res/file directory.
     *
     * NOTE: When running these test from right-clicking on the tests in the project
     * window and then choosing 'run Test in ...' or when right-clicking on a test
     * method to run the test, the getClass().getResource() will return a null URL.
     * This does not happen when the tests are run by ./gradlew third-party:axmlparser:test.
     *
     * @param dataFileName Path to either a UTF-8 or UTF-9 style APK test resource.
     * @return An APK file object.
     * @throws FileNotFoundException Thrown when test file is not found.
     */
    private File getTestDataFileFromResources(String dataFileName) throws FileNotFoundException {
        URL url = getClass().getResource(dataFileName);
        if (url == null) {
            // Must be running from within Intellij IDE, so use fallback filesystem
            // method to load the data file.
            return getTestDataFileFromFileSystem(dataFileName);
        }

        String dataPath = url.getFile();
        assertNotNull("Should be able to get a file from the resource URL.", dataPath);

        File dataFile = new File(dataPath);
        assertTrue("Data file " + dataFileName + "should exist.", dataFile.exists());

        return dataFile;
    }

    /**
     * Fallback method to get at APK test data files. This works when running the
     * test in the Intellij IDE but not on the command line with ./gradlew test.
     *
     * @param dataFileName Path to ea UTF-8 or UTF-9 style APK test resource.
     * @return An APK file object.
     */
    private File getTestDataFileFromFileSystem(String dataFileName) {
        // Works for gradle test task."
        File resourcesDirectory = new File(PATH_TO_RESOURCES);
        if (!resourcesDirectory.isDirectory()) {
            // Works for IDE right-click "Run test..."
            resourcesDirectory = new File(PATH_TO_PROJECT + "/" + PATH_TO_RESOURCES);
        }

        assertTrue("Resources directory should exist.", resourcesDirectory.isDirectory());

        File dataFile = new File(resourcesDirectory, dataFileName);
        assertTrue("Test data file " + dataFile.getPath() + " should exist.", dataFile.exists());

        return dataFile;
    }

    /**
     * Helper method that is a copy of the parseFromFile method in
     * spoon-runner's InstrumentationInfo class.
     *
     * @param apkFilePath File to test (from test resources)
     * @throws Exception Any test exception.
     */
    private void testParseApk(String apkFilePath) throws Exception {
        File apkTestFile = getTestDataFileFromResources(apkFilePath);

        try (ZipFile zip = new ZipFile(apkTestFile)) {
            ZipEntry entry = zip.getEntry("AndroidManifest.xml");

            InputStream inputStream = zip.getInputStream(entry);
            AXMLParser parser = new AXMLParser(inputStream);
            int eventType = parser.getType();

            boolean isManifest;
            boolean isUsesSdk;
            boolean isInstrumentation;

            String appPackage = null;
            Integer minSdkVersion = null;
            String testPackage = null;
            String testRunnerClass = null;

            while (eventType != AXMLParser.END_DOCUMENT) {
                if (eventType == AXMLParser.START_TAG) {
                    String parserName = parser.getName();
                    isManifest = "manifest".equals(parserName);
                    isUsesSdk = "uses-sdk".equals(parserName);
                    isInstrumentation = "instrumentation".equals(parserName);
                    if (isManifest || isInstrumentation || isUsesSdk) {
                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            String parserAttributeName = parser.getAttributeName(i);
                            if (isManifest && "package".equals(parserAttributeName)) {
                                testPackage = parser.getAttributeValueString(i);
                            } else if (isUsesSdk && "minSdkVersion".equals(parserAttributeName)) {
                                minSdkVersion = parser.getAttributeValue(i);
                            } else if (isInstrumentation && "targetPackage".equals(parserAttributeName)) {
                                appPackage = parser.getAttributeValueString(i);
                            } else if (isInstrumentation && "name".equals(parserAttributeName)) {
                                testRunnerClass = parser.getAttributeValueString(i);
                            }
                        }
                    }
                }
                eventType = parser.next();
            }

            // InstrumentationInfo's tests.
            assertNotNull("Could not find minSdkVersion.", minSdkVersion);
            assertNotNull("Could not find test application package.", testPackage);
            assertNotNull("Could not find application package.", appPackage);
            assertNotNull("Could not find test runner class.", testRunnerClass);

            // More strict tests to ensure proper return values.
            assertSame("minSdkVersions should match", 21, minSdkVersion);
            assertEquals("testPackages should match", "edu.vanderbilt.demoapp.test", testPackage);
            assertEquals("appPackages should match", "edu.vanderbilt.demoapp", appPackage);
            assertEquals("testRunnerClasses should match",
                    "android.support.test.runner.AndroidJUnitRunner", testRunnerClass);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Helper method that is used to test both UTF-8 and UTF-16 input data against
     * expected results stored in static string arrays..
     *
     * @param inputTestFilePath Input file path.
     * @param expectedStrings   String array containing expected string results.
     * @throws IOException Any test exception.
     */
    private void testApkForExpectedStringsInStringPool(String inputTestFilePath, String[] expectedStrings)
            throws IOException {
        File apkTestFile = getTestDataFileFromResources(inputTestFilePath);
        assertTrue(apkTestFile.exists());
        try (ZipFile zip = new ZipFile(apkTestFile)) {
            ZipEntry entry = zip.getEntry("AndroidManifest.xml");
            InputStream inputStream = zip.getInputStream(entry);

            // Skip over chunk header so that we can directly call StringBlock class.
            ReadUtil.readInt(inputStream);
            ReadUtil.readInt(inputStream);

            StringBlock stringBlock = StringBlock.read(new IntReader(inputStream, false));

            assertEquals(expectedStrings.length, stringBlock.getCount());

            for (int i = 0; i < stringBlock.getCount(); i++) {
                assertEquals("String in string pool should match expected string.",
                        expectedStrings[i], stringBlock.getRaw(i));
            }
        }
    }
}