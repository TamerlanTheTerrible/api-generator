package uz.atmos.gaf.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.protobuf.InvalidProtocolBufferException;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.Set;

/**
 * Created by Temurbek Ismoilov on 05/03/24.
 */

public interface ApiGenerator {
    void generate(Element element, ProcessingEnvironment processingEnv) throws JsonProcessingException, InvalidProtocolBufferException;
}
