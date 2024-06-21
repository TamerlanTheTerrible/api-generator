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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import uz.atmos.gaf.server.EnableGrpcServer;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

@Configuration
public class GrpcServerConfiguration {

    @Bean
    public Server startGRPC(){
        System.out.println("gRPC server start attempt");
        final int port = 9999;
        try {
            ServerBuilder<?> serverBuilder = ServerBuilder.forPort(port);
            // Discover all classes implementing BindableService
            Set<Class<? extends BindableService>> serviceClasses = discoverServices("/target");
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