/* Copyright 2011 LinkedIn Corp.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package com.linkedin.math.bigFloat;

import org.apfloat.Apint;
import java.math.BigInteger;

import org.apfloat.Apfloat;
import org.apfloat.ApfloatMath;

/**
 * Algorithms to convert between BigFloat and Apfloat. The Apfloat radixes must be
 * integral powers of 2 (2, 4, 8, 16 or 32).
 * 
 * @author <a href="mailto:jkristian@linkedin.com">John Kristian</a>
 */
public class BigApfloatConverter
{
  private static final BigInteger BIG_INTEGER_3 = BigInteger.valueOf(3);
  private static final BigInteger MAX_LONG = BigInteger.valueOf(Long.MAX_VALUE);
  private static final BigInteger[] MAX_EXPONENT = { BigInteger.valueOf(64), // radix 1
      MAX_LONG, // radix 2
      MAX_LONG.shiftLeft(1), // radix 4
      MAX_LONG.shiftLeft(2), // radix 8
      MAX_LONG.shiftLeft(3), // radix 16
      MAX_LONG.shiftLeft(4) // radix 32
      };

  /**
   * Convert a BigFloat to an Apfloat.
   * 
   * @param digitBits
   *          the number of bits per digit in the resulting Apfloat
   * @return the same number, represented as an Apfloat. Its precision is
   *         Apfloat.INFINITE, since it represents precisely the same number as the given
   *         BigFloat.
   */
  public static Apfloat toApfloat(final BigFloat from, final int digitBits)
  {
    if (from == null)
      return null;
    switch (from.getRange())
    {
    case INFINITE:
      throw new IllegalArgumentException(from + " is infinite");
    case NaN:
      throw new IllegalArgumentException(from + " is NaN");
    default:
    }
    if (from.isZero())
      return Apfloat.ZERO;
    final int radix = 1 << digitBits;
    final BigInteger e = from.getExponent();
    if (e.abs().compareTo(MAX_EXPONENT[digitBits]) > 0)
      throw new ArithmeticException(from + " overflows Apfloat scale with radix " + radix);

    Apfloat a = Apfloat.ZERO;
    {
      BigInteger s = from.getSignificand().abs();
      s = s.shiftRight(s.getLowestSetBit()); // should do nothing
      {
        // Align the significand with digits of the Apfloat.
        // The number is s * (2 ** (e - (s.bitLength() - 1))).
        BigInteger bitScale = e.subtract(BigInteger.valueOf(s.bitLength() - 1));
        // Align s so that (s.bitLength() - e) % digitBits = 1:
        s = s.shiftLeft(modFloor(bitScale, digitBits));
      }
      final BigInteger digitMask = BigInteger.valueOf(radix - 1);
      for (; s.signum() != 0; s = s.shiftRight(digitBits))
      {
        Apint digit = new Apint(s.and(digitMask).intValue(), radix);
        a = ApfloatMath.scale(a, -1).add(digit);
      }
    }
    a = ApfloatMath.scale(a, divideFloor(e, digitBits).longValue());
    if (from.isNegative())
      a = a.negate();
    return a;
  }

  /**
   * Convert an Apfloat to a BigFloat.
   * 
   * @throws IllegalArgumentException
   *           from.radix() is > 35 or not an integral power of 2
   */
  public static BigFloat toBigFloat(final Apfloat from)
      throws IllegalArgumentException
  {
    if (from == null)
      return null;
    if (from.signum() == 0)
      return BigFloat.ZERO;
    if (from.equals(Apfloat.ONE))
      return BigFloat.ONE;
    final int radix = from.radix();
    final int digitBits = toDigitBits(radix);
    // BigInteger uses an int to represent the number of bits it contains.
    // But an Apfloat can have much higher precision.  Check for overflow:
    if (from.precision() != Apfloat.INFINITE && from.precision() * digitBits > Integer.MAX_VALUE)
      throw new ArithmeticException(from + " radix " + radix + " precision " + from.precision()
          + " overflows BigFloat significand");

    BigInteger significand = BigInteger.ZERO;
    {
      String s = ApfloatMath.scale(ApfloatMath.abs(from), -from.scale()).toString(true);
      if (s.startsWith("0.")) // it should
        s = s.substring(2);
      else if (s.startsWith(".")) // it might
        s = s.substring(1);
      else
        throw new NumberFormatException("ApfloatMath.scale(" + from + ", -" + from.scale() + ").toString(true) = " + s);
      for (char c : s.toCharArray())
      {
        BigInteger digit = BigInteger.valueOf(Character.getNumericValue(c));
        significand = significand.shiftLeft(digitBits).or(digit);
      }
    }
    long scaleBits = (from.scale() - 1) * digitBits;
    int align = (significand.bitLength() - 1) % digitBits;
    BigInteger exponent = BigInteger.valueOf(scaleBits + align);
    if (from.signum() < 0)
      significand = significand.negate();
    return new BigFloat(significand, exponent);
  }

  private final int _digitBits;

  /**
   * Construct a converter.
   * 
   * @param digitBits
   *          the number of bits per digit in the resulting Apfloats
   */
  public BigApfloatConverter(int digitBits)
  {
    _digitBits = digitBits;
  }

  /**
   * Construct a converter with 4 bits per digit.
   */
  public BigApfloatConverter()
  {
    this(4);
  }

  /**
   * The radix of Apfloats returned from this.toApfloat(BigFloat).
   */
  public int getRadix()
  {
    return 1 << _digitBits;
  }

  /**
   * Convert a BigFloat to an Apfloat.
   */
  public Apfloat toApfloat(BigFloat from)
  {
    return toApfloat(from, _digitBits);
  }

  /** Compute the number of bits per digit for the given radix. */
  private static byte toDigitBits(int radix)
  {
    for (byte log = 0; log <= 5; ++log)
    {
      int x = 1 << log;
      if (x == radix)
        return log;
      if (x > radix)
        throw new IllegalArgumentException("radix " + radix + " isn't a power of 2");
    }
    throw new IllegalArgumentException("radix " + radix + " > 36");
  }

  /** x/y rounded toward negative infinity; that is the largest integer <= x/y. */
  private static BigInteger divideFloor(BigInteger x, int y)
  {
    if (y == 4)
      return x.shiftRight(2);
    if (y == 2)
      return x.shiftRight(1);
    BigInteger[] qr = x.divideAndRemainder(BigInteger.valueOf(y));
    BigInteger q = qr[0];
    if (qr[1].signum() < 0)
      q = q.subtract(BigInteger.ONE);
    return q;
  }

  /**
   * The remainder after divideFloor(x, y). Unlike the % operator, the result is always
   * positive; for example modFloor(-5, 4) is 3 and modFloor(-6, 4) is 2.
   */
  private static int modFloor(BigInteger x, int y)
  {
    if (y == 4)
      return x.and(BIG_INTEGER_3).intValue();
    if (y == 2)
      return x.and(BigInteger.ONE).intValue();
    int m = x.mod(BigInteger.valueOf(y)).intValue();
    if (m < 0)
      m += y;
    return m;
  }

}
