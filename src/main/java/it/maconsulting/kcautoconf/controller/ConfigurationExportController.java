package it.maconsulting.kcautoconf.controller;

import it.maconsulting.kcautoconf.pojo.AuthorizationSettingsDTO;
import it.maconsulting.kcautoconf.services.KeycloakConfigurationGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Michele Arciprete
 * @since 0.3.0
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("${kcautoconf.export-path:/mac/configuration/export}")
public class ConfigurationExportController {

    private final KeycloakConfigurationGeneratorService keycloakConfigurationGeneratorService;

    @GetMapping
    public ResponseEntity<AuthorizationSettingsDTO> configure() {
        return
                ResponseEntity.ok(
                keycloakConfigurationGeneratorService.generateConfigurationAsJson()
                );
    }
}
