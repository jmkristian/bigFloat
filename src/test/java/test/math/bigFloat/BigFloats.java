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
package test.math.bigFloat;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import com.linkedin.math.bigFloat.BigFloat;

/**
 * @author <a href="mailto:jkristian@linkedin.com">John Kristian</a>
 */
public class BigFloats
{
  public List<String> _strings = new ArrayList<String>();
  public List<Number> _numbers = new ArrayList<Number>();

  public BigFloats()
  {
    // sign, decimal e, hex fraction
    addPair("bf4000", Math.pow(2.0d, +256)); // + 256
    addPair("bf3fe", Math.pow(2.0d, +255)); // + 255
    addPair("bf2fc", Math.pow(2.0d, +127));
    addPair("be2a8", 53); //
    addPair("bc4", 5); //
    addPair("b88", 3.000d); // + 1 8
    addPair("b8", 2.000d); // + 1
    addPair("b0fff", 2d - (1d / 4096));
    addPair("b0ff", 2d - (1d / 256));
    addPair("b0f", 2d - (1d / 16));
    addPair("b08", 1.500d); // + 0 8
    addPair("b02", 1.125d); // + 0 2
    addPair("b0", 1.000d); // + 0
    addPair("a7", 1d / 2); // + -1
    addPair("a34", 5d / 16); //
    addPair("a3", 1d / 4);
    addPair("a19", 1d / 128);
    addPair("a17", 1d / 256);
    addPair("a0d03", Math.pow(2.0d, -127));
    addPair("a0bfff", Math.pow(2.0d, -256));
    addPair("a0bff7", Math.pow(2.0d, -257));
    addPair("a0af9d", Double.MIN_VALUE * 2); // + -1073
    addPair("a0af9b", Double.MIN_VALUE); // + -1074
    addPair("8", 0d);

    addPair("7", -0d);
    addPair("5f5064", -Double.MIN_VALUE); // - 1075
    addPair("5f5062", -Double.MIN_VALUE * 2); // - 1074
    addPair("5f4000", -Math.pow(2.0d, -256)); // - 257
    addPair("5f3fe", -Math.pow(2.0d, -255)); // - 256
    addPair("5f2fc", -Math.pow(2.0d, -127));
    addPair("5e8", -1d / 256); // - 9
    addPair("5e6", -1d / 128); // - 8
    addPair("5c", -1d / 4);
    addPair("58", -1d / 2);
    addPair("50", -1.000d); // - 1
    addPair("47e", -1.125d); // - 0 -2
    addPair("478", -1.500d); // - 0 -8
    addPair("471", -(2d - (1d / 16)));
    addPair("4701", -(2d - (1d / 256)));
    addPair("47001", -(2d - (1d / 4096)));
    addPair("47", -2.000d); // - 0
    addPair("438", -3.000d); // - -1 -8
    addPair("40d03", -Math.pow(2.0d, 127));
    addPair("42c", -5d); //
    addPair("40bfff", -Math.pow(2.0d, 256)); // - -255
    addPair("40bff7", -Math.pow(2.0d, 257)); // - -256
  }

  public void addBig()
  {
    addPair("ff4118", Double.longBitsToDouble(0x7ff8000000000123L)); // NaN
    addPair("f8", Double.longBitsToDouble(0x7ff8000000000001L)); // NaN
    addPair("f0", Double.longBitsToDouble(0x7ff8000000000000L)); // NaN
    addPair("c", BigFloat.INFINITY); // 1 infinity
    addPair("c", Double.POSITIVE_INFINITY); // 1 infinity
    addPair("bf4ffcfffffffffffff", Double.MAX_VALUE); // 1 1023 11...
    addPair("bf1f00000000000000004", BigInteger.ONE.shiftLeft(62).not().negate());

    addPair("40e07fffffffffffffffc", BigInteger.ONE.shiftLeft(62).not());
    addPair("40afff0000000000001", -Double.MAX_VALUE); // 0 -(1023 11...)
    addPair("3", Double.NEGATIVE_INFINITY); // 0 -infinity
    addPair("3", BigFloat.NEGATIVE_INFINITY);
    addPair("10", Double.longBitsToDouble(0xfff8000000000000L)); // NaN
  }

  public void addPair(String s, Number n)
  {
    _strings.add(s);
    _numbers.add(n);
  }

}
