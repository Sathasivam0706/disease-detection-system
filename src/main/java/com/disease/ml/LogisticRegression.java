package com.disease.ml;

import java.io.Serializable;
import java.util.*;

public class LogisticRegression implements Serializable {
    private static final long serialVersionUID = 1L;

    private double[] weights;
    private double bias;
    private double learningRate;
    private int epochs;

    public LogisticRegression(double learningRate, int epochs) {
        this.learningRate = learningRate;
        this.epochs = epochs;
    }

    public void train(double[][] X, int[] y) {
        int nSamples = X.length;
        int nFeatures = X[0].length;

        this.weights = new double[nFeatures];
        this.bias = 0.0;

        // Gradient Descent
        for (int epoch = 0; epoch < epochs; epoch++) {
            double[] dw = new double[nFeatures];
            double db = 0.0;

            for (int i = 0; i < nSamples; i++) {
                double z = bias;
                for (int j = 0; j < nFeatures; j++) {
                    z += X[i][j] * weights[j];
                }
                double yPred = sigmoid(z);
                double error = yPred - y[i];

                db += error;
                for (int j = 0; j < nFeatures; j++) {
                    dw[j] += error * X[i][j];
                }
            }

            // Update parameters
            bias -= (learningRate * db) / nSamples;
            for (int j = 0; j < nFeatures; j++) {
                weights[j] -= (learningRate * dw[j]) / nSamples;
            }
        }
    }

    public double predictProbability(double[] sample) {
        double z = bias;
        for (int j = 0; j < sample.length; j++) {
            z += sample[j] * weights[j];
        }
        return sigmoid(z);
    }

    public int predict(double[] sample) {
        return predictProbability(sample) >= 0.5 ? 1 : 0;
    }

    private double sigmoid(double z) {
        return 1.0 / (1.0 + Math.exp(-Math.max(-20.0, Math.min(20.0, z)))); // Clamped to avoid overflow
    }

    /**
     * Local feature importance for Logistic Regression.
     * The linear contribution of feature j is: sample[j] * weights[j].
     * (Translates direct log-odds shifts to feature impact).
     */
    public Map<String, Double> explainPrediction(double[] sample, List<String> featureNames) {
        Map<String, Double> importance = new LinkedHashMap<>();
        for (int j = 0; j < sample.length; j++) {
            double contribution = sample[j] * weights[j];
            importance.put(featureNames.get(j), contribution);
        }
        return importance;
    }
}
