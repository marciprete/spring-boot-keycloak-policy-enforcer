package it.maconsulting.kcautoconf.services;

import java.lang.reflect.Method;
import java.util.List;

public interface SwaggerOperationService {

    /**
     * Gets the list of scopes associated with a method
     * @param method the method to get scopes for
     * @return the list of scopes
     */
    List<String> getScopes(Method method);

    /**
     * Gets the name of the method
     * @param method the method to get the name for
     * @return the name of the method
     */
    String getName(Method method);

    /**
     * Gets the display name of the method
     * @param method the method to get the display name for
     * @return the display name of the method
     */
    String getDisplayName(Method method);
}
