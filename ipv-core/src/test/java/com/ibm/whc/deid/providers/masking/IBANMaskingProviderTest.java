/*
 * (C) Copyright IBM Corp. 2016,2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.whc.deid.providers.masking;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import com.ibm.whc.deid.providers.identifiers.IBANIdentifier;
import com.ibm.whc.deid.providers.identifiers.Identifier;
import com.ibm.whc.deid.shared.pojo.config.masking.IBANMaskingProviderConfig;
import com.ibm.whc.deid.shared.pojo.config.masking.UnexpectedMaskingInputHandler;

public class IBANMaskingProviderTest extends TestLogSetUp {

  /*
   * Tests preserve country option and its boolean values (true and false). Also tests for an
   * invalid value
   */

  @Test
  public void testMask() {
    // The preserve country option by default is set to true.
    IBANMaskingProvider maskingProvider = new IBANMaskingProvider();
    IBANIdentifier identifier = new IBANIdentifier();

    String iban = "IE71WZXH31864186813343";
    String maskedValue = maskingProvider.mask(iban);

    assertFalse(maskedValue.equals(iban));
    assertTrue(identifier.isOfThisType(maskedValue));
    // by default we preserve the country
    assertTrue(maskedValue.startsWith("IE"));
  }

  @Test
  public void testMaskNoCountryPreservation() {
    IBANMaskingProviderConfig configuration = new IBANMaskingProviderConfig();
    configuration.setMaskPreserveCountry(false);
    IBANMaskingProvider maskingProvider = new IBANMaskingProvider(configuration);
    IBANIdentifier identifier = new IBANIdentifier();

    String iban = "IE71WZXH31864186813343";
    int randomizationOK = 0;
    for (int i = 0; i < 500; i++) {
      String maskedValue = maskingProvider.mask(iban);
      assertTrue(identifier.isOfThisType(maskedValue));
      if (!maskedValue.startsWith("IE")) {
        randomizationOK++;
      }
    }
    assertTrue(randomizationOK > 0);

    // input not needed to be valid if not preserving
    iban = "";
    for (int i = 0; i < 500; i++) {
      String maskedValue = maskingProvider.mask(iban);
      assertTrue(identifier.isOfThisType(maskedValue));
    }
  }

  @Test
  public void testMaskNullIBANInputReturnNull() throws Exception {
    IBANMaskingProviderConfig configuration = new IBANMaskingProviderConfig();
    MaskingProvider maskingProvider = new IBANMaskingProvider(configuration);

    String invalidIBAN = null;
    String maskedIBAN = maskingProvider.mask(invalidIBAN);

    assertEquals(null, maskedIBAN);
    assertThat(outContent.toString(), containsString("DEBUG - WPH1015D"));
  }

  @Test
  public void testMaskInvalidIBANInputValidHandlingReturnNull() throws Exception {
    IBANMaskingProviderConfig configuration = new IBANMaskingProviderConfig();
    configuration.setUnexpectedInputHandling(UnexpectedMaskingInputHandler.NULL);
    MaskingProvider maskingProvider = new IBANMaskingProvider(configuration);

    String invalidIBAN = "Invalid IBAN";
    String maskedIBAN = maskingProvider.mask(invalidIBAN);

    assertEquals(null, maskedIBAN);
    assertThat(outContent.toString(), containsString("DEBUG - WPH1015D"));
  }

  @Test
  public void testMaskInvalidIBANInputValidHandlingReturnRandomNew() throws Exception {
    IBANMaskingProviderConfig configuration = new IBANMaskingProviderConfig();
    configuration.setUnexpectedInputHandling(UnexpectedMaskingInputHandler.RANDOM);
    MaskingProvider maskingProvider = new IBANMaskingProvider(configuration);
    Identifier identifier = new IBANIdentifier();

    String invalidIBAN = "Invalid IBAN";
    String maskedIBAN = maskingProvider.mask(invalidIBAN);

    assertNotNull(maskedIBAN);
    assertNotEquals(maskedIBAN, invalidIBAN);
    assertTrue(identifier.isOfThisType(maskedIBAN));
    assertThat(outContent.toString(), containsString("DEBUG - WPH1015D"));
  }

  @Test
  public void testMaskInvalidIBANInputValidHandlingReturnDefaultCustomValue() throws Exception {
    IBANMaskingProviderConfig configuration = new IBANMaskingProviderConfig();
    configuration.setUnexpectedInputHandling(UnexpectedMaskingInputHandler.MESSAGE);
    MaskingProvider maskingProvider = new IBANMaskingProvider(configuration);

    String invalidIBAN = "Invalid IBAN";
    String maskedIBAN = maskingProvider.mask(invalidIBAN);

    assertEquals("OTHER", maskedIBAN);
    assertThat(outContent.toString(), containsString("DEBUG - WPH1015D"));
  }

  @Test
  public void testMaskInvalidIBANInputValidHandlingReturnNonDefaultCustomValue() throws Exception {
    IBANMaskingProviderConfig configuration = new IBANMaskingProviderConfig();
    configuration.setUnexpectedInputHandling(UnexpectedMaskingInputHandler.MESSAGE);
    configuration.setUnexpectedInputReturnMessage("Test IBAN");
    MaskingProvider maskingProvider = new IBANMaskingProvider(configuration);

    String invalidIBAN = "Invalid IBAN";
    String maskedIBAN = maskingProvider.mask(invalidIBAN);

    assertEquals("Test IBAN", maskedIBAN);
    assertThat(outContent.toString(), containsString("DEBUG - WPH1015D"));
  }
}
