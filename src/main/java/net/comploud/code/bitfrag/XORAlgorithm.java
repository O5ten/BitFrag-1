package net.comploud.code.bitfrag;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
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
     * Cluster upon which methods will operate.
     */
    protected Cluster<XORFragment> cluster;
    // TODO This is a bad programming pattern. Use generics in the interface instead. But how!? (See Algorithm.java)

    /**
     * Supported fragment format as of this version.
     * This works like a magic cookie to identify the fragment format. When the fragment format is changed, this number
     * will be re-generated during development.
     * So, just a little heads up for those of you who wishes to actually use BitFrag in its early versions:
     * This may change in future versions with no backwards compatibility. However, when BitFrag hits its first stable
     * release, there will be backwards compatibility in case of fragment format is changed.
     */
    public static final short SUPPORTED_FRAGMENT_VERSION = (short)0xDCA1;    // Calculated by fair die roll


    /**
     * Constructor.
     * @param cluster The cluster to operate upon
     */
    public XORAlgorithm(Cluster<XORFragment> cluster) {
        this.cluster = cluster;
    }


    /**
     * Perform data fragmentation.
     * @param input The raw input data to frag
     * @return A fresh complete cluster
     */
    public Cluster<XORFragment> fragment(ByteBuffer input) {
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

        int clusterDataSize = dataRaw.length;
        clust.add(new XORFragment(SUPPORTED_FRAGMENT_VERSION, clust.getId(), UUID.nameUUIDFromBytes(x1), clusterDataSize, (byte)1, ByteBuffer.wrap(x1)));   // TODO Use enum or something neat to represent the piece parameter?
        clust.add(new XORFragment(SUPPORTED_FRAGMENT_VERSION, clust.getId(), UUID.nameUUIDFromBytes(x2), clusterDataSize, (byte)2, ByteBuffer.wrap(x2)));
        clust.add(new XORFragment(SUPPORTED_FRAGMENT_VERSION, clust.getId(), UUID.nameUUIDFromBytes(p), clusterDataSize, (byte)3, ByteBuffer.wrap(p)));
        return clust;
    }

    /**
     * Attempt a data reconstruction operation by the specified cluster.
     * @param output Destination buffer to write the reconstructed data, if possible
     * @return Reconstruction report with further details
     * @throws InsufficientFragmentsException If insufficient fragments are provided by the cluster
     * @throws ReconstructionException if the cluster digest (UUID) check fails
     */
    public ReconstructionReport reconstruct(ByteBuffer output) throws InsufficientFragmentsException, ReconstructionException {
        Iterator<XORFragment> iter = cluster.iterator();
        byte[] dataRaw = new byte[concurDataSize()];    int dri = 0;    // TODO Use Java NIO? Yes, use Java NIO!
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
            int missing = 3 - (x1 == null ? 1 : 0) - (x2 == null ? 1 : 0) - (p == null ? 1 : 0);
            throw new InsufficientFragmentsException(missing);
        }

        // Calculate (and verify) the supplied digest with the reconstructed data
        UUID verification = UUID.nameUUIDFromBytes(dataRaw);
        if(!verification.equals(cluster.getId())) {
            // Verification failed
            throw new ReconstructionException(cluster.getId(), verification);
        } else {
            // All cases have been covered. Success!
            output.put(dataRaw);    // Copy the internal buffer into the caller specified buffer
            output.flip();
            // TODO Not sure about this... Just returning this might be easier? Or put() in the output buffer in the first place!

            int missing = (x1 == null ? 1 : 0) + (x2 == null ? 1 : 0) + (p == null ? 1 : 0);
            // TODO Actually identify the corrupted and tempered fragments
            return new ReconstructionReport(missing, new CopyOnWriteArraySet<Fragment>(), new CopyOnWriteArraySet<Fragment>());
        }

        /*
         * In theory, this algorithm is very parallelizable ("parallelizable" is now a word!). But given the simplicity
         * of the core operations (the XOR operation and some occasional branches) this may be counter productive.
         * The added complexity of spawning threads and distributing work - not to mention cache coherency - might
         * simply end up with greater execution times. However, if the core algorithm operations were more
         * computationally complex, parallelizing this method could be beneficial (if the computations exceeds the
         * IO_WAIT time). Might be worthwhile a consideration for the Reed-Solomon algorithm?
         */
    }

    // TODO Add consensus check for the fragment headers (concurDataSize(), etc)

    /**
     * Concur upon the data size.
     * This is a non-trivial operation.
     * @see Algorithm
     * @return Data size (in bytes)
     * @throws InsufficientFragmentsException If cluster is empty
     */
    @Override
    public int concurDataSize() throws InsufficientFragmentsException {
        // Perform consensus negotiation recursively upon the cluster
        return concurDataSizeRecv(new HashMap<Integer, Integer>(), cluster.iterator());
    }
    // TODO Move this method (and helper method) to an AbstractAlgorithm implementation? If moving of getClusterDataSize() to Fragment interface is a good idea, that is...
    private int concurDataSizeRecv(Map<Integer, Integer> observed, Iterator<XORFragment> left) throws InsufficientFragmentsException {
        if(left.hasNext()) {
            // Process the next fragment
            XORFragment frag = left.next();
            int fragValue = frag.getClusterDataSize();
            Integer count = observed.get(fragValue);
            if(count == null) {
                // This is the first observation of this value
                observed.put(fragValue, 1);
            } else {
                // Increase the count of this observation
                ++count;
                //observed.put(fragValue, count);   // Not necessary since count is a reference to the Integer object already in the map
            }

            // Proceed with recursion
            return concurDataSizeRecv(observed, left);
        } else {
            // There are no more fragments to inspect

            // Find the most observed value
            Map.Entry<Integer, Integer> winner = null;
            for(Map.Entry<Integer, Integer> observation : observed.entrySet()) {
                if(winner == null) {
                    winner = observation;
                } else if(observation.getValue() > winner.getValue()) {
                    winner = observation;
                }
                // else: Do nothing and proceed to next map entry
            }

            // We're done here
            if(winner == null) {
                // There were no fragments in the cluster to begin with!
                throw new InsufficientFragmentsException(0);
            } else {
                // This is the end of recursion
                return winner.getKey();
            }
        }
    }
}
