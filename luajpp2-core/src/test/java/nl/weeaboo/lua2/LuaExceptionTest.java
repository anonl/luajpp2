package nl.weeaboo.lua2;

import org.junit.Assert;
import org.junit.Test;

import nl.weeaboo.lua2.vm.LuaString;

public final class LuaExceptionTest {

    /**
     * It should be possible to initialize a {@link LuaException} even if no {@link LuaRunState} is current.
     */
    @Test
    public void testNoLuaRunStateCurrent() {
        Assert.assertNull(LuaRunState.getCurrent());

        LuaException ex = new LuaException(LuaString.valueOf("abc"));
        Assert.assertEquals("abc", ex.getMessage());
    }

}
