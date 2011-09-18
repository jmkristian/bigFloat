package test.math.bigFloat;

import org.testng.annotations.Test;
import org.testng.Assert;
import org.testng.AssertJUnit;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import junit.framework.AssertionFailedError;
import com.linkedin.math.bigFloat.BigFloat;
import com.linkedin.math.bigFloat.BigFloatCodec;

/**
 * @author <a href="mailto:jkristian@linkedin.com">John Kristian</a>
 */
public class TestBigFloatCodec
{
  public TestBigFloatCodec(String name)
  {
  }

  private static String bigIntegerString(int bits)
  {
    return BigInteger.ZERO.setBit(bits).subtract(BigInteger.ONE).toString();
  }

  private static String repeat(String s, int times)
  {
    StringBuilder into = new StringBuilder();
    for (int i = 0; i < times; ++i)
      into.append(s);
    return into.toString();
  }

  private static final String[][] ENCODED_INTEGERS =
      { { "0", "0", "0" }, { "1", "8", "7" }, { "2", "c", "3" }, { "5", "e2", "1d" }, { "11", "eb", "14" },
          { "17", "f01", "0fe" },
      { "3", "d", "2" }, // 2 bits
      { "7", "e6", "19" }, // 3 bits
      { 0xf + "", "ef", "10" }, // 4 bits
      { 0x1f + "", "f0f", "0f0" }, // 5 bits
      { 0x3f + "", "f1f8", "0e07" }, // 6 bits
      { 0x7f + "", "f2fc", "0d03" }, // 7 bits
      { 0xff + "", "f3fe", "0c01" }, // 8 bits
      { 0x1ff + "", "f47f8", "0b807" }, // 9 bits
      { 0x3ff + "", "f4ffc", "0b003" }, // 10 bits
      { 0x7ff + "", "f57fe", "0a801" }, // 11 bits
      { 0xfff + "", "f5fff", "0a000" }, // 12 bits
      { 0x1fff + "", "f67ff8", "098007" }, // 13 bits
      { 0x3fff + "", "f6fffc", "090003" }, // 14 bits
      { 0x7fff + "", "f77ffe", "088001" }, // 15 bits
      { 0xffff + "", "f7ffff", "080000" }, // 16 bits
      { 0x1ffff + "", "f807fff8", "07f80007" }, // 17 bits
      { 0x3ffff + "", "f80ffffc", "07f00003" }, // 18 bits
      { 0x7ffff + "", "f817fffe", "07e80001" }, // 19 bits
      { 0xfffff + "", "f81fffff", "07e00000" }, // 20 bits
      { 0x1fffff + "", "f827ffff8", "07d800007" }, // 21 bits
      { 0x3fffff + "", "f82fffffc", "07d000003" }, // 22 bits
      { 0x7fffff + "", "f837ffffe", "07c800001" }, // 23 bits
      { 0xffffff + "", "f83ffffff", "07c000000" }, // 24 bits
      { 0x1ffffff + "", "f847fffff8", "07b8000007" }, // 25 bits
      { 0x3ffffff + "", "f84ffffffc", "07b0000003" }, // 26 bits
      { 0x7ffffff + "", "f857fffffe", "07a8000001" }, // 27 bits
      { 0xfffffff + "", "f85fffffff", "07a0000000" }, // 28 bits
      { 0x1fffffff + "", "f867ffffff8", "07980000007" }, // 29 bits
      { 0x3fffffff + "", "f86fffffffc", "07900000003" }, // 30 bits
      { 0x7fffffff + "", "f877ffffffe", "07880000001" }, // 31 bits
      { 0xffffffffL + "", "f87ffffffff", "07800000000" }, // 32 bits
      { 0x1ffffffffL + "", "f883fffffffc", "077c00000003" }, // 33 bits
      { 0x3ffffffffL + "", "f887fffffffe", "077800000001" }, // 34 bits
      { 0x7ffffffffL + "", "f88bffffffff", "077400000000" }, // 35 bits
      { 0xfffffffffL + "", "f88fffffffff8", "0770000000007" }, // 36 bits
      { 0x1fffffffffL + "", "f893ffffffffc", "076c000000003" }, // 37 bits
      { 0x3fffffffffL + "", "f897ffffffffe", "0768000000001" }, // 38 bits
      { 0x7fffffffffL + "", "f89bfffffffff", "0764000000000" }, // 39 bits
      { bigIntegerString(40), "f89ffffffffff8", "07600000000007" },
      { bigIntegerString(41), "f8a3fffffffffc", "075c0000000003" },
      { bigIntegerString(42), "f8a7fffffffffe", "07580000000001" },
      { bigIntegerString(43), "f8abffffffffff", "07540000000000" },
      { bigIntegerString(44), "f8afffffffffff8", "075000000000007" },
      { bigIntegerString(45), "f8b3ffffffffffc", "074c00000000003" },
      { bigIntegerString(46), "f8b7ffffffffffe", "074800000000001" },
      { bigIntegerString(47), "f8bbfffffffffff", "074400000000000" },
      { bigIntegerString(48), "f8bffffffffffff8", "0740000000000007" },
      { bigIntegerString(49), "f8c3fffffffffffc", "073c000000000003" },
      { bigIntegerString(50), "f8c7fffffffffffe", "0738000000000001" },
      { bigIntegerString(51), "f8cbffffffffffff", "0734000000000000" },
      { bigIntegerString(52), "f8cfffffffffffff8", "07300000000000007" },
      { bigIntegerString(53), "f8d3ffffffffffffc", "072c0000000000003" },
      { bigIntegerString(54), "f8d7ffffffffffffe", "07280000000000001" },
      { bigIntegerString(55), "f8dbfffffffffffff", "07240000000000000" },
      { bigIntegerString(56), "f8dffffffffffffff8", "072000000000000007" },
      { bigIntegerString(57), "f8e3fffffffffffffc", "071c00000000000003" },
      { bigIntegerString(58), "f8e7fffffffffffffe", "071800000000000001" },
      { bigIntegerString(59), "f8ebffffffffffffff", "071400000000000000" },
      { bigIntegerString(60), "f8efffffffffffffff8", "0710000000000000007" },
      { bigIntegerString(61), "f8f3ffffffffffffffc", "070c000000000000003" },
      { bigIntegerString(62), "f8f7ffffffffffffffe", "0708000000000000001" },
      { bigIntegerString(63), "f8fbfffffffffffffff", "0704000000000000000" },
          { bigIntegerString(64), "f8f" + repeat("f", 16) + "8", "070" + repeat("0", 16) + "7" },
          { bigIntegerString(128), "f97" + repeat("f", 32) + "c", "068" + repeat("0", 32) + "3" },
          { bigIntegerString(256), "f9f" + repeat("f", 64) + "e", "060" + repeat("0", 64) + "1" },
          { bigIntegerString(1024), "fa7f" + repeat("f", 256) + "c", "0580" + repeat("0", 256) + "3" } };

  private final BigFloats _data = new BigFloats();
  private final BigFloatCodec _formatter = new BigFloatCodec(false);

  private static BigFloat toBigFloat(Number n)
  {
    return TestBigFloat.toBigFloat(n);
  }

  private BigFloat decode(String f)
      throws NumberFormatException
  {
    return _formatter.decode(f);
  }

  @Test(enabled = false)
  private String testFormat(String expected, Number n)
  {
    String actual = _formatter.encode(toBigFloat(n));
    if (!expected.equals(actual))
      throw new AssertionFailedError(n + " expected: " + expected + " but was: " + actual);
    return actual;
  }

  @Test(enabled = false)
  private BigFloat testDecode(BigFloat expected, String s)
  {
    BigFloat actual = decode(s);
    if (!expected.equals(actual))
      throw new AssertionFailedError(s + " expected: " + expected + " but was: " + actual);
    return actual;
  }

  /** Integers are correctly encoded. */
  @Test()
  public void testEncodeInteger()
      throws Exception
  {
    for (String[] testCase : ENCODED_INTEGERS)
    {
      BigInteger n = new BigInteger(testCase[0]);
      testEncodeInteger(testCase[1], n);
      testEncodeInteger(testCase[2], n.negate());
    }
  }

  @Test(enabled = false)
  private void testEncodeInteger(String expected, BigInteger n)
  {
    AssertJUnit.assertEquals(hexInteger(n), expected, encodeInteger(n));
  }

  private static String hexInteger(BigInteger n)
  {
    return n.toString(16);
  }

  /** Integers are correctly decoded. */
  @Test()
  public void testDecodeInteger()
      throws Exception
  {
    for (String[] testCase : ENCODED_INTEGERS)
    {
      BigInteger n = new BigInteger(testCase[0]);
      testDecodeInteger(n, testCase[1]);
      testDecodeInteger(n.negate(), testCase[2]);
    }
  }

  @Test(enabled = false)
  private void testDecodeInteger(BigInteger expected, String s)
  {
    s = ((expected.signum() < 0) ? "0" : "1") + s;
    BigInteger actual = _formatter.decodeInteger(s);
    AssertJUnit.assertEquals(hexInteger(expected), hexInteger(actual));
  }

  /** Encoded integers alphabetize correctly. */
  @Test()
  public void testIntegerOrdering()
      throws Exception
  {
    List<BigInteger> numbers = new ArrayList<BigInteger>();
    for (String[] testCase : ENCODED_INTEGERS)
    {
      BigInteger n = new BigInteger(testCase[0]);
      numbers.add(n);
      numbers.add(n.negate());
    }
    testIntegerOrdering(numbers);
    numbers.clear();
    for (int i = 0; i <= 256; ++i)
    {
      BigInteger n = BigInteger.valueOf(i);
      numbers.add(n);
      numbers.add(n.negate());
    }
    testIntegerOrdering(numbers);
  }

  @Test(enabled = false)
  private void testIntegerOrdering(List<BigInteger> numbers)
      throws Exception
  {
    Collections.sort(numbers);
    List<String> expected = new ArrayList<String>();
    for (BigInteger n : numbers)
    {
      expected.add(((n.signum() < 0) ? "0" : "1") + encodeInteger(n));
    }
    List<String> actual = new ArrayList<String>(expected);
    Collections.sort(actual);
    AssertJUnit.assertEquals(expected, actual);
  }

  private String encodeInteger(BigInteger n)
  {
    StringBuilder s = new StringBuilder();
    _formatter.encodeInteger(s, n);
    return s.toString();
  }

  /** Some easy values are correctly encoded. */
  @Test()
  public void testEncode()
      throws Exception
  {
    for (String[] testCase : ENCODED_INTEGERS)
    {
      BigInteger n = new BigInteger(testCase[0]);
      AssertJUnit.assertTrue(n.signum() >= 0);
      if (n.compareTo(BigInteger.valueOf(52)) < 0)
      {
        testFormat("b" + testCase[1], Math.pow(2d, n.longValue()));
        // testFormat("4" + testCase[2], -Math.pow(2d, n.longValue()));
        if (n.signum() != 0)
        {
          testFormat("a" + testCase[2], Math.pow(2d, -(n.longValue())));
          // testFormat("5" + testCase[1], -Math.pow(2d, -(n.longValue())));
        }
      }
    }
    _data.addBig();
    for (int i = 0; i < _data._numbers.size(); ++i)
    {
      testFormat(_data._strings.get(i), _data._numbers.get(i));
    }
  }

  /** Some easy values are correctly parsed. */
  @Test()
  public void testDecode()
      throws Exception
  {
    _data.addBig();
    for (int i = 0; i < _data._strings.size(); ++i)
    {
      testDecode(toBigFloat(_data._numbers.get(i)), _data._strings.get(i));
    }
  }

  /** Formatted numbers sort the same as the numbers. */
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
    List<String> expected = new ArrayList<String>();
    for (Double n : doubles)
    {
      expected.add(_formatter.encode(BigFloat.valueOf(n.doubleValue())));
    }
    List<String> actual = new ArrayList<String>(expected);
    Collections.sort(actual);
    AssertJUnit.assertEquals(expected, actual);
  }

  @Test()
  public void testNaN()
  {
    testFormat("f8", BigFloat.valueOfNaN(false, BigInteger.ONE));
    testFormat("e7", BigFloat.valueOfNaN(false, BigInteger.ONE.negate()));
    testFormat("18", BigFloat.valueOfNaN(true, BigInteger.ONE.negate()));
    testFormat("07", BigFloat.valueOfNaN(true, BigInteger.ONE));
  }

  @Test()
  public void testBadStrings()
      throws Exception
  {
    // testBadString("Be8"); // upper case
    // testBadString("bE8"); // upper case
    testBadString("dead beef"); // not hex
    testBadString("9ffffffffffffffff"); // overflow Levenshtein prefix
    testBadString("60000000000000000"); // overflow Levenshtein prefix
  }

  @Test(enabled = false)
  private void testBadString(String s)
      throws Exception
  {
    try
    {
      decode(s);
      Assert.fail(s + " expected NumberFormatException");
    }
    catch (NumberFormatException expected)
    {
      System.out.println(expected + "");
    }
  }

}
