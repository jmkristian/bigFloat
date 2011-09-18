package test.math.bigFloat;

import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.Assert;
import org.testng.AssertJUnit;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.linkedin.math.bigFloat.BigFloat;

/**
 * @author <a href="mailto:jkristian@linkedin.com">John Kristian</a>
 */
public class TestBigFloat
{
  public TestBigFloat(String name)
  {
  }

  private BigFloats _data;

  @BeforeMethod()
  public void setUp()
      throws Exception
  {
    _data = new BigFloats();
  }

  protected static BigFloat toBigFloat(Number n)
  {
    return BigFloat.valueOf(n);
  }

  /** Convert to and from double. */
  @Test()
  public void testDouble()
  {
    for (Number expected : _data._numbers)
      if (expected instanceof Double)
      {
        BigFloat actual = BigFloat.valueOf(((Double) expected).doubleValue());
        AssertJUnit.assertEquals(actual.toString(), expected, actual.doubleValue());
        try
        {
          actual.getNaNPayload();
          Assert.fail("expected UnsupportedOperationException");
        }
        catch (UnsupportedOperationException good)
        {
        }
      }
  }

  /** Convert to and from long. */
  @Test()
  public void testLong()
  {
    for (long n : new long[] { -3, -2, -1, 0, 1, 2, 3, 5 })
    {
      AssertJUnit.assertEquals(n, BigFloat.valueOf(n).longValue());
      AssertJUnit.assertEquals((double) n, BigFloat.valueOf(n).doubleValue());
    }
    AssertJUnit.assertEquals(Long.MAX_VALUE, BigFloat.valueOf(Long.MAX_VALUE).longValue());
    AssertJUnit.assertEquals(Long.MIN_VALUE, BigFloat.valueOf(Long.MIN_VALUE).longValue());
  }

  /** Convert to and from Number. */
  @Test()
  public void testNumber()
  {
    for (Number n : _data._numbers)
    {
      AssertJUnit.assertEquals(BigFloat.valueOf(n), n);
    }
    AssertJUnit.assertEquals(BigFloat.valueOf(BigInteger.ZERO), 0);
    AssertJUnit.assertEquals(BigFloat.valueOf(BigDecimal.ZERO), 0);
    AssertJUnit.assertEquals(BigFloat.valueOf(BigInteger.TEN), 10);
    AssertJUnit.assertEquals(BigFloat.valueOf(BigDecimal.TEN), 10);
  }

  /** Compare BigFloats for equality. */
  @Test()
  public void testEquals()
  {
    _data.addBig();
    for (Number n : _data._numbers)
    {
      BigFloat f = toBigFloat(n);
      BigFloat f2 = toBigFloat(n);
      AssertJUnit.assertTrue(f.equals(f2));
      if (f.isNaN())
        AssertJUnit.assertFalse(f.equalsNumber(f2));
      else
      {
        AssertJUnit.assertTrue(f.equalsNumber(f2));
        if (f.getRange() == BigFloat.Range.FINITE)
        {
          f2 = new BigFloat(f.getSignificand().negate().subtract(BigInteger.ONE), f.getExponent());
          if (f.equals(f2))
            Assert.fail(f + ".equals(" + f2 + ")");
          if (!f.isZero())
          {
            f2 = new BigFloat(f.getSignificand(), f.getExponent().add(BigInteger.ONE));
            if (f.equals(f2))
              Assert.fail(f + ".equals(" + f2 + ")");
          }
        }
      }
    }
    AssertJUnit.assertFalse(BigFloat.NEGATIVE_ZERO.equals(BigFloat.ZERO));
    AssertJUnit.assertTrue(BigFloat.NEGATIVE_ZERO.equalsNumber(BigFloat.ZERO));
  }

  @Test()
  public void testNaN()
  {
    for (BigInteger payload : new BigInteger[] { BigInteger.ONE, BigInteger.valueOf(7), BigInteger.valueOf(1234) })
    {
      testNaN(payload);
      testNaN(payload.negate());
    }
    testNaN(0x7ff8000000000000L, BigFloat.valueOfNaN(false, BigInteger.ZERO));
    testNaN(0xfff8000000000000L, BigFloat.valueOfNaN(true, BigInteger.ZERO));
    testNaN(0x7ff8000000000001L, BigFloat.valueOfNaN(false, BigInteger.ONE));
    testNaN(0x7ff8000000000123L, BigFloat.valueOfNaN(false, BigInteger.valueOf(0x123)));
    // Sadly, Double.longBitsToDouble can't construct a signalling NaN:
    // testNaN(0x7ff7ffffffffffffL), BigFloat.newNaN(false, -1));
  }

  @Test(enabled = false)
  private void testNaN(BigInteger payload)
  {
    String message = String.format("NaN payload %x", payload);
    for (boolean negative : new boolean[] { false, true })
    {
      BigFloat nan = BigFloat.valueOfNaN(negative, payload);
      AssertJUnit.assertTrue(message, nan.isNaN());
      AssertJUnit.assertEquals(message, payload, nan.getNaNPayload());
      AssertJUnit.assertEquals(message, payload.signum() < 0, nan.isSignallingNaN());
      try
      {
        nan.getExponent();
        Assert.fail("expected UnsupportedOperationException");
      }
      catch (UnsupportedOperationException expected)
      {
      }
      try
      {
        nan.getSignificand();
        Assert.fail("expected UnsupportedOperationException");
      }
      catch (UnsupportedOperationException expected)
      {
      }
    }
  }

  @Test(enabled = false)
  private void testNaN(long expected, BigFloat from)
  {
    AssertJUnit.assertEquals(toHex(expected), toHex(Double.doubleToRawLongBits(from.doubleValue())));
    AssertJUnit.assertEquals(from, BigFloat.valueOf(Double.longBitsToDouble(expected)));
  }

  @Test()
  public void testOrdering()
      throws Exception
  {
    _data.addBig();
    List<Number> numbers = new ArrayList<Number>(_data._numbers);
    testOrdering(numbers);
    for (double n : new double[] { Double.MAX_VALUE, Double.MIN_VALUE, Double.MIN_VALUE * 3, })
    {
      numbers.add(n);
      numbers.add(-n);
    }
    testOrdering(numbers);
    numbers.clear();
    for (int i = 3; i <= 9; ++i)
    {
      numbers.add(new Double(i));
      numbers.add(new Double(-i));
    }
    testOrdering(numbers);
    numbers.clear();
    for (double boundary : new double[] { 0d, -0d, Math.pow(2d, 8), Math.pow(2d, 255), Math.pow(2d, 256) })
    {
      for (double increment : new double[] { Double.MIN_VALUE, 3 * Double.MIN_VALUE, boundary / 2, boundary / 4 * 3 })
      {
        numbers.add(boundary);
        numbers.add(boundary + increment);
        numbers.add(boundary - increment);
        numbers.add(-boundary);
        numbers.add(-boundary + increment);
        numbers.add(-boundary - increment);
        numbers.add((1 / boundary));
        numbers.add((1 / boundary) + increment);
        numbers.add((1 / boundary) - increment);
        numbers.add(-(1 / boundary));
        numbers.add(-(1 / boundary) + increment);
        numbers.add(-(1 / boundary) - increment);
      }
    }
    testOrdering(numbers);
    numbers.clear();
    {
      double d = 0.5d;
      for (int i = 1; i <= 10; ++i)
      {
        numbers.add(2d - d);
        numbers.add(-(2d - d));
        d = d / 2;
      }
    }
    testOrdering(numbers);
    numbers.clear();
    for (int i = 0; i < 20; ++i)
    {
      double n = Math.random() * 2653;
      numbers.add(n);
      numbers.add(-n);
    }
    testOrdering(numbers);
  }

  @Test(enabled = false)
  private void testOrdering(List<Number> numbers)
      throws Exception
  {
    List<Double> doubles = new ArrayList<Double>();
    for (Number n : numbers)
    {
      if (n instanceof Double
      // NaN values aren't ordered in Java:
          && !Double.isNaN(n.doubleValue()))
      {
        doubles.add(n.doubleValue());
      }
    }
    Collections.sort(doubles);
    List<BigFloat> expected = new ArrayList<BigFloat>();
    for (Double n : doubles)
    {
      expected.add(BigFloat.valueOf(n));
    }
    List<BigFloat> actual = new ArrayList<BigFloat>(expected);
    Collections.sort(actual);
    AssertJUnit.assertEquals(expected, actual);
  }

  private static String toHex(long n)
  {
    return String.format("%x", n);
  }

}
