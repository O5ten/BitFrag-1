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
     * Perform fragmentation.
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
     * @param output Destination buffer to write the reconstructed data, if possible
     * @return A reconstruction report with further details
     * @throws InsufficientFragmentsException if insufficient fragments are provided by the cluster
     * @throws ReconstructionException if the cluster digest (UUID) verification fails
     */
    public ReconstructionReport reconstruct(ByteBuffer output) throws InsufficientFragmentsException, ReconstructionException;

    /**
     * Concur upon a consensus of the data size parameter.
     * Since this value is passed in each fragment, it may (erroneously) vary among the different fragments.
     * Such case would imply a corrupted or tampered fragment. In order to avoid such fragment to ruin the
     * reconstruction, this parameter is negotiated using a consensus between the fragments.
     * The same limitations as the reconstruction algorithm apples to the tolerance of corrupted/tampered parameters.
     * @return Concluded data size
     */
    public int concurDataSize() throws InsufficientFragmentsException;

    // TODO Use generics for these methods and let the cluster be agument1. But how!?
    /*
     * Cluster<Fragment> is NOT super to Cluster<XORFragment>! Admittedly, I fell for that one in my code design...
     * Not really sure how do best wield the object orientation ninja sword to get this done as elegantly as adequately
     * possible... Chase down this TO-DO later!
     * Meanwhile, the implementation needs to take a cluster (with its own fragment implementation) in the constructor.
     */
}
