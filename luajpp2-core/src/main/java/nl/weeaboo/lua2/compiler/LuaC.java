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

package nl.weeaboo.lua2.compiler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import nl.weeaboo.lua2.LuaException;
import nl.weeaboo.lua2.LuaRunState;
import nl.weeaboo.lua2.internal.SharedByteAlloc;
import nl.weeaboo.lua2.vm.LocVars;
import nl.weeaboo.lua2.vm.Lua;
import nl.weeaboo.lua2.vm.LuaClosure;
import nl.weeaboo.lua2.vm.LuaString;
import nl.weeaboo.lua2.vm.LuaValue;
import nl.weeaboo.lua2.vm.Prototype;

/**
 * Compiler for Lua.
 * <p>
 * Compiles lua source files into lua bytecode within a {@link Prototype}, loads lua binary files directly
 * into a{@link Prototype}, and optionaly instantiates a {@link LuaClosure} around the result using a
 * user-supplied environment.
 * <p>
 * Implements the {@link ILuaCompiler} interface for loading initialized chunks, which is an interface common
 * to lua bytecode compiling and java bytecode compiling.
 * <p>
 * The {@link LuaC} compiler is installed by default by {@link LuaRunState}, so in the following example, the
 * default {@link LuaC} compiler will be used:
 *
 * <pre>
 * {
 *     &#064;code
 *     LuaValue _G = JsePlatform.standardGlobals();
 *     LoadState.load(new ByteArrayInputStream(&quot;print 'hello'&quot;.getBytes()), &quot;main.lua&quot;, _G).call();
 * }
 * </pre>
 */
public final class LuaC extends Lua implements ILuaCompiler {

    static final int LUAI_MAXUPVALUES = 60;
    static final int LUAI_MAXVARS = 200;

    private final Map<LuaString, LuaString> strings = new HashMap<>();

    int nCcalls;

    public LuaC() {
    }

    @Override
    public LuaClosure load(InputStream stream, String name, LuaValue env) throws IOException {
        Prototype p = compile(stream, name);
        return new LuaClosure(p, env);
    }

    /**
     * @throws IOException If an I/O error occurs.
     * @see #compile(InputStream, String)
     */
    public static Prototype compile(String source, String name) throws IOException {
        return new LuaC().compileLua(source, name);
    }

    /**
     * Compile a prototype or load as a binary chunk.
     *
     * @throws IOException If an I/O error occurs.
     */
    public static Prototype compile(InputStream stream, String name) throws IOException {
        return new LuaC().compileLua(stream, name);
    }

    Prototype compileLua(String source, String name) throws IOException {
        return compileLua(new ByteArrayInputStream(source.getBytes("UTF-8")), name);
    }

    Prototype compileLua(InputStream stream, String name) throws IOException {
        int firstByte = stream.read();
        if (firstByte == '\033') {
            return LoadState.loadBinaryChunk(firstByte, stream, name);
        } else {
            return luaY_parser(firstByte, stream, name);
        }
    }

    /** Parse the input. */
    private Prototype luaY_parser(int firstByte, InputStream z, String name) {
        LexState lexstate = new LexState(this, z);
        FuncState funcstate = new FuncState();
        lexstate.setinput(this, firstByte, z, LuaValue.valueOf(name));
        lexstate.open_func(funcstate);
        // Main function is always vararg
        funcstate.f.isVararg = Lua.VARARG_ISVARARG;
        funcstate.f.source = LuaValue.valueOf(name);
        lexstate.next(); /* read first token */
        lexstate.chunk();
        lexstate.check(LexState.TK_EOS);
        lexstate.close_func();
        LuaC.luaAssert(funcstate.prev == null);
        LuaC.luaAssert(funcstate.f.nups == 0);
        LuaC.luaAssert(lexstate.fs == null);
        return funcstate.f;
    }

    /** Look up and keep at most one copy of each string. */
    LuaString newTString(byte[] bytes, int offset, int len) {
        LuaString tmp = LuaString.valueOf(bytes, offset, len);
        LuaString v = strings.get(tmp);
        if (v == null) {
            // must copy bytes, since bytes could be from reusable buffer
            SharedByteAlloc sba = SharedByteAlloc.getInstance();
            int n = sba.reserve(len);
            byte[] copy = sba.getReserved();
            System.arraycopy(bytes, offset, copy, n, len);
            v = LuaString.valueOf(copy, n, len);
            strings.put(v, v);
        }
        return v;
    }

    protected static void luaAssert(boolean b) {
        if (!b) {
            throw new LuaException("compiler assert failed");
        }
    }

    static void setOpcode(InstructionPtr i, int o) {
        i.set((i.get() & MASK_NOT_OP) | ((o << POS_OP) & MASK_OP));
    }

    static void setArgA(InstructionPtr i, int u) {
        i.set((i.get() & MASK_NOT_A) | ((u << POS_A) & MASK_A));
    }

    static void setArgB(InstructionPtr i, int u) {
        i.set((i.get() & MASK_NOT_B) | ((u << POS_B) & MASK_B));
    }

    static void setArgC(InstructionPtr i, int u) {
        i.set((i.get() & MASK_NOT_C) | ((u << POS_C) & MASK_C));
    }

    static void setArgBx(InstructionPtr i, int u) {
        i.set((i.get() & MASK_NOT_Bx) | ((u << POS_Bx) & MASK_Bx));
    }

    static void setArgSBx(InstructionPtr i, int u) {
        setArgBx(i, u + MAXARG_sBx);
    }

    static int createAbc(int o, int a, int b, int c) {
        return ((o << POS_OP) & MASK_OP) | ((a << POS_A) & MASK_A) | ((b << POS_B) & MASK_B)
                | ((c << POS_C) & MASK_C);
    }

    static int createAbx(int o, int a, int bc) {
        return ((o << POS_OP) & MASK_OP) | ((a << POS_A) & MASK_A) | ((bc << POS_Bx) & MASK_Bx);
    }

    // vector reallocation

    static LuaValue[] realloc(LuaValue[] v, int n) {
        LuaValue[] a = new LuaValue[n];
        if (v != null) {
            System.arraycopy(v, 0, a, 0, Math.min(v.length, n));
        }
        return a;
    }

    static Prototype[] realloc(Prototype[] v, int n) {
        Prototype[] a = new Prototype[n];
        if (v != null) {
            System.arraycopy(v, 0, a, 0, Math.min(v.length, n));
        }
        return a;
    }

    static LuaString[] realloc(LuaString[] v, int n) {
        LuaString[] a = new LuaString[n];
        if (v != null) {
            System.arraycopy(v, 0, a, 0, Math.min(v.length, n));
        }
        return a;
    }

    static LocVars[] realloc(LocVars[] v, int n) {
        LocVars[] a = new LocVars[n];
        if (v != null) {
            System.arraycopy(v, 0, a, 0, Math.min(v.length, n));
        }
        return a;
    }

    static int[] realloc(int[] v, int n) {
        int[] a = new int[n];
        if (v != null) {
            System.arraycopy(v, 0, a, 0, Math.min(v.length, n));
        }
        return a;
    }

    static byte[] realloc(byte[] v, int n) {
        byte[] a = new byte[n];
        if (v != null) {
            System.arraycopy(v, 0, a, 0, Math.min(v.length, n));
        }
        return a;
    }

}
