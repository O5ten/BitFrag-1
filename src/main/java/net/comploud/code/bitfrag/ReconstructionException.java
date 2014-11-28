package net.comploud.code.bitfrag;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Exception that signals that a reconstruction operation failed due to mismatching
 * cluster digest (UUID) values.
 * Created by tek-ti on 2014-11-12.
 */
public class ReconstructionException extends Exception {
    /**
     * The digest (UUID) that was specified (consensus elected) during reconstruction.
     */
    private final UUID invalidDigest;

    /**
     * the digest (UUID) that was calculated from the reconstructed data.
     */
    private final UUID actualDigest;

    /**
     * Constructor with necessary details.
     * @param invalidDigest Supplied invalid digest
     * @param actualDigest Calculated digest
     */
    public ReconstructionException(UUID invalidDigest, UUID actualDigest) {
        super(invalidDigest + " != " + actualDigest);
        this.invalidDigest = invalidDigest;
        this.actualDigest = actualDigest;
    }


    /**
     * Returns the digest (UUID) that was specified (consensus elected) during reconstruction.
     * @return Invalid digest
     */
    public UUID getInvalidDigest() {
        return invalidDigest;
    }

    /**
     * Returns the digest (UUID) that was calculated using the reconstructed data.
     * @return Actual digest
     */
    public UUID getActualDigest() {
        return actualDigest;
    }
}
