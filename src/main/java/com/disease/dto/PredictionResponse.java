package com.disease.dto;

import java.util.Map;

public class PredictionResponse {
    private int prediction;
    private double probability;
    private Map<String, Double> importance;

    public PredictionResponse() {}

    public PredictionResponse(int prediction, double probability, Map<String, Double> importance) {
        this.prediction = prediction;
        this.probability = probability;
        this.importance = importance;
    }

    public int getPrediction() { return prediction; }
    public void setPrediction(int prediction) { this.prediction = prediction; }

    public double getProbability() { return probability; }
    public void setProbability(double probability) { this.probability = probability; }

    public Map<String, Double> getImportance() { return importance; }
    public void setImportance(Map<String, Double> importance) { this.importance = importance; }
}
