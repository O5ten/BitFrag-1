package net.comploud.code.bitfrag;

import java.io.IOException;

/**
 * Exception specific to parsing a fragment.
 * Created by tek-ti on 2014-09-08.
 */
public class FragmentFormatException extends IOException {
    public FragmentFormatException(String message) {
        super(message);
    }

}
