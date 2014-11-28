package net.comploud.code.bitfrag;

/**
 * To be thrown whenever something is wrong with a specified cluster.
 * Created by tek-ti on 2014-09-29.
 */
public class InvalidClusterException extends Exception {
    public InvalidClusterException(String message) {
        super(message);
    }
}
