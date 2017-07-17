package com.squareup.spoon.internal.thirdparty.axmlparser;

import org.junit.Test;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class StringBlockTests {
    /** Test APKs are located in axmlparser's res directory */
    private static final String APK_MANIFEST_UTF_8 = "file/manifest-utf-8.apk";
    private static final String APK_MANIFEST_UTF_16 = "file/manifest-utf-16.apk";

    /** Path the resource directory for axmlparser (relative to project root). */
    private static final String PATH_TO_RESOURCES = "third-party/axmlparser/src/test/res/";

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
     * @param apkPath Path to either a UTF-8 or UTF-9 style APK test resource.
     * @return File object for APK test resource.
     * @throws FileNotFoundException Thrown when test file is not found.
     */
    private File getTestDataFile(String apkPath) throws FileNotFoundException {
        File resourcesDirectory = new File(PATH_TO_RESOURCES);
        assert(resourcesDirectory.isDirectory());
        return new File(resourcesDirectory, apkPath);
    }

    /**
     * Helper method that is a copy of the parseFromFile method in
     * spoon-runner's InstrumentationInfo class.
     *
     * @param apkFilePath File to test (from test resources)
     * @throws Exception Any test exception.
     */
    private void testParseApk(String apkFilePath) throws Exception {
        File apkTestFile = getTestDataFile(apkFilePath);

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

            assertNotNull("Could not find minSdkVersion.", minSdkVersion);
            assertNotNull("Could not find test application package.", testPackage);
            assertNotNull("Could not find application package.", appPackage);
            assertNotNull("Could not find test runner class.", testRunnerClass);

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
        File apkTestFile = getTestDataFile(inputTestFilePath);
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
                assertEquals("String in string pool should match expected string.", expectedStrings[i], stringBlock.getRaw(i));
            }
        }
    }
}