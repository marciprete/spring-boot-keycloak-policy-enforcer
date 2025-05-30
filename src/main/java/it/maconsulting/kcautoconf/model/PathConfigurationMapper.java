package it.maconsulting.kcautoconf.model;

import org.keycloak.representations.adapters.config.PolicyEnforcerConfig;

import java.util.ArrayList;
import java.util.List;

public class PathConfigurationMapper {

    public PolicyEnforcerConfig.PathConfig toPathConfig(PathConfiguration configuration, boolean mapNames) {
        if (configuration == null) {
            return null;
        }

        PolicyEnforcerConfig.PathConfig pathConfig = new PolicyEnforcerConfig.PathConfig();
        pathConfig.setPath(configuration.getPath());
        if(mapNames) {
            pathConfig.setName(configuration.getName());
        }
        pathConfig.setId(configuration.getId());

        if (configuration.getEnforcementMode() != null) {
            pathConfig.setEnforcementMode(
                    PolicyEnforcerConfig.EnforcementMode.valueOf(configuration.getEnforcementMode().name())
            );
        }

        pathConfig.setMethods(
                configuration.getMethods().stream()
                        .map(this::toMethodConfig)
                        .toList()
        );

        return pathConfig;
    }

    private PolicyEnforcerConfig.MethodConfig toMethodConfig(MethodConfiguration configuration) {
        PolicyEnforcerConfig.MethodConfig methodConfig = new PolicyEnforcerConfig.MethodConfig();
        methodConfig.setMethod(configuration.getMethod());
        methodConfig.setScopes(new ArrayList<>(configuration.getScopes()));
        return methodConfig;
    }

    public List<PolicyEnforcerConfig.PathConfig> toPathConfigs(List<PathConfiguration> configurations, boolean mapNames) {
        if (configurations == null) {
            return new ArrayList<>();
        }
        return configurations.stream()
                .map( it -> toPathConfig(it, mapNames))
                .toList();
    }
}
