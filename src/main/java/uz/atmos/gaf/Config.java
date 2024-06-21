//package uz.atmos.gaf;
//
//import io.grpc.BindableService;
//import io.grpc.Server;
//import io.grpc.ServerBuilder;
//import lombok.extern.slf4j.Slf4j;
//import org.reflections.Reflections;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import java.io.IOException;
//import java.lang.reflect.InvocationTargetException;
//import java.lang.reflect.Modifier;
//import java.util.HashSet;
//import java.util.Set;
//
///**
// * Created by Temurbek Ismoilov on 10/06/24.
// */
//
//@Slf4j
//@Configuration
//public class Config {
//
//    @Bean
//    public Server startGRPC() throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
//        System.out.println("gRPC server start attempt");
//
//        final int port = 9999;
//        ServerBuilder<?> serverBuilder = ServerBuilder.forPort(port);
//
//        // Discover all classes implementing BindableService
//        Set<Class<? extends BindableService>> serviceClasses = discoverServices("");
//
//        // Instantiate and register each service found
//        for (Class<? extends BindableService> serviceClass : serviceClasses) {
//            if (!Modifier.isAbstract(serviceClass.getModifiers())) {
//                BindableService serviceInstance = serviceClass.getDeclaredConstructor().newInstance();
//                serverBuilder.addService(serviceInstance);
//                System.out.println("Added gRPC service: " + serviceClass.getName());
//            }
//        }
//
//        // Build and start the server
//        Server server = serverBuilder.build();
//
//        try {
//            server.start();
//        } catch (IOException e) {
//            log.error("gRPC server start error: " + e.getMessage());
//            throw new RuntimeException(e);
//        }
//
//        System.out.println("gRPC server started on port " + port);
//        return server;
//    }
//
//    public static Set<Class<? extends BindableService>> discoverServices(String basePackage) {
//        Reflections reflections = new Reflections(basePackage);
//        Set<Class<? extends BindableService>> serviceClasses = reflections.getSubTypesOf(BindableService.class);
//        return new HashSet<>(serviceClasses);
//    }
//
//}
