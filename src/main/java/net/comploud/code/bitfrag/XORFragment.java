package net.comploud.code.bitfrag;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Encoded data fragment.
 * Created by tek-ti on 2014-09-08.
 */
public class XORFragment implements Fragment, FragmentWriter/*, Serializable*/ {
    /**
     * The fragment version header.
     */
    private final short version;

    /**
     * The cluster ID header.
     * This ID also contains an embedded message digest of the original data.
     */
    private final UUID clusterId;

    /**
     * The fragment ID header.
     * This ID also contains an embedded message digest of the payload data.
     */
    private final UUID fragId;

    /**
     * The cluster data size header.
     * The total size of the original data.
     */
    private final int clusterDataSize;

    /**
     * The fragment piece header.
     * This header specifies what part of the original data (or parity) this fragments corresponds to.
     */
    private final byte piece;    // TODO Use an enum for this?

    /**
     * The actual fragment data.
     */
    private final ByteBuffer payload;


    /**
     * Constructor to be used when creating a fragment for the first time (during the fragmentation process).
     * @param piece What piece of the cluster (algorithm-wise) this is
     * @param payload Payload data for this particular fragment
     */
    public XORFragment(short version, UUID clusterId, UUID fragId, int clusterDataSize, byte piece, ByteBuffer payload) {
        this.version = version;
        this.clusterId = clusterId;
        this.fragId = fragId;
        this.clusterDataSize = clusterDataSize;
        this.piece = piece;
        this.payload = payload;
    }


    // TODO The "cluster parameters" (well, algorithm parameters to be exact) may be the same and may have to match, but they should all uniquely belong to each fragment and be matched during reconstruction!
    // This is because corrupted fragments may set an invalid standard for a cluster and thereby deny all other (valid) fragments that tries to mergeInto() the cluster!


    @Override
    public void writeFragment(ByteBuffer buffer) throws IOException {
        // TODO Stub. Go for nio-only or both io and nio?
    }

    /**
     * Write this fragment to a stream.
     * The stream must be initialized and open.
     * @param stream Destination stream
     * @throws IOException Upon a failed stream operation
     */
    @Override
    public void writeFragment(OutputStream stream) throws IOException {
        DataOutputStream dout = new DataOutputStream(stream);

        // Write version and algorithm header
        dout.writeShort(getVersion());

        // Write cluster UUID
        UUID clusterUuid = getClusterId();
        dout.writeLong(clusterUuid.getMostSignificantBits());
        dout.writeLong(clusterUuid.getLeastSignificantBits());

        // Write frag UUID
        dout.writeLong(getId().getMostSignificantBits());
        dout.writeLong(getId().getLeastSignificantBits());

        // Write cluster parameters
        dout.writeInt(getClusterDataSize());

        // Write fragment parameters
        dout.writeByte(getPiece());

        // Write payload size
        dout.writeInt(getPayload().array().length);

        // Write payload data
        dout.write(getPayload().array());
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
     * Get the type 3 UUID for this particular fragment.
     * @return net.comploud.code.bitfrag.Fragment UUID
     */
    @Override
    public UUID getId() { return fragId; }

    /**
     * Get the version header.
     * @return Version
     */
    public short getVersion() {
        return version;
    }

    /**
     * Get the cluster ID header.
     * @return net.comploud.code.bitfrag.Cluster ID
     */
    @Override
    public UUID getClusterId() {
        return clusterId;
    }

    /**
     * Get the cluster data size header.
     * @return net.comploud.code.bitfrag.Cluster data size
     */
    public int getClusterDataSize() {
        return clusterDataSize;
    }

    /**
     * Get the flag indicating what piece of the original data this fragment corresponds to.
     * This is essentially a parameter how to fit this fragment into the reconstruction algorithm.
     * @return net.comploud.code.bitfrag.Fragment piece
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
