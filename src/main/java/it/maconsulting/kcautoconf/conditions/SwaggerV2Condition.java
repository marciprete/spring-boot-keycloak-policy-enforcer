package it.maconsulting.kcautoconf.conditions;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * @author Michele Arciprete
 * @since 0.4.0
 */
public class SwaggerV2Condition implements Condition {

    /**
     * Checks if the Swagger v2 library is present in the classpath.
     * @param conditionContext the condition context
     * @param annotatedTypeMetadata the annotated type metadata
     * @return true if the library is present, false otherwise
     */
    @Override
    public boolean matches(ConditionContext conditionContext, AnnotatedTypeMetadata annotatedTypeMetadata) {
        try {
            Class.forName("io.swagger.annotations.ApiOperation", false, this.getClass().getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

}
