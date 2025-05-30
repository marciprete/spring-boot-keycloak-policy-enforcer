package it.maconsulting.kcautoconf.model;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class PathConfiguration {
    private String path;
    private String name;
    private String displayName;
    private EnforcementMode enforcementMode;
    private String id;
    private List<MethodConfiguration> methods = new ArrayList<>();
}



