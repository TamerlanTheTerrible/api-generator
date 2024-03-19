<#assign packageName = packageName>
<#assign imports = imports>
<#assign className = className>
<#assign apiName = apiName>
<#assign serviceClassName = serviceClassName>
package ${packageName};

import feign.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

${imports}

@FeignClient(value = "${className?lower_case}-feign-client", url = "${baseUrl}")
public interface ${apiName} extends ${serviceClassName} {
<#list methods as method>
    <#assign requestMethod = method.requestMethod>
    <#assign urlValue = method.urlValue>
    <#assign returnType = method.returnType>
    <#assign methodName = method.methodName>
    <#assign parameters = method.parameters>

    @Override
    @${requestMethod}Mapping("${urlValue}")
    ${returnType} ${methodName}(${parameters});
</#list>
}
