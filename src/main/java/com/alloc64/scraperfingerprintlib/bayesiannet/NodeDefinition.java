package com.alloc64.scraperfingerprintlib.bayesiannet;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class NodeDefinition {
    private String name;
    private List<String> parentNames;
    private List<String> possibleValues;
    private Map<String, Object> conditionalProbabilities;
}