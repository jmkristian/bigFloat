package com.linkedin.math.bigFloat;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A number represented in binary floating point; that is significand * 2**exponent; where
 * exponent is an integer and significand is either zero or a number >= 1 and < 2. The
 * significand is represented as an BigInteger; the most significant bit of its absolute
 * value is taken to be 1. For example, significands 0, 1, 3/2 and 5/4 are represented as
 * integers 0, 1, 3 and 5. A BigFloat may also be infinite or NaN (not a number), like
 * IEEE floating point numbers.
 * <p>
 * A BigFloat is immutable.
 * 
 * @author <a href="mailto:jkristian@linkedin.com">John Kristian</a>
 */
public class BigFloat extends Number implements Comparable<Number>
{
  private static final BigInteger POSITIVE = BigInteger.ONE;
  private static final BigInteger NEGATIVE = POSITIVE.negate();

  public static final BigFloat NaN = valueOf(Double.NaN);
  public static final BigFloat INFINITY = new BigFloat(Range.INFINITE, POSITIVE, BigInteger.ZERO);
  public static final BigFloat ZERO = new BigFloat(Range.FINITE, BigInteger.ZERO, POSITIVE);
  public static final BigFloat NEGATIVE_ZERO = new BigFloat(Range.FINITE, BigInteger.ZERO, NEGATIVE);
  public static final BigFloat NEGATIVE_INFINITY = new BigFloat(Range.INFINITE, NEGATIVE, BigInteger.ZERO);
  public static final BigFloat NEGATIVE_NaN = valueOf(-Double.NaN);

  public static final BigFloat ONE = new BigFloat(BigInteger.ONE, BigInteger.ZERO);

  /** A range of values. */
  public static enum Range
  {
    FINITE, INFINITE, NaN
  }

  // Characteristics of a Java double (IEEE binary double):
  private static final long SIGN_MASK = 1L << 63;
  private static final int EXPONENT_BIT = 52;
  private static final int EXPONENT_BITS = 11;
  private static final long EXPONENT_MASK = ((1L << EXPONENT_BITS) - 1) << EXPONENT_BIT;
  private static final int SIGNIFICAND_BITS = EXPONENT_BIT;
  private static final long SIGNIFICAND_MASK = (1L << SIGNIFICAND_BITS) - 1;
  private static final int BIAS = 1023;
  private static final long ONE_MASK = 1L << SIGNIFICAND_BITS;
  private static final long NaN_SIGNAL_MASK = 1L << (SIGNIFICAND_BITS - 1);
  private static final BigInteger BIG_BIAS = BigInteger.valueOf(BIAS);
  private static final BigInteger MAX_DOUBLE_EXPONENT = BIG_BIAS;
  private static final BigInteger MIN_DOUBLE_EXPONENT =
      BIG_BIAS.negate().subtract(BigInteger.valueOf(SIGNIFICAND_BITS));

  private static final boolean TRACE = false;
  private static final long serialVersionUID = 1L;

  private final Range _range;
  private final BigInteger _significand;
  private final BigInteger _exponent;

  private BigFloat(Range range, BigInteger significand, BigInteger exponent)
  {
    _range = range;
    _significand = significand;
    _exponent = exponent;
  }

  /**
   * The significand is interpreted as a bit string (not really an integer) representing
   * either zero or a number >= 1 and < 2. For example BigIntegers 0, 1, 3 and 5 are
   * interpreted as significands 0, 1, 3/2 and 5/4. BigIntegers 2, 4 and 8 are also
   * interpreted as 1 (in each case, a binary 1 followed by binary zeroes).
   */
  public BigFloat(BigInteger significand, BigInteger exponent)
  {
    this(Range.FINITE, normalize(significand), exponent);
  }

  static BigInteger normalize(BigInteger f)
  {
    if (f.signum() == 0)
      return f;
    return f.shiftRight(f.abs().getLowestSetBit());
  }

  /**
   * The returned object is a signalling NaN if the given payload is negative.
   */
  public static BigFloat valueOfNaN(boolean negative, BigInteger payload)
  {
    BigFloat n = new BigFloat(Range.NaN, negative ? NEGATIVE : POSITIVE, payload);
    if (TRACE)
      System.out.println("newNaN(" + negative + "," + payload + ") = " + n);
    return n;
  }

  public static BigFloat valueOf(Number n)
  {
    if (n instanceof BigFloat)
      return (BigFloat) n;
    if (n instanceof BigDecimal && ((BigDecimal) n).scale() <= 0)
      n = ((BigDecimal) n).toBigIntegerExact();
    if (n instanceof BigInteger)
    {
      BigInteger b = (BigInteger) n;
      if (b.signum() == 0)
        return ZERO;
      if (b.compareTo(BigInteger.ONE) == 0)
        return ONE;
      return new BigFloat(b, BigInteger.valueOf(b.abs().bitLength() - 1));
    }
    if ((n instanceof Long) || (n instanceof AtomicLong) || (n instanceof Integer))
      return valueOf(n.longValue());
    return valueOf(n.doubleValue());
  }

  public static BigFloat valueOf(long value)
  {
    if (value == 0)
      return ZERO;
    if (value == 1)
      return ONE;
    BigInteger significand = BigInteger.valueOf(value);
    return new BigFloat(significand, BigInteger.valueOf(significand.abs().bitLength() - 1));
  }

  public static BigFloat valueOf(double value)
  {
    Range range = Double.isNaN(value) ? Range.NaN : Double.isInfinite(value) ? Range.INFINITE : Range.FINITE;
    final long bits = Double.doubleToRawLongBits(value);
    final boolean negative = (bits & SIGN_MASK) != 0;
    if (value == 0d)
      return negative ? NEGATIVE_ZERO : ZERO;
    long e = ((bits & EXPONENT_MASK) >>> EXPONENT_BIT) - BIAS;
    long s = bits & SIGNIFICAND_MASK;
    BigInteger significand = negative ? NEGATIVE : POSITIVE;
    BigInteger exponent = null;
    switch (range)
    {
    case INFINITE:
      exponent = BigInteger.ZERO;
      break;

    case NaN:
      if ((s & NaN_SIGNAL_MASK) == 0)
        s = -s;
      else
        s = s & ~NaN_SIGNAL_MASK;
      exponent = BigInteger.valueOf(s);
      break;

    default:
      if (e > -BIAS)
        s |= ONE_MASK;
      else if (s == 0) // zero
        e = negative ? -1 : 1;
      else
      {
        // For very small numbers, IEEE double format
        // adds leading zeros to the significand, but
        // this representation decreases the exponent.
        ++e;
        do
        {
          e -= 1;
          s <<= 1;
        }
        while ((s & ONE_MASK) == 0);
      }
      if (s != 0)
      {
        while ((s & 1) == 0)
          s >>>= 1;
      }
      if (negative)
        s = -s;
      exponent = BigInteger.valueOf(e);
      significand = BigInteger.valueOf(s);
    }
    BigFloat result = new BigFloat(range, significand, exponent);
    if (TRACE)
      System.out.println(String.format("BigFloat(%g=%016x) = BigFloat(%s,%s,%s) = %s",
                                       value,
                                       Double.doubleToRawLongBits(value),
                                       range,
                                       hexFraction(significand),
                                       exponent,
                                       result));
    return result;
  }

  public Range getRange()
  {
    return _range;
  }

  public BigInteger getSignificand()
  {
    if (!Range.FINITE.equals(getRange()))
      throw new UnsupportedOperationException(this + ".getSignificand()");
    return _significand;
  }

  public BigInteger getExponent()
  {
    if (!Range.FINITE.equals(getRange()))
      throw new UnsupportedOperationException(this + ".getExponent()");
    return _exponent;
  }

  public boolean isNegative()
  {
    return ((isZero() ? _exponent : _significand).signum()) < 0;
  }

  public boolean isZero()
  {
    return Range.FINITE.equals(getRange()) && getSignificand().signum() == 0;
  }

  public boolean isNaN()
  {
    return getRange() == Range.NaN;
  }

  public boolean isSignallingNaN()
  {
    return isNaN() && _exponent.signum() < 0;
  }

  public BigInteger getNaNPayload()
  {
    if (!isNaN())
      throw new UnsupportedOperationException(this + ".getNaNPayload()");
    return _exponent;
  }

  @Override
  public int intValue()
  {
    return (int) longValue();
  }

  @Override
  public long longValue()
  {
    if (isNaN())
      return 0;
    if (getExponent().signum() < 0)
      return 0;
    if (getExponent().compareTo(BigInteger.valueOf(63)) >= 0)
      return isNegative() ? Long.MIN_VALUE : Long.MAX_VALUE;
    BigInteger s = getSignificand();
    return s.longValue() << (getExponent().longValue() + 1 - s.abs().bitLength());
  }

  @Override
  public float floatValue()
  {
    return (float) doubleValue();
  }

  @Override
  public double doubleValue()
  {
    StringBuilder traceMessage = TRACE ? new StringBuilder("(" + this + ").doubleValue()") : null;
    switch (getRange())
    {
    case NaN:
    {
      long s = _exponent.longValue();
      if (s >= 0)
        s = s | NaN_SIGNAL_MASK;
      else
        s = s & ~NaN_SIGNAL_MASK;
      long bits = isNegative() ? SIGN_MASK : 0;
      bits |= EXPONENT_MASK;
      bits |= s & SIGNIFICAND_MASK;
      double n = Double.longBitsToDouble(bits);
      if (traceMessage != null)
        System.out.println(String.format("%s = %016x = %g", traceMessage, bits, n));
      return n;
      // return isNegative() ? -Double.NaN : Double.NaN;
    }
    case INFINITE:
      return isNegative() ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
    case FINITE:
    }
    if (isZero() || _exponent.compareTo(MIN_DOUBLE_EXPONENT) < 0)
      return isNegative() ? -0d : 0d;
    if (_exponent.compareTo(MAX_DOUBLE_EXPONENT) > 0)
      return isNegative() ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
    long e = _exponent.longValue() + BIAS;
    BigInteger bigS = _significand.abs();
    long s = bigS.longValue() << (SIGNIFICAND_BITS - bigS.bitLength() + 1);
    if (e <= 0)
    {
      int shift = (int) -e + 1;
      if (TRACE)
        System.out.print(shift + " subnormal");
      s >>>= shift;
      e = 0;
    }
    long bits = isNegative() ? SIGN_MASK : 0;
    bits |= (e << EXPONENT_BIT);
    bits |= s & SIGNIFICAND_MASK;
    double n = Double.longBitsToDouble(bits);
    if (traceMessage != null)
      System.out.println(String.format("%s = %016x = %g", traceMessage, bits, n));
    return n;
  }

  public int compareTo(Number n)
  {
    BigFloat that = valueOf(n);
    if (this.isNegative())
    {
      if (!that.isNegative())
        return -1;
    }
    else if (that.isNegative())
    {
      return 1;
    }
    int result = this.getRange().ordinal() - that.getRange().ordinal();
    if (result == 0)
    {
      if (this.isZero())
        result = that.isZero() ? 0 : -1;
      else if (that.isZero())
        result = 1;
      else
      {
        result = compare(this._exponent, that._exponent);
        if (result == 0)
        {
          BigInteger thisS = this._significand.abs();
          BigInteger thatS = that._significand.abs();
          long shift = ((long) thisS.bitLength()) - ((long) thatS.bitLength());
          if (shift < 0)
            thisS = thisS.shiftLeft((int) -shift);
          else if (shift > 0)
            thatS = thatS.shiftLeft((int) shift);
          result = compare(thisS, thatS);
        }
      }
    }
    if (result != 0)
      result = ((result < 0) ? -1 : 1) * (isNegative() ? -1 : 1);
    return result;
  }

  private static int compare(BigInteger x, BigInteger y)
  {
    if (x == null)
      return (y == null) ? 0 : 1;
    else if (y == null)
      return -1;
    return x.compareTo(y);
  }

  @Override
  public boolean equals(Object obj)
  {
    if (obj == this)
      return true;
    if (obj == null)
      return false;
    if (obj instanceof Number)
      return compareTo((Number) obj) == 0;
    return false;
  }

  @Override
  public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((_range == null) ? 0 : _range.hashCode());
    result = prime * result + ((_exponent == null) ? 0 : _exponent.hashCode());
    result = prime * result + ((_significand == null) ? 0 : _significand.hashCode());
    return result;
  }

  /**
   * Like the == operator, ZERO and NEGATIVE_ZERO are equal; NaN is not equal to anything.
   */
  public boolean equalsNumber(BigFloat that)
  {
    if (this.isZero() && that.isZero())
      return true;
    if (this.isNaN() || that.isNaN())
      return false;
    return equals(that);
  }

  public String toString()
  {
    String sign = isNegative() ? "-" : "";
    switch (getRange())
    {
    case NaN:
      return sign + "NaN." + _exponent;
    case INFINITE:
      return sign + "Infinity";
    case FINITE:
    }
    if (isZero())
      return sign + "0";
    return hexFraction(getSignificand()) + "*2**" + getExponent();
  }

  private static String hexFraction(BigInteger n)
  {
    if (n.signum() == 0)
      return "0";
    StringBuilder h = new StringBuilder();
    if (n.signum() < 0)
      h.append("-");
    h.append("1.");
    BigFloatCodec.encodeFraction(h, n.abs());
    return h.toString();
  }
}
