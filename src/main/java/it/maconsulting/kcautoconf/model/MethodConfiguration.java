package it.maconsulting.kcautoconf.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MethodConfiguration {
    private String method;
    private List<String> scopes = new ArrayList<>();
}