package net.comploud.code.bitfrag;


import java.util.*;

/**
 * Maps fragments into clusters.
 * This is a helper class that sorts fragments into their corresponding clusters.
 * Created by tek-ti on 2014-11-18.
 */
public class FragmentMapper {
    /**
     * The known - or observed - clusters.
     * This collection will be updated as new fragments are mapped with the map() method.
     */
    protected Map<UUID, Cluster> knownClusters = new TreeMap<UUID, Cluster>();

    /**
     * Default constructor.
     * No clusters will be initially known.
     */
    public FragmentMapper() {
    }


    /**
     * Constructor with an initial set of known clusters.
     * @param knownClusters Initially known clusters. Note that this set will not be modified!
     */
    public FragmentMapper(Set<Cluster> knownClusters) {
        // Since there's no clever method in the Java Collections API to add a set to a map, do this explicitly
        for(Cluster clust : knownClusters) {
            this.knownClusters.put(clust.getId(), clust);   // Extract the ID and use it as key
        }
    }


    /**
     * Maps a fragment into its cluster.
     * If the fragments specified cluster is known, it will be added to that cluster. If not, a new cluster data
     * structure will be created prior to the fragment being added.
     * @param frag Fragment to map
     * @return true if a new cluster was discovered, false if the cluster was already known
     */ // TODO This method contains unchecked generics...
    public synchronized boolean map(Fragment frag) {
        Cluster clust = knownClusters.get(frag.getClusterId());
        if(clust == null) {
            // This is a newly discovered cluster
            UUID clustId = frag.getClusterId();
            Cluster newClust = new Cluster(clustId);
            newClust.add(frag);
            knownClusters.put(clustId, newClust);
            return true;
        } else {
            // The cluster for this fragment is known
            clust.add(frag);
            return false;
        }
    }

    /**
     * Returns the currently known clusters.
     * @return Known clusters
     */
    public Collection<Cluster> getKnownClusters() {
        return knownClusters.values();
    }
}
