package com.linkedin.math.bigFloat;

import org.apfloat.Apfloat;

/** A command line program to decode big floats. */
public class Decode
{
  /** Decode the given big floats, and print their values in decimal notation. */
  public static void main(String[] args)
  {
    BigApfloatConverter converter = new BigApfloatConverter();
    BigFloatCodec codec = new BigFloatCodec();
    for (String arg : args)
    {
      BigFloat b = codec.decode(arg);
      Apfloat a = converter.toApfloat(b);
      a = a.precision(10).toRadix(10).precision(10);
      System.out.printf("%s\n", a);
    }
    System.out.println();
  }
}
