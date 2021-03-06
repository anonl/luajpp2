package nl.weeaboo.lua2.stdlib;

import static nl.weeaboo.lua2.vm.LuaBoolean.FALSE;
import static nl.weeaboo.lua2.vm.LuaBoolean.TRUE;
import static nl.weeaboo.lua2.vm.LuaConstants.META_INEXT;
import static nl.weeaboo.lua2.vm.LuaConstants.META_METATABLE;
import static nl.weeaboo.lua2.vm.LuaConstants.NEXT;
import static nl.weeaboo.lua2.vm.LuaConstants.NONE;
import static nl.weeaboo.lua2.vm.LuaNil.NIL;
import static nl.weeaboo.lua2.vm.LuaValue.argerror;
import static nl.weeaboo.lua2.vm.LuaValue.valueOf;
import static nl.weeaboo.lua2.vm.LuaValue.varargsOf;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.weeaboo.lua2.LuaException;
import nl.weeaboo.lua2.LuaRunState;
import nl.weeaboo.lua2.compiler.ScriptLoader;
import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.lib.LuaBoundFunction;
import nl.weeaboo.lua2.lib.LuaLib;
import nl.weeaboo.lua2.vm.LuaConstants;
import nl.weeaboo.lua2.vm.LuaInteger;
import nl.weeaboo.lua2.vm.LuaString;
import nl.weeaboo.lua2.vm.LuaTable;
import nl.weeaboo.lua2.vm.LuaThread;
import nl.weeaboo.lua2.vm.LuaUserdata;
import nl.weeaboo.lua2.vm.LuaValue;
import nl.weeaboo.lua2.vm.Varargs;

/**
 * Basic Lua library
 */
@LuaSerializable
public final class BaseLib extends LuaLib {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(BaseLib.class);

    private static final LuaString TOSTRING = valueOf("tostring");

    static @Nullable InputStream STDIN = null;
    static PrintStream STDOUT = System.out;
    static PrintStream STDERR = System.err;

    BaseLib() {
    }

    @Override
    public void register() throws LuaException {
        LuaTable globals = getGlobals();
        globals.set("_G", globals);
        globals.set("_VERSION", LuaConstants.getEngineVersion());

        registerFunctions(globals, globals);
    }

    private LuaTable getGlobals() {
        LuaRunState lrs = LuaRunState.getCurrent();
        LuaTable globals = lrs.getGlobalEnvironment();
        return globals;
    }

    /**
     * {@code ( opt [,arg] ) -> value}
     */
    @LuaBoundFunction
    public Varargs collectgarbage(Varargs args) {
        String opt = args.optjstring(1, "collect");
        if ("collect".equals(opt)) {
            fullGC();
            return LuaInteger.valueOf(0);
        } else if ("count".equals(opt)) {
            Runtime rt = Runtime.getRuntime();
            rt.gc();
            long used = rt.totalMemory() - rt.freeMemory();
            return valueOf(used / 1024.0);
        } else if ("step".equals(opt)) {
            Runtime rt = Runtime.getRuntime();
            long initialUsed = rt.totalMemory() - rt.freeMemory();
            rt.gc();
            rt.runFinalization();
            // Returns true if a GC-step occurred
            return valueOf(rt.totalMemory() - rt.freeMemory() < initialUsed);
        } else if ("stop".equals(opt)) {
            // Not implemented
        } else if ("restart".equals(opt)) {
            // Not implemented
        } else if ("setpause".equals(opt)) {
            // Not implemented
        } else if ("setstepmul".equals(opt)) {
            // Not implemented
        } else {
            argerror(1, "gc op");
        }
        return NIL;
    }

    private void fullGC() {
        // Attempt to force a full GC
        for (int n = 0; n < 10; n++) {
            System.gc();
            System.runFinalization();
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                // Ignore
            }
        }
    }

    /**
     * @param args Not used.
     */
    @Deprecated
    @LuaBoundFunction
    public Varargs gcinfo(Varargs args) {
        return collectgarbage(valueOf("count"));
    }

    /**
     * {@code ( message [,level] ) -> ERR}
     */
    @LuaBoundFunction
    public Varargs error(Varargs args) {
        throw new LuaException(args.arg(1), null, args.optint(2, 1));
    }

    /**
     * {@code (f, table) -> void}
     */
    @LuaBoundFunction
    public Varargs setfenv(Varargs args) {
        LuaValue f = getfenvobj(args.arg(1));
        LuaTable t = args.checktable(2);
        f.setfenv(t);
        return f.isthread() ? NONE : f;
    }

    /**
     * {@code ( v [,message] ) -> v, message | ERR}
     */
    @LuaBoundFunction(luaName = "assert")
    public Varargs assert_(Varargs args) {
        if (!args.arg1().toboolean()) {
            String message = "assertion failed!";
            if (args.narg() > 1) {
                message = args.optjstring(2, "assertion failed!");
            }
            throw new LuaException(message);
        }
        return args;
    }

    /**
     * {@code ( filename ) -> result1, ...}
     */
    @LuaBoundFunction
    public Varargs dofile(Varargs args) {
        Varargs v;
        if (args.isnil(1)) {
            v = ScriptLoader.loadStream(STDIN, "=stdin");
        } else {
            v = ScriptLoader.loadFile(args.checkjstring(1));
        }

        if (v.isnil(1)) {
            throw new LuaException(v.tojstring(2));
        }
        return v.arg1().invoke();
    }

    /**
     * {@code ( [f] ) -> env}
     */
    @LuaBoundFunction
    public Varargs getfenv(Varargs args) {
        LuaValue f = getfenvobj(args.arg1());
        LuaValue e = f.getfenv();
        return (e != null ? e : NIL);
    }

    /**
     * {@code ( object ) -> table}
     */
    @LuaBoundFunction
    public Varargs getmetatable(Varargs args) {
        LuaValue mt = args.checkvalue(1).getmetatable();
        if (mt.isnil()) {
            return NIL;
        }
        return mt.rawget(META_METATABLE).optvalue(mt);
    }

    /**
     * {@code ( func [,chunkname] ) -> chunk | nil, msg}
     */
    @LuaBoundFunction
    public Varargs load(Varargs args) {
        LuaValue func = args.checkfunction(1);
        String chunkname = args.optjstring(2, "function");

        StringInputStream in = new StringInputStream(func);
        try {
            return ScriptLoader.loadStream(in, chunkname);
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    /**
     * {@code ( [filename] ) -> chunk | nil, msg}
     */
    @LuaBoundFunction
    public Varargs loadfile(Varargs args) {
        if (args.isnil(1)) {
            return ScriptLoader.loadStream(STDIN, "stdin");
        } else {
            return ScriptLoader.loadFile(args.checkjstring(1));
        }
    }

    /**
     * {@code ( string [,chunkname] ) -> chunk | nil, msg}
     */
    @LuaBoundFunction
    public Varargs loadstring(Varargs args) {
        LuaString script = args.checkstring(1);
        String chunkname = args.optjstring(2, script.tojstring());
        return ScriptLoader.loadStream(script.toInputStream(), chunkname);
    }

    /**
     * {@code (f, arg1, ...) -> status, result1, ...}
     */
    @LuaBoundFunction
    public Varargs pcall(Varargs args) {
        LuaValue func = args.checkvalue(1);
        return pcall(func, args.subargs(2), null);
    }

    /**
     * @param errfunc is ignored, should replace thread's errfunc which it doesn't have anymore
     */
    private static Varargs pcall(LuaValue func, Varargs args, @SuppressWarnings("unused") LuaValue errfunc) {
        try {
            Varargs funcResult = func.invoke(args);
            return varargsOf(TRUE, funcResult);
        } catch (LuaException le) {
            LOG.trace("Error in pcall: {} {}", func, args, le);
            return varargsOf(FALSE, le.getMessageObject());
        } catch (Exception e) {
            LOG.debug("Error in pcall: {} {}", func, args, e);
            String m = e.getMessage();
            return varargsOf(FALSE, valueOf(m != null ? m : e.toString()));
        }
    }

    /**
     * {@code (f, err) -> result1, ...}
     */
    @LuaBoundFunction
    public Varargs xpcall(Varargs args) {
        return pcall(args.arg1(), NONE, args.checkvalue(2));
    }

    /**
     * {@code (...) -> void}
     */
    @LuaBoundFunction
    public Varargs print(Varargs args) {
        LuaThread running = LuaThread.getRunning();
        LuaValue tostring = running.getfenv().get(TOSTRING);
        for (int i = 1, n = args.narg(); i <= n; i++) {
            if (i > 1) {
                STDOUT.write('\t');
            }
            LuaString s = tostring.call(args.arg(i)).strvalue();
            int z = s.indexOf((byte)0, 0);
            try {
                s.write(STDOUT, 0, z >= 0 ? z : s.length());
            } catch (IOException e) {
                LOG.warn("Unable to write to stdout", e);
            }
        }
        STDOUT.println();
        return NONE;
    }

    /**
     * {@code (f, ...) -> value1, ...}
     */
    @LuaBoundFunction
    public Varargs select(Varargs args) {
        int n = args.narg() - 1;
        if (args.arg1().equals(valueOf("#"))) {
            return valueOf(n);
        }
        int i = args.checkint(1);
        if (i == 0 || i < -n) {
            argerror(1, "index out of range");
        }
        return args.subargs(i < 0 ? n + i + 2 : i + 1);
    }

    /**
     * {@code (list [,i [,j]]) -> result1, ...}
     */
    @LuaBoundFunction
    public Varargs unpack(Varargs args) {
        LuaTable t = args.checktable(1);
        int i = args.optint(2, 1);
        int j = args.isnil(3) ? t.getn().checkint() : args.checkint(3);
        int n = j - i + 1;
        if (n < 0) {
            return NONE;
        }
        if (n == 1) {
            return t.get(i);
        }
        if (n == 2) {
            return varargsOf(t.get(i), t.get(j));
        }
        LuaValue[] v = new LuaValue[n];
        for (int k = 0; k < n; k++) {
            v[k] = t.get(i + k);
        }
        return varargsOf(v);
    }

    /**
     * {@code (v) -> value}
     */
    @LuaBoundFunction
    public Varargs type(Varargs args) {
        return valueOf(args.checkvalue(1).typename());
    }

    /**
     * {@code (v1, v2) -> boolean}
     */
    @LuaBoundFunction
    public Varargs rawequal(Varargs args) {
        return valueOf(args.checkvalue(1) == args.checkvalue(2));
    }

    /**
     * {@code (table, index) -> value}
     */
    @LuaBoundFunction
    public Varargs rawget(Varargs args) {
        return args.checktable(1).rawget(args.checkvalue(2));
    }

    /**
     * {@code (table, index, value) -> table}
     */
    @LuaBoundFunction
    public Varargs rawset(Varargs args) {
        LuaTable t = args.checktable(1);
        t.rawset(args.checknotnil(2), args.checkvalue(3));
        return t;
    }

    /**
     * {@code (table, metatable) -> table}
     */
    @LuaBoundFunction
    public Varargs setmetatable(Varargs args) {
        final LuaValue t = args.arg1();
        final LuaValue mt0 = t.getmetatable();
        if (!mt0.isnil() && !mt0.rawget(META_METATABLE).isnil()) {
            throw new LuaException("cannot change a protected metatable");
        }
        final LuaValue mt = args.checkvalue(2);
        return t.setmetatable(mt.isnil() ? NIL : mt.checktable());
    }

    /**
     * {@code (e) -> value}
     */
    @LuaBoundFunction
    public Varargs tostring(Varargs args) {
        LuaValue arg = args.checkvalue(1);
        LuaValue h = arg.metatag(LuaConstants.META_TOSTRING);
        if (!h.isnil()) {
            return h.call(arg);
        }
        LuaValue v = arg.tostring();
        if (!v.isnil()) {
            return v;
        }
        return valueOf(arg.tojstring());
    }

    /**
     * {@code (e [,base]) -> value}
     */
    @LuaBoundFunction
    public Varargs tonumber(Varargs args) {
        LuaValue arg1 = args.checkvalue(1);
        final int base = args.optint(2, 10);
        if (base == 10) { /* standard conversion */
            return arg1.tonumber();
        } else {
            if (base < 2 || base > 36) {
                argerror(2, "base out of range");
            }
            return arg1.checkstring().tonumber(base);
        }
    }

    /**
     * {@code "pairs" (t) -> iter-func, t, nil}
     */
    @LuaBoundFunction
    public Varargs pairs(Varargs args) {
        LuaValue next = getGlobals().get(NEXT);
        return varargsOf(next, args.checktable(1), NIL);
    }

    /**
     * {@code "ipairs", // (t) -> iter-func, t, 0}
     */
    @LuaBoundFunction
    public Varargs ipairs(Varargs args) {
        LuaValue inext = getGlobals().get(META_INEXT);
        return varargsOf(inext, args.checktable(1), LuaInteger.valueOf(0));
    }

    /**
     * {@code "next" ( table, [index] ) -> next-index, next-value}
     */
    @LuaBoundFunction
    public Varargs next(Varargs args) {
        return args.checktable(1).next(args.arg(2));
    }

    /**
     * {@code "inext" ( table, [int-index] ) -> next-index, next-value}
     */
    @LuaBoundFunction(luaName = "__inext")
    public Varargs inext(Varargs args) {
        return args.checktable(1).inext(args.arg(2));
    }

    private static LuaValue getfenvobj(LuaValue arg) {
        if (arg.isfunction()) {
            return arg;
        }

        int level = arg.optint(1);
        arg.argcheck(level >= 0, 1, "level must be non-negative");
        if (level == 0) {
            return LuaThread.getRunning();
        }

        LuaThread running = LuaThread.getRunning();
        LuaValue f = running.getCallstackFunction(level);
        if (f == null) {
            throw LuaValue.argerror(1, "invalid level");
        }
        return f;
    }

    /**
     * This method is undocumented in Lua 5.1
     * <p>
     * Creates a blank userdata with an empty metatable.
     */
    @Deprecated
    @LuaBoundFunction
    public Varargs newproxy(Varargs args) {
        LuaUserdata userdata = LuaUserdata.userdataOf(Proxy.INSTANCE);
        if (!args.toboolean(1)) {
            return userdata;
        }

        if (args.type(1) == LuaConstants.TBOOLEAN) {
            userdata.setmetatable(new LuaTable());
        } else {
            LuaValue metatable = args.arg(1).getmetatable();
            userdata.setmetatable(metatable);
        }
        return userdata;
    }

    private enum Proxy {
        INSTANCE;
    }

}
