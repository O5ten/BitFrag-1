package net.comploud.code.bitfrag;

import java.util.Set;

/**
 * Data structure object containing details about a reconstruction operation.
 * Created by tek-ti on 2014-11-11.
 */
public class ReconstructionReport {
    /**
     * Flags how many fragments were missing (and thus reanimated) during reconstruction.
     */
    private final int missing;

    /**
     * The corrupted (and thus corrected) fragments during reconstruction.
     * A corrupted fragment is a fragment that has an invalid fragment digest (UUID) in
     * respect to its payload data. This may be an indication of a transmission error.
     */
    private final Set<Fragment> corrupted;

    /**
     * The tampered (and thus corrected) fragments during reconstruction.
     * A tampered fragment is a fragment that has a valid fragment digest (UUID) but fails
     * the consensus check during reconstruction. This can only be detected with a
     * excess of fragments during reconstruction (such as a full set).
     */
    private final Set<Fragment> tampered;


    /**
     * Create a fresh reconstruction report.
     * The caster may choose any Set implementation but CopyOnWriteArraySet is usually suitable.
     * If corrupted or tampered fragments are unknown/unavailable, it is encouraged to pass
     * empty sets rather than null.
     * @param missing Amount of missing fragments
     * @param corrupted Corrupted fragments
     * @param tampered Tampered fragments
     */
    public ReconstructionReport(int missing, Set<Fragment> corrupted, Set<Fragment> tampered) {
        this.missing = missing;
        this.corrupted = corrupted;
        this.tampered = tampered;
    }


    /**
     * Checks if reconstruction succeeded flawlessly.
     * If there were any complications (missing, corrupted and/or tempered fragments) this method will briefly hint such
     * with a boolean.
     * @returns true if there were no complications at all, or false otherwise
     */
    public boolean flawless() {
        if(missing == 0 && corrupted.isEmpty() && tampered.isEmpty()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * The amount of fragments that were missing.
     * @return The amount of fragments missing
     */
    public int getMissing() {
        return missing;
    }

    /**
     * Returns the set of fragments that was corrupted during reconstruction.
     * @return Corrupted fragments or an empty set if none
     */
    public Set<Fragment> getCorrupted() {
        return corrupted;
    }

    /**
     * Returns the set of fragments that was deemed tampered with during reconstruction.
     * @return Tampered fragments or an empty set if none
     */
    public Set<Fragment> getTampered() {
        return tampered;
    }
}
