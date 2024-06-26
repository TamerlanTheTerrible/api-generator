package ${packageName};

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;
import uz.atmos.gaf.${className};
${imports}
@Configuration
public class GrpcServerConfig {

    @Bean
    public Server startGRPC(){
        System.out.println("gRPC server start attempt");

        final int port = 9999;
        Server server = ServerBuilder
                .forPort(port)
                .addService(new ${implementationClassName}())
                .build();

        try {
            server.start();
        } catch (IOException e) {
            System.err.println("gRPC server started on port " + port);
            throw new RuntimeException(e);
        }

        System.out.println("gRPC server started on port " + port);
        return server;
    }

    static class ${implementationClassName} extends ${className}.${baseClassName}{
<#--        <#list methods as method>-->
<#--            <#assign methodName = method.methodName>-->
<#--            <#assign returnType = method.returnType>-->
<#--            <#assign paramTypesAndNames = method.paramTypesAndNames>-->
<#--            <#assign paramNames = method.paramNames>-->

<#--            ${returnType} ${methodName}(${paramTypesAndNames}) {-->
<#--            return super.${methodName}(${paramNames});-->
<#--        }-->
<#--        </#list>-->
    }
}
