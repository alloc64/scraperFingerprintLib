package com.alloc64.scraperfingerprintlib.bayesiannet;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class NetworkDefinition {
    private List<NodeDefinition> nodes;
}
