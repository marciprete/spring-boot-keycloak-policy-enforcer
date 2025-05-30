package it.maconsulting.kcautoconf;

import it.maconsulting.kcautoconf.model.EnforcementMode;
import it.maconsulting.kcautoconf.model.MethodConfiguration;
import it.maconsulting.kcautoconf.model.PathConfiguration;
import it.maconsulting.kcautoconf.pojo.AuthorizationSettingsDTO;
import it.maconsulting.kcautoconf.pojo.AuthorizedResourceDTO;
import it.maconsulting.kcautoconf.services.AutoconfigurationService;
import it.maconsulting.kcautoconf.services.JsonKeycloakConfigurationGenerator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.when;

/**
 * @author Michele Arciprete
 * @since 0.3.0-SNAPSHOT
 */
@ExtendWith(MockitoExtension.class)
class JsonKeycloakConfigurationGeneratorTest {

    @Mock
    private AutoconfigurationService autoconfigurationService;

    @InjectMocks
    private JsonKeycloakConfigurationGenerator sut;


    @Test
    void givenExistingPath_isNotAdded() {
        List<PathConfiguration> pathConfigurations = new ArrayList<>();
        PathConfiguration existingPath = new PathConfiguration();
        existingPath.setPath("/my/path");
        existingPath.setId("123456");
        pathConfigurations.add(existingPath);

        when(autoconfigurationService.getPathConfigurationsDom()).thenReturn(pathConfigurations);

        AuthorizationSettingsDTO settings = sut.generateConfigurationAsJson();
        Assertions.assertTrue(settings.getResources().isEmpty());
    }

    @Test
    void givenProperties_exportObjectIsCreated() {
        List<PathConfiguration> pathConfigurations = new ArrayList<>();
        PathConfiguration existingPath = new PathConfiguration();
        existingPath.setPath("/my/path");
        existingPath.setName("Add User");
        existingPath.setDisplayName("Add User Details");
        existingPath.setEnforcementMode(EnforcementMode.ENFORCING);
        MethodConfiguration myMethod = new MethodConfiguration();
        myMethod.setMethod("POST");
        myMethod.setScopes(List.of("user:add"));

        existingPath.getMethods().add(myMethod);
        pathConfigurations.add(existingPath);

        when(autoconfigurationService.getPathConfigurationsDom()).thenReturn(pathConfigurations);
        AuthorizationSettingsDTO settings = sut.generateConfigurationAsJson();

        Assertions.assertEquals("ENFORCING", settings.getPolicyEnforcementMode());
        Assertions.assertFalse(settings.isAllowRemoteResourceManagement());
        Assertions.assertEquals("AFFIRMATIVE", settings.getDecisionStrategy());
        Assertions.assertTrue(settings.getPolicies().isEmpty());
        List<AuthorizedResourceDTO> resources = settings.getResources();
        Assertions.assertFalse(resources.isEmpty());
        AuthorizedResourceDTO resource = resources.get(0);

        Assertions.assertEquals("Add User", resource.getName());
        Assertions.assertEquals("Add User Details", resource.getDisplayName());
        Assertions.assertTrue(resource.getUris().contains("/my/path"));
        Assertions.assertEquals("user:add", resource.getScopes().iterator().next().getName());

    }
}
