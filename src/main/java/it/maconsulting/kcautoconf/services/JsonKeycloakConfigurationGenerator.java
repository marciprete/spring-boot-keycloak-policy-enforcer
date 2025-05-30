package it.maconsulting.kcautoconf.services;

import it.maconsulting.kcautoconf.model.EnforcementMode;
import it.maconsulting.kcautoconf.model.PathConfiguration;
import it.maconsulting.kcautoconf.pojo.AuthorizationScopeDTO;
import it.maconsulting.kcautoconf.pojo.AuthorizationSettingsDTO;
import it.maconsulting.kcautoconf.pojo.AuthorizedResourceDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JsonKeycloakConfigurationGenerator implements KeycloakConfigurationGeneratorService {

    private final AutoconfigurationService autoconfigurationService;

    @Override
    public AuthorizationSettingsDTO generateConfigurationAsJson() {
        AuthorizationSettingsDTO settings = new AuthorizationSettingsDTO();
        //DEFAULTS
        settings.setDecisionStrategy("AFFIRMATIVE");
        settings.setPolicyEnforcementMode("ENFORCING");

        List<PathConfiguration> paths = autoconfigurationService.getPathConfigurationsDom();
        List<AuthorizedResourceDTO> resourceDTOS = new ArrayList<>();
        paths.forEach(pathConfig -> {
            log.trace("Processing path: {}", pathConfig.getPath());
            if(!EnforcementMode.DISABLED.equals(pathConfig.getEnforcementMode()) &&
                    //skip existing pathconfigs
                    (pathConfig.getId() == null)) {
                AuthorizedResourceDTO resourceDTO = new AuthorizedResourceDTO();
                resourceDTO.setName(pathConfig.getName());
                resourceDTO.setDisplayName(pathConfig.getDisplayName());
                resourceDTO.getUris().add(pathConfig.getPath());
                Set<String> scopes = pathConfig.getMethods().stream().flatMap(methodConfig -> methodConfig.getScopes().stream()).collect(Collectors.toSet());
                scopes.forEach(scope -> {
                    if(!scope.isEmpty()) {
                        AuthorizationScopeDTO scopeDTO = new AuthorizationScopeDTO();
                        scopeDTO.setName(scope);
                        settings.getScopes().add(scopeDTO);
                        resourceDTO.getScopes().add(scopeDTO);
                    }
                });
                resourceDTOS.add(resourceDTO);
            }
        });

        settings.setResources(resourceDTOS);
        return settings;
    }
}
