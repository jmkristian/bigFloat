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

import java.math.BigInteger;

/**
 * Algorithms to convert a binary floating point number to or from a printable (but not
 * readily understandable) String. No information is lost in conversion. The natural
 * ordering of the strings is the same as the natural ordering of the corresponding
 * numbers. The string representation is reasonably compact, and the conversion algorithms
 * are reasonably fast.
 * <p>
 * The string representation is the hexadecimal encoding of:
 * <ul>
 * <li>4 bits (one hex character) encoding the sign and range:
 * <ul>
 * <li>0: negative NaN with a positive payload
 * <li>1: negative NaN with a negative payload
 * <li>3: NEGATIVE_INFINITY
 * <li>4: negative significand with positive exponent
 * <li>5: negative significand with negative exponent
 * <li>7: NEGATIVE_ZERO
 * <li>8: ZERO
 * <li>a: positive significand with negative exponent
 * <li>b: positive significand with positive exponent
 * <li>c: INFINITY
 * <li>e: positive NaN with a negative payload
 * <li>f: positive NaN with a positive payload
 * </ul>
 * <li>the exponent, encoded as follows:
 * <ul>
 * <li>if the significand is negative, invert the exponent (~ operator) and, if the
 * significand is one (no fractional bits), add 1 (+ operator)
 * <li>encode the absolute value, using <a
 * href="http://en.wikipedia.org/wiki/Levenshtein_coding">Levenshtein encoding</a>
 * <li>append 0 bits to a multiple of 4 bits
 * <li>if the significand is negative or the exponent is negative but not both, invert the
 * bits (~ operator)
 * </ul>
 * <li>the fractional bits of the significand, in two's complement representation, with 0
 * bits appended to a multiple of 4 bits
 * </ul>
 * A NaN is encoded as though it were a power of 2 (no fractional bits) with the payload in
 * place of the exponent.
 * 
 * @author <a href="mailto:jkristian@linkedin.com">John Kristian</a>
 */
public class BigFloatCodec
{
  private static final String NEGATIVE_INFINITY = "3";
  private static final String NEGATIVE_ZERO = "7";
  private static final String ZERO = "8";
  private static final String INFINITY = "c";
  private static final char[] HEX = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

  private final boolean _trace;

  public BigFloatCodec()
  {
    this(false);
  }

  public BigFloatCodec(boolean trace)
  {
    _trace = trace;
  }

  /** Convert a number to its string representation. */
  public String encode(BigFloat n)
  {
    switch (n.getRange())
    {
    case NaN:
    {
      StringBuilder into = new StringBuilder();
      BigInteger e = n.getNaNPayload();
      if (!n.isNegative())
        // Encode the payload as we would an exponent
        // with a significand == 1 (no fraction).
        into.append((e.signum() < 0) ? "e" : "f");
      else
      {
        into.append((e.signum() > 0) ? "0" : "1");
        e = e.negate();
      }
      encodeInteger(new BitStringBuilder(into), e);
      return into.toString();
    }
    case INFINITE:
      return n.isNegative() ? NEGATIVE_INFINITY : INFINITY;
    default:
    }
    if (n.isZero())
      return n.isNegative() ? NEGATIVE_ZERO : ZERO;

    // Encode a finite non-zero number:
    StringBuilder traceMessage = _trace ? new StringBuilder("encode(" + n + ")") : null;
    StringBuilder into = new StringBuilder();
    BigInteger e = n.getExponent();
    BigInteger s = n.getSignificand();
    if (!n.isNegative())
      into.append((e.signum() < 0) ? "a" : "b");
    else
    {
      if (s.bitLength() == 0) // significand == -1
        e = e.negate();
      else
        // significand < -1
        e = e.not();
      into.append((e.signum() < 0) ? "4" : "5");
    }
    BitStringBuilder bits = new BitStringBuilder(into);
    encodeInteger(bits, e);
    if (traceMessage != null)
      traceMessage.append(" -> " + into);
    encodeFraction(bits, s);
    String result = into.toString();
    if (traceMessage != null)
      trace(traceMessage + " = " + result);
    return result;
  }

  /** Convert a string representation to a number. */
  public BigFloat decode(CharSequence from)
      throws NumberFormatException
  {
    StringBuilder traceMessage = _trace ? new StringBuilder("decode(" + from + ")") : null;
    try
    {
      BitBuffer bits = new BitBuffer(from);
      final int c0 = (int) bits.getBits(4);
      final boolean negative = c0 < 8;
      final boolean eNegative = (c0 & 1) == 0;
      bits.setInvert(eNegative);
      switch (c0)
      {
      case 0:
        return BigFloat.valueOfNaN(true, decodeInteger(bits));
      case 1:
        return BigFloat.valueOfNaN(true, decodeInteger(bits).negate());
      case 3:
        return BigFloat.NEGATIVE_INFINITY;
      case 7:
        return BigFloat.NEGATIVE_ZERO;
      case 8:
        return BigFloat.ZERO;
      case 0xC:
        return BigFloat.INFINITY;
      case 0xE:
        return BigFloat.valueOfNaN(false, decodeInteger(bits).negate());
      case 0xF:
        return BigFloat.valueOfNaN(false, decodeInteger(bits));
      default:
      }
      BigInteger e = decodeInteger(bits);
      BigInteger s = bits.getFraction(negative);
      if (eNegative)
        e = e.negate();
      if (negative)
      {
        if (s.bitLength() == 1) // significand == -1
          e = e.negate();
        else
          // significand < -1
          e = e.not();
      }
      BigFloat result = new BigFloat(s, e);
      if (traceMessage != null)
        trace(traceMessage + " = BigFloat(" + hexInteger(s) + "," + e + ")" + " = " + result);
      return result;
    }
    catch (RuntimeException e)
    {
      StringBuilder msg = new StringBuilder(from);
      msg.append(" " + e.getMessage());
      if (traceMessage != null)
        msg.append(" (" + traceMessage + ")");
      RuntimeException wrapper = new NumberFormatException(msg.toString());
      wrapper.initCause(e);
      throw wrapper;
    }
  }

  /**
   * Encode an integer, using Levenshtein encoding, with zero bits appended to a multiple
   * of 4 bits. If the given number is negative, encode its absolute value and invert the
   * encoded bits. Finally, convert to hexadecimal.
   */
  public void encodeInteger(StringBuilder into, BigInteger n)
  {
    encodeInteger(new BitStringBuilder(into), n);
  }

  private void encodeInteger(BitStringBuilder into, BigInteger n)
  {
    if (n.signum() == 0)
      into.append(4, 0);
    else
    {
      StringBuilder traceMessage = _trace ? new StringBuilder("encodeInteger(" + n + ")") : null;
      into.setInvert(n.signum() < 0);
      BigInteger m = n.abs();
      int mBits = m.bitLength() - 1; // omit the leading 1
      encodeSize(into, 1, mBits);
      if (traceMessage != null)
        traceMessage.append(String.format("; pBits=%d; mBits=%d -> %x+%s",
                                          into._length,
                                          mBits,
                                          into._bits,
                                          hexInteger(m)));
      if (mBits != 0)
        into.append(mBits, m.toByteArray());
      if (traceMessage != null)
        trace(traceMessage + String.format(" -> %x+%s", into._bits, hexInteger(m)));
    }
    into.flush();
  }

  /** Insert the size of a number into the builder, recursively. */
  private static void encodeSize(BitStringBuilder into, int c, long m)
  {
    if (m <= 0)
      into.insert(c + 1, ((1L << c) - 1) << 1);
    else
    {
      int inserted = bitLength(m) - 1;
      into.insert(inserted, m);
      encodeSize(into, c + 1, inserted);
    }
  }

  private static short bitLength(long n)
  {
    short length = 0;
    while (n != 0)
    {
      ++length;
      n >>>= 1;
    }
    return length;
  }

  private static void encodeFraction(BitStringBuilder into, BigInteger n)
  {
    into.setInvert(false);
    if (n.signum() == 0)
      return;
    BigInteger a = n;
    { // Align to a multiple of 4 bits:
      int padBits = 3 - ((a.bitLength() + 2) % 4);
      if (padBits != 0)
        a = a.shiftLeft(padBits);
    }
    byte[] bytes = a.toByteArray();
    if (((a.bitLength() / 4) % 2) != 0)
      into.append(4, bytes[0]);
    for (int b = 1; b < bytes.length; ++b)
    {
      into.append(8, bytes[b]);
    }
    into.flush();
  }

  static void encodeFraction(StringBuilder into, BigInteger n)
  {
    encodeFraction(new BitStringBuilder(into), n);
  }

  /** Decode an exponent or NaN payload. */
  public BigInteger decodeInteger(CharSequence from)
  {
    BitBuffer bits = new BitBuffer(from);
    boolean negative = ((bits.getBits(4)) & 1) == 0;
    bits.setInvert(negative);
    BigInteger n = decodeInteger(bits);
    if (negative)
      n = n.negate();
    return n;
  }

  private BigInteger decodeInteger(BitBuffer from)
  {
    try
    {
      // Decode the preamble, and then read the actual number separately.
      final long count = from.getNatural();
      if (count == 0)
        return BigInteger.ZERO;
      else if (count == 1)
        return BigInteger.ONE;

      long nBits = 1;
      for (int i = 2; i < count; ++i)
        nBits = from.getInteger(nBits);
      BigInteger n = from.getBigInteger(nBits);
      return n;
    }
    finally
    {
      from._length -= (from._length % 4);
    }
  }

  private static String hexInteger(BigInteger n)
  {
    return n.toString(16);
  }

  /** A source of bits, decoded from hexadecimal. */
  static class BitBuffer
  {
    private final CharSequence _from;
    private int _next = 0; // index into _from
    private boolean _invert = false;
    private int _length = 0; // number of bits
    private long _bits = 0;

    BitBuffer(CharSequence from)
    {
      _from = from;
    }

    void setInvert(boolean invert)
    {
      _invert = invert;
    }

    /**
     * Get a natural number; that is a sequence of 1 bits followed by a zero bit.
     * 
     * @returns the number of 1 bits
     */
    long getNatural()
    {
      int n = 0;
      while (getBits(1) != 0)
        ++n;
      return n;
    }

    /**
     * Get a positive integer, consisting of a binary 1 followed by numBits.
     */
    long getInteger(long numBits)
    {
      if (numBits >= 62)
      {
      if (numBits >= 63)
          // The result would be > Long.MAX_VALUE.
          throw new NumberFormatException(_from + " Levenshtein number " + (numBits + 1) + " > 63 bits");
        // We might need space for up to 3 more bits (from the last hex digit),
        // which would overflow the _bits buffer.
        return (getInteger(numBits - 1) << 1) | getBits(1);
    }
      return (1L << numBits) // the implicit initial 1 bit
          | getBits((int) numBits);
    }

    /** The number of bits in the largest possible BigInteger. */
    private static final long MAX_BITS = (((long) Integer.MAX_VALUE) * 8) - 1;

    /**
     * Get a positive integer, consisting of a binary 1 followed by numBits.
     */
    BigInteger getBigInteger(long numBits)
    {
      if (numBits >= MAX_BITS)
        throw new NumberFormatException(_from + " Levenshtein number " + (numBits + 1) + " > " + MAX_BITS + " bits");
      /*
       * Copy the bits into an array of bytes, from which to construct a BigInteger. Allow
       * space for the initial 1 bit. Also, insert a zero byte at the beginning, if needed
       * to make the BigInteger non-negative. (BigInteger interprets the most significant
       * bit of the first byte as a sign bit.)
       */
      final int numBytes = (int) (((numBits + 1) / 8) + 1);
      byte[] bytes = new byte[numBytes];
      int b = 0;
      {
        int chunk = (int) (numBits % 8);
        if (chunk == 7)
          bytes[b++] = 0; // to make the BigInteger non-negative
        bytes[b++] = (byte) ((1 << chunk) // the implicit initial 1 bit
            | getBits(chunk));
      }
      while (b < bytes.length)
        bytes[b++] = (byte) getBits(8);
      return new BigInteger(bytes);
    }

    BigInteger getFraction(boolean negative)
    {
      int firstNybble = negative ? -2 : 1;
      setInvert(false);
      int length = ((_length + 3) / 4) + _from.length() - _next;
      byte[] bytes = new byte[(length + 2) / 2];
      int i = 2 - (length % 2);
      bytes[0] = (byte) ((i == 2) ? firstNybble : firstNybble << 4);
      while (_length >= 4 || _next < _from.length())
      {
        if ((i & 1) == 0)
          bytes[i / 2] |= (getBits(4) << 4);
        else
          bytes[i / 2] |= (getBits(4) & 0xF);
        ++i;
      }
      if (_length > 0)
      {
        int pad = 4 - _length;
        bytes[i / 2] |= getBits(_length) << pad;
      }
      BigInteger result = new BigInteger(bytes);
      return result;
    }

    /** Get the given number of bits, as a non-negative number. */
    private long getBits(int numBits)
    {
      while (_length < numBits)
      {
        int nybble = fromHex(_from.charAt(_next++));
        if (_invert)
          nybble = ~nybble;
        _bits = (_bits << 4) | (nybble & 0xF);
        _length += 4;
      }
      _length -= numBits;
      return (_bits >>> _length) & ((1L << numBits) - 1);
    }

    public String toString()
    {
      return hexString(_from);
    }

    private static String hexString(CharSequence from)
    {
      StringBuilder into = new StringBuilder(from + " = ");
      for (int f = 0; f < from.length(); ++f)
      {
        int b = fromHex(from.charAt(f));
        if (f > 0)
          into.append(" ");
        into.append((b >> 3) & 1).append((b >> 2) & 1).append((b >> 1) & 1).append(b & 1);
      }
      return into.toString();
    }

    private static byte fromHex(char c)
    {
      int d = Character.digit(c, 16);
      if (d < 0)
        throw new NumberFormatException("'" + c + "' is not a hexadecimal digit");
      return (byte) d;
    }

  }

  /** Converts bits into a hexadecimal string. */
  private static class BitStringBuilder
  {
    private final StringBuilder _into;
    private boolean _invert = false;
    private long _bits = 0;
    private int _length = 0; // number of bits

    BitStringBuilder(StringBuilder into)
    {
      _into = into;
    }

    void setInvert(boolean invert)
    {
      _invert = invert;
    }

    /** Insert bits at the beginning of the string. */
    void insert(int numBits, long bits)
    {
      _bits |= (bits & ((1L << numBits) - 1)) << _length;
      _length += numBits;
      if (_length > 64)
        throw new ArithmeticException("Levenshtein preamble > 64 bits");
    }

    /** Add bits to the end of the string. */
    void append(int numBits, long bits)
    {
      if (_length + numBits <= 64)
      {
        _bits = (_bits << numBits) | (bits & ((1L << numBits) - 1));
        _length += numBits;
      }
      else
      {
        int partial = 64 - _length;
        append(partial, (bits >>> (numBits - partial)));
        flush();
        _bits = bits;
        _length = numBits - partial;
      }
    }

    /** Add bits to the end of the string. */
    void append(int numBits, byte[] bits)
    {
      int chunk = numBits % 8;
      if (chunk == 0)
        chunk = 8;
      for (int b = ((bits.length * 8) - numBits) / 8; b < bits.length; ++b)
      {
        append(chunk, bits[b]);
        chunk = 8;
      }
    }

    /**
     * Flush all the buffered bits into the StringBuilder, with zero padding at the end if
     * needed.
     */
    void flush()
    {
      while (_length > 0)
      {
        int n = (int) (((_length -= 4) >= 0) ? _bits >> _length : _bits << -_length);
        if (_invert)
          n = ~n;
        _into.append(HEX[n & 0xF]);
      }
      _length = 0;
    }
  }

  private void trace(String msg)
  {
    if (_trace)
      System.out.println(msg);
  }

}
