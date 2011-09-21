package com.linkedin.math.bigFloat;

import org.apfloat.Apfloat;

/** Decode the given big floats, and print their values in decimal notation. */
public class Decode
{
  public static void main(String[] args)
  {
    BigApfloatConverter converter = new BigApfloatConverter();
    BigFloatCodec codec = new BigFloatCodec();
    for (String arg : args)
    {
      BigFloat b = codec.decode(arg);
      Apfloat a = converter.toApfloat(b);
      a = a.toRadix(10).precision(10);
      System.out.printf("%s\n", a);
    }
    System.out.println();
  }
}
