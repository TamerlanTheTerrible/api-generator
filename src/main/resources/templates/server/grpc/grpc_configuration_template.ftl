import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;
import uz.atmos.gaf.GafBillingConfigServiceGrpc;
<#--package ${packageName};-->

<#--${imports}-->

@Slf4j
@Configuration
public class GrpcServerConfiguration {

    @Bean
    public Server startGRPC(){
        System.out.println("gRPC server start attempt");

        final int port = 9999;
        Server server = ServerBuilder
                .forPort(port)
                .addService(new ${className}())
                .build();

        try {
            server.start();
        } catch (IOException e) {
            log.error("gRPC server start error: " + e.getMessage());
            throw new RuntimeException(e);
        }

        System.out.println("gRPC server started on port " + port);
        return server;
    }

    static class ${className} extends GafBillingConfigServiceGrpc.GafBillingConfigServiceImplBase {
<#--        <#assign methodName = method.methodName>-->
<#--        <#assign returnType = method.returnType>-->
<#--        <#assign serviceParams = method.serviceParams>-->

<#--        ${returnType} ${methodName}(${serviceParams}) {-->
<#--            return super.${methodName}(${serviceParams});-->
<#--        }-->
    }
}
