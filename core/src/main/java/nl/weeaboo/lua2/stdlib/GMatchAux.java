package nl.weeaboo.lua2.stdlib;

import static nl.weeaboo.lua2.vm.LuaNil.NIL;

import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.lua2.lib.VarArgFunction;
import nl.weeaboo.lua2.vm.LuaString;
import nl.weeaboo.lua2.vm.Varargs;

@LuaSerializable
final class GMatchAux extends VarArgFunction {

    private static final long serialVersionUID = 1L;

    private final int srclen;
    private final MatchState ms;
    private int soffset;

    public GMatchAux(Varargs args, LuaString src, LuaString pat) {
        this.srclen = src.length();
        this.ms = new MatchState(args, src, pat);
        this.soffset = 0;
    }

    @Override
    public Varargs invoke(Varargs args) {
        for (; soffset < srclen; soffset++) {
            ms.reset();
            int res = ms.match(soffset, 0);
            if (res >= 0) {
                int soff = soffset;
                soffset = res;
                return ms.push_captures(true, soff, res);
            }
        }
        return NIL;
    }
}
