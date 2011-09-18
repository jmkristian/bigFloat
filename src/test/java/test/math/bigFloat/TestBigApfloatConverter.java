package test.math.bigFloat;

import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.AssertJUnit;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import junit.framework.TestCase;

import org.apfloat.Apfloat;
import org.apfloat.ApfloatMath;

import com.linkedin.math.bigFloat.BigApfloatConverter;
import com.linkedin.math.bigFloat.BigFloat;

/**
 * @author <a href="mailto:jkristian@linkedin.com">John Kristian</a>
 */
public class TestBigApfloatConverter extends TestCase
{
  public TestBigApfloatConverter(String name)
  {
    super(name);
  }

  private static int[] DIGIT_BITS = { 3, 4, 5 };

  private List<Apfloat> _apfloats;
  private List<BigFloat> _bigFloats;
  private BigApfloatConverter _converter;
  private int _digitBits;
  private int _radix;

  
  private void setUp(int digitBits)
  {
    _apfloats = new ArrayList<Apfloat>();
    _bigFloats = new ArrayList<BigFloat>();
    _converter = new BigApfloatConverter(digitBits);
    _digitBits = digitBits;
    _radix = 1 << _digitBits;
    AssertJUnit.assertEquals(_radix, _converter.getRadix());
    addPair(null, null);
    addPair(Apfloat.ZERO, BigFloat.ZERO);
    addPair(newApfloat(1), BigFloat.ONE);
    addPair(newApfloat(8), BigFloat.valueOf(8));
    addPair(newApfloat(10), BigFloat.valueOf(10));
    addPair(newApfloat(_radix), BigFloat.valueOf(_radix));
    addPair(newApfloat("10"), BigFloat.valueOf(_radix));
    addPair(newApfloat("." + Character.forDigit(_radix >> 1, _radix)), //
            new BigFloat(BigInteger.valueOf(1), BigInteger.valueOf(-1)));
    addPair(newApfloat("." + Character.forDigit((_radix >> 1) + (_radix >> 3), _radix)),
            new BigFloat(BigInteger.valueOf(5), BigInteger.valueOf(-1)));
    final int doublePrecision = (64 / digitBits) + 1;
    addPair(new Apfloat(Double.MAX_VALUE, doublePrecision, _radix), BigFloat.valueOf(Double.MAX_VALUE));
    for (long bits : new long[] { 0x000cba987654321L, 0x123010101010101L, 0x8fe010101010101L, 0xf8e010101010101L })
    {
      double d = Double.longBitsToDouble(bits);
      addPair(new Apfloat(d, doublePrecision, _radix), BigFloat.valueOf(d));
    }
    for (long digits : new long[] { 1, 5, 1234567, Integer.MAX_VALUE, Long.MAX_VALUE >> 3 })
    // Larger values together with a large radix will overflow BigFloat._exponent.
    {
      addLargeAndSmall(digits, BigInteger.ONE);
      addLargeAndSmall(digits, BigInteger.valueOf(5));
      addLargeAndSmall(digits, BigInteger.valueOf(37));
    }
  }

  private void addLargeAndSmall(long digits, BigInteger significand)
  {
    final long offset = significand.bitLength() - significand.getLowestSetBit() - 1;
    final long precision = (offset / _digitBits) + 1;
    addPair(ApfloatMath.scale(new Apfloat(significand.longValue(), precision, _radix), digits),
            new BigFloat(significand, BigInteger.valueOf(digits * _digitBits + offset)));
    addPair(ApfloatMath.scale(new Apfloat(significand.longValue(), precision, _radix), -digits),
            new BigFloat(significand, BigInteger.valueOf(-digits * _digitBits + offset)));
  }

  private void addPair(Apfloat ap, BigFloat big)
  {
    _apfloats.add(ap);
    _bigFloats.add(big);
  }

  @Test()
  public void testZero()
  {
    for (int digitBits : DIGIT_BITS)
    {
      setUp(digitBits);
      final Apfloat summand = newApfloat("1.52");
      for (BigFloat from : new BigFloat[] { BigFloat.ZERO, BigFloat.valueOf(0), BigFloat.valueOf(0.0), BigFloat.NEGATIVE_ZERO,
          BigFloat.valueOf(-0.0) })
      {
        final String msg = from + " radix " + _radix;
        final Apfloat actual = _converter.toApfloat(from);
        AssertJUnit.assertEquals(msg, Apfloat.ZERO, actual);
        AssertJUnit.assertEquals(msg, summand, summand.add(actual));
        AssertJUnit.assertEquals(msg, summand, actual.add(summand));
      }
    }
  }

  @Test()
  public void testOne()
  {
    for (int digitBits : DIGIT_BITS)
    {
      setUp(digitBits);
      final Apfloat summand = newApfloat("2.52");
      for (BigFloat from : new BigFloat[] { BigFloat.ONE, BigFloat.valueOf(1), BigFloat.valueOf(1.0) })
      {
        final String msg = from + " radix " + _radix;
        final Apfloat sum = newApfloat("3.52");
        final Apfloat actual = _converter.toApfloat(from).precision(summand.precision());
        AssertJUnit.assertEquals(msg, _radix, actual.radix());
        AssertJUnit.assertEquals(msg, newApfloat(1), actual);
        AssertJUnit.assertEquals(msg, sum, actual.add(summand));
        AssertJUnit.assertEquals(msg, sum, summand.add(actual));
      }
      for (BigFloat from : new BigFloat[] { BigFloat.valueOf(-1), BigFloat.valueOf(-1.0) })
      {
        final String msg = from + " radix " + _radix;
        final Apfloat sum = newApfloat("1.52");
        final Apfloat actual = _converter.toApfloat(from).precision(summand.precision());
        AssertJUnit.assertEquals(msg, _radix, actual.radix());
        AssertJUnit.assertEquals(msg, newApfloat(-1), actual);
        AssertJUnit.assertEquals(msg, sum, actual.add(summand));
        AssertJUnit.assertEquals(msg, sum, summand.add(actual));
      }
    }
  }

  /** Confirm that toApfloat returns numbers with infinite precision. */
  @Test()
  public void testPrecision()
  {
    for (int digitBits : DIGIT_BITS)
    {
      setUp(digitBits);
      AssertJUnit.assertEquals(Apfloat.INFINITE, _converter.toApfloat(BigFloat.ZERO).precision());
      AssertJUnit.assertEquals(Apfloat.INFINITE, _converter.toApfloat(BigFloat.NEGATIVE_ZERO).precision());
      for (int digits : new int[] { 1, 2, 3, 4, 5, 6 })
      {
        int bits = digits * _digitBits;

        // Try a single digit followed by zeros, e.g. hexadecimal F0000:
        BigFloat from = new BigFloat(BigInteger.valueOf(_radix - 1), BigInteger.valueOf(bits - 1));
        AssertJUnit.assertEquals(from + " radix " + _radix, Apfloat.INFINITE, _converter.toApfloat(from).precision());

        // Try multiple digits, e.g. hexadecimal FFFFF:
        from = new BigFloat(BigInteger.ONE.shiftLeft(bits).subtract(BigInteger.ONE), BigInteger.ONE.negate());
        AssertJUnit.assertEquals(from + " radix " + _radix, Apfloat.INFINITE, _converter.toApfloat(from).precision());
      }
    }
  }

  @Test()
  public void testToApfloat()
  {
    for (int digitBits : DIGIT_BITS)
    {
      setUp(digitBits);
      Iterator<BigFloat> bfs = _bigFloats.iterator();
      for (Apfloat expected : _apfloats)
      {
        BigFloat from = bfs.next();
        AssertJUnit.assertEquals("radix " + _radix, expected, _converter.toApfloat(from));
        if (from != null)
          AssertJUnit.assertEquals("radix " + _radix, expected.negate(), _converter.toApfloat(negate(from)));
      }
    }
  }

  @Test()
  public void testToBigFloat()
  {
    for (int digitBits : DIGIT_BITS)
    {
      setUp(digitBits);
      Iterator<BigFloat> bfs = _bigFloats.iterator();
      for (Apfloat from : _apfloats)
      {
        BigFloat expected = bfs.next();
        AssertJUnit.assertEquals("radix " + _radix, expected, BigApfloatConverter.toBigFloat(from));
        if (from != null)
          AssertJUnit.assertEquals("radix " + _radix, negate(expected), BigApfloatConverter.toBigFloat(from.negate()));
      }
    }
  }

  @Test()
  public void testRoundTrip()
  {
    for (int digitBits : DIGIT_BITS)
    {
      BigApfloatConverter converter = new BigApfloatConverter(digitBits);
      BigFloats data = new BigFloats();
      for (Number n : data._numbers)
      {
        BigFloat expected = BigFloat.valueOf(n);
        Apfloat a = converter.toApfloat(expected);
        BigFloat actual = BigApfloatConverter.toBigFloat(a);
        if (expected.isZero())
          AssertJUnit.assertTrue("radixBits " + digitBits + "; Apfloat " + a, actual.isZero());
        else
          AssertJUnit.assertEquals("radixBits " + digitBits + "; Apfloat " + a, expected, actual);
      }
    }
  }

  private Apfloat newApfloat(String s)
  {
    int precision = 0;
    for (char c : s.toCharArray())
    {
      if (c != '.')
      {
        ++precision;
      }
    }
    return new Apfloat(s, precision, _radix);
  }

  private Apfloat newApfloat(long n)
  {
    return new Apfloat(n, Apfloat.INFINITE, _radix);
    }

  private static BigFloat negate(BigFloat f)
  {
    return new BigFloat(f.getSignificand().negate(), f.getExponent());
  }
}
