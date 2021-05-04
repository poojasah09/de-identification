/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.whc.deid.models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

public class LatitudeLongitudeTest {

  @Test
  public void testEqualsHashcode() {
    LatitudeLongitude latlon1 = new LatitudeLongitude(90, 180, LatitudeLongitudeFormat.COMPASS);
    LatitudeLongitude latlon2 = new LatitudeLongitude(90, -180, LatitudeLongitudeFormat.DMS);
    assertTrue(latlon1.equals(latlon2));
    assertEquals(latlon1.hashCode(), latlon2.hashCode());
    
    LatitudeLongitude latlon3 = new LatitudeLongitude(-90, 180, LatitudeLongitudeFormat.DECIMAL);
    assertFalse(latlon1.equals(latlon3));
    assertFalse(latlon2.equals(latlon3));
    
    LatitudeLongitude latlon4 = new LatitudeLongitude(90, 179.99, LatitudeLongitudeFormat.DECIMAL);
    assertFalse(latlon1.equals(latlon4));
    assertFalse(latlon2.equals(latlon4));
    assertFalse(latlon3.equals(latlon4));
  }

  @Test
  public void testConstructor() {
    LatitudeLongitude latlon = new LatitudeLongitude(90, 179.99);
    assertEquals(90.0, latlon.getLatitude(), 0);
    assertEquals(179.99, latlon.getLongitude(), 0);
    assertEquals(LatitudeLongitudeFormat.DECIMAL, latlon.getFormat());

    latlon = new LatitudeLongitude(-90, -179.99, null);
    assertEquals(-90.0, latlon.getLatitude(), 0);
    assertEquals(-179.99, latlon.getLongitude(), 0);
    assertEquals(LatitudeLongitudeFormat.DECIMAL, latlon.getFormat());

    latlon = new LatitudeLongitude(-90, -179.99, LatitudeLongitudeFormat.COMPASS);
    assertEquals(-90.0, latlon.getLatitude(), 0);
    assertEquals(-179.99, latlon.getLongitude(), 0);
    assertEquals(LatitudeLongitudeFormat.COMPASS, latlon.getFormat());

    latlon = new LatitudeLongitude(0, 0, LatitudeLongitudeFormat.COMPASS);
    assertEquals(0, latlon.getLatitude(), 0);
    assertEquals(0, latlon.getLongitude(), 0);
    assertEquals(LatitudeLongitudeFormat.COMPASS, latlon.getFormat());

    try {
      new LatitudeLongitude(90.01, 180.0);
      fail("expected exception");
    } catch (IllegalArgumentException e) {
      assertEquals("The value \"90.01\" for \"latitude\" is invalid", e.getMessage());
    }

    try {
      new LatitudeLongitude(-90.01, -180.0);
      fail("expected exception");
    } catch (IllegalArgumentException e) {
      assertEquals("The value \"-90.01\" for \"latitude\" is invalid", e.getMessage());
    }

    try {
      new LatitudeLongitude(90.0, 180.01);
      fail("expected exception");
    } catch (IllegalArgumentException e) {
      assertEquals("The value \"180.01\" for \"longitude\" is invalid", e.getMessage());
    }

    try {
      new LatitudeLongitude(-90.0, -180.01);
      fail("expected exception");
    } catch (IllegalArgumentException e) {
      assertEquals("The value \"-180.01\" for \"longitude\" is invalid", e.getMessage());
    }

    latlon = new LatitudeLongitude("10", "-11");
    assertEquals(10.0, latlon.getLatitude(), 0);
    assertEquals(-11.0, latlon.getLongitude(), 0);
    assertEquals(LatitudeLongitudeFormat.DECIMAL, latlon.getFormat());

    try {
      new LatitudeLongitude(null, "-11");
      fail("expected exception");
    } catch (IllegalArgumentException e) {
      assertEquals("The value \"null\" for \"latitude\" is invalid", e.getMessage());
    }

    try {
      new LatitudeLongitude("", "-11");
      fail("expected exception");
    } catch (IllegalArgumentException e) {
      assertEquals("The value \"\" for \"latitude\" is invalid", e.getMessage());
    }

    try {
      new LatitudeLongitude("10", "");
      fail("expected exception");
    } catch (IllegalArgumentException e) {
      assertEquals("The value \"\" for \"longitude\" is invalid", e.getMessage());
    }

    try {
      new LatitudeLongitude("10", "  ");
      fail("expected exception");
    } catch (IllegalArgumentException e) {
      assertEquals("The value \"  \" for \"longitude\" is invalid", e.getMessage());
    }

    try {
      new LatitudeLongitude("10", "600");
      fail("expected exception");
    } catch (IllegalArgumentException e) {
      assertEquals("The value \"600.0\" for \"longitude\" is invalid", e.getMessage());
    }

    try {
      new LatitudeLongitude("100", "60");
      fail("expected exception");
    } catch (IllegalArgumentException e) {
      assertEquals("The value \"100.0\" for \"latitude\" is invalid", e.getMessage());
    }
  }
}
