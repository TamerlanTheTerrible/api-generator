<#assign className = element.simpleName.toString()>
<#assign packageName = element.enclosingElement.toString()>
<#assign apiName = className.remove_suffix("Service") + "FeignClient">
<#assign builderFullName = packageName + "." + apiName>

package ${packageName};

import feign.Headers;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

<#-- Add necessary imports -->
${generateImports()}

@FeignClient(value = "${className.toLowerCase()}-feign-client", url = "${gafClientAnnotation.url()}")
public interface ${apiName} extends ${className} {
<#list methods as method>
    ${method}
</#list>
}
