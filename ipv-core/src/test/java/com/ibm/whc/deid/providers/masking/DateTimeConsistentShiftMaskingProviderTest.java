/*
 * (C) Copyright IBM Corp. 2021,2022
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.whc.deid.providers.masking;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.junit.Test;
import com.fasterxml.jackson.databind.JsonNode;
import com.ibm.whc.deid.ObjectMapperFactory;
import com.ibm.whc.deid.providers.masking.fhir.MaskingActionInputIdentifier;
import com.ibm.whc.deid.shared.pojo.config.masking.DateTimeConsistentShiftMaskingProviderConfig;
import com.ibm.whc.deid.shared.pojo.config.masking.DateTimeConsistentShiftMaskingProviderConfig.DateShiftDirection;
import com.ibm.whc.deid.utils.log.LogCodes;
import com.ibm.whc.deid.shared.pojo.config.masking.UnexpectedMaskingInputHandler;

public class DateTimeConsistentShiftMaskingProviderTest implements MaskingProviderTest {

  private class TestDateTimeConsistentShiftMaskingProvider
      extends DateTimeConsistentShiftMaskingProvider {

    private static final long serialVersionUID = 1L;

    public long longFromSeed;

    public TestDateTimeConsistentShiftMaskingProvider(
        DateTimeConsistentShiftMaskingProviderConfig configuration) {
      super(configuration, null);
    }

    @Override
    protected long generateLongFromString(String seed) {
      return longFromSeed;
    }
  }

  @Test
  public void testApplyOffsetAndReformat() {
    DateTimeConsistentShiftMaskingProviderConfig config =
        new DateTimeConsistentShiftMaskingProviderConfig();
    config.setPatientIdentifierPath("/id");
    DateTimeConsistentShiftMaskingProvider provider =
        new DateTimeConsistentShiftMaskingProvider(config, null);
    verifyStandardReplacements(provider, null);

    config.setUnexpectedInputHandling(UnexpectedMaskingInputHandler.MESSAGE);
    provider = new DateTimeConsistentShiftMaskingProvider(config, null);
    verifyStandardReplacements(provider, "OTHER");

    config.setUnexpectedInputHandling(UnexpectedMaskingInputHandler.RANDOM);
    provider = new DateTimeConsistentShiftMaskingProvider(config, null);
    verifyStandardReplacements(provider, null);

    config.setUnexpectedInputHandling(UnexpectedMaskingInputHandler.ERROR_EXIT);
    provider = new DateTimeConsistentShiftMaskingProvider(config, null);
    provider.setName("nameABC");
    try {
      verifyStandardReplacements(provider, null);
      fail("expected exception");
    } catch (PrivacyProviderInvalidInputException e) {
      assertTrue(e.getMessage().contains("nameABC"));
      assertFalse(e.getMessage().contains("value-abc"));
    }
  }

  private void verifyStandardReplacements(DateTimeConsistentShiftMaskingProvider provider,
      String badInputValue) {

    assertEquals(badInputValue, provider.applyOffsetAndReformat("value-abc", 24, null));
    assertEquals(badInputValue, provider.applyOffsetAndReformat("", 24, null));
    assertEquals(badInputValue, provider.applyOffsetAndReformat("13", 24, null));

    assertEquals("2020-02-29T01:02:03+10:30",
        provider.applyOffsetAndReformat("2021-03-01T01:02:03+10:30", -366, null));
    assertEquals("2020-02-29T14:16:18+10:30",
        provider.applyOffsetAndReformat("2019-02-28T14:16:18+10:30", 366, null));
    assertEquals("2020-02-29T01:02:03Z",
        provider.applyOffsetAndReformat("2020-03-01T01:02:03Z", -1, null));
    assertEquals("2020-10-31T15:16:17-05:00",
        provider.applyOffsetAndReformat("2020-11-01T15:16:17-05:00", -1, null));
    assertEquals("2020-10-31T02:04:06-05:00",
        provider.applyOffsetAndReformat("2020-11-01T02:04:06-05:00", -1, null));
    assertEquals("2020-10-31T01:03:05-05:00",
        provider.applyOffsetAndReformat("2020-11-01T01:03:05-05:00", -1, null));
    assertEquals("2020-03-08T01:10:20-05:00",
        provider.applyOffsetAndReformat("2020-03-09T01:10:20-05:00", -1, null));
    assertEquals("2020-03-08T02:10:20-05:00",
        provider.applyOffsetAndReformat("2020-03-09T02:10:20-05:00", -1, null));
    assertEquals("2020-03-08T03:10:20-05:00",
        provider.applyOffsetAndReformat("2020-03-11T03:10:20-05:00", -3, null));
    assertEquals("2020-03-08T02:03:04-05:00",
        provider.applyOffsetAndReformat("2020-03-09T02:03:04-05:00", -1, null));
    assertEquals("2020-02-29T01:02:03-01:00",
        provider.applyOffsetAndReformat("2020-03-01T01:02:03-01:00", -1, null));
    assertEquals("2020-02-29T01:02:03-01:00",
        provider.applyOffsetAndReformat("2020-03-01T01:02:03-01:00", -1, null));
    assertEquals("2020-02-29T01:02:03-01:00",
        provider.applyOffsetAndReformat("2020-03-01T01:02:03-01:00", -1, null));
    assertEquals("2020-02-29T01:02:00-01:00",
        provider.applyOffsetAndReformat("2020-03-01T01:02-01:00", -1, null));
    assertEquals("2019-12-29T01:02:03+03:00",
        provider.applyOffsetAndReformat("2020-03-01T01:02:03+03:00", -63, null));

    assertEquals(badInputValue, provider.applyOffsetAndReformat("2020-03-01T01-01:00", -1, null));
    assertEquals(badInputValue, provider.applyOffsetAndReformat("2020-03-01T01:-01:00", -1, null));

    // offset is not optional in builtin ISO pattern
    assertEquals(badInputValue, provider.applyOffsetAndReformat("2020-03-01T01:02:03", -1, null));

    assertEquals("2020-02-29", provider.applyOffsetAndReformat("2020-03-01", -1, null));
    assertEquals("2020-01-03", provider.applyOffsetAndReformat("2020-01-03", 0, null));
    assertEquals("2019-12-24", provider.applyOffsetAndReformat("2020-01-03", -10, null));
    assertEquals("2019-12-31", provider.applyOffsetAndReformat("2020-03-01", -61, null));
    assertEquals("2020-01-03", provider.applyOffsetAndReformat("2019-12-24", 10, null));
    assertEquals("2020-03-01", provider.applyOffsetAndReformat("2019-12-31", 61, null));

    assertEquals("2020/02/29", provider.applyOffsetAndReformat("2020/03/01", -1, null));
    assertEquals("2020/01/03", provider.applyOffsetAndReformat("2020/01/03", 0, null));
    assertEquals("2019/12/24", provider.applyOffsetAndReformat("2020/01/03", -10, null));
    assertEquals("2019/12/31", provider.applyOffsetAndReformat("2020/03/01", -61, null));
    assertEquals("2020/01/03", provider.applyOffsetAndReformat("2019/12/24", 10, null));
    assertEquals("2020/03/01", provider.applyOffsetAndReformat("2019/12/31", 61, null));

    assertEquals("2020-02-29 13:14:15",
        provider.applyOffsetAndReformat("2020-03-02 13:14:15", -2, null));
    assertEquals("2020-02-29 13:14:15",
        provider.applyOffsetAndReformat("2020-03-01 13:14:15", -1, null));
    assertEquals("2020-02-29 13:14:15",
        provider.applyOffsetAndReformat("2019-12-31 13:14:15", 60, null));

    assertEquals(badInputValue, provider.applyOffsetAndReformat("2020-03-01 3:4:5", -1, null));

    assertEquals("2020/02/29 13:14:15",
        provider.applyOffsetAndReformat("2020/02/19 13:14:15", 10, null));
    assertEquals("2020/02/29 13:14:15",
        provider.applyOffsetAndReformat("2020/03/10 13:14:15", -10, null));

    assertEquals(badInputValue, provider.applyOffsetAndReformat("2020/03/01 3:4:5", -1, null));

    assertEquals("16/04/1967", provider.applyOffsetAndReformat("16/04/1965", 730, null));
    assertEquals("16-04-1967", provider.applyOffsetAndReformat("16-04-1968", -366, null));
    assertEquals("16/04/1967 13:14:15",
        provider.applyOffsetAndReformat("16/04/1965 13:14:15", 730, null));
    assertEquals("16-04-1967 02:04:06",
        provider.applyOffsetAndReformat("16-04-1968 02:04:06", -366, null));

    assertEquals(badInputValue, provider.applyOffsetAndReformat("04-16-1967", 1, null));
    assertEquals(badInputValue, provider.applyOffsetAndReformat("16-04-1967 14:16", 1, null));
    assertEquals(badInputValue, provider.applyOffsetAndReformat("04/16/1967", 1, null));
    assertEquals(badInputValue, provider.applyOffsetAndReformat("16/04/1967 14", 1, null));

    String[] abbreviations = getMonthAbrvs(); // 0-based indexing for month names

    StringBuilder buffer = new StringBuilder(40);
    buffer.append("29-").append(abbreviations[3]).append("-1967");
    String original = buffer.toString();
    buffer.setLength(0);
    buffer.append("01-").append(abbreviations[4]).append("-1967");
    String expected = buffer.toString();
    assertEquals(expected, provider.applyOffsetAndReformat(original, 2, null));
    assertEquals(expected.toUpperCase(),
        provider.applyOffsetAndReformat(original.toUpperCase(), 2, null));
    assertEquals(expected.toLowerCase(),
        provider.applyOffsetAndReformat(original.toLowerCase(), 2, null));

    buffer.setLength(0);
    buffer.append("02-").append(abbreviations[5]).append("-1967");
    expected = buffer.toString();
    assertEquals(expected, provider.applyOffsetAndReformat(original, 34, null));
    assertEquals(expected.toUpperCase(),
        provider.applyOffsetAndReformat(original.toUpperCase(), 34, null));
    assertEquals(expected.toLowerCase(),
        provider.applyOffsetAndReformat(original.toLowerCase(), 34, null));

    assertEquals(badInputValue, provider.applyOffsetAndReformat("29-apx-1967", 33, null));
  }

  @Test
  public void testApplyOffsetAndReformat_custom() {
    DateTimeConsistentShiftMaskingProviderConfig config =
        new DateTimeConsistentShiftMaskingProviderConfig();
    config.setPatientIdentifierPath("/id");
    config.setUnexpectedInputHandling(UnexpectedMaskingInputHandler.MESSAGE);
    config.setUnexpectedInputReturnMessage("Bad");
    config.setCustomFormats(
        Arrays.asList("yyyy-MM-dd'T'HH:mm:ssXXX'['VV']'", "MM-dd-yy", "yyDDD",
            "yyyy-MM-dd'T'HH:mm:ss z"));
    DateTimeConsistentShiftMaskingProvider provider =
        new DateTimeConsistentShiftMaskingProvider(config, null);

    verifyStandardReplacements(provider, "Bad");

    List<DateTimeFormatter> customFormatters = provider.buildCustomFormatters();

    // ---------------------------------------------------------------------------
    // 08 MAR 2020 is start of daylight savings time in America/Chicago (CST/CDT)
    // 01 NOV 2020 is end of daylight savings time in America/Chicago (CST/CDT)
    // ---------------------------------------------------------------------------

    // When an offset and a zone ID are both supplied, the behavior is different
    // between Java 8 and Java 11.  Java 11 javadoc appears to indicate the 
    // offset will be used in preference to the zone ID.  The tests here do not 
    // include that type of input conflict.

    assertEquals("2020-10-31T05:06:07-05:00[America/Chicago]", provider.applyOffsetAndReformat(
        "2020-11-01T05:06:07-06:00[America/Chicago]", -1, customFormatters));
    assertEquals("2020-11-01T01:06:07-06:00[America/Chicago]",
        provider.applyOffsetAndReformat("2020-11-04T01:06:07-06:00[America/Chicago]", -3,
            customFormatters));
    assertEquals("2020-11-01T01:06:07-05:00[America/Chicago]",
        provider.applyOffsetAndReformat("2020-10-27T01:06:07-05:00[America/Chicago]", 5,
            customFormatters));
    assertEquals("2020-11-01T02:06:07-06:00[America/Chicago]",
        provider.applyOffsetAndReformat("2020-10-27T02:06:07-05:00[America/Chicago]", 5,
            customFormatters));
    assertEquals("2020-05-01T13:14:15-05:00[America/Chicago]",
        provider.applyOffsetAndReformat("2020-04-01T13:14:15-05:00[America/Chicago]", 30,
            customFormatters));
    
    // only do these tests if in English in the US or Canada - otherwise the legacy 
    // time zone names might be interpreted differently
    String lang = Locale.getDefault().getLanguage();
    String region = Locale.getDefault().getCountry();
    if ("en".equals(lang) && ("US".equals(region) || "CA".equals(region))) {
      assertEquals("2020-10-31T05:06:07 CDT",
          provider.applyOffsetAndReformat("2020-11-01T05:06:07 CDT", -1, customFormatters));
      // pattern is for short zone name
      assertEquals("2020-11-03T05:06:07 CST",
          provider.applyOffsetAndReformat("2020-11-01T05:06:07 America/Chicago", 2,
              customFormatters));
      // target day is 23 hours
      assertEquals("2020-03-08T03:03:04 CDT",
          provider.applyOffsetAndReformat("2020-03-11T02:03:04 CDT", -3, customFormatters));
      // target day is 23 hours
      assertEquals("2020-03-08T01:03:04 CST",
          provider.applyOffsetAndReformat("2020-03-01T02:03:04 America/Chicago", 7,
              customFormatters));
      // target day is 23 hours, but target hour exists
      assertEquals("2020-03-08T14:03:04 CDT",
          provider.applyOffsetAndReformat("2020-03-01T14:03:04 America/Chicago", 7,
              customFormatters));
    }
    
    assertEquals("99355", provider.applyOffsetAndReformat("99365", -10, customFormatters));
    assertEquals("98365", provider.applyOffsetAndReformat("99365", -365, customFormatters));
    assertEquals("00001", provider.applyOffsetAndReformat("99365", 1, customFormatters));
  }

  @Test
  public void testApplyOffsetAndReformat_customError() {
    String badInputValue = "Bad Pattern";
    DateTimeConsistentShiftMaskingProviderConfig config =
        new DateTimeConsistentShiftMaskingProviderConfig();
    config.setPatientIdentifierPath("/id");
    config.setUnexpectedInputHandling(UnexpectedMaskingInputHandler.MESSAGE);
    config.setUnexpectedInputReturnMessage(badInputValue);
    config.setCustomFormats(
        Arrays.asList("yyyy-MM-dd'T'HH:mm:ssXXX'['VV']'", "MM-dd-yy", "yyyy-MM-dd'T'HH:mm:ss z", "MMDDD"));
    DateTimeConsistentShiftMaskingProvider provider =
        new DateTimeConsistentShiftMaskingProvider(config, null);

    verifyStandardReplacements(provider, badInputValue);

    List<DateTimeFormatter> customFormatters = provider.buildCustomFormatters();

    try {
      provider.applyOffsetAndReformat("10333", -1, customFormatters);
      fail("expected exception");
    } catch (IllegalArgumentException e) {
      System.out.println(e.getMessage());
      assertTrue(e.getMessage().startsWith(LogCodes.WPH1025W));
      assertTrue(e.getMessage().contains("`MMDDD`"));
    }
  }

  @Test
  public void testGenerateShiftNumberOfDays_before() {
    DateTimeConsistentShiftMaskingProviderConfig config =
        new DateTimeConsistentShiftMaskingProviderConfig();
    config.setPatientIdentifierPath("/id");
    config.setDateShiftDirection(DateShiftDirection.BEFORE);
    config.setDateShiftMinimumDays(1);
    config.setDateShiftMaximumDays(10);
    TestDateTimeConsistentShiftMaskingProvider provider =
        new TestDateTimeConsistentShiftMaskingProvider(config);

    int target = -10;
    for (provider.longFromSeed = 0L; provider.longFromSeed < 30L; provider.longFromSeed++) {
      assertEquals(target, provider.generateShiftNumberOfDays(""));
      target = target == -1 ? -10 : target + 1;
    }
    
    provider.longFromSeed = 1000000003L;
    assertEquals(-7, provider.generateShiftNumberOfDays(""));

    config.setDateShiftMinimumDays(0);
    provider = new TestDateTimeConsistentShiftMaskingProvider(config);

    target = -10;
    for (provider.longFromSeed = 0L; provider.longFromSeed < 33L; provider.longFromSeed++) {
      assertEquals(target, provider.generateShiftNumberOfDays(""));
      target = target == 0 ? -10 : target + 1;
    }

    config.setDateShiftMaximumDays(0);
    provider = new TestDateTimeConsistentShiftMaskingProvider(config);

    for (provider.longFromSeed = 0L; provider.longFromSeed < 20L; provider.longFromSeed++) {
      assertEquals(0, provider.generateShiftNumberOfDays(""));
    }

    config.setDateShiftMinimumDays(2);
    config.setDateShiftMaximumDays(2);
    provider = new TestDateTimeConsistentShiftMaskingProvider(config);

    for (provider.longFromSeed = 0L; provider.longFromSeed < 20L; provider.longFromSeed++) {
      assertEquals(-2, provider.generateShiftNumberOfDays(""));
    }
  }

  @Test
  public void testGenerateShiftNumberOfDays_after() {
    DateTimeConsistentShiftMaskingProviderConfig config =
        new DateTimeConsistentShiftMaskingProviderConfig();
    config.setPatientIdentifierPath("/id");
    config.setDateShiftDirection(DateShiftDirection.AFTER);
    config.setDateShiftMinimumDays(1);
    config.setDateShiftMaximumDays(10);
    TestDateTimeConsistentShiftMaskingProvider provider =
        new TestDateTimeConsistentShiftMaskingProvider(config);

    int target = 1;
    for (provider.longFromSeed = 0L; provider.longFromSeed < 30L; provider.longFromSeed++) {
      assertEquals(target, provider.generateShiftNumberOfDays(""));
      target = target == 10 ? 1 : target + 1;
    }

    provider.longFromSeed = 1000000003L;
    assertEquals(4, provider.generateShiftNumberOfDays(""));

    config.setDateShiftMinimumDays(0);
    provider = new TestDateTimeConsistentShiftMaskingProvider(config);

    target = 0;
    for (provider.longFromSeed = 0L; provider.longFromSeed < 33L; provider.longFromSeed++) {
      assertEquals(target, provider.generateShiftNumberOfDays(""));
      target = target == 10 ? 0 : target + 1;
    }

    config.setDateShiftMaximumDays(0);
    provider = new TestDateTimeConsistentShiftMaskingProvider(config);

    for (provider.longFromSeed = 0L; provider.longFromSeed < 20L; provider.longFromSeed++) {
      assertEquals(0, provider.generateShiftNumberOfDays(""));
    }

    config.setDateShiftMinimumDays(3);
    config.setDateShiftMaximumDays(3);
    provider = new TestDateTimeConsistentShiftMaskingProvider(config);

    for (provider.longFromSeed = 0L; provider.longFromSeed < 20L; provider.longFromSeed++) {
      assertEquals(3, provider.generateShiftNumberOfDays(""));
    }
  }

  @Test
  public void testGenerateShiftNumberOfDays_beforeOrAfter() {
    DateTimeConsistentShiftMaskingProviderConfig config =
        new DateTimeConsistentShiftMaskingProviderConfig();
    config.setPatientIdentifierPath("/id");
    config.setDateShiftDirection(DateShiftDirection.BEFORE_OR_AFTER);
    config.setDateShiftMinimumDays(1);
    config.setDateShiftMaximumDays(10);
    TestDateTimeConsistentShiftMaskingProvider provider =
        new TestDateTimeConsistentShiftMaskingProvider(config);

    int target = -10;
    for (provider.longFromSeed = 0L; provider.longFromSeed < 60L; provider.longFromSeed++) {
      assertEquals(target, provider.generateShiftNumberOfDays(""));
      if (target == 10) {
        target = -10;
      } else if (target == -1) {
        target = 1;
      } else {
        target++;
      }
    }

    provider.longFromSeed = 1000000003L;
    assertEquals(-7, provider.generateShiftNumberOfDays(""));
    provider.longFromSeed = 1000000000013L;
    assertEquals(4, provider.generateShiftNumberOfDays(""));

    config.setDateShiftMinimumDays(0);
    provider = new TestDateTimeConsistentShiftMaskingProvider(config);

    target = -10;
    for (provider.longFromSeed = 0L; provider.longFromSeed < 63L; provider.longFromSeed++) {
      assertEquals(target, provider.generateShiftNumberOfDays(""));
      target = target == 10 ? -10 : target + 1;
    }

    config.setDateShiftMaximumDays(0);
    provider = new TestDateTimeConsistentShiftMaskingProvider(config);

    for (provider.longFromSeed = 0L; provider.longFromSeed < 20L; provider.longFromSeed++) {
      assertEquals(0, provider.generateShiftNumberOfDays(""));
    }

    config.setDateShiftMinimumDays(3);
    config.setDateShiftMaximumDays(3);
    provider = new TestDateTimeConsistentShiftMaskingProvider(config);

    target = -3;
    for (provider.longFromSeed = 0L; provider.longFromSeed < 20L; provider.longFromSeed++) {
      assertEquals(target, provider.generateShiftNumberOfDays(""));
      target *= -1;
    }
  }

  @Test
  public void testGenerateLongFromString() {
    DateTimeConsistentShiftMaskingProviderConfig config =
        new DateTimeConsistentShiftMaskingProviderConfig();
    config.setPatientIdentifierPath("/id");
    config.setSalt(null);
    DateTimeConsistentShiftMaskingProvider provider =
        new DateTimeConsistentShiftMaskingProvider(config, null);

    // all values generated should always be the same at any time on
    // any system for the consistent input used in this test

    assertEquals(5794991048039723396L, verifyRepeatable(provider, null));

    config.setSalt("");
    provider = new DateTimeConsistentShiftMaskingProvider(config, null);

    assertEquals(5794991048039723396L, verifyRepeatable(provider, ""));

    config.setSalt("    ");
    provider = new DateTimeConsistentShiftMaskingProvider(config, null);

    assertEquals(5794991048039723396L, verifyRepeatable(provider, "   "));

    config.setSalt("THIS is a random value");
    provider = new DateTimeConsistentShiftMaskingProvider(config, null);
    
    assertEquals(2592115912263418881L, verifyRepeatable(provider, "   "));


    config.setSalt("");
    provider = new DateTimeConsistentShiftMaskingProvider(config, null);

    assertEquals(8246779727611716507L, verifyRepeatable(provider, "patient1"));
    assertEquals(8246779727611716507L, verifyRepeatable(provider, " PATIENT1 "));

    config.setSalt("THIS is a random value");
    provider = new DateTimeConsistentShiftMaskingProvider(config, null);

    assertEquals(1812857385794692832L, verifyRepeatable(provider, " PATIENT1 "));

    config.setSalt("THIS is a random value ");
    provider = new DateTimeConsistentShiftMaskingProvider(config, null);

    assertEquals(1705215707037854198L, verifyRepeatable(provider, " PATIENT1 "));

    config.setSalt("");
    provider = new DateTimeConsistentShiftMaskingProvider(config, null);

    String input = " now is the time to check special chars !@#$%^&*()-_+=/\\ ";
    assertEquals(6097152143258431240L, verifyRepeatable(provider, input));
    assertEquals(6097152143258431240L, verifyRepeatable(provider, input.trim().toUpperCase()));
  }

  private long verifyRepeatable(DateTimeConsistentShiftMaskingProvider provider, String input) {
    long value = provider.generateLongFromString(input);
    for (int i = 0; i < 10; i++) {
      assertEquals(value, provider.generateLongFromString(input));
    }
    return value;
  }

  @Test
  public void testGetPatientIdentifier() throws Exception {
    String jsonstr =
        "{\"a\": \"z\", \"b\": {\"one\": 1, \"two\": 2, \"three\": 3, \"four\": null}, \"d\": [6,7,8]}";
    JsonNode root = ObjectMapperFactory.getObjectMapper().readTree(jsonstr);
    JsonNode parent = root.get("b");
    JsonNode target = parent.get("three");

    DateTimeConsistentShiftMaskingProviderConfig config =
        new DateTimeConsistentShiftMaskingProviderConfig();
    config.setPatientIdentifierPath("/a");
    DateTimeConsistentShiftMaskingProvider provider =
        new DateTimeConsistentShiftMaskingProvider(config, null);

    MaskingActionInputIdentifier identifier =
        new MaskingActionInputIdentifier(provider, target, parent, "three", "type", "id", root);

    assertEquals("z", provider.getPatientIdentifier(identifier));

    config.setPatientIdentifierPath("/c");
    provider = new DateTimeConsistentShiftMaskingProvider(config, null);
    identifier =
        new MaskingActionInputIdentifier(provider, target, parent, "three", "type", "id", root);

    assertEquals("", provider.getPatientIdentifier(identifier));

    config.setPatientIdentifierPath("/d/2");
    provider = new DateTimeConsistentShiftMaskingProvider(config, null);
    identifier =
        new MaskingActionInputIdentifier(provider, target, parent, "three", "type", "id", root);

    assertEquals("8", provider.getPatientIdentifier(identifier));

    config.setPatientIdentifierPath("/d/4");
    provider = new DateTimeConsistentShiftMaskingProvider(config, null);
    identifier =
        new MaskingActionInputIdentifier(provider, target, parent, "three", "type", "id", root);

    assertEquals("", provider.getPatientIdentifier(identifier));

    config.setPatientIdentifierPath("b/two");
    provider = new DateTimeConsistentShiftMaskingProvider(config, null);
    identifier =
        new MaskingActionInputIdentifier(provider, target, parent, "three", "type", "id", root);

    assertEquals("2", provider.getPatientIdentifier(identifier));

    config.setPatientIdentifierPath("b/four");
    provider = new DateTimeConsistentShiftMaskingProvider(config, null);
    identifier =
        new MaskingActionInputIdentifier(provider, target, parent, "three", "type", "id", root);

    assertEquals("", provider.getPatientIdentifier(identifier));
  }

  @Test
  public void testGenerateReplacement_errors() throws Exception {
    String jsonstr =
        "{\"a\": \"z\", \"b\": {\"one\": \"20-01-02\", \"two\": 2, \"three\": 3, \"four\": null, \"five\": \" \"}, \"d\": [6,7,8]}";
    JsonNode root = ObjectMapperFactory.getObjectMapper().readTree(jsonstr);
    JsonNode parent = root.get("b");
    JsonNode target = parent.get("one");

    DateTimeConsistentShiftMaskingProviderConfig config =
        new DateTimeConsistentShiftMaskingProviderConfig();
    config.setUnexpectedInputHandling(UnexpectedMaskingInputHandler.ERROR_EXIT);
    config.setPatientIdentifierPath("/b/four");  // = null, invalid
    DateTimeConsistentShiftMaskingProvider provider =
        new DateTimeConsistentShiftMaskingProvider(config, null);
    provider.setName("name-XX");
    MaskingActionInputIdentifier identifier =
        new MaskingActionInputIdentifier(provider, target, parent, "three", "type", "id", root);

    try {
      provider.generateReplacement(identifier, null);
      fail("expected exception");
    } catch (PrivacyProviderInvalidInputException e) {
      assertTrue(e.getMessage().contains("name-XX"));
      assertFalse(e.getMessage().contains("`patient identifier ```"));
    }

    config = new DateTimeConsistentShiftMaskingProviderConfig();
    config.setUnexpectedInputHandling(UnexpectedMaskingInputHandler.ERROR_EXIT);
    config.setPatientIdentifierPath("/a");  // = z, valid
    provider = new DateTimeConsistentShiftMaskingProvider(config, null);
    provider.setName("name-X2");
    identifier =
        new MaskingActionInputIdentifier(provider, target, parent, "three", "type", "id", root);

    try {
      provider.generateReplacement(identifier, null);
      fail("expected exception");
    } catch (PrivacyProviderInvalidInputException e) {
      assertTrue(e.getMessage().contains("name-X2"));
      assertFalse(e.getMessage().contains("`20-01-02`"));
    }

    target = parent.get("five");
    identifier =
        new MaskingActionInputIdentifier(provider, target, parent, "three", "type", "id", root);
    try {
      provider.generateReplacement(identifier, null);
      fail("expected exception");
    } catch (PrivacyProviderInvalidInputException e) {
      assertTrue(e.getMessage().contains("name-X2"));
      assertFalse(e.getMessage().contains("` `"));
    }
  }

  @Test
  public void testMain_happy() throws Exception {
    DateTimeConsistentShiftMaskingProviderConfig config =
        new DateTimeConsistentShiftMaskingProviderConfig();
    config.setUnexpectedInputHandling(UnexpectedMaskingInputHandler.ERROR_EXIT);
    config.setPatientIdentifierPath("/a");
    config.setDateShiftDirection(DateShiftDirection.AFTER);
    config.setDateShiftMinimumDays(2);
    config.setDateShiftMaximumDays(5);
    DateTimeConsistentShiftMaskingProvider provider =
        new DateTimeConsistentShiftMaskingProvider(config, null);
    provider.setName("name-main");

    List<MaskingActionInputIdentifier> list = new ArrayList<>();
    list.add(buildMaskingIdentifier(
        "{\"a\": \"patient1\", \"b\": {\"one\": \"16-04-2021\", \"two\": 2, \"three\": 3, \"four\": null, \"five\": \" \"}, \"d\": [6,7,8]}",
        provider));
    list.add(buildMaskingIdentifier(
        "{\"a\": \"Patient1\", \"b\": {\"one\": \"16-04-2021\", \"two\": 2, \"three\": 3, \"four\": null, \"five\": \" \"}, \"d\": [6,7,8]}",
        provider));
    list.add(buildMaskingIdentifier(
        "{\"a\": \"patient2x\", \"b\": {\"one\": \"16-04-2021\", \"two\": 2, \"three\": 3, \"four\": null, \"five\": \" \"}, \"d\": [6,7,8]}",
        provider));
    list.add(buildMaskingIdentifier(
        "{\"a\": \"Patient2x\", \"b\": {\"one\": \"16-04-2021\", \"two\": 2, \"three\": 3, \"four\": null, \"five\": \" \"}, \"d\": [6,7,8]}",
        provider));
    list.add(buildMaskingIdentifier(
        "{\"a\": \" patient1 \", \"b\": {\"one\": \"16-04-2021\", \"two\": 2, \"three\": 3, \"four\": null, \"five\": \" \"}, \"d\": [6,7,8]}",
        provider));

    provider.maskIdentifierBatch(list);

    List<String> possibles = Arrays.asList("18-04-2021", "19-04-2021", "20-04-2021", "21-04-2021");

    String result1 = list.get(0).getCurrentNode().asText();
    assertTrue(result1, possibles.contains(result1));
    System.out.println(result1);
    assertEquals(result1, list.get(1).getCurrentNode().asText());
    assertEquals(result1, list.get(4).getCurrentNode().asText());

    String result2 = list.get(2).getCurrentNode().asText();
    System.out.println(result2);
    assertTrue(result2, possibles.contains(result2));
    assertEquals(result2, list.get(3).getCurrentNode().asText());
  }

  private MaskingActionInputIdentifier buildMaskingIdentifier(String jsonstr,
      DateTimeConsistentShiftMaskingProvider provider) throws Exception {
    JsonNode root = ObjectMapperFactory.getObjectMapper().readTree(jsonstr);
    JsonNode parent = root.get("b");
    JsonNode target = parent.get("one");
    return new MaskingActionInputIdentifier(provider, target, parent, "three", "type", "id", root);
  }

  /**
   * Gets the month abbreviations in the default locale, which can be different for each caller.
   */
  private String[] getMonthAbrvs() {
    String[] abrvs = new String[12];
    DateTimeFormatter f = new DateTimeFormatterBuilder().appendPattern("MMM").toFormatter();
    for (int i = 0; i < 12; i++) {
      abrvs[i] = f.format(LocalDate.of(2022, i + 1, 20));
    }
    return abrvs;
  }
}
