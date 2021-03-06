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

package nl.weeaboo.lua2.compiler;

import static nl.weeaboo.lua2.vm.LuaBoolean.FALSE;
import static nl.weeaboo.lua2.vm.LuaBoolean.TRUE;
import static nl.weeaboo.lua2.vm.LuaConstants.NUMBER_FORMAT_FLOATS_OR_DOUBLES;
import static nl.weeaboo.lua2.vm.LuaConstants.NUMBER_FORMAT_INTS_ONLY;
import static nl.weeaboo.lua2.vm.LuaConstants.NUMBER_FORMAT_NUM_PATCH_INT32;
import static nl.weeaboo.lua2.vm.LuaConstants.TBOOLEAN;
import static nl.weeaboo.lua2.vm.LuaConstants.TINT;
import static nl.weeaboo.lua2.vm.LuaConstants.TNIL;
import static nl.weeaboo.lua2.vm.LuaConstants.TNUMBER;
import static nl.weeaboo.lua2.vm.LuaConstants.TSTRING;
import static nl.weeaboo.lua2.vm.LuaNil.NIL;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Nullable;

import nl.weeaboo.lua2.LuaException;
import nl.weeaboo.lua2.internal.SharedByteAlloc;
import nl.weeaboo.lua2.vm.LocVars;
import nl.weeaboo.lua2.vm.LuaClosure;
import nl.weeaboo.lua2.vm.LuaDouble;
import nl.weeaboo.lua2.vm.LuaFunction;
import nl.weeaboo.lua2.vm.LuaInteger;
import nl.weeaboo.lua2.vm.LuaString;
import nl.weeaboo.lua2.vm.LuaValue;
import nl.weeaboo.lua2.vm.Prototype;

/**
 * Class to manage loading of {@link Prototype} instances.
 * <p>
 * The {@link LoadState} class exposes one main function, namely {@link #load(InputStream, String, LuaValue)},
 * to be used to load code from a particular input stream.
 * <p>
 * A simple pattern for loading and executing code is
 *
 * <pre>
 * {
 *     &#064;code
 *     LuaValue _G = JsePlatform.standardGlobals();
 *     LoadState.load(new FileInputStream(&quot;main.lua&quot;), &quot;main.lua&quot;, _G).call();
 * }
 * </pre>
 *
 * This should work regardless of which {@link ILuaCompiler} has been installed.
 * <p>
 *
 * Prior to loading code, a compiler should be installed.
 * <p>
 * To override the default compiler with, say, the LuaJC lua-to-java bytecode compiler, install it before
 * loading, for example:
 *
 * <pre>
 * {
 *     &#064;code
 *     LuaValue _G = JsePlatform.standardGlobals();
 *     LuaJC.install();
 *     LoadState.load(new FileInputStream(&quot;main.lua&quot;), &quot;main.lua&quot;, _G).call();
 * }
 * </pre>
 *
 * @see ILuaCompiler
 * @see LuaClosure
 * @see LuaFunction
 * @see LoadState#load(InputStream, String, LuaValue)
 * @see LuaC
 */
public final class LoadState {

    /** Signature byte indicating the file is a compiled binary chunk. */
    private static final byte[] LUA_SIGNATURE = { '\033', 'L', 'u', 'a' };

    /** Name for compiled chunks. */
    private static final String SOURCE_BINARY_STRING = "binary string";

    private static final LuaValue[] NOVALUES = {};
    private static final Prototype[] NOPROTOS = {};
    private static final LocVars[] NOLOCVARS = {};
    private static final LuaString[] NOSTRVALUES = {};
    private static final int[] NOINTS = {};

    private final SharedByteAlloc alloc = SharedByteAlloc.getInstance();

    /** input stream from which we are loading. */
    private final DataInputStream is;

    /** Read buffer. */
    private byte[] buf = new byte[512];

    // values read from the header
    @SuppressWarnings("unused")
    private int luacVersion;
    @SuppressWarnings("unused")
    private int luacFormat;
    private boolean luacLittleEndian;
    @SuppressWarnings("unused")
    private int luacSizeofInt;
    @SuppressWarnings("unused")
    private int luacSizeofSizeT;
    @SuppressWarnings("unused")
    private int luacSizeofInstruction;
    @SuppressWarnings("unused")
    private int luacSizeofLuaNumber;
    private int luacNumberFormat;

    /** Private constructor for create a load state. */
    private LoadState(InputStream stream) {
        this.is = new DataInputStream(stream);
    }

    /**
     * Load a 4-byte int value from the input stream
     *
     * @return the int value laoded.
     **/
    int loadInt() throws IOException {
        is.readFully(buf, 0, 4);
        if (luacLittleEndian) {
            return (buf[3] << 24) | ((0xff & buf[2]) << 16) | ((0xff & buf[1]) << 8) | (0xff & buf[0]);
        } else {
            return (buf[0] << 24) | ((0xff & buf[1]) << 16) | ((0xff & buf[2]) << 8) | (0xff & buf[3]);
        }
    }

    /**
     * Load an array of int values from the input stream
     *
     * @return the array of int values laoded.
     **/
    int[] loadIntArray() throws IOException {
        int n = loadInt();
        if (n == 0) {
            return NOINTS;
        }

        // read all data at once
        int m = n << 2;
        if (buf.length < m) {
            buf = new byte[m];
        }
        is.readFully(buf, 0, m);
        int[] array = new int[n];
        for (int i = 0, j = 0; i < n; ++i, j += 4) {
            array[i] = luacLittleEndian
                    ? (buf[j + 3] << 24) | ((0xff & buf[j + 2]) << 16) | ((0xff & buf[j + 1]) << 8)
                            | (0xff & buf[j + 0])
                    : (buf[j + 0] << 24) | ((0xff & buf[j + 1]) << 16) | ((0xff & buf[j + 2]) << 8)
                            | (0xff & buf[j + 3]);
        }

        return array;
    }

    /**
     * Load a long value from the input stream
     *
     * @return the long value laoded.
     **/
    long loadInt64() throws IOException {
        int a;
        int b;
        if (this.luacLittleEndian) {
            a = loadInt();
            b = loadInt();
        } else {
            b = loadInt();
            a = loadInt();
        }
        return (((long)b) << 32) | (a & 0xffffffffL);
    }

    /**
     * Loads a lua string value from the input stream.
     *
     * @return the {@link LuaString} value loaded.
     **/
    private @Nullable LuaString loadString() throws IOException {
        int size = loadInt();
        if (size == 0) {
            return null;
        }

        int offset = alloc.reserve(size);
        byte[] bytes = alloc.getReserved();
        is.readFully(bytes, offset, size);
        return LuaString.valueOf(bytes, offset, size - 1);
    }

    /**
     * Convert bits in a long value to a {@link LuaValue}.
     *
     * @param bits long value containing the bits
     * @return {@link LuaInteger} or {@link LuaDouble} whose value corresponds to the bits provided.
     */
    private static LuaValue longBitsToLuaNumber(long bits) {
        if ((bits & ((1L << 63) - 1)) == 0L) {
            return LuaInteger.valueOf(0);
        }

        int e = (int)((bits >> 52) & 0x7ffL) - 1023;

        if (e >= 0 && e < 31) {
            long f = bits & 0xFFFFFFFFFFFFFL;
            int shift = 52 - e;
            long intPrecMask = (1L << shift) - 1;
            if ((f & intPrecMask) == 0) {
                int intValue = (int)(f >> shift) | (1 << e);
                return LuaInteger.valueOf(((bits >> 63) != 0) ? -intValue : intValue);
            }
        }

        return LuaValue.valueOf(Double.longBitsToDouble(bits));
    }

    /**
     * Load a number from a binary chunk.
     *
     * @return the {@link LuaValue} loaded
     * @throws IOException if an i/o exception occurs
     */
    LuaValue loadNumber() throws IOException {
        if (luacNumberFormat == NUMBER_FORMAT_INTS_ONLY) {
            return LuaInteger.valueOf(loadInt());
        } else {
            return longBitsToLuaNumber(loadInt64());
        }
    }

    /**
     * Load a list of constants from a binary chunk.
     *
     * @param f the function prototype
     * @throws IOException if an i/o exception occurs
     */
    void loadConstants(Prototype f) throws IOException {
        int n = loadInt();
        LuaValue[] values = (n > 0 ? new LuaValue[n] : NOVALUES);
        for (int i = 0; i < n; i++) {
            switch (is.readByte()) {
            case TNIL:
                values[i] = NIL;
                break;
            case TBOOLEAN:
                values[i] = (0 != is.readUnsignedByte() ? TRUE : FALSE);
                break;
            case TINT:
                values[i] = LuaInteger.valueOf(loadInt());
                break;
            case TNUMBER:
                values[i] = loadNumber();
                break;
            case TSTRING:
                values[i] = loadString();
                break;
            default:
                throw new IllegalStateException("bad constant");
            }
        }
        f.k = values;

        n = loadInt();
        Prototype[] protos = n > 0 ? new Prototype[n] : NOPROTOS;
        for (int i = 0; i < n; i++) {
            protos[i] = loadFunction(f.source);
        }
        f.p = protos;
    }

    /**
     * Load the debug infor for a function prototype.
     *
     * @param f the function Prototype
     * @throws IOException if there is an i/o exception
     */
    void loadDebug(Prototype f) throws IOException {
        f.lineinfo = loadIntArray();
        int n = loadInt();
        f.locvars = n > 0 ? new LocVars[n] : NOLOCVARS;
        for (int i = 0; i < n; i++) {
            LuaString varname = loadString();
            int startpc = loadInt();
            int endpc = loadInt();
            f.locvars[i] = new LocVars(varname, startpc, endpc);
        }

        n = loadInt();
        f.upvalues = n > 0 ? new LuaString[n] : NOSTRVALUES;
        for (int i = 0; i < n; i++) {
            f.upvalues[i] = loadString();
        }
    }

    /**
     * Load a function prototype from the input stream.
     *
     * @param p name of the source
     * @return {@link Prototype} instance that was loaded
     * @throws IOException If an I/O error occurs.
     */
    private Prototype loadFunction(LuaString p) throws IOException {
        Prototype f = new Prototype();
        f.source = loadString();
        if (f.source == null) {
            f.source = p;
        }
        f.linedefined = loadInt();
        f.lastlinedefined = loadInt();
        f.nups = is.readUnsignedByte();
        f.numparams = is.readUnsignedByte();
        f.isVararg = is.readUnsignedByte();
        f.maxstacksize = is.readUnsignedByte();
        f.code = loadIntArray();
        loadConstants(f);
        loadDebug(f);
        return f;
    }

    /**
     * Load the lua chunk header values.
     *
     * @throws IOException if an i/o exception occurs.
     */
    private void loadHeader() throws IOException {
        luacVersion = is.readByte();
        luacFormat = is.readByte();
        luacLittleEndian = (0 != is.readByte());
        luacSizeofInt = is.readByte();
        luacSizeofSizeT = is.readByte();
        luacSizeofInstruction = is.readByte();
        luacSizeofLuaNumber = is.readByte();
        luacNumberFormat = is.readByte();
    }

    /**
     * @throws IOException If an I/O error occurs.
     * @see #load(InputStream, String, LuaValue)
     */
    public static LuaFunction load(String source, String name, LuaValue env) throws IOException {
        return load(new ByteArrayInputStream(source.getBytes("UTF-8")), name, env);
    }

    /**
     * Load lua in either binary or text form from an input stream.
     *
     * @param stream InputStream to read, after having read the first byte already
     * @param name Name to apply to the loaded chunk
     * @return {@link Prototype} that was loaded
     * @throws IllegalArgumentException if the signature is bad
     * @throws IOException if an IOException occurs
     */
    public static LuaFunction load(InputStream stream, String name, LuaValue env) throws IOException {
        return new LuaC().load(stream, name, env);
    }

    /**
     * Load lua thought to be a binary chunk from its first byte from an input stream.
     *
     * @param firstByte the first byte of the input stream
     * @param stream InputStream to read, after having read the first byte already
     * @param name Name to apply to the loaded chunk
     * @return {@link Prototype} that was loaded
     * @throws IllegalArgumentException if the signature is bac
     * @throws IOException if an IOException occurs
     */
    public static Prototype loadBinaryChunk(int firstByte, InputStream stream, String name)
            throws IOException {

        // check rest of signature
        if (firstByte != LUA_SIGNATURE[0]
                || stream.read() != LUA_SIGNATURE[1]
                || stream.read() != LUA_SIGNATURE[2]
                || stream.read() != LUA_SIGNATURE[3]) {
            throw new IllegalArgumentException("bad signature");
        }

        // load file as a compiled chunk
        String sname = getSourceName(name);
        LoadState s = new LoadState(stream);
        s.loadHeader();

        // check format
        switch (s.luacNumberFormat) {
        case NUMBER_FORMAT_FLOATS_OR_DOUBLES:
        case NUMBER_FORMAT_INTS_ONLY:
        case NUMBER_FORMAT_NUM_PATCH_INT32:
            break;
        default:
            throw new LuaException("unsupported int size");
        }
        return s.loadFunction(LuaString.valueOf(sname));
    }

    /**
     * Construct a source name from a supplied chunk name.
     *
     * @param name String name that appears in the chunk
     * @return source file name
     */
    public static String getSourceName(String name) {
        String sname = name;
        if (name.startsWith("@") || name.startsWith("=")) {
            sname = name.substring(1);
        } else if (name.startsWith("\033")) {
            sname = SOURCE_BINARY_STRING;
        }
        return sname;
    }

}
