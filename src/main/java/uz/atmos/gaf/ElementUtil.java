package uz.atmos.gaf;

import java.util.stream.Stream;

/**
 * Created by Temurbek Ismoilov on 13/03/24.
 */

public class ElementUtil {

    public static boolean isCollection(String fieldType) {
        return Stream.of("List", "ArrayList", "Set", "HashSet", "Collection").anyMatch(fieldType::contains);
    }
}
