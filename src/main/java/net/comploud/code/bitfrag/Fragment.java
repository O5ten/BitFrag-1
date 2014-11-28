package net.comploud.code.bitfrag;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * A general fragment.
 * Created by tek-ti on 2014-11-10.
 */
public interface Fragment {
    /**
     * Returns the UUID for this fragment.
     * @return net.comploud.code.bitfrag.Fragment ID
     */
    public UUID getId();        // TODO Use custom implementation of UUID (with possibilities of smaller sizes)?

    /**
     * Returns the UUID of the cluster of which this fragment is associated with.
     * @return net.comploud.code.bitfrag.Cluster ID
     */
    public UUID getClusterId();

    /**
     * Returns the fragment payload data.
     * @return Payload data
     */
    public ByteBuffer getPayload();

    /**
     * Calculates a message digest of the payload data and compare it with the supplied digest (ID).
     * @return true if digest (ID) is correct, false otherwise
     */
    public boolean integrityCheck();
}
