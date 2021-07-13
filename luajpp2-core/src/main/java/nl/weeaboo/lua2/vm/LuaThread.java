/*******************************************************************************
 * Copyright (c) 2007 LuaJ. All rights reserved.
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

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.weeaboo.lua2.LuaException;
import nl.weeaboo.lua2.LuaRunState;
import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.stdlib.CoroutineLib;
import nl.weeaboo.lua2.stdlib.DebugLib;
import nl.weeaboo.lua2.stdlib.DebugTrace;

/**
 * Lua thread object
 */
@LuaSerializable
public final class LuaThread extends LuaValue implements Serializable {

    private static final Logger LOG = LoggerFactory.getLogger(LuaThread.class);
    private static final long serialVersionUID = 4L;

    private static final AtomicInteger threadIdGenerator = new AtomicInteger();

    private final int threadId = threadIdGenerator.incrementAndGet();
    private LuaRunState luaRunState;
    private LuaValue env;
    private String name = Integer.toString(threadId);

    private LuaThreadStatus status = LuaThreadStatus.INITIAL;
    private int callstackMin;
    private boolean isMainThread;
    private boolean isPersistent;

    private int sleep;

    @Nullable StackFrame callstack;
    public @Nullable Object debugState;

    /**
     * Do not use. Required for efficient serialization.
     */
    @Deprecated
    public LuaThread() {
    }

    public LuaThread(LuaRunState lrs, LuaValue environment) {
        this.luaRunState = lrs;
        this.env = environment;

        callstack = null;
    }

    /**
     * Create a LuaThread around a function and environment
     *
     * @param function The function to execute
     *
     * @deprecated Use {@link LuaRunState#newThread(LuaClosure, Varargs)} instead.
     */
    @Deprecated
    public LuaThread(LuaThread parent, LuaClosure function) {
        this(parent.luaRunState, parent.getfenv());

        callstack = StackFrame.newInstance(function, NONE, function.tojstring(), null, 0, 0);
    }

    /**
     * @deprecated For internal use only.
     */
    @Deprecated
    public static LuaThread createMainThread(LuaRunState lrs, LuaValue env) {
        LuaThread thread = new LuaThread(lrs, env);
        thread.setName("main");
        thread.isMainThread = true;
        thread.isPersistent = true;
        return thread;
    }

    /** Resets the thread to its initial state. */
    public void reset() {
        resetCallstack();
        debugState = null;
    }

    private void resetCallstack() {
        StackFrame.releaseCallstack(callstack);

        status = LuaThreadStatus.INITIAL;
        callstackMin = 0;
        callstack = null;

        DebugLib.debugResetCallstack(this);
    }

    public int getThreadId() {
        return threadId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String tojstring() {
        return typename() + ": " + name;
    }

    @Override
    public int type() {
        return LuaConstants.TTHREAD;
    }

    @Override
    public String typename() {
        return "thread";
    }

    @Override
    public boolean isthread() {
        return true;
    }

    @Override
    public LuaThread optthread(LuaThread defval) {
        return this;
    }

    @Override
    public LuaThread checkthread() {
        return this;
    }

    @Override
    public LuaValue getmetatable() {
        return luaRunState.getMetatables().getThreadMetatable();
    }

    @Override
    public LuaValue getfenv() {
        return env;
    }

    @Override
    public void setfenv(LuaValue env) {
        this.env = env;
    }

    /** Returns the thread's status. */
    public LuaThreadStatus getStatus() {
        return status;
    }

    /** Returns {@code true} if the thread's status is {@link LuaThreadStatus#RUNNING}. */
    public boolean isRunning() {
        return status == LuaThreadStatus.RUNNING;
    }

    /** Returns {@code true} if the thread can be resumed. */
    public boolean isRunnable() {
        return !isDead() && callstack != null;
    }

    /** Returns {@code true} if the thread's status is {@link LuaThreadStatus#DEAD}. */
    public boolean isDead() {
        return status == LuaThreadStatus.DEAD;
    }

    /** Destroys the thread, making it dead. */
    public void destroy() {
        status = LuaThreadStatus.DEAD;
    }

    /** Returns the number of frames on the thread's call stack. */
    public int callstackSize() {
        return (callstack != null ? callstack.size() : 0);
    }

    void preCall(StackFrame sf) {
        if (DebugLib.isDebugEnabled()) {
            DebugLib.debugSetupCall(this, sf.args, sf.stack);
            DebugLib.debugOnCall(this, sf.func, sf.functionName);

            LOG.trace(">>({}) {}", sf.size(), sf);
        }
    }

    /**
     * @param sf The stack frame that was just popped from the callstack.
     */
    void postReturn(StackFrame sf) {
        if (DebugLib.isDebugEnabled()) {
            LOG.trace("<<({}) {}", sf.size(), sf);

            DebugLib.debugOnReturn(this);
        }

        if (callstack == null) {
            LOG.trace("Final stack frame popped: {}", sf);
        }
    }

    /**
     * Pushes a new Lua closure onto the call stack.
     */
    public void pushPending(LuaClosure func, Varargs args) {
        pushPending(func, args, -1, 0);
    }

    void pushPending(LuaClosure func, Varargs args, int returnBase, int returnCount) {
        if (callstack != null && callstack.status == StackFrame.Status.FINISHED) {
            LOG.error("Callstack was corrupted -- parent stack frame is finished :: parent={}",
                    callstack);
        }

        String funcName = DebugTrace.getCalledFunctionName(this);
        callstack = StackFrame.newInstance(func, args, funcName, callstack, returnBase, returnCount);

        /*
         * When adding something to the call stack, change the status from initial to something else.
         * Otherwise, the first attempt to resume the thread will overwrite the arguments for the topmost
         * stack frame.
         */
        if (status == LuaThreadStatus.INITIAL) {
            status = LuaThreadStatus.SUSPENDED;
        }
    }

    /**
     * Pushes a function on the call stack of this thread, then runs the thread until that function returns or
     * yields. This method ignores the sleep count of the thread.
     *
     * @see #pushPending(LuaClosure, Varargs)
     * @see #getSleep()
     */
    public Varargs callFunctionInThread(LuaClosure function, Varargs args) {
        pushPending(function, args);

        Varargs result;
        int oldSleep = getSleep();
        try {
            sleep = 0;
            result = resume(1);
        } finally {
            setSleep(oldSleep);
        }
        return result;
    }

    /**
     * Returns the function at the requested call stack offset.
     * @param level 1-based offset, where 1 is the current level.
     * @return The function, or {@code null} if not found.
     */
    public @Nullable LuaFunction getCallstackFunction(int level) {
        if (callstack == null) {
            return null;
        }
        return callstack.getCallstackFunction(level);
    }

    @Nullable StackFrame getStackFrame(int level) {
        if (callstack == null) {
            return null;
        }
        return callstack.getStackFrame(level);
    }

    /**
     * Returns the currently running thread.
     */
    public static LuaThread getRunning() {
        LuaRunState lrs = LuaRunState.getCurrent();
        if (lrs == null) {
            throw new RuntimeException("No LuaRunState valid on current thread: " + Thread.currentThread());
        }
        return lrs.getRunningThread();
    }

    /**
     * Returns {@code true} if this is the main Lua thread.
     */
    public boolean isMainThread() {
        return isMainThread;
    }

    /**
     * Yield this thread with arguments.
     */
    public Varargs yield(Varargs args) {
        if (!isRunning()) {
            error(this + " not running");
        }

        status = LuaThreadStatus.SUSPENDED;
        return args;
    }

    /**
     * @deprecated For internal use only.
     */
    @Deprecated
    public Varargs endCall(Varargs args) {
        args = yield(args);
        status = LuaThreadStatus.END_CALL;
        return args;
    }

    /**
     * Runs the thread until it suspends or finishes.
     */
    public Varargs resume(Varargs args) {
        LuaThreadStatus status = getStatus();
        if (status == LuaThreadStatus.INITIAL) {
            // Start new coroutine
            if (callstack != null) {
                callstack.setArgs(args);
            }
        } else if (status == LuaThreadStatus.SUSPENDED || status == LuaThreadStatus.END_CALL) {
            // Resume coroutine
            // Place args on the thread's stack as though it was returned from the call that yielded
            if (callstack != null) {
                callstack.setReturnedValues(args);
            }
        } else {
            throw new LuaException("Unable to resume coroutine: " + this + ", status="
                    + CoroutineLib.getCoroutineStatus(status));
        }

        return resume(-1);
    }

    /**
     * Runs the thread until it suspends or finishes.
     *
     * @param maxDepth If {@code >= 0} suspends the thread when the call stack becomes more than
     *        {@code maxDepth} smaller than it was when the resume method was called.
     */
    private Varargs resume(int maxDepth) {
        if (isDead()) {
            throw new LuaException("cannot resume dead thread");
        }

        if (sleep != 0) {
            if (sleep > 0) {
                sleep--;
            }
            return NONE;
        }

        final int oldCallstackMin = callstackMin;
        final LuaThread prior = luaRunState.getRunningThread();
        final LuaThreadStatus priorStatus = prior.getStatus();

        Varargs result;
        try {
            if (priorStatus == LuaThreadStatus.RUNNING) {
                prior.status = LuaThreadStatus.SUSPENDED;
            }
            status = LuaThreadStatus.RUNNING;
            setRunningThread(this);

            callstackMin = Math.max(callstackMin, (maxDepth < 0 ? 0 : callstackSize() - maxDepth));
            result = LuaInterpreter.resume(this, callstackMin);
        } catch (LuaException e) {
            popStackFrames();
            throw e;
        } catch (RuntimeException e) {
            popStackFrames();
            throw LuaException.wrap("Runtime error in Lua thread", e);
        } finally {
            callstackMin = oldCallstackMin;
            setRunningThread(prior);

            if (callstack == null) {
                status = (isPersistent ? LuaThreadStatus.SUSPENDED : LuaThreadStatus.DEAD);
            } else if (status == LuaThreadStatus.RUNNING) {
                status = LuaThreadStatus.SUSPENDED;
            }

            if (prior.status == LuaThreadStatus.SUSPENDED && priorStatus == LuaThreadStatus.RUNNING) {
                prior.status = LuaThreadStatus.RUNNING;
            }
        }

        return result;
    }

    @SuppressWarnings("deprecation")
    private void setRunningThread(LuaThread thread) {
        luaRunState.setRunningThread(thread);
    }

    private void popStackFrames() {
        // Note: maxDepth may be negative
        while (callstack != null && callstackSize() > callstackMin) {
            popStackFrame();
        }
    }

    void popStackFrame() {
        final StackFrame sf = callstack;
        if (sf == null) {
            throw new IllegalStateException("Unable to pop stack frame; callstack is empty");
        }

        // Pop from call stack
        callstack = sf.parent;

        // Close stack frame
        sf.close();

        // Notify debuglib that we've returned from our current call
        postReturn(sf);

        if (callstack != null && callstack.status == StackFrame.Status.FINISHED) {
            LOG.error("Callstack was corrupted -- parent stack frame is finished :: popped={}, parent={}",
                    sf, callstack);
        }
    }

    /**
     * @deprecated For internal use only.
     */
    @Deprecated
    public LuaValue getCallEnv() {
        StackFrame sf = callstack;
        if (sf != null) {
            LuaFunction func = sf.getCallstackFunction(1);
            if (func != null) {
                return func.getfenv();
            }
        }
        return getfenv();
    }

    /** Jumps to the given closure, ending the current call. */
    public void jump(LuaClosure closure, LuaValue args) {
        resetCallstack();

        pushPending(closure, args);
    }

    /**
     * Sets the sleep counter for the current thread.
     *
     * @param count If positive, ignores the next {@code count} attempts to resume the thread, decrementing
     *        the internal sleep count by one every time. Use a count of {@code -1} to sleep forever.
     */
    public void setSleep(int count) {
        sleep = count;
    }

    /**
     * Returns the current value of the internal sleep counter.
     *
     * @see #setSleep(int)
     */
    public int getSleep() {
        return sleep;
    }

    /**
     * A persistent thread doesn't die when it finishes running all of its code. This allows you to reuse a
     * single thread to occasionally run pieces of code.
     */
    public boolean isPersistent() {
        return isPersistent;
    }

    /**
     * @see #isPersistent()
     */
    public void setPersistent(boolean isPersistent) {
        this.isPersistent = isPersistent;
    }

}
