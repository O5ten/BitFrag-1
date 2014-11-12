package net.comploud.code.bitfrag;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Simple "Proof of Concept" prototype algorithm.
 * Splits data into two fragments and calculates a third parity fragment.
 * Tolerance: 1 fragment lost/corrupted (1/3 of the cluster).
 * Created by tek-ti on 2014-11-11.
 */
public class XORAlgorithm implements Algorithm {
    /**
     * Size of the data contained within this cluster (in bytes).
     */
    private final int dataSize;

    /**
     * Amount of fragments associated with this cluster.
     */
    private final int clusterSize;

    /**
     * The amount of fragments that are redundant to the original data.
     * This is the amount of fragments that can be lost/tampered without compromizing the cluster.
     */
    private final int clusterOverhead;

    /**
     * Fragment format version of this cluster.
     */
    private final short formatVersion;

    /**
     * Supported fragment format as of this version.
     * This works like a magic cookie to identify the fragment format. When the fragment format is changed, this number
     * will be re-generated during development.
     * So, just a little heads up for those of you who wishes to actually use BitFrag in its early versions:
     * This may change in future versions with no backwards compatibility. However, when BitFrag hits its first stable
     * release, there will be backwards compatibility in case of fragment format is changed.
     */
    public static final short SUPPORTED_FRAGMENT_VERSION = 0x6C96;    // Calculated by fair die roll


    public XORAlgorithm(ByteBuffer data, int clusterSize, int clusterOverhead) {
        this.dataSize = data.array().length;
        this.clusterSize = clusterSize;
        this.clusterOverhead = clusterOverhead;
        this.formatVersion = SUPPORTED_FRAGMENT_VERSION;
    }


    /**
     * Perform data fragmentation.
     * @param input The raw input data to frag
     * @return A fresh complete cluster
     */
    public Cluster fragment(ByteBuffer input) {
        Cluster<XORFragment> clust = new Cluster<XORFragment>(UUID.nameUUIDFromBytes(input.array()));

        // Perform fragmentation
        byte[] dataRaw = input.array();                      int dri = 0;
        int fragSize = dataRaw.length / 2;
        int fragSizeSpare = dataRaw.length % 2;             // Is, clearly, either 0 or 1
        byte[] x1 = new byte[fragSize + fragSizeSpare];     int x1i = 0;
        byte[] x2 = new byte[fragSize];                     int x2i = 0;
        byte[] p = new byte[fragSize + fragSizeSpare];      int pi = 0;
        while(dri < dataRaw.length) {
            // This is every even byte
            x1[x1i++] = dataRaw[dri++];
            if(dri < dataRaw.length) {  // Yes, same check, I know...
                // This is every odd byte
                x2[x2i++] = dataRaw[dri++];
                p[pi++] = (byte)(x1[x1i-1] ^ x2[x2i-1]);  // XOR operation to calculate parity
            } else {
                // Special case: There is no x2 for this x1 byte. Pad it!
                p[pi++] = x1[x1i-1];    // Actually, XOR operation with 0x00 pad is not necessary as it evaluates to x1
                // The payload size parameter will make sure that the x2 pad is not accidentally "included" during reconstruction
            }
        }

        clust.add(new XORFragment(clust, (byte)1, ByteBuffer.wrap(x1)));   // TODO Use enum or something neat to represent the piece parameter?
        clust.add(new XORFragment(clust, (byte)2, ByteBuffer.wrap(x2)));
        clust.add(new XORFragment(clust, (byte)3, ByteBuffer.wrap(p)));
        return clust;
    }

    /**
     * Attempt a data reconstruction operation by the specified cluster.
     * @param cluster Complete or partial cluster
     * @param output Destination buffer to write the reconstructed data, if possible
     * @return Reconstruction report with further details
     * @throws InsufficientFragmentsException If insufficient fragments are provided by the cluster
     * @throws ReconstructionException if the cluster digest (UUID) check fails
     */
    public ReconstructionReport reconstruct(Cluster cluster, ByteBuffer output) throws InsufficientFragmentsException, ReconstructionException {
        Iterator<XORFragment> iter = cluster.iterator();
        byte[] dataRaw = new byte[dataSize];    int dri = 0;    // TODO Use Java NIO?
        byte[] x1 = null;                       int x1i = 0;
        byte[] x2 = null;                       int x2i = 0;
        byte[] p = null;                        int pi = 0;

        // Find/sort the different pieces (fragments)
        while(iter.hasNext()) {
            XORFragment f = iter.next();
            switch(f.getPiece()) {      // Heh, it's not everyday you get a reason to use switch-case... Is this uncool?
                case 1:
                    x1 = f.getPayload().array();
                    break;
                case 2:
                    x2 = f.getPayload().array();
                    break;
                case 3:
                    p = f.getPayload().array();
                    break;
                default:
                    // This wrong (at the moment of XOR test implementation)
                    break;
            }
        }

        // Now, let's see what we have
        // TODO Add the case where all fragments are known. In this case, parity should be used as a verification to detect/correct malicious fragment modifications
        // Call this "consensus check"
        if(x1 != null && x2 != null) {
            // This is the easiest case: Just byte-by-byte concatenate x1 and x2
            while(dri < dataRaw.length) {
                if(dri % 2 == 0) {
                    // This is every even byte
                    if(x1i < x1.length) {
                        dataRaw[dri++] = x1[x1i++];
                    } else {
                        // This is an erroneous state (x1 is less than required)!
                        throw new Error("Erroneous state during reconstruction!");
                    }
                } else {
                    // This is every odd byte
                    if(x2i < x2.length) {
                        dataRaw[dri++] = x2[x2i++];
                    } else {
                        // There's an x1 but no x2. The previous byte was the last one. We're done here.
                        // Actually, this is an erroneous state (x2 is smaller than required)
                        throw new Error("Erroneous state during reconstruction!");
                    }
                }
            }
        } else if(x1 != null && p != null) {
            // In this case, we've got x1 and the parity
            while(dri < dataRaw.length) {
                if(x1i < x1.length) {
                    if(dri % 2 == 0) {
                        // Even: Time for a x1 byte (this is trivial)
                        dataRaw[dri++] = x1[x1i];   // Note that x1i is NOT incremented! The next byte (parity) will need this intact (see the while statement)
                    } else {
                        // Odd: Time for an x2 byte. This need to be calculated from parity.
                        if(pi < p.length) {
                            dataRaw[dri++] = (byte)(x1[x1i++] ^ p[pi++]);   // XOR operation to calculate x2 from parity
                        } else {
                            // This is an erroneous state (p is smaller than necessary)!
                            throw new Error("Erroneous state during reconstruction!");
                            // Special case: There's an x1 but no p. This is the last byte.
                            //dataRaw[dri++] = x1[x1i++] /* ^ 0x00*/;         // XOR with padding not necessary
                        }
                    }
                } else {
                    // This is an erroneous condition (x1 is smaller than necessary)!
                    throw new Error("Erroneous state during reconstruction!");
                }
            }
        } else if(x2 != null && p != null) {
            // This is _almost_ the same case as above, but with x2 instead if x1 (and a different special case)
            while(dri < dataRaw.length) {
                if(x2i < x2.length) {
                    if(dri % 2 == 0) {
                        // Even: Time for an x1 byte. This needs to be calculated from parity.
                        if(pi < p.length) {
                            dataRaw[dri++] = (byte)(x2[x2i] ^ p[pi++]); // XOR operation to calculate x1 from parity
                            // Note that x2i is NOT incremented!
                        } else {
                            // This is an erroneous condition (p is smaller than necessary)!
                            throw new Error("Erroneous state during reconstruction!");
                            // Special case: There's an x2 but no p left. Use padding.
                            //dataRaw[dri++] = x2[x2i++]/* ^ 0x00*/;  // XOR with padding not necessary
                        }
                    } else {
                        // Odd: This is an x2 byte
                        dataRaw[dri++] = x2[x2i++];
                    }
                } else {
                    // Special case: There is an x1 byte (missing) and no x2 byte. Use padding with the next (last) parity byte.
                    if(pi < p.length) {
                        dataRaw[dri++] = p[pi++]/* ^ 0x00*/;    // XOR with padding not necessary
                    } else {
                        // This is an erroneous state (p is smaller than necessary)!
                        throw new Error("Erroneous state during reconstruction!");
                    }
                }
            }
        } else {
            // There are insufficient fragments to reconstruct the data!
            int missing = 3 - (x1 != null ? 1 : 0) + (x1 != null ? 1 : 0) + (p != null ? 1 : 0);
            throw new InsufficientFragmentsException(missing);
        }

        // Calculate (and verify) the supplied digest with the reconstructed data
        UUID verification = UUID.nameUUIDFromBytes(dataRaw);
        if(!verification.equals(cluster.getId())) {
            // Verification failed
            throw new ReconstructionException(cluster.getId(), verification, ByteBuffer.wrap(dataRaw));
        } else {
            // All cases have been covered. Success!
            output.put(dataRaw);    // Copy the internal buffer into the caller specified buffer
            int missing = 0 + (x1 == null ? 1 : 0) + (x1 == null ? 1 : 0) + (p == null ? 1 : 0);
            // TODO Actually identify the corrupted and tempered fragments
            return new ReconstructionReport(missing, new CopyOnWriteArraySet<Fragment>(), new CopyOnWriteArraySet<Fragment>());
        }
    }

    /**
     * Get the fragment format version.
     * @return Fragment format version
     */
    public int getFormatVersion() { return formatVersion; }

    /**
     * Get the size of the data contained within this cluster (in bytes).
     * @return Data size
     */
    public int getDataSize() { return dataSize; }

    /**
     * Get the specified cluster size.
     * @return Cluster size
     */
    public int getClusterSize() { return clusterSize; }

    /**
     * Get the specified cluster overhead.
     * @return Cluster overhead
     */
    public int getOverhead() { return clusterOverhead; }

}
