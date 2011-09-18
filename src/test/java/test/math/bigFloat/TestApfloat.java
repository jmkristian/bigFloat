package test.math.bigFloat;

import org.testng.annotations.Test;
import org.testng.AssertJUnit;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import org.apfloat.Apfloat;
import org.apfloat.ApfloatMath;

public class TestApfloat extends TestCase
{
  /**
   * Throw AssertionFailedError if the numbers are more different than the expected
   * precision.
   */
  public static void assertEquals(Apfloat expected, Apfloat actual)
  {
    if (!equals(expected, actual))
    {
      throw new AssertionFailedError("expected " + expected + "; actual " + actual);
    }
  }

  /**
   * Throw AssertionFailedError if the numbers are not different within the expected
   * precision.
   */
  public static void assertNotEquals(Apfloat expected, Apfloat actual)
  {
    if (equals(expected, actual))
    {
      throw new AssertionFailedError("not expected " + expected + "; actual " + actual);
    }
  }

  private static boolean equals(Apfloat expected, Apfloat actual)
  {
    final Apfloat tolerance = // e.g. 0.5 for a decimal integer
        ApfloatMath.scale(new Apfloat(expected.radix() / 2, 1, expected.radix()), expected.scale()
            - expected.precision() - 1);
    final Apfloat nominal = expected.precision(expected.precision() + 1);
    final Apfloat min = nominal.subtract(tolerance);
    final Apfloat max = nominal.add(tolerance);
    if (actual.compareTo(min) < 0) // too small
      return false;
    if (actual.compareTo(max) >= 0) // too large
      return false;
    return true;
  }

  private static long precision(String s)
  {
    if (s.indexOf('.') < 0)
      return s.length();
    return s.length() - 1;
  }

  @Test()
  public void testPrecision()
  {
    assertEquals(4, precision("abcd"));
    assertEquals(4, precision("a.bcd"));
    assertEquals(4, precision(".abcd"));
    assertEquals(6, precision("abcd00"));
    assertEquals(6, precision("a.bcd00"));
  }

  @Test()
  public void testAssertEquals()
  {
    testAssertEquals("0.4999", "0.5", new Apfloat("1"), "1.4999", "1.5", 10);
    testAssertEquals("1233.4999", "1233.6", new Apfloat("1234"), "1234.4999", "1234.5", 10);
    testAssertEquals("6.999994999", "6.999995", new Apfloat(7, 6), "7.000004999", "7.000005", 10);
    testAssertEquals("6.fffff7fff", "6.fffff8", new Apfloat(7, 6, 16), "7.000007fff", "7.000008", 16);
    testAssertEquals("6.vvvvvfvvv", "6.vvvvvg", new Apfloat(7, 6, 32), "7.00000fvvv", "7.00000g", 32);
    assertEquals(new Apfloat("1.vvv", 4, 32), new Apfloat("1.vvv1", 5, 32));
    assertNotEquals(new Apfloat("1.vvv", 5, 32), new Apfloat("1.vvv1", 5, 32));
  }

  @Test(enabled = false)
  private static void testAssertEquals(String tooSmall, String small, Apfloat expected, String large, String tooLarge,
                                       int radix)
  {
    assertNotEquals(expected, new Apfloat(tooSmall, precision(tooSmall), radix));
    assertEquals(expected, new Apfloat(small, precision(small), radix));
    assertEquals(expected, new Apfloat(large, precision(large), radix));
    assertNotEquals(expected, new Apfloat(tooLarge, precision(tooLarge), radix));
  }

  @Test()
  public void testMultiply()
  {
    assertEquals(new Apfloat("4", 20), new Apfloat("2").multiply(new Apfloat("2")));
    assertEquals(new Apfloat("27", 20), new Apfloat("9", 2).multiply(new Apfloat("3", 2)));
    assertEquals(new Apfloat("27", 20), new Apfloat("90000").multiply(new Apfloat(".0003", 5)));
  }

  /** Raise numbers to fractional exponents. */
  @Test()
  public void testPow()
  {
    // inverse:
    assertEquals(new Apfloat("0.5", 20), ApfloatMath.pow(new Apfloat(2, 20), new Apfloat(-1, 20)));
    // square:
    assertEquals(new Apfloat(25, 20), ApfloatMath.pow(new Apfloat(5, 21), new Apfloat(2, 21)));
    // square root:
    assertEquals(new Apfloat(3, 20), ApfloatMath.pow(new Apfloat(9, 20), new Apfloat("0.5", 20)));
    // cube root:
    assertEquals(new Apfloat(4, 20), ApfloatMath.pow(new Apfloat(64, 20), //
                                                     new Apfloat(1, 20).divide(new Apfloat(3, 20))));
    assertEquals(new Apfloat(4, 20, 16), ApfloatMath.pow(new Apfloat(64, 20, 16),
                                                         new Apfloat(1, 30, 16).divide(new Apfloat(3, 30, 16))));
  }

  /** Add numbers with radixes that are powers of two. */
  @Test()
  public void testBinary()
  {
    testRadixAdd(16, "11", "9", "8");
    testRadixAdd(16, "fff.000000", "cde", "321");
    testRadixAdd(16, "fff.fff000", "9ab.cde", "654.321");
    testRadixAdd(32, "vvv.vvv000", "pqr.stu", "654.321");
  }

  @Test(enabled = false)
  private static void testRadixAdd(int radix, String expected, String x, String y)
  {
    assertEquals(new Apfloat(expected, precision(expected), radix), //
                 new Apfloat(x, precision(x), radix).add(new Apfloat(y, precision(y), radix)));
  }
}
