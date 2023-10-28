package com.alloc64.scraperfingerprintlib.bayesiannet;

import lombok.RequiredArgsConstructor;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class BayesianNode {
    private final NodeDefinition nodeDefinition;

    public NodeDefinition toJSON() {
        return this.nodeDefinition;
    }

    private Map<String, Double> getProbabilitiesGivenKnownValues(Map<String, String> parentValues) {
        Map<String, Object> probabilities = this.nodeDefinition.getConditionalProbabilities();

        for (var parentName : getParentNames()) {
            var parentValue = parentValues.get(parentName);

            var deeper = probabilities.get("deeper");

            if (deeper instanceof Map map) {
                var deeperValue = map.getOrDefault(parentValue, null);

                if (deeperValue != null)
                    probabilities = (Map<String, Object>) deeperValue;
            } else {
                probabilities = (Map<String, Object>) probabilities.get("skip");
            }
        }

        Map<String, Double> result = new LinkedHashMap<>();

        for (var entry : probabilities.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            Double doubleValue = null;

            if (value instanceof Double d) {
                doubleValue = d;
            } else if (value instanceof Float i) {
                doubleValue = (double) i;
            } else if (value instanceof Integer i) {
                doubleValue = (double) i;
            } else if (value instanceof String s) {
                doubleValue = Double.parseDouble(s);
            }

            assert doubleValue != null;
            result.put(key, doubleValue);
        }

        return result;
    }

    private String sampleRandomValueFromPossibilities(List<String> possibleValues,
                                                      double totalProbabilityOfPossibleValues,
                                                      Map<String, Double> probabilities) {
        var chosenValue = possibleValues.get(0);
        var anchor = Math.random() * totalProbabilityOfPossibleValues;
        double cumulativeProbability = 0;
        for (var possibleValue : possibleValues) {
            cumulativeProbability += probabilities.get(possibleValue);
            if (cumulativeProbability > anchor) {
                chosenValue = possibleValue;
                break;
            }
        }

        return chosenValue;
    }

    public String sample(Map<String, String> parentValues) {
        Map<String, Double> probabilities = this.getProbabilitiesGivenKnownValues(parentValues);
        List<String> possibleValues = new ArrayList<>(probabilities.keySet());
        return this.sampleRandomValueFromPossibilities(possibleValues, 1.0, probabilities);
    }

    public String sampleAccordingToRestrictions(Map<String, String> parentValues, List<String> valuePossibilities, List<String> bannedValues) {
        Map<String, Double> probabilities = this.getProbabilitiesGivenKnownValues(parentValues);
        double totalProbability = 0.0;
        List<String> validValues = new ArrayList<>();
        List<String> valuesInDistribution = new ArrayList<>(probabilities.keySet());
        List<String> possibleValues = (valuePossibilities != null) ? valuePossibilities : valuesInDistribution;

        for (String value : possibleValues) {
            if (!bannedValues.contains(value) && valuesInDistribution.contains(value)) {
                validValues.add(value);
                totalProbability += probabilities.get(value);
            }
        }

        if (validValues.isEmpty())
            return null;

        return this.sampleRandomValueFromPossibilities(validValues, totalProbability, probabilities);
    }

    public void setProbabilitiesAccordingToData(List<Map<String, String>> data, Map<String, List<String>> possibleParentValues) {
        this.nodeDefinition.setPossibleValues(new ArrayList<>(data.stream().map(x -> x.get(getName())).collect(Collectors.toSet())));
        this.nodeDefinition.setConditionalProbabilities(
                this.recursivelyCalculateConditionalProbabilitiesAccordingToData(
                        data,
                        possibleParentValues,
                        0
                )
        );
    }

    private Map<String, Object> recursivelyCalculateConditionalProbabilitiesAccordingToData(List<Map<String, String>> data, Map<String, List<String>> possibleParentValues, int depth) {
        Map<String, Object> probabilities = new HashMap<>();
        probabilities.put("deeper", new HashMap<String, Object>());

        if (depth < getParentNames().size()) {
            String currentParentName = getParentNames().get(depth);
            for (String possibleValue : possibleParentValues.get(currentParentName)) {
                boolean skip = data.stream().noneMatch(record -> record.get(currentParentName).equals(possibleValue));
                List<Map<String, String>> filteredData = data;
                if (!skip) {
                    filteredData = new ArrayList<>();
                    for (Map<String, String> record : data) {
                        if (record.get(currentParentName).equals(possibleValue)) {
                            filteredData.add(record);
                        }
                    }
                }
                Map<String, Object> nextLevel = recursivelyCalculateConditionalProbabilitiesAccordingToData(
                        filteredData,
                        possibleParentValues,
                        depth + 1
                );

                if (!skip) {
                    ((Map<String, Object>) probabilities.get("deeper")).put(possibleValue, nextLevel);
                } else {
                    probabilities.put("skip", nextLevel);
                }
            }
        } else {
            probabilities = getRelativeFrequencies(data, getName());
        }

        return probabilities;
    }

    public String getName() {
        return this.nodeDefinition.getName();
    }

    public List<String> getParentNames() {
        return this.nodeDefinition.getParentNames();
    }

    public List<String> getPossibleValues() {
        return this.nodeDefinition.getPossibleValues();
    }

    public NodeDefinition getNodeDefinition() {
        return this.nodeDefinition;
    }

    public static Map<String, Object> getRelativeFrequencies(List<Map<String, String>> data, String attributeName) {
        Map<String, Integer> frequencies = new HashMap<>();
        int totalCount = data.size();

        for (Map<String, String> record : data) {
            String value = record.get(attributeName);
            frequencies.put(value, frequencies.getOrDefault(value, 0) + 1);
        }

        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, Integer> entry : frequencies.entrySet()) {
            String key = entry.getKey();
            int value = entry.getValue();
            result.put(key, (double) value / totalCount);
        }

        return result;
    }
}
