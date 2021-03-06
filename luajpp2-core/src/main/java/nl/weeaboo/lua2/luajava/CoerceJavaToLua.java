package nl.weeaboo.lua2.luajava;

import static nl.weeaboo.lua2.vm.LuaNil.NIL;

import javax.annotation.Nullable;

import nl.weeaboo.lua2.vm.LuaTable;
import nl.weeaboo.lua2.vm.LuaValue;
import nl.weeaboo.lua2.vm.Varargs;

/**
 * Converts Java objects to their equivalent Lua objects.
 */
public final class CoerceJavaToLua {

    private CoerceJavaToLua() {
    }

    /** Converts a sequence of values to an equivalent LuaTable. */
    public static <T> LuaTable toTable(Iterable<? extends T> values, Class<T> type) {
        LuaTable table = new LuaTable();
        int i = 1;
        for (T value : values) {
            table.rawset(i, coerce(value, type));
            i++;
        }
        return table;
    }

    /**
     * Coerces multiple Java objects to their equivalent Lua values (automatically tries to deduce their
     * types).
     *
     * @see #coerce(Object)
     */
    public static Varargs coerceArgs(Object[] values) {
        LuaValue[] luaArgs = new LuaValue[values.length];
        for (int n = 0; n < luaArgs.length; n++) {
            luaArgs[n] = coerce(values[n]);
        }
        return LuaValue.varargsOf(luaArgs);
    }

    /**
     * Coerces a single Java object to its equivalent Lua value (automatically tries to deduce a type).
     *
     * @see #coerce(Object, Class)
     */
    public static LuaValue coerce(@Nullable Object obj) {
        if (obj == null) {
            return NIL;
        }
        return coerce(obj, obj.getClass());
    }

    /**
     * Converts a Java objects to its equivalent Lua value.
     *
     * @param declaredType This determines which interface/class methods are available to Lua. This can be
     *        used to avoid accidentally too many methods to Lua. For example, when a Java method returns an
     *        interface you'd want Lua to only have access to the methods in that interface and not also all
     *        methods available in whatever the runtime type of the returned value is.
     */
    public static LuaValue coerce(@Nullable Object javaValue, Class<?> declaredType) {
        return ITypeCoercions.getCurrent().toLua(javaValue, declaredType);
    }

}
