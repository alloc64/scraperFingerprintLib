package com.alloc64.scraperfingerprintlib.bayesiannet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public class BayesianNetwork {
    private final List<BayesianNode> nodesInSamplingOrder;
    private final Map<String, BayesianNode> nodesByName;
    private final ObjectMapper mapper = new ObjectMapper();

    public BayesianNetwork(InputStream is) throws IOException {
        NetworkDefinition networkDefinition = mapper.readValue(is, NetworkDefinition.class);
        this.nodesInSamplingOrder = new ArrayList<>();

        for (NodeDefinition nodeDefinition : networkDefinition.getNodes())
            nodesInSamplingOrder.add(new BayesianNode(nodeDefinition));

        this.nodesByName = nodesInSamplingOrder.stream()
                .collect(Collectors.toMap(BayesianNode::getName, node -> node));
    }

    public Map<String, String> generateSample(Map<String, String> inputValues) {
        Map<String, String> sample = new HashMap<>(inputValues);
        for (BayesianNode node : nodesInSamplingOrder)
            if (!sample.containsKey(node.getName()))
                sample.put(node.getName(), node.sample(sample));

        return sample;
    }

    public Map<String, String> generateConsistentSampleWhenPossible(Map<String, List<String>> valuePossibilities) {
        return recursivelyGenerateConsistentSampleWhenPossible(new HashMap<>(), valuePossibilities, 0);
    }

    private Map<String, String> recursivelyGenerateConsistentSampleWhenPossible(Map<String, String> sampleSoFar,
                                                                                Map<String, List<String>> valuePossibilities,
                                                                                int depth) {
        List<String> bannedValues = new ArrayList<>();
        BayesianNode node = nodesInSamplingOrder.get(depth);
        String sampleValue;

        do {
            sampleValue = node.sampleAccordingToRestrictions(sampleSoFar, valuePossibilities.get(node.getName()), bannedValues);
            if (sampleValue == null)
                break;

            sampleSoFar.put(node.getName(), sampleValue);

            if (depth + 1 < nodesInSamplingOrder.size()) {
                Map<String, String> sample = recursivelyGenerateConsistentSampleWhenPossible(sampleSoFar, valuePossibilities, depth + 1);
                if (!sample.isEmpty())
                    return sample;
            } else {
                return sampleSoFar;
            }

            bannedValues.add(sampleValue);
        } while (true);

        return new HashMap<>();
    }

    public void setProbabilitiesAccordingToData(List<Map<String, String>> data) {
        for (BayesianNode node : nodesInSamplingOrder) {
            Map<String, List<String>> possibleParentValues = new HashMap<>();
            for (String parentName : node.getParentNames()) {
                possibleParentValues.put(parentName, nodesByName.get(parentName).getPossibleValues());
            }
            node.setProbabilitiesAccordingToData(data, possibleParentValues);
        }
    }

    public void saveNetworkDefinition(File path) throws IOException {
        NetworkDefinition network = new NetworkDefinition()
                .setNodes(nodesInSamplingOrder.stream()
                        .map(BayesianNode::getNodeDefinition)
                        .collect(Collectors.toList())
                );

        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(path, network);
    }
}