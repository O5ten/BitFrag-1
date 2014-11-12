package net.comploud.code.bitfrag;

import java.nio.ByteBuffer;
import net.comploud.code.bitfrag.InsufficientFragmentsException;
import net.comploud.code.bitfrag.ReconstructionException;

/**
 * An algorithm to frag and reconstruct clusters/data.
 * Created by tek-ti on 2014-11-10.
 */
public interface Algorithm {
    /**
     * Perorm fragmentation.
     * @param input The raw input data to frag
     * @return A complete cluster
     */
    public Cluster fragment(ByteBuffer input);

    /**
     * Reconstruct (defragment) the data.
     * The implementation may return a positive value to indicate success with an
     * indication of how many fragments that were reanimated (missing or corrupted).
     * If the reconstruction failed, a negative number should be returned with the optional
     * indication of how many fragments that are missing (if possible).
     * Note that a return value of 0 always signals that the reconstruction went flawless using
     * a full cluster with no corrupted fragments.
     * @param cluster Complete or partial cluster
     * @param output Destination buffer to write the reconstructed data, if possible
     * @return A reconstruction report with further details
     * @throws InsufficientFragmentsException if insufficient fragments are provided by the cluster
     * @throws ReconstructionException if the cluster digest (UUID) verification fails
     */
    public ReconstructionReport reconstruct(Cluster cluster, ByteBuffer output) throws InsufficientFragmentsException, ReconstructionException;
}
