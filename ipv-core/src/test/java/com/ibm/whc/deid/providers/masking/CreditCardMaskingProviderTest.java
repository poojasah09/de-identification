/*
 * (C) Copyright IBM Corp. 2016,2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.whc.deid.providers.masking;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Ignore;
import org.junit.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.whc.deid.providers.identifiers.CreditCardIdentifier;
import com.ibm.whc.deid.providers.identifiers.Identifier;
import com.ibm.whc.deid.shared.pojo.config.masking.CreditCardMaskingProviderConfig;
import com.ibm.whc.deid.shared.pojo.config.masking.UnexpectedMaskingInputHandler;
import com.ibm.whc.deid.shared.pojo.masking.MaskingProviderType;
import com.ibm.whc.deid.util.localization.LocalizationManager;

public class CreditCardMaskingProviderTest extends TestLogSetUp implements MaskingProviderTest {

  private String localizationProperty = LocalizationManager.DEFAULT_LOCALIZATION_PROPERTIES;

  /*
   * Tests for various credit card number, format, length, and pattern. Also, test for preserve
   * issuer option and its boolean values (true and false).
   */
  @Test
  public void testMask() throws Exception {
    CreditCardMaskingProviderConfig defaultMaskingConfiguration =
        new CreditCardMaskingProviderConfig();
    CreditCardMaskingProvider ccMaskingProvider =
        new CreditCardMaskingProvider(defaultMaskingConfiguration, tenantId, localizationProperty);

    // different values
    assertNotEquals("123456789", ccMaskingProvider.mask("123456789"));
    assertFalse("123456789".equals(ccMaskingProvider.mask("123456789")));

    String test = "1234-1234-1234-1234";
    String res = ccMaskingProvider.mask(test);

    // same length
    assertEquals(test.length(), res.length());

    // same pattern
    for (int i = 0; i < test.length(); ++i) {
      assertTrue(Character.isDigit(res.charAt(i)) == Character.isDigit(test.charAt(i)));
    }
  }

  @Ignore
  @Test
  public void testPerformance() {

    CreditCardMaskingProviderConfig defaultMaskingConfiguration =
        new CreditCardMaskingProviderConfig();
    CreditCardMaskingProviderConfig nopreserveMaskingConfiguration =
        new CreditCardMaskingProviderConfig();
    nopreserveMaskingConfiguration.setIssuerPreserve(false);

    CreditCardMaskingProviderConfig[] maskingConfigurations =
        {defaultMaskingConfiguration, nopreserveMaskingConfiguration};

    for (CreditCardMaskingProviderConfig maskingConfiguration : maskingConfigurations) {
      CreditCardMaskingProvider maskingProvider =
          new CreditCardMaskingProvider(maskingConfiguration, tenantId, localizationProperty);

      int N = 1000000;
      String originalCC = "5523527012345678";

      long startMillis = System.currentTimeMillis();

      for (int i = 0; i < N; i++) {
        maskingProvider.mask(originalCC);
      }

      long diff = System.currentTimeMillis() - startMillis;
      System.out.println(String.format("%d operations took %d milliseconds (%f per op)", N, diff,
          (double) diff / N));
      // Assert test always should finish in less than 10 seconds
      assertTrue(diff < 10000);
    }
  }

  @Test
  public void testMaskNullCreditCardInputReturnNull() throws Exception {
    CreditCardMaskingProviderConfig maskingConfiguration = new CreditCardMaskingProviderConfig();

    ObjectMapper objectMapper = new ObjectMapper();
    String str = objectMapper.writeValueAsString(maskingConfiguration);

    CreditCardMaskingProviderConfig config =
        objectMapper.readValue(str, CreditCardMaskingProviderConfig.class);

    assertEquals(MaskingProviderType.CREDIT_CARD, config.getType());

    MaskingProvider maskingProvider =
        new CreditCardMaskingProvider(config, tenantId, localizationProperty);

    String invalidCreditCard = null;
    String maskedCreditCard = maskingProvider.mask(invalidCreditCard);

    assertEquals(null, maskedCreditCard);
    assertTrue(outContent.toString().contains("DEBUG - WPH1015D"));
  }

  @Test
  public void testMaskInvalidCreditCardInputValidHandlingReturnNull() throws Exception {
    CreditCardMaskingProviderConfig maskingConfiguration = new CreditCardMaskingProviderConfig();
    maskingConfiguration.setUnexpectedInputHandling(UnexpectedMaskingInputHandler.NULL);
    MaskingProvider maskingProvider =
        new CreditCardMaskingProvider(maskingConfiguration, tenantId, localizationProperty);

    String invalidCreditCard = "Invalid Credit Card";
    String maskedCreditCard = maskingProvider.mask(invalidCreditCard);

    assertEquals(null, maskedCreditCard);
    assertTrue(outContent.toString().contains("DEBUG - WPH1015D"));
  }

  @Test
  public void testMaskInvalidCreditCardInputValidHandlingReturnRandom() throws Exception {
    CreditCardMaskingProviderConfig maskingConfiguration = new CreditCardMaskingProviderConfig();
    maskingConfiguration.setUnexpectedInputHandling(UnexpectedMaskingInputHandler.RANDOM);
    MaskingProvider maskingProvider =
        new CreditCardMaskingProvider(maskingConfiguration, tenantId, localizationProperty);
    Identifier identifier = new CreditCardIdentifier();

    String invalidCreditCard = "Invalid Credit Card";
    String maskedCreditCard = maskingProvider.mask(invalidCreditCard);

    assertFalse(maskedCreditCard.equals(invalidCreditCard));
    assertTrue(identifier.isOfThisType(maskedCreditCard));
    assertTrue(outContent.toString().contains("DEBUG - WPH1015D"));
  }

  @Test
  public void testMaskInvalidCreditCardInputValidHandlingReturnDefaultCustomValue()
      throws Exception {
    CreditCardMaskingProviderConfig maskingConfiguration = new CreditCardMaskingProviderConfig();
    maskingConfiguration.setUnexpectedInputHandling(UnexpectedMaskingInputHandler.MESSAGE);
    MaskingProvider maskingProvider =
        new CreditCardMaskingProvider(maskingConfiguration, tenantId, localizationProperty);

    String invalidCreditCard = "Invalid Credit Card";
    String maskedCreditCard = maskingProvider.mask(invalidCreditCard);

    assertEquals("OTHER", maskedCreditCard);
    assertTrue(outContent.toString().contains("DEBUG - WPH1015D"));
  }

  @Test
  public void testMaskInvalidCreditCardInputValidHandlingReturnNonDefaultCustomValue()
      throws Exception {
    CreditCardMaskingProviderConfig maskingConfiguration = new CreditCardMaskingProviderConfig();
    maskingConfiguration.setUnexpectedInputHandling(UnexpectedMaskingInputHandler.MESSAGE);
    maskingConfiguration.setUnexpectedInputReturnMessage("Test Credit Card");
    MaskingProvider maskingProvider =
        new CreditCardMaskingProvider(maskingConfiguration, tenantId, localizationProperty);

    String invalidCreditCard = "Invalid Credit Card";
    String maskedCreditCard = maskingProvider.mask(invalidCreditCard);

    assertEquals("Test Credit Card", maskedCreditCard);
    assertTrue(outContent.toString().contains("DEBUG - WPH1015D"));
  }

  @Test
  public void testPreservesIssuer() throws Exception {
    CreditCardMaskingProviderConfig defaultMaskingConfiguration =
        new CreditCardMaskingProviderConfig();
    CreditCardMaskingProvider ccMaskingProvider =
        new CreditCardMaskingProvider(defaultMaskingConfiguration, tenantId, localizationProperty);

    CreditCardIdentifier identifier = new CreditCardIdentifier();

    String originalCC = "5584637593005095";
    String maskedCC = ccMaskingProvider.mask(originalCC);

    assertTrue(originalCC.length() == maskedCC.length());
    assertFalse(originalCC.equals(maskedCC));

    assertTrue(maskedCC.startsWith("558463"));
    assertTrue(identifier.isOfThisType(maskedCC));
  }

  @Test
  public void testNoIssuer() throws Exception {
    CreditCardMaskingProviderConfig maskingConfiguration = new CreditCardMaskingProviderConfig();
    maskingConfiguration.setIssuerPreserve(false);

    CreditCardMaskingProvider ccMaskingProvider =
        new CreditCardMaskingProvider(maskingConfiguration, tenantId, localizationProperty);
    CreditCardIdentifier identifier = new CreditCardIdentifier();

    String originalCC = "5584637593005095";

    for (int i = 0; i < 100; i++) {
      String maskedCC = ccMaskingProvider.mask(originalCC);
      assertFalse(originalCC.equals(maskedCC));
      assertFalse(maskedCC.startsWith("558463"));
      assertTrue(identifier.isOfThisType(maskedCC));
    }
  }
}
