package nl.weeaboo.lua2;

import java.util.List;

import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.stdlib.DebugTrace;
import nl.weeaboo.lua2.vm.LuaNil;
import nl.weeaboo.lua2.vm.LuaStackTraceElement;
import nl.weeaboo.lua2.vm.LuaString;
import nl.weeaboo.lua2.vm.LuaValue;

/**
 * Represents an error thrown by the Lua VM.
 */
@LuaSerializable
public final class LuaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private static final int MAX_LEVELS = 8;

    private final LuaValue message;

    public LuaException(String message) {
        this(LuaString.valueOf(message));
    }

    public LuaException(LuaValue message) {
        this(message, null, 0);
    }

    public LuaException(LuaValue message, Throwable cause, int level) {
        super(cause);

        this.message = (message != null ? message : LuaNil.NIL);

        if (level >= 0) {
            initStackTrace(cause, level);
        }
    }

    private void initStackTrace(Throwable cause, int level) {
        LuaRunState lrs = LuaRunState.getCurrent();
        if (lrs == null) {
            return; // No Lua context is current
        }

        List<LuaStackTraceElement> stack = DebugTrace.stackTrace(lrs.getRunningThread(), level, MAX_LEVELS);
        if (cause != null) {
            setStackTrace(prefixLuaStackTrace(cause, stack));
        } else {
            setStackTrace(prefixLuaStackTrace(this, stack));
        }
    }

    /**
     * Converts an arbitrary exception to an equivalent {@link LuaException}.
     */
    public static LuaException wrap(String message, Throwable ex) {
        LuaException le;
        if (ex instanceof LuaException) {
            if (ex.getMessage() != null) {
                message += ": " + ex.toString();
            }
            le = new LuaException(LuaString.valueOf(message), ex.getCause(), 0);

            // Copy stacktrace and cause from the existing exception
            le.setStackTrace(ex.getStackTrace());
        } else {
            // For non-Lua exceptions, add the original exception as a cause
            le = new LuaException(LuaString.valueOf(message), ex, 0);
        }

        return le;
    }

    private static StackTraceElement[] prefixLuaStackTrace(Throwable t, List<LuaStackTraceElement> luaStack) {
        StackTraceElement[] javaStack = t.getStackTrace();
        StackTraceElement[] newStack = new StackTraceElement[luaStack.size() + javaStack.length];

        int joff = 0;
        for (int n = 0; n < javaStack.length; n++) {
            StackTraceElement ste = javaStack[n];
            if (ste.getClassName().contains("LuaInterpreter") && ste.getMethodName().equals("resume")) {
                joff = n;
                break;
            }
        }
        System.arraycopy(javaStack, 0, newStack, 0, joff);
        for (int n = 0; n < luaStack.size(); n++) {
            newStack[joff + n] = luaStack.get(n).toJavaStackTraceElement();
        }
        System.arraycopy(javaStack, joff, newStack, joff + luaStack.size(), javaStack.length - joff);
        return newStack;
    }

    @Override
    public String getMessage() {
        return String.valueOf(message);
    }

    /** Retuns the error message for this exception as a Lua object. */
    public LuaValue getMessageObject() {
        return message;
    }

}
