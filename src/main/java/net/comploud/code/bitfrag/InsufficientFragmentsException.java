package net.comploud.code.bitfrag;

/**
 * Exception for signalling that there are insufficient fragments available to perform a
 * successful reconstruction operation.
 * Created by tek-ti on 2014-11-12.
 */
public class InsufficientFragmentsException extends Exception {
    /**
     * Amount of missing fragments during a failed reconstruction operation.
     */
    private int missing;


    /**
     * Constructor with necessary details.
     * @param missing Amount of missing fragments
     */
    public InsufficientFragmentsException(int missing) {
        super("" + missing);
        this.missing = missing;
    }


    /**
     * Returns amount of fragments missing in order to fulfill a successful reconstruction.
     * @return Amount of missing fragments
     */
    public int getAmountMissing() {
        return missing;
    }
}
