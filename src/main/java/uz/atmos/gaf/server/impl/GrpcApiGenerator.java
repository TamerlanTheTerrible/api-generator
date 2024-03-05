package uz.atmos.gaf.server.impl;

import uz.atmos.gaf.server.ApiGenerator;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;

/**
 * Created by Temurbek Ismoilov on 05/03/24.
 */

public class GrpcApiGenerator implements ApiGenerator {
    @Override
    public void generate(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv, ProcessingEnvironment processingEnv) {
        annotations.forEach(annotation -> roundEnv.getElementsAnnotatedWith(annotation)
                .forEach(element -> generate(element, processingEnv))
        );
    }

    private void generate(Element element, ProcessingEnvironment processingEnv) {
        try {
            // Get the Filer from the ProcessingEnvironment
            Filer filer = processingEnv.getFiler();

            // Create a new resource file (here, we'll create a properties file)
            FileObject resourceFile = filer.createResource(
                    StandardLocation.CLASS_OUTPUT,
                    element.getEnclosingElement().toString(), // No package for this example
                    "myproto.proto", // Name of the resource file
                    element // Associated elements
            );

            // Write content to the resource file
            try (PrintWriter writer = new PrintWriter(resourceFile.openWriter())) {
                writer.print("""
                        syntax = "proto3";
                        package com.proto;
                        option java_multiple_files = true;

                        message FromApiProcessor {
                          string hello =3;
                        }
                        """);
            }
        } catch (IOException e) {
            e.printStackTrace(); // Handle or log the exception as needed
        }
    }
}
