package uz.atmos.gaf.exception;

/**
 * Created by Temurbek Ismoilov on 10/03/24.
 */

public class GafException extends RuntimeException {
    public GafException(String errorMessage) {
        super(errorMessage);
    }
}
