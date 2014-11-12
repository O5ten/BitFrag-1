package net.comploud.code.bitfrag;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Encoded data fragment.
 * Created by tek-ti on 2014-09-08.
 */
public class XORFragment implements Fragment/*, Serializable*/ {
    /**
     * Reference to the cluster that this fragment is associated with.
     * Upon importing a fragment using parseFragment(), this will be a new cluster with the according parameters as
     * imported. This reference can (and should) later be updated using the mergeInto() method.
     */
    private Cluster cluster;

    /**
     * UUID (type 3) including a digest of the fragment payload.
     * This digest can be used to verify the fragment payload data.
     */
    private final UUID fragUuid;

    /**
     * Fragment role in the cluster.
     * This is a parameter essential for the fragment/reconstruct process.
     * Basically, it identifies what piece of the data this fragment corresponds to.
     */
    private final byte piece;    // TODO Use an enum for this?

    /**
     * The actual fragment data.
     */
    private final ByteBuffer payload;


    /**
     * Constructor to be used when creating a fragment for the first time (during the fragmentation process).
     * @param cluster Cluster reference
     * @param piece What piece of the cluster (algorithm-wise) this is
     * @param payload Payload data for this particular fragment
     */
    public XORFragment(Cluster cluster, byte piece, ByteBuffer payload) {
        this.cluster = cluster;
        this.payload = payload;
        this.piece = piece;
        this.fragUuid = UUID.nameUUIDFromBytes(getPayload().array());
    }

    /**
     * Constructor to be used internally when importing/parsing a fragment.
     */
    private XORFragment(Cluster cluster, UUID frag, byte piece, ByteBuffer payload) {
        this.cluster = cluster;
        this.fragUuid = frag;
        this.piece = piece;
        this.payload = payload;
    }


    // TODO The "cluster parameters" (well, algorithm parameters to be exact) may be the same and may have to match, but they should all uniquely belong to each fragment and be matched during reconstruction!
    // This is because corrupted fragments may set an invalid standard for a cluster and thereby deny all other (valid) fragments that tries to mergeInto() the cluster!


    /**
     * Parse a possible fragment from an open input stream.
     * The input stream have to be initialized and readable.
     * This method will read an arbitrary amount of data from the stream (hence advancing its position).
     * @param src Open and readable input stream to read fragment from
     * @return A verified fragment
     * @throws IOException Upon a failed stream operation
     * @throws FragmentFormatException If fragment UUID (digest) mismatches with the payload data
     */ // TODO Move this to XORFragmentReader class
    //static XORFragment parseFragment(InputStream src) throws IOException, FragmentFormatException {  // TODO Use Serialization API instead?
        /*DataInputStream din = new DataInputStream(src);

        // Read version header
        short hVersion = din.readShort();
        if(hVersion != XORAlgorithm.SUPPORTED_FRAGMENT_VERSION) {     // The only supported/expected version as of now
            throw new FragmentFormatException("Unsupported version header: " + hVersion);
        }

        // Read cluster UUID
        long hCUuidMBits = din.readLong();
        long hCUuidLBits = din.readLong();
        UUID hCUuid = new UUID(hCUuidMBits, hCUuidLBits);
        // Keep this for later

        // Read fragment UUID
        long hFUuidMBits = din.readLong();
        long hFUuidLBits = din.readLong();
        UUID hFUuid = new UUID(hFUuidMBits, hFUuidLBits);
        // Keep this for later

        // Read cluster parameters
        int hDataSize = din.readInt();
        int hClusterSize = din.readInt();
        int hClusterOverhead = din.readInt();

        // Read fragment parameters
        byte hPart = din.readByte();

        // Read the payload data size
        int hPayloadSize = din.readInt();

        // Finally, read the payload data
        byte[] hPayload = new byte[hPayloadSize];   // TODO Protect against aggressive mallocs?
        int hPayloadRead = din.read(hPayload);
        if(hPayloadRead != hPayloadSize) {
            throw new FragmentFormatException("Insufficient payload data: " + hPayloadSize + " != " + hPayloadRead);
        }

        // But first, let's do a consistency check!
        UUID checkUuid = UUID.nameUUIDFromBytes(hPayload);
        if(!checkUuid.equals(hFUuid)) {
            // Consistency check failed!
            throw new FragmentFormatException("Fragment possibly corrupted (hash check failed)");
        }

        // Create the fresh fragment with preliminary cluster and link them properly
        Cluster hCluster = new Cluster(hCUuid, hDataSize, hClusterSize, hClusterOverhead, hVersion);
        XORFragment frag = new XORFragment(hCluster, hFUuid, hPart, ByteBuffer.wrap(hPayload));
        hCluster.add(frag);
        return frag;
        */

        /* Hey! There's room for some optimization here! The way a new cluster is created for each fragment can be
         * refactored to using some sort of FragmentParser that holds discovered clusters - in, say, a HashMap - and
         * sorts the fragments directly into their appropriate clusters.
         * This way, most of these (cluster) memory allocations could be replaced by a respective log(N) lookup.
         * But this would require a different design pattern. Maybe consider in some future?
         */
    //}

    /**
     * Write this fragment to a stream.
     * The stream must be initialized and open.
     * @param dest Destination stream
     * @throws IOException Upon a failed stream operation
     */ // TODO Move this to XORFragmentWriter class
    /*public void export(OutputStream dest) throws IOException {  // TODO Use Serialization API instead?
        DataOutputStream dout = new DataOutputStream(dest);

        // Write version and algorithm header
        dout.writeShort(cluster.getFormatVersion());

        // Write cluster UUID
        UUID clusterUuid = cluster.getId();
        dout.writeLong(clusterUuid.getMostSignificantBits());
        dout.writeLong(clusterUuid.getLeastSignificantBits());

        // Write frag UUID
        dout.writeLong(getId().getMostSignificantBits());
        dout.writeLong(getId().getLeastSignificantBits());

        // Write cluster parameters
        dout.writeInt(cluster.getDataSize());
        dout.writeInt(cluster.getClusterSize());
        dout.writeInt(cluster.getOverhead());

        // Write fragment parameters
        dout.writeByte(piece);

        // Write payload size
        dout.writeInt(getPayload().array().length);

        // Write payload data
        dout.write(getPayload().array());
    }*/


    /**
     * Merge this fragment into the cluster where it belongs.
     * This will abandon this fragments' current cluster reference.
     * Naturally, when a fragment is parsed, a new cluster object will be created according to the specifications in the
     * fragment header data. This method can later be used to merge this fragment into an already existing cluster that
     * matches this fragment specifications (cluster UUID, parameters, etc).
     * @param destination Existing cluster to merge this fragment into.
     * @throws InvalidClusterException If this fragments' cluster specification does no match the destination cluster
     */ // TODO This method is probably obsolete
    public void mergeInto(Cluster destination) throws InvalidClusterException { // TODO Really use an exception for this?
        if(cluster.equals(destination)) {
            if(destination.add(this)) {
                // Successfully added this fragment to the cluster
                this.cluster = destination;
            } else {
                // This fragment already exists in the cluster
            }
            //System.gc();
        } else {
            throw new InvalidClusterException("Fragment does not match to cluster");
        }
    }

    /**
     * Matches this fragment to another.
     * For two fragments to be considered the same, all fragment parameters and the UUID must match.
     * @param other Another fragment instance to compare to
     * @return true if all essential properties of the instances match (and hence are the same fragment), false otherwise
     */
    @Override
    public boolean equals(Object other) {
        if(other == null || !(other instanceof Fragment)) {  // Yep, instanceof. What can you do about it!? :/
            return false;
        } else {
            XORFragment otherFrag = (XORFragment)other;
            return getPiece() == otherFrag.getPiece() &&
                    getId().equals(otherFrag.getId());  // Lets _not_ match payload data (UUID will suffice)
        }
    }

    /**
     * Get the cluster that this fragment is associated with.
     * Note that a newly parsed fragment will be alone in its own cluster unless later merged with an already existing
     * cluster.
     * @return The cluster
     */
    public Cluster getCluster() {
        return cluster;
    }

    /**
     * Get the type 3 UUID for this particular fragment.
     * @return Fragment UUID
     */
    @Override
    public UUID getId() { return fragUuid; }

    /**
     * Get the flag indicating what piece of the original data this fragment corresponds to.
     * This is essentially a parameter how to fit this fragment into the reconstruction algorithm.
     * @return Fragment piece
     */
    public byte getPiece() {
        return piece;
    }

    /**
     * Get the payload data of this fragment.
     * @return Payload data
     */
    @Override
    public ByteBuffer getPayload() { return payload; }

    /**
     * Perform integrity check.
     * @return true if specified digest (ID) is correct, false otherwise
     */
    @Override
    public boolean integrityCheck() {
        UUID checkUuid = UUID.nameUUIDFromBytes(getPayload().array());
        return checkUuid.equals(getId());
    }

}
