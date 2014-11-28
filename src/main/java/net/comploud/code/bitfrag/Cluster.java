package net.comploud.code.bitfrag;

import java.util.HashSet;
import java.util.UUID;

/**
 * A cluster is a complete set of fragments for a block of "original" data.
 * This class is used to represent a set - but not necessarily the complete set - of fragments.
 * A set of fragments may or may not be complete, meaning it may or may not be sufficient to reconstruct the original data.
 * Created by tek-ti on 2014-09-09.
 */
public class Cluster<T extends Fragment> extends HashSet<T> {
    /**
     * UUID (type 3) including a digest of the fully assembled data.
     * This data digest can be used to verify the complete cluster data upon an attempt to reconstruct the clusters' data.
     */
    private final UUID uuid;    // This may also be a potential security issue as the any verification aid may help an exhaustive search.

    // TODO Add reference to net.comploud.code.bitfrag.Algorithm?

    /**
     * Create a new cluster.
     */
    public Cluster(UUID id) {
        this.uuid = id;
    }

    /**
     * Matches this cluster to another.
     * For two clusters to be considered the same, all cluster parameters and the UUID must match.
     * @param other Another cluster instance to compare to
     * @return true if all essential properties of the instances match (and hence are the same cluster), false otherwise
     */
    /*@Override
    public boolean equals(Object other) {
        if(other == null || !(other instanceof net.comploud.code.bitfrag.Cluster)) {  // Yep, instanceof. What can you do about it!? :/
            return false;
        } else {
            net.comploud.code.bitfrag.Cluster otherCluster = (net.comploud.code.bitfrag.Cluster)other;
            return getClusterSize() == otherCluster.getClusterSize() &&           // Is this big chunk of lines good-looking code?
                    getOverhead() == otherCluster.getOverhead() &&
                    getFormatVersion() == otherCluster.getFormatVersion() &&
                    getId().equals(otherCluster.getId());
        }   // Wow, this method looks like hell, haha!
    }*/

    /*@Override
    public int hashCode() {
        // Quite sloppy way really. Certainly not uniform in practice but should work for now.
        // TODO Do a proper hash (may boost performance in large data sets)
        return (clusterSize != 0 ? clusterSize : 0) *
                (clusterOverhead != 0 ? clusterOverhead : 0) *
                (formatVersion != 0 ? formatVersion : 0) *
                (int) getId().getMostSignificantBits() *
                (int) getId().getLeastSignificantBits();
    }*/
    // TODO Can these methods be deleted or migrated? Or adapted!

    /**
     * Get the type 3 UUID for the complete cluster data.
     * @return net.comploud.code.bitfrag.Cluster UUID
     */
    public UUID getId() { return uuid; }

}
