/*
 * Copyright 2020-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package it.maconsulting.kcautoconf;

import it.maconsulting.kcautoconf.services.AutoconfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "kcautoconf")
public class KeycloakSettingsControllerConfiguration {

    /**
     * Enables the autoconfiguration of the Resource Server by scanning the @{@link org.springframework.web.bind.annotation.RestController}
     * @param autoconfigurationService the autoconfiguration service
     */
    @Autowired
    public KeycloakSettingsControllerConfiguration(AutoconfigurationService autoconfigurationService) {
        autoconfigurationService.enableConfigurationPage();
    }

}
