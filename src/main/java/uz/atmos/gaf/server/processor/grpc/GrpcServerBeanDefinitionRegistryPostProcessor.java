package uz.atmos.gaf.server.processor.grpc;

/**
 * Created by Temurbek Ismoilov on 20/06/24.
 */

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.reflections.Reflections;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import uz.atmos.gaf.server.EnableGrpcServer;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

@Configuration
public class GrpcServerBeanDefinitionRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor {
    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        // Create a scanner to find classes annotated with @EnableGrpcServer
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(EnableGrpcServer.class));

        // Scan the classpath for annotated classes
        Set<BeanDefinition> beanDefinitions = scanner.findCandidateComponents("target/classes/");

        // If any annotated class is found, register the gRPC Server bean
        if (!beanDefinitions.isEmpty()) {
            BeanDefinition beanDefinition = BeanDefinitionBuilder
                    .genericBeanDefinition(Server.class, this::startGRPC)
                    .getBeanDefinition();
            registry.registerBeanDefinition("grpcServer", beanDefinition);
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // No-op
    }

    private Server startGRPC(){
        System.out.println("gRPC server start attempt");
        final int port = 9999;
        try {
            ServerBuilder<?> serverBuilder = ServerBuilder.forPort(port);
            // Discover all classes implementing BindableService
            Set<Class<? extends BindableService>> serviceClasses = discoverServices("");
            // Instantiate and register each service found
            for (Class<? extends BindableService> serviceClass : serviceClasses) {
                if (!Modifier.isAbstract(serviceClass.getModifiers())) {
                    BindableService serviceInstance = serviceClass.getDeclaredConstructor().newInstance();
                    serverBuilder.addService(serviceInstance);
                    System.out.println("Added gRPC service: " + serviceClass.getName());
                }
            }
            // Build and start the server
            Server server = serverBuilder.build();
            server.start();
            System.out.println("gRPC server started on port " + port);
            return server;
        } catch (Exception e) {
            System.err.println("gRPC server start error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static Set<Class<? extends BindableService>> discoverServices(String basePackage) {
        Reflections reflections = new Reflections(basePackage);
        Set<Class<? extends BindableService>> serviceClasses = reflections.getSubTypesOf(BindableService.class);
        return new HashSet<>(serviceClasses);
    }
}
