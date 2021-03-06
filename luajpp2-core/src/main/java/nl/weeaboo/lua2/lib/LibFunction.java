/*******************************************************************************
 * Copyright (c) 2009-2011 Luaj.org. All rights reserved.
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

package nl.weeaboo.lua2.lib;

import static nl.weeaboo.lua2.vm.LuaNil.NIL;

import nl.weeaboo.lua2.LuaException;
import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.stdlib.BaseLib;
import nl.weeaboo.lua2.stdlib.TableLib;
import nl.weeaboo.lua2.vm.LuaFunction;
import nl.weeaboo.lua2.vm.LuaValue;

/**
 * Subclass of {@link LuaFunction} common to Java functions exposed to lua.
 * <p>
 * To provide for common implementations in JME and JSE, library functions are typically grouped on one or
 * more library classes and an opcode per library function is defined and used to key the switch to the
 * correct function within the library.
 * <p>
 * Since lua functions can be called with too few or too many arguments, and there are overloaded
 * {@link LuaValue#call()} functions with varying number of arguments, a Java function exposed in lua needs to
 * handle the argument fixup when a function is called with a number of arguments differs from that expected.
 * <p>
 * To simplify the creation of library functions, there are 5 direct subclasses to handle common cases based
 * on number of argument values and number of return return values.
 * <ul>
 * <li>{@link ZeroArgFunction}</li>
 * <li>{@link OneArgFunction}</li>
 * <li>{@link TwoArgFunction}</li>
 * <li>{@link ThreeArgFunction}</li>
 * <li>{@link VarArgFunction}</li>
 * </ul>
 * <p>
 * To be a Java library that can be loaded via {@code require}, it should have a public constructor that
 * returns a {@link LuaValue} that, when executed, initializes the library.
 * <p>
 * For example, the following code will implement a library called "hyperbolic" with two functions, "sinh",
 * and "cosh":
 *
 * <pre>
 * <code>
 * import org.luaj.vm2.LuaValue;
 * public class hyperbolic extends org.luaj.vm2.lib.OneArgFunction {
 *     public hyperbolic() {}
 *     public LuaValue call(LuaValue arg) {
 *         switch ( opcode ) {
 *         case 0: {
 *             LuaValue t = tableOf();
 *             this.bind(t, hyperbolic.class, new String[] { "sinh", "cosh" }, 1 );
 *             env.set("hyperbolic", t);
 *             return t;
 *         }
 *         case 1: return valueOf(Math.sinh(arg.todouble()));
 *         case 2: return valueOf(Math.cosh(arg.todouble()));
 *         default: return error("bad opcode: "+opcode);
 *         }
 *     }
 * }
 * </code>
 * </pre>
 *
 * The default constructor is both to instantiate the library in response to {@code require 'hyperbolic'}
 * statement, provided it is on Javas class path, and to instantiate copies of the {@code hyperbolic} class
 * when initializing library instances. . The instance returned by the default constructor will be invoked as
 * part of library loading. In response, it creates two more instances, one for each library function, in the
 * body of the {@code switch} statement {@code case 0} via the
 * <code>bind(LuaValue, Class, String[], int)</code> utility method. It also registers the table in the
 * globals via the {@code env} local variable, which should be the global environment unless it has been
 * changed. {@code case 1} and {@code case 2} will be called when {@code hyperbolic.sinh}
 * {@code hyperbolic.sinh} and {@code hyperbolic.cosh} are invoked.
 * <p>
 * To test it, a script such as this can be used:
 *
 * <pre>
 * {@code
 * local t = require('hyperbolic')
 * print( 't', t )
 * print( 'hyperbolic', hyperbolic )
 * for k,v in pairs(t) do
 *     print( 'k,v', k,v )
 * end
 * print( 'sinh(.5)', hyperbolic.sinh(.5) )
 * print( 'cosh(.5)', hyperbolic.cosh(.5) )
 * }
 * </pre>
 * <p>
 * It should produce something like:
 *
 * <pre>
 * {@code
 * t    table: 3dbbd23f
 * hyperbolic    table: 3dbbd23f
 * k,v    cosh    cosh
 * k,v    sinh    sinh
 * sinh(.5)    0.5210953
 * cosh(.5)    1.127626
 * }
 * </pre>
 * <p>
 * See the source code in any of the library functions such as {@link BaseLib} or {@link TableLib} for
 * specific examples.
 */
@LuaSerializable
public abstract class LibFunction extends LuaFunction {

    private static final long serialVersionUID = -4025668290315326469L;

    /**
     * User-defined opcode to differentiate between instances of the library function class.
     * <p>
     * Subclass will typicall switch on this value to provide the specific behavior for each function.
     */
    protected int opcode;

    /**
     * The common name for this function, useful for debugging.
     * <p>
     * Binding functions initialize this to the name to which it is bound.
     */
    protected String name;

    /** Default constructor for use by subclasses */
    protected LibFunction() {
    }

    @Override
    public String tojstring() {
        if (name != null) {
            return typename() + ":" + name;
        } else {
            return super.tojstring();
        }
    }

    /**
     * Bind a set of library functions.
     * <p>
     * An array of names is provided, and the first name is bound with opcode = 0, second with 1, etc.
     *
     * @param env The environment to apply to each bound function
     * @param factory the Class to instantiate for each bound function
     * @param names array of String names, one for each function.
     * @see #bind(LuaValue, Class, String[], int)
     */
    @Deprecated
    protected void bind(LuaValue env, Class<?> factory, String[] names) {
        bind(env, factory, names, 0);
    }

    /**
     * Bind a set of library functions, with an offset
     * <p>
     * An array of names is provided, and the first name is bound with opcode = {@code firstopcode}, second
     * with {@code firstopcode+1}, etc.
     *
     * @param env The environment to apply to each bound function
     * @param factory the Class to instantiate for each bound function
     * @param names array of String names, one for each function.
     * @param firstopcode the first opcode to use
     * @see #bind(LuaValue, Class, String[])
     */
    @Deprecated
    protected void bind(LuaValue env, Class<?> factory, String[] names, int firstopcode) {
        try {
            for (int i = 0, n = names.length; i < n; i++) {
                LibFunction f = (LibFunction)factory.getConstructor().newInstance();
                f.opcode = firstopcode + i;
                f.name = names[i];
                f.env = env;
                env.set(names[i], f);
            }
        } catch (Exception e) {
            throw LuaException.wrap("Bind failed: " + factory.getName(), e);
        }
    }

    /**
     * Java code generation utility to allocate storage for upvalue, leave it empty
     */
    protected static LuaValue[] newupe() {
        return new LuaValue[1];
    }

    /**
     * Java code generation utility to allocate storage for upvalue, initialize with nil
     */
    protected static LuaValue[] newupn() {
        return new LuaValue[] { NIL };
    }

    /**
     * Java code generation utility to allocate storage for upvalue, initialize with value
     */
    protected static LuaValue[] newupl(LuaValue v) {
        return new LuaValue[] { v };
    }
}
