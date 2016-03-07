/*******************************************************************************
 * Copyright (c) 2009 Luaj.org. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 ******************************************************************************/
package org.luaj.vm2;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import nl.weeaboo.lua2.io.LuaSerializable;

import org.luaj.vm2.lib.MathLib;

/**
 * Extension of {@link LuaNumber} which can hold a Java int as its value.
 * <p>
 * These instance are not instantiated directly by clients, but indirectly via
 * the static functions {@link LuaValue#valueOf(int)} or
 * {@link LuaValue#valueOf(double)} functions. This ensures that policies
 * regarding pooling of instances are encapsulated.
 * <p>
 * There are no API's specific to LuaInteger that are useful beyond what is
 * already exposed in {@link LuaValue}.
 * 
 * @see LuaValue
 * @see LuaNumber
 * @see LuaDouble
 * @see LuaValue#valueOf(int)
 * @see LuaValue#valueOf(double)
 */
@LuaSerializable
public final class LuaInteger extends LuaNumber implements Externalizable {

	private static final LuaInteger[] intValues = new LuaInteger[512];
	static {
		for (int i = 0; i < 512; i++) {
			intValues[i] = new LuaInteger(i - 256);
		}
	}

	public static LuaInteger valueOf(int i) {
		if (i <= 255 && i >= -256) {
			return intValues[i + 256];
		}
		return new LuaInteger(i);
	}
	
	/**
	 * Return a LuaNumber that represents the value provided
	 * 
	 * @param l long value to represent.
	 * @return LuaNumber that is eithe LuaInteger or LuaDouble representing l
	 * @see LuaValue#valueOf(int)
	 * @see LuaValue#valueOf(double)
	 */
	public static LuaNumber valueOf(long l) {
		int i = (int) l;
		if (i == l) {
			return valueOf(i);
		} else {
			return LuaDouble.valueOf(l);
		}
	}

	/** The value being held by this instance. */
	private int v;

	/**
	 * Do not use. Required for efficient serialization. 
	 */
	@Deprecated
	public LuaInteger() {		
	}
	
	/**
	 * Package protected constructor.
	 * 
	 * @see LuaValue#valueOf(int)
	 **/
	LuaInteger(int i) {
		this.v = i;
	}

	protected Object readResolve() {
		// Special serialization returning the cached values
		return valueOf(v);
	}
	
	@Override
	public boolean isint() {
		return true;
	}

	@Override
	public boolean isinttype() {
		return true;
	}

	@Override
	public boolean islong() {
		return true;
	}

	@Override
	public byte tobyte() {
		return (byte) v;
	}

	@Override
	public char tochar() {
		return (char) v;
	}

	@Override
	public double todouble() {
		return v;
	}

	@Override
	public float tofloat() {
		return v;
	}

	@Override
	public int toint() {
		return v;
	}

	@Override
	public long tolong() {
		return v;
	}

	@Override
	public short toshort() {
		return (short) v;
	}

	@Override
	public double optdouble(double defval) {
		return v;
	}

	@Override
	public int optint(int defval) {
		return v;
	}

	@Override
	public LuaInteger optinteger(LuaInteger defval) {
		return this;
	}

	@Override
	public long optlong(long defval) {
		return v;
	}

	@Override
	public String tojstring() {
		return Integer.toString(v);
	}

	@Override
	public LuaString strvalue() {
		return LuaString.valueOf(Integer.toString(v));
	}

	@Override
	public LuaString optstring(LuaString defval) {
		return LuaString.valueOf(Integer.toString(v));
	}

	@Override
	public LuaValue tostring() {
		return LuaString.valueOf(Integer.toString(v));
	}

	@Override
	public String optjstring(String defval) {
		return Integer.toString(v);
	}

	@Override
	public LuaInteger checkinteger() {
		return this;
	}

	@Override
	public boolean isstring() {
		return true;
	}

	@Override
	public int hashCode() {
		return v;
	}

	// unary operators
	@Override
	public LuaValue neg() {
		return valueOf(-(long) v);
	}

	// object equality, used for key comparison
	@Override
	public boolean equals(Object o) {
		return o instanceof LuaInteger ? ((LuaInteger) o).v == v : false;
	}

	// equality w/ metatable processing
	@Override
	public LuaValue eq(LuaValue val) {
		return val.raweq(v) ? TRUE : FALSE;
	}

	@Override
	public boolean eq_b(LuaValue val) {
		return val.raweq(v);
	}

	// equality w/o metatable processing
	@Override
	public boolean raweq(LuaValue val) {
		return val.raweq(v);
	}

	@Override
	public boolean raweq(double val) {
		return v == val;
	}

	@Override
	public boolean raweq(int val) {
		return v == val;
	}

	// arithmetic operators
	@Override
	public LuaValue add(LuaValue rhs) {
		return rhs.add(v);
	}

	@Override
	public LuaValue add(double lhs) {
		return LuaDouble.valueOf(lhs + v);
	}

	@Override
	public LuaValue add(int lhs) {
		return LuaInteger.valueOf(lhs + (long) v);
	}

	@Override
	public LuaValue sub(LuaValue rhs) {
		return rhs.subFrom(v);
	}

	@Override
	public LuaValue sub(double rhs) {
		return LuaDouble.valueOf(v - rhs);
	}

	@Override
	public LuaValue sub(int rhs) {
		return LuaValue.valueOf(v - rhs);
	}

	@Override
	public LuaValue subFrom(double lhs) {
		return LuaDouble.valueOf(lhs - v);
	}

	@Override
	public LuaValue subFrom(int lhs) {
		return LuaInteger.valueOf(lhs - (long) v);
	}

	@Override
	public LuaValue mul(LuaValue rhs) {
		return rhs.mul(v);
	}

	@Override
	public LuaValue mul(double lhs) {
		return LuaDouble.valueOf(lhs * v);
	}

	@Override
	public LuaValue mul(int lhs) {
		return LuaInteger.valueOf(lhs * (long) v);
	}

	@Override
	public LuaValue pow(LuaValue rhs) {
		return rhs.powWith(v);
	}

	@Override
	public LuaValue pow(double rhs) {
		return valueOf(MathLib.getInstance().dpow(v, rhs));
	}

	@Override
	public LuaValue pow(int rhs) {
		return valueOf(MathLib.getInstance().dpow(v, rhs));
	}

	@Override
	public LuaValue powWith(double lhs) {
		return valueOf(MathLib.getInstance().dpow(lhs, v));
	}

	@Override
	public LuaValue powWith(int lhs) {
		return valueOf(MathLib.getInstance().dpow(lhs, v));
	}

	@Override
	public LuaValue div(LuaValue rhs) {
		return rhs.divInto(v);
	}

	@Override
	public LuaValue div(double rhs) {
		return LuaDouble.ddiv(v, rhs);
	}

	@Override
	public LuaValue div(int rhs) {
		return LuaDouble.ddiv(v, rhs);
	}

	@Override
	public LuaValue divInto(double lhs) {
		return LuaDouble.ddiv(lhs, v);
	}

	@Override
	public LuaValue mod(LuaValue rhs) {
		return rhs.modFrom(v);
	}

	@Override
	public LuaValue mod(double rhs) {
		return LuaDouble.dmod(v, rhs);
	}

	@Override
	public LuaValue mod(int rhs) {
		return LuaDouble.dmod(v, rhs);
	}

	@Override
	public LuaValue modFrom(double lhs) {
		return LuaDouble.dmod(lhs, v);
	}

	// relational operators
	@Override
	public LuaValue lt(LuaValue rhs) {
		return rhs.gt_b(v) ? TRUE : FALSE;
	}

	@Override
	public LuaValue lt(double rhs) {
		return v < rhs ? TRUE : FALSE;
	}

	@Override
	public LuaValue lt(int rhs) {
		return v < rhs ? TRUE : FALSE;
	}

	@Override
	public boolean lt_b(LuaValue rhs) {
		return rhs.gt_b(v);
	}

	@Override
	public boolean lt_b(int rhs) {
		return v < rhs;
	}

	@Override
	public boolean lt_b(double rhs) {
		return v < rhs;
	}

	@Override
	public LuaValue lteq(LuaValue rhs) {
		return rhs.gteq_b(v) ? TRUE : FALSE;
	}

	@Override
	public LuaValue lteq(double rhs) {
		return v <= rhs ? TRUE : FALSE;
	}

	@Override
	public LuaValue lteq(int rhs) {
		return v <= rhs ? TRUE : FALSE;
	}

	@Override
	public boolean lteq_b(LuaValue rhs) {
		return rhs.gteq_b(v);
	}

	@Override
	public boolean lteq_b(int rhs) {
		return v <= rhs;
	}

	@Override
	public boolean lteq_b(double rhs) {
		return v <= rhs;
	}

	@Override
	public LuaValue gt(LuaValue rhs) {
		return rhs.lt_b(v) ? TRUE : FALSE;
	}

	@Override
	public LuaValue gt(double rhs) {
		return v > rhs ? TRUE : FALSE;
	}

	@Override
	public LuaValue gt(int rhs) {
		return v > rhs ? TRUE : FALSE;
	}

	@Override
	public boolean gt_b(LuaValue rhs) {
		return rhs.lt_b(v);
	}

	@Override
	public boolean gt_b(int rhs) {
		return v > rhs;
	}

	@Override
	public boolean gt_b(double rhs) {
		return v > rhs;
	}

	@Override
	public LuaValue gteq(LuaValue rhs) {
		return rhs.lteq_b(v) ? TRUE : FALSE;
	}

	@Override
	public LuaValue gteq(double rhs) {
		return v >= rhs ? TRUE : FALSE;
	}

	@Override
	public LuaValue gteq(int rhs) {
		return v >= rhs ? TRUE : FALSE;
	}

	@Override
	public boolean gteq_b(LuaValue rhs) {
		return rhs.lteq_b(v);
	}

	@Override
	public boolean gteq_b(int rhs) {
		return v >= rhs;
	}

	@Override
	public boolean gteq_b(double rhs) {
		return v >= rhs;
	}

	// string comparison
	@Override
	public int strcmp(LuaString rhs) {
		typerror("attempt to compare number with string");
		return 0;
	}

	@Override
	public int checkint() {
		return v;
	}

	@Override
	public long checklong() {
		return v;
	}

	@Override
	public double checkdouble() {
		return v;
	}

	@Override
	public String checkjstring() {
		return String.valueOf(v);
	}

	@Override
	public LuaString checkstring() {
		return valueOf(String.valueOf(v));
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt(v);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		v = in.readInt();
	}

}
