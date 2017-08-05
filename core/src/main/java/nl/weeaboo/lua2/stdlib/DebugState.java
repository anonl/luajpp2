package nl.weeaboo.lua2.stdlib;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.vm.LuaError;
import nl.weeaboo.lua2.vm.LuaString;
import nl.weeaboo.lua2.vm.LuaThread;
import nl.weeaboo.lua2.vm.LuaValue;

/** DebugState is associated with a Thread. */
@LuaSerializable
final class DebugState implements Externalizable {

    private static final Logger LOG = LoggerFactory.getLogger(DebugState.class);

    // --- Uses manual serialization, don't add variables ---
    private LuaThread thread;
    int debugCalls;
    private DebugInfo[] debugInfo;
    LuaValue hookfunc;
    boolean hookcall;
    boolean hookline;
    boolean hookrtrn;
    boolean inhook;
    int hookcount;
    int hookcodes;
    int line;
    // --- Uses manual serialization, don't add variables ---

    /**
     * Do not use. Required for efficient serialization.
     */
    @Deprecated
    public DebugState() {
    }

    DebugState(LuaThread t) {
        thread = t;
        debugInfo = new DebugInfo[LuaThread.MAX_CALLSTACK + 1];
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(thread);
        out.writeInt(debugCalls);
        out.writeInt(debugInfo.length);
        for (int n = 0; n < debugCalls; n++) {
            out.writeObject(debugInfo[n]);
        }
        out.writeObject(hookfunc);
        out.writeBoolean(hookcall);
        out.writeBoolean(hookline);
        out.writeBoolean(hookrtrn);
        out.writeBoolean(inhook);
        out.writeInt(hookcount);
        out.writeInt(hookcodes);
        out.writeInt(line);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        thread = (LuaThread)in.readObject();
        debugCalls = in.readInt();
        int debugInfoL = in.readInt();
        debugInfo = new DebugInfo[debugInfoL];
        for (int n = 0; n < debugCalls; n++) {
            debugInfo[n] = (DebugInfo)in.readObject();
        }
        hookfunc = (LuaValue)in.readObject();
        hookcall = in.readBoolean();
        hookline = in.readBoolean();
        hookrtrn = in.readBoolean();
        inhook = in.readBoolean();
        hookcount = in.readInt();
        hookcodes = in.readInt();
        line = in.readInt();
    }

    public DebugInfo nextInfo() {
        DebugInfo di = debugInfo[debugCalls];
        if (di == null) {
            di = new DebugInfo();
            debugInfo[debugCalls] = di;
        }
        return di;
    }

    public DebugInfo pushInfo(int calls) {
        if (calls > LuaThread.MAX_CALLSTACK) {
            throw new LuaError("Stack overflow: " + calls);
        }
        while (debugCalls < calls) {
            nextInfo();
            ++debugCalls;
        }
        return debugInfo[debugCalls - 1];
    }

    public void popInfo(int calls) {
        if (debugCalls <= calls) {
            LOG.debug("No-op popInfo call: currentDepth={}, targetDepth={}");
        }

        while (debugCalls > calls) {
            DebugInfo di = debugInfo[--debugCalls];
            di.clear();
        }
    }

    void callHookFunc(LuaString type, LuaValue arg) {
        if (inhook || hookfunc == null) {
            return;
        }

        inhook = true;
        try {
            hookfunc.call(type, arg);
        } catch (Exception e) {
            throw new LuaError(e);
        } finally {
            inhook = false;
        }
    }

    public void sethook(LuaValue func, boolean call, boolean line, boolean rtrn, int count) {
        this.hookcount = count;
        this.hookcall = call;
        this.hookline = line;
        this.hookrtrn = rtrn;
        this.hookfunc = func;
    }

    DebugInfo getDebugInfo() {
        return getDebugInfo(1);
    }

    DebugInfo getDebugInfo(int level) {
        if (level <= 0 || level > debugCalls) {
            return null;
        }
        return debugInfo[debugCalls - level];
    }

    public DebugInfo findDebugInfo(LuaValue func) {
        for (int i = debugCalls; --i >= 0;) {
            if (debugInfo[i].func == func) {
                return debugInfo[i];
            }
        }
        return new DebugInfo(func);
    }

    public String tojstring() {
        return DebugTrace.traceback(thread, 0);
    }

}