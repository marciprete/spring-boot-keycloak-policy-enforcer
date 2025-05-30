package it.maconsulting.kcautoconf.services;

import it.maconsulting.kcautoconf.model.MethodConfiguration;
import it.maconsulting.kcautoconf.model.PathConfiguration;
import it.maconsulting.kcautoconf.model.PathConfigurationMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Predicate;


@Slf4j
@Service
@RequiredArgsConstructor
public class AutoconfigurationService {

    @Getter
    private final ApplicationContext context;

    private final PolicyEnforcerConfig policyEnforcerConfig;

    private final PathConfigurationMapper pathConfigurationMapper = new PathConfigurationMapper();

    @Getter
    private final List<SwaggerOperationService> swaggerOperationServices;

    @Value("${kcautoconf.export-path:/mac/configuration/export}")
    private String exportPath;

    @Value("${kcautoconf.protect-export-path:true}")
    private boolean protectExportPath;

    @Value("${kcautoconf.map-names:false}")
    private boolean mapNames;

    @Value("${kcautoconf.export-path-access-scope:configuration:export}")
    private String exportPathAccessScope;

    public void updateKeycloakConfiguration() {
        log.info("Automatic resources and scopes configuration process started.");
        List<PolicyEnforcerConfig.PathConfig> pathConfigurations = pathConfigurationMapper.toPathConfigs(getPathConfigurationsDom(), mapNames);
        policyEnforcerConfig.getPaths().addAll(pathConfigurations);
    }

    public List<PathConfiguration> getPathConfigurationsDom() {
        log.info("Automatic resources and scopes configuration process started.");
        Map<String, PathConfiguration> pathConfigMap = new HashMap<>();

        Map<String, Object> controllers = context.getBeansWithAnnotation(RestController.class);
        for (Map.Entry<String, Object> entry : controllers.entrySet()) {
            processController(entry.getKey(), entry.getValue(), pathConfigMap);
        }

        return new ArrayList<>(pathConfigMap.values());
    }

    private void processController(String beanName, Object bean, Map<String, PathConfiguration> pathConfigMap) {
        Class<?> targetClass = AopUtils.getTargetClass(bean);
        RequestMapping classMapping = AnnotationUtils.getAnnotation(targetClass, RequestMapping.class);
        List<String> classPaths = getClassLevelAnnotatedPaths(classMapping);

        log.debug("Parsing controller {}", beanName);

        for (Method method : targetClass.getDeclaredMethods()) {
            RequestMapping methodMapping = AnnotationUtils.getAnnotation(method, RequestMapping.class);
            if (methodMapping == null) continue;

            processMethod(method, classPaths, methodMapping, pathConfigMap);
        }
    }

    private void processMethod(Method method, List<String> classPaths, RequestMapping methodMapping, Map<String, PathConfiguration> pathConfigMap) {
        log.trace("Found method: {}", method);

        List<String> methodPaths = extractExtraPathsFromClassMethod(method);
        RequestMethod[] httpMethods = methodMapping.method();

        for (String basePath : classPaths) {
            for (String methodPath : methodPaths) {
                String fullPath = buildHttpPath(basePath, methodPath);
                for (RequestMethod httpMethod : httpMethods) {
                    log.debug("Configuring {} request for path: {}", httpMethod, fullPath);

                    PathConfiguration pathConfig = pathConfigMap.computeIfAbsent(fullPath, k -> {
                        PathConfiguration pc = new PathConfiguration();
                        pc.setPath(fullPath);
                        return pc;
                    });

                    MethodConfiguration methodConfig = buildMethodConfiguration(method, httpMethod);
                    pathConfig.getMethods().add(methodConfig);

                    populatePathMetadata(pathConfig, method);
                }
            }
        }
    }

    private MethodConfiguration buildMethodConfiguration(Method method, RequestMethod httpMethod) {
        MethodConfiguration methodConfig = new MethodConfiguration();
        methodConfig.setMethod(httpMethod.name());

        swaggerOperationServices.stream().findFirst().ifPresent(swagger -> {
            List<String> scopes = swagger.getScopes(method).stream()
                    .filter(Predicate.not(String::isBlank))
                    .toList();

            if (!scopes.isEmpty()) {
                scopes.forEach(scope -> log.debug("Found authorization scope: {}", scope));
                methodConfig.setScopes(scopes);
            }
        });
        return methodConfig;
    }

    private void populatePathMetadata(PathConfiguration pathConfig, Method method) {
        swaggerOperationServices.stream().findFirst().ifPresent(swagger -> {
            pathConfig.setName(swagger.getName(method));
            pathConfig.setDisplayName(swagger.getDisplayName(method));
        });
    }

    private List<String> getClassLevelAnnotatedPaths(RequestMapping requestMappingAnnotation) {
        List<String> paths = new ArrayList<>();
        paths.add("");
        if (requestMappingAnnotation != null &&
                requestMappingAnnotation.path().length > 0) {
            paths = Arrays.asList(requestMappingAnnotation.path());
        }
        return paths;
    }

    private String buildHttpPath(String... paths) {
        StringBuilder sb = new StringBuilder();
        for (String path : paths) {
            if (!path.isEmpty()) {
                sb.append(addLeadingSlash(path));
            }
        }
        String path = addLeadingSlash(sb.toString());
        return (path.length() > 1 && path.endsWith("/")) ? path.substring(0, path.lastIndexOf("/")) : path;
    }

    private List<String> extractExtraPathsFromClassMethod(Method method) {
        List<String> extraPaths = List.of("");
        RequestMapping merged = AnnotatedElementUtils.getMergedAnnotation(method, RequestMapping.class);
        if (merged != null && merged.path().length > 0) {
            extraPaths = Arrays.asList(merged.path());
        }
        return extraPaths;
    }

    private String addLeadingSlash(String path) {
        return !path.startsWith("/") ? "/" + path : path;
    }

    public void enableConfigurationPage() {
        PolicyEnforcerConfig.PathConfig configurationPath = new PolicyEnforcerConfig.PathConfig();
        configurationPath.setPath(exportPath + "*");
        if (protectExportPath) {
            log.debug("ENFORCING protection over export path");
            configurationPath.setEnforcementMode(PolicyEnforcerConfig.EnforcementMode.ENFORCING);
            configurationPath.setScopes(List.of(exportPathAccessScope));
        } else {
            configurationPath.setEnforcementMode(PolicyEnforcerConfig.EnforcementMode.DISABLED);
        }
        policyEnforcerConfig.getPaths().add(configurationPath);
        log.info("Configuration page enabled and available @ {}", exportPath);

    }
}
