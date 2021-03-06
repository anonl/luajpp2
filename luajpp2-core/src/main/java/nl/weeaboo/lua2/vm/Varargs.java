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

package nl.weeaboo.lua2.vm;

import static nl.weeaboo.lua2.vm.LuaConstants.NONE;
import static nl.weeaboo.lua2.vm.LuaNil.NIL;

import java.io.Serializable;

import javax.annotation.Nullable;

import nl.weeaboo.lua2.LuaException;
import nl.weeaboo.lua2.io.LuaSerializable;

/**
 * Class to encapsulate varargs values, either as part of a variable argument list, or multiple return values.
 * <p>
 * To construct varargs, use one of the static methods such as {@code LuaValue.varargsOf(LuaValue,LuaValue)}
 * <p>
 * <p>
 * Any LuaValue can be used as a stand-in for Varargs, for both calls and return values. When doing so,
 * nargs() will return 1 and arg1() or arg(1) will return this. This simplifies the case when calling or
 * implementing varargs functions with only 1 argument or 1 return value.
 * <p>
 * Varargs can also be derived from other varargs by appending to the front with a call such as
 * {@code LuaValue.varargsOf(LuaValue,Varargs)} or by taking a portion of the args using
 * {@code Varargs.subargs(int start)}
 * <p>
 *
 * @see LuaValue#varargsOf(LuaValue[])
 * @see LuaValue#varargsOf(LuaValue, Varargs)
 * @see LuaValue#varargsOf(LuaValue[], Varargs)
 * @see LuaValue#varargsOf(LuaValue, LuaValue, Varargs)
 * @see LuaValue#varargsOf(LuaValue[], int, int)
 * @see LuaValue#varargsOf(LuaValue[], int, int, Varargs)
 * @see LuaValue#subargs(int)
 */
public abstract class Varargs {

    /**
     * Get the n-th argument value (1-based).
     *
     * @param i the index of the argument to get, 1 is the first argument
     * @return Value at position i, or LuaValue.NIL if there is none.
     * @see Varargs#arg1()
     */
    public abstract LuaValue arg(int i);

    /**
     * Get the number of arguments, or 0 if there are none.
     *
     * @return number of arguments.
     */
    public abstract int narg();

    /**
     * Get the first argument in the list.
     *
     * @return LuaValue which is first in the list, or LuaValue.NIL if there are no values.
     * @see Varargs#arg(int)
     */
    public abstract LuaValue arg1();

    // -----------------------------------------------------------------------
    // utilities to get specific arguments and type-check them.
    // -----------------------------------------------------------------------

    /**
     * Gets the type of argument {@code i}.
     *
     * @param i the index of the argument to convert, 1 is the first argument
     * @return int value corresponding to one of the LuaValue integer type values
     */
    public int type(int i) {
        return arg(i).type();
    }

    /**
     * Tests if argument i is nil.
     *
     * @param i the index of the argument to test, 1 is the first argument
     * @return true if the argument is nil or does not exist, false otherwise
     */
    public boolean isnil(int i) {
        return arg(i).isnil();
    }

    /**
     * Tests if argument i is a function.
     *
     * @param i the index of the argument to test, 1 is the first argument
     * @return true if the argument exists and is a function or closure, false otherwise
     */
    public boolean isfunction(int i) {
        return arg(i).isfunction();
    }

    /**
     * Tests if argument i is a number. Since anywhere a number is required, a string can be used that is a
     * number, this will return true for both numbers and strings that can be interpreted as numbers.
     *
     * @param i the index of the argument to test, 1 is the first argument
     * @return true if the argument exists and is a number or string that can be interpreted as a number,
     *         false otherwise
     */
    public boolean isnumber(int i) {
        return arg(i).isnumber();
    }

    /**
     * Tests if argument i is a string. Since all lua numbers can be used where strings are used, this will
     * return true for both strings and numbers.
     *
     * @param i the index of the argument to test, 1 is the first argument
     * @return true if the argument exists and is a string or number, false otherwise
     */
    public boolean isstring(int i) {
        return arg(i).isstring();
    }

    /**
     * Tests if argument i is a table.
     *
     * @param i the index of the argument to test, 1 is the first argument
     * @return true if the argument exists and is a lua table, false otherwise
     */
    public boolean istable(int i) {
        return arg(i).istable();
    }

    /**
     * Tests if argument i is a thread.
     *
     * @param i the index of the argument to test, 1 is the first argument
     * @return true if the argument exists and is a lua thread, false otherwise
     */
    public boolean isthread(int i) {
        return arg(i).isthread();
    }

    /**
     * Tests if argument i is a userdata.
     *
     * @param i the index of the argument to test, 1 is the first argument
     * @return true if the argument exists and is a userdata, false otherwise
     */
    public boolean isuserdata(int i) {
        return arg(i).isuserdata();
    }

    /**
     * Tests if a value exists at argument i.
     *
     * @param i the index of the argument to test, 1 is the first argument
     * @return true if the argument exists, false otherwise
     */
    public boolean isvalue(int i) {
        return i > 0 && i <= narg();
    }

    /**
     * Return argument i as a boolean value, {@code defval} if nil, or throw a LuaError if any other type.
     *
     * @param i the index of the argument to test, 1 is the first argument
     * @return true if argument i is boolean true, false if it is false, or defval if not supplied or nil
     * @exception LuaException if the argument is not a lua boolean
     */
    public boolean optboolean(int i, boolean defval) {
        return arg(i).optboolean(defval);
    }

    /**
     * Return argument i as a closure, {@code defval} if nil, or throw a LuaError if any other type.
     *
     * @param i the index of the argument to test, 1 is the first argument
     * @return LuaClosure if argument i is a closure, or defval if not supplied or nil
     * @exception LuaException if the argument is not a lua closure
     */
    public LuaClosure optclosure(int i, LuaClosure defval) {
        return arg(i).optclosure(defval);
    }

    /**
     * Return argument i as a double, {@code defval} if nil, or throw a LuaError if it cannot be converted to
     * one.
     *
     * @param i the index of the argument to test, 1 is the first argument
     * @return java double value if argument i is a number or string that converts to a number, or defval if
     *         not supplied or nil
     * @exception LuaException if the argument is not a number
     */
    public double optdouble(int i, double defval) {
        return arg(i).optdouble(defval);
    }

    /**
     * Return argument i as a function, {@code defval} if nil, or throw a LuaError if an incompatible type.
     *
     * @param i the index of the argument to test, 1 is the first argument
     * @return LuaValue that can be called if argument i is lua function or closure, or defval if not supplied
     *         or nil
     * @exception LuaException if the argument is not a lua function or closure
     */
    public LuaFunction optfunction(int i, LuaFunction defval) {
        return arg(i).optfunction(defval);
    }

    /**
     * Return argument i as a java int value, discarding any fractional part, {@code defval} if nil, or throw
     * a LuaError if not a number.
     *
     * @param i the index of the argument to test, 1 is the first argument
     * @return int value with fraction discarded and truncated if necessary if argument i is number, or defval
     *         if not supplied or nil
     * @exception LuaException if the argument is not a number
     */
    public int optint(int i, int defval) {
        return arg(i).optint(defval);
    }

    /**
     * Return argument i as a java int value, {@code defval} if nil, or throw a LuaError if not a number or is
     * not representable by a java int.
     *
     * @param i the index of the argument to test, 1 is the first argument
     * @return LuaInteger value that fits in a java int without rounding, or defval if not supplied or nil
     * @exception LuaException if the argument cannot be represented by a java int value
     */
    public LuaInteger optinteger(int i, LuaInteger defval) {
        return arg(i).optinteger(defval);
    }

    /**
     * Return argument i as a java long value, discarding any fractional part, {@code defval} if nil, or throw
     * a LuaError if not a number.
     *
     * @param i the index of the argument to test, 1 is the first argument
     * @return long value with fraction discarded and truncated if necessary if argument i is number, or
     *         defval if not supplied or nil
     * @exception LuaException if the argument is not a number
     */
    public long optlong(int i, long defval) {
        return arg(i).optlong(defval);
    }

    /**
     * Return argument i as a LuaNumber, {@code defval} if nil, or throw a LuaError if not a number or string
     * that can be converted to a number.
     *
     * @param i the index of the argument to test, 1 is the first argument, or defval if not supplied or nil
     * @return LuaNumber if argument i is number or can be converted to a number
     * @exception LuaException if the argument is not a number
     */
    public LuaNumber optnumber(int i, LuaNumber defval) {
        return arg(i).optnumber(defval);
    }

    /**
     * Return argument i as a java String if a string or number, {@code defval} if nil, or throw a LuaError if
     * any other type.
     *
     * @param i the index of the argument to test, 1 is the first argument
     * @return String value if argument i is a string or number, or defval if not supplied or nil
     * @exception LuaException if the argument is not a string or number
     */
    public String optjstring(int i, String defval) {
        return arg(i).optjstring(defval);
    }

    /**
     * Return argument i as a LuaString if a string or number, {@code defval} if nil, or throw a LuaError if
     * any other type.
     *
     * @param i the index of the argument to test, 1 is the first argument
     * @return LuaString value if argument i is a string or number, or defval if not supplied or nil
     * @exception LuaException if the argument is not a string or number
     */
    public LuaString optstring(int i, LuaString defval) {
        return arg(i).optstring(defval);
    }

    /**
     * Return argument i as a LuaTable if a lua table, {@code defval} if nil, or throw a LuaError if any other
     * type.
     *
     * @param i the index of the argument to test, 1 is the first argument
     * @return LuaTable value if a table, or defval if not supplied or nil
     * @exception LuaException if the argument is not a lua table
     */
    public LuaTable opttable(int i, LuaTable defval) {
        return arg(i).opttable(defval);
    }

    /**
     * Return argument i as a LuaThread if a lua thread, {@code defval} if nil, or throw a LuaError if any
     * other type.
     *
     * @param i the index of the argument to test, 1 is the first argument
     * @return LuaThread value if a thread, or defval if not supplied or nil
     * @exception LuaException if the argument is not a lua thread
     */
    public LuaThread optthread(int i, LuaThread defval) {
        return arg(i).optthread(defval);
    }

    /**
     * Return argument i as a java Object if a userdata, {@code defval} if nil, or throw a LuaError if any
     * other type.
     *
     * @param i the index of the argument to test, 1 is the first argument
     * @return java Object value if argument i is a userdata, or defval if not supplied or nil
     * @exception LuaException if the argument is not a userdata
     */
    public Object optuserdata(int i, Object defval) {
        return arg(i).optuserdata(defval);
    }

    /**
     * Return argument i as a java Object if it is a userdata whose instance Class c or a subclass,
     * {@code defval} if nil, or throw a LuaError if any other type.
     *
     * @param i the index of the argument to test, 1 is the first argument
     * @param c the class to which the userdata instance must be assignable
     * @return java Object value if argument i is a userdata whose instance Class c or a subclass, or defval
     *         if not supplied or nil
     * @exception LuaException if the argument is not a userdata or from whose instance c is not assignable
     */
    public <T> T optuserdata(int i, Class<T> c, T defval) {
        return arg(i).optuserdata(c, defval);
    }

    /**
     * Return argument i as a LuaValue if it exists, or {@code defval}.
     *
     * @param i the index of the argument to test, 1 is the first argument
     * @return LuaValue value if the argument exists, defval if not
     * @exception LuaException if the argument does not exist.
     */
    public LuaValue optvalue(int i, LuaValue defval) {
        return i > 0 && i <= narg() ? arg(i) : defval;
    }

    /**
     * Return argument i as a boolean value, or throw an error if any other type.
     *
     * @param i the index of the argument to test, 1 is the first argument
     * @return true if argument i is boolean true, false if it is false
     * @exception LuaException if the argument is not a lua boolean
     */
    public boolean checkboolean(int i) {
        return arg(i).checkboolean();
    }

    /**
     * Return argument i as a closure, or throw an error if any other type.
     *
     * @param i the index of the argument to test, 1 is the first argument
     * @return LuaClosure if argument i is a closure.
     * @exception LuaException if the argument is not a lua closure
     */
    public LuaClosure checkclosure(int i) {
        return arg(i).checkclosure();
    }

    /**
     * Return argument i as a double, or throw an error if it cannot be converted to one.
     *
     * @param i the index of the argument to test, 1 is the first argument
     * @return java double value if argument i is a number or string that converts to a number
     * @exception LuaException if the argument is not a number
     */
    public double checkdouble(int i) {
        return arg(i).checknumber().todouble();
    }

    /**
     * Return argument i as a function, or throw an error if an incompatible type.
     *
     * @param i the index of the argument to test, 1 is the first argument
     * @return LuaValue that can be called if argument i is lua function or closure
     * @exception LuaException if the argument is not a lua function or closure
     */
    public LuaValue checkfunction(int i) {
        return arg(i).checkfunction();
    }

    /**
     * Return argument i as a java int value, discarding any fractional part, or throw an error if not a
     * number.
     *
     * @param i the index of the argument to test, 1 is the first argument
     * @return int value with fraction discarded and truncated if necessary if argument i is number
     * @exception LuaException if the argument is not a number
     */
    public int checkint(int i) {
        return arg(i).checknumber().toint();
    }

    /**
     * Return argument i as a java int value, or throw an error if not a number or is not representable by a
     * java int.
     *
     * @param i the index of the argument to test, 1 is the first argument
     * @return LuaInteger value that fits in a java int without rounding
     * @exception LuaException if the argument cannot be represented by a java int value
     */
    public LuaInteger checkinteger(int i) {
        return arg(i).checkinteger();
    }

    /**
     * Return argument i as a java long value, discarding any fractional part, or throw an error if not a
     * number.
     *
     * @param i the index of the argument to test, 1 is the first argument
     * @return long value with fraction discarded and truncated if necessary if argument i is number
     * @exception LuaException if the argument is not a number
     */
    public long checklong(int i) {
        return arg(i).checknumber().tolong();
    }

    /**
     * Return argument i as a LuaNumber, or throw an error if not a number or string that can be converted to
     * a number.
     *
     * @param i the index of the argument to test, 1 is the first argument
     * @return LuaNumber if argument i is number or can be converted to a number
     * @exception LuaException if the argument is not a number
     */
    public LuaNumber checknumber(int i) {
        return arg(i).checknumber();
    }

    /**
     * Return argument i as a java String if a string or number, or throw an error if any other type.
     *
     * @param i the index of the argument to test, 1 is the first argument
     * @return String value if argument i is a string or number
     * @exception LuaException if the argument is not a string or number
     */
    public String checkjstring(int i) {
        return arg(i).checkjstring();
    }

    /**
     * Return argument i as a LuaString if a string or number, or throw an error if any other type.
     *
     * @param i the index of the argument to test, 1 is the first argument
     * @return LuaString value if argument i is a string or number
     * @exception LuaException if the argument is not a string or number
     */
    public LuaString checkstring(int i) {
        return arg(i).checkstring();
    }

    /**
     * Return argument i as a LuaTable if a lua table, or throw an error if any other type.
     *
     * @param i the index of the argument to test, 1 is the first argument
     * @return LuaTable value if a table
     * @exception LuaException if the argument is not a lua table
     */
    public LuaTable checktable(int i) {
        return arg(i).checktable();
    }

    /**
     * Return argument i as a LuaThread if a lua thread, or throw an error if any other type.
     *
     * @param i the index of the argument to test, 1 is the first argument
     * @return LuaThread value if a thread
     * @exception LuaException if the argument is not a lua thread
     */
    public LuaThread checkthread(int i) {
        return arg(i).checkthread();
    }

    /**
     * Return argument i as a java Object if a userdata, or throw an error if any other type.
     *
     * @param i the index of the argument to test, 1 is the first argument
     * @return java Object value if argument i is a userdata
     * @exception LuaException if the argument is not a userdata
     */
    public Object checkuserdata(int i) {
        return arg(i).checkuserdata();
    }

    /**
     * Return argument i as a java Object if it is a userdata whose instance Class c or a subclass, or throw
     * an error if any other type.
     *
     * @param i the index of the argument to test, 1 is the first argument
     * @param c the class to which the userdata instance must be assignable
     * @return java Object value if argument i is a userdata whose instance Class c or a subclass
     * @exception LuaException if the argument is not a userdata or from whose instance c is not assignable
     */
    public <T> T checkuserdata(int i, Class<T> c) {
        return arg(i).checkuserdata(c);
    }

    /**
     * Return argument i as a LuaValue if it exists, or throw an error.
     *
     * @param i the index of the argument to test, 1 is the first argument
     * @return LuaValue value if the argument exists
     * @exception LuaException if the argument does not exist.
     */
    public LuaValue checkvalue(int i) {
        if (i <= narg()) {
            return arg(i);
        }
        throw LuaValue.argerror(i, "value expected");
    }

    /**
     * Return argument i as a LuaValue if it is not nil, or throw an error if it is nil.
     *
     * @param i the index of the argument to test, 1 is the first argument
     * @return LuaValue value if the argument is not nil
     * @exception LuaException if the argument doesn't exist or evaluates to nil.
     */
    public LuaValue checknotnil(int i) {
        return arg(i).checknotnil();
    }

    /**
     * Return argument i as a LuaValue when a user-supplied assertion passes, or throw an error.
     *
     * @param test user supplied assertion to test against
     * @param i the index to report in any error message
     * @param msg the error message to use when the test fails
     * @exception LuaException if the the value of {@code test} is {@code false}
     */
    public void argcheck(boolean test, int i, String msg) {
        if (!test) {
            LuaValue.argerror(i, msg);
        }
    }

    /**
     * Return true if there is no argument or nil at argument i.
     *
     * @param i the index of the argument to test, 1 is the first argument
     * @return true if argument i contains either no argument or nil
     */
    public boolean isnoneornil(int i) {
        return i > narg() || arg(i).isnil();
    }

    /**
     * Convert argument {@code i} to java boolean based on lua rules for boolean evaluation.
     *
     * @param i the index of the argument to convert, 1 is the first argument
     * @return {@code false} if argument i is nil or false, otherwise {@code true}
     */
    public boolean toboolean(int i) {
        return arg(i).toboolean();
    }

    /**
     * Return argument i as a java byte value, discarding any fractional part and truncating, or 0 if not a
     * number.
     *
     * @param i the index of the argument to convert, 1 is the first argument
     * @return byte value with fraction discarded and truncated if necessary if argument i is number,
     *         otherwise 0
     */
    public byte tobyte(int i) {
        return arg(i).tobyte();
    }

    /**
     * Return argument i as a java char value, discarding any fractional part and truncating, or 0 if not a
     * number.
     *
     * @param i the index of the argument to convert, 1 is the first argument
     * @return char value with fraction discarded and truncated if necessary if argument i is number,
     *         otherwise 0
     */
    public char tochar(int i) {
        return arg(i).tochar();
    }

    /**
     * Return argument i as a java double value or 0 if not a number.
     *
     * @param i the index of the argument to convert, 1 is the first argument
     * @return double value if argument i is number, otherwise 0
     */
    public double todouble(int i) {
        return arg(i).todouble();
    }

    /**
     * Return argument i as a java float value, discarding excess fractional part and truncating, or 0 if not
     * a number.
     *
     * @param i the index of the argument to convert, 1 is the first argument
     * @return float value with excess fraction discarded and truncated if necessary if argument i is number,
     *         otherwise 0
     */
    public float tofloat(int i) {
        return arg(i).tofloat();
    }

    /**
     * Return argument i as a java int value, discarding any fractional part and truncating, or 0 if not a
     * number.
     *
     * @param i the index of the argument to convert, 1 is the first argument
     * @return int value with fraction discarded and truncated if necessary if argument i is number, otherwise
     *         0
     */
    public int toint(int i) {
        return arg(i).toint();
    }

    /**
     * Return argument i as a java long value, discarding any fractional part and truncating, or 0 if not a
     * number.
     *
     * @param i the index of the argument to convert, 1 is the first argument
     * @return long value with fraction discarded and truncated if necessary if argument i is number,
     *         otherwise 0
     */
    public long tolong(int i) {
        return arg(i).tolong();
    }

    /**
     * Convert the list of varargs values to a human readable java String.
     *
     * @return String value in human readable form such as {1,2}.
     */
    public String tojstring() {
        Buffer sb = new Buffer();
        sb.append("(");
        for (int i = 1, n = narg(); i <= n; i++) {
            if (i > 1) {
                sb.append(",");
            }
            sb.append(arg(i).tojstring());
        }
        sb.append(")");
        return sb.tojstring();
    }

    /**
     * Return argument i as a java String based on the type of the argument.
     *
     * @param i the index of the argument to convert, 1 is the first argument
     * @return String value representing the type
     */
    public String tojstring(int i) {
        return arg(i).tojstring();
    }

    /**
     * Return argument i as a java short value, discarding any fractional part and truncating, or 0 if not a
     * number.
     *
     * @param i the index of the argument to convert, 1 is the first argument
     * @return short value with fraction discarded and truncated if necessary if argument i is number,
     *         otherwise 0
     */
    public short toshort(int i) {
        return arg(i).toshort();
    }

    /**
     * Return argument i as a java Object if a userdata, or null.
     *
     * @param i the index of the argument to convert, 1 is the first argument
     * @return java Object value if argument i is a userdata, otherwise null
     */
    public @Nullable Object touserdata(int i) {
        return arg(i).touserdata();
    }

    /**
     * Return argument i as a java Object if it is a userdata whose instance Class c or a subclass, or null.
     *
     * @param i the index of the argument to convert, 1 is the first argument
     * @param c the class to which the userdata instance must be assignable
     * @return java Object value if argument i is a userdata whose instance Class c or a subclass, otherwise
     *         null
     */
    public @Nullable <T> T touserdata(int i, Class<T> c) {
        return arg(i).touserdata(c);
    }

    /**
     * Convert the value or values to a java String using Varargs.tojstring()
     *
     * @return String value in human readable form.
     * @see Varargs#tojstring()
     */
    @Override
    public String toString() {
        return tojstring();
    }

    /**
     * Create a {@code Varargs} instance containing arguments starting at index {@code start}
     *
     * @param start the index from which to include arguments, where 1 is the first argument.
     * @return Varargs containing argument { start, start+1, ... , narg-start-1 }
     */
    public Varargs subargs(final int start) {
        int end = narg();
        switch (end - start) {
        case 0:
            return arg(start);
        case 1:
            return new PairVarargs(arg(start), arg(end));
        default:
            if (end < start) {
                return NONE;
            } else {
                return new SubVarargs(this, start, end);
            }
        }
    }

    /**
     * Implementation of Varargs for use in the Varargs.subargs() function.
     *
     * @see Varargs#subargs(int)
     */
    @LuaSerializable
    private static class SubVarargs extends Varargs implements Serializable {

        private static final long serialVersionUID = 4816221998053924293L;

        private final Varargs v;
        private final int start;
        private final int end;

        public SubVarargs(Varargs varargs, int start, int end) {
            this.v = varargs;
            this.start = start;
            this.end = end;
        }

        @Override
        public LuaValue arg(int i) {
            i += start - 1;
            return i >= start && i <= end ? v.arg(i) : NIL;
        }

        @Override
        public LuaValue arg1() {
            return v.arg(start);
        }

        @Override
        public int narg() {
            return end + 1 - start;
        }
    }
}
