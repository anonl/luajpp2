package nl.weeaboo.lua2.io;

import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 * This interface declares the {@link #readResolve()} method with the proper signature for use with Java serialization.
 *
 * @see Serializable
 */
public interface IReadResolveSerializable extends Serializable {

    /**
     * This is the interface version of the magic 'readResolve' method used by Java's {@link ObjectInputStream}.
     *
     * @throws ObjectStreamException If the object can't be read.
     */
    Object readResolve() throws ObjectStreamException;

}
