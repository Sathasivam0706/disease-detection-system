package com.disease.ml;

import java.io.Serializable;
import java.util.*;

public class EnsembleModel implements Serializable {
    private static final long serialVersionUID = 1L;

    private RandomForest rf;
    private LogisticRegression lr;

    public EnsembleModel(RandomForest rf, LogisticRegression lr) {
        this.rf = rf;
        this.lr = lr;
    }

    public void train(double[][] X, int[] y, long seed) {
        // Train Random Forest
        rf.train(X, y, seed);
        
        // Train Logistic Regression
        lr.train(X, y);
    }

    public double predictProbability(double[] sample) {
        double probRf = rf.predictProbability(sample);
        double probLr = lr.predictProbability(sample);
        return (probRf + probLr) / 2.0;
    }

    public int predict(double[] sample) {
        return predictProbability(sample) >= 0.5 ? 1 : 0;
    }

    /**
     * Extracts explanations using the Random Forest Tree Interpreter,
     * mirroring the python SHAP explanation behavior (which isolates the RF estimator).
     */
    public Map<String, Double> explainPrediction(double[] sample, List<String> featureNames) {
        return rf.explainPrediction(sample, featureNames);
    }

    public RandomForest getRf() {
        return rf;
    }

    public LogisticRegression getLr() {
        return lr;
    }
}
