package com.disease.ml;

import java.io.Serializable;
import java.util.*;

public class RandomForest implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<DecisionTree> trees;
    private int numTrees;
    private int maxDepth;
    private int minSamplesSplit;

    public RandomForest(int numTrees, int maxDepth, int minSamplesSplit) {
        this.numTrees = numTrees;
        this.maxDepth = maxDepth;
        this.minSamplesSplit = minSamplesSplit;
        this.trees = new ArrayList<>();
    }

    public void train(double[][] X, int[] y, long seed) {
        Random rand = new Random(seed);
        int numFeatures = X[0].length;
        int maxFeatures = (int) Math.max(1, Math.sqrt(numFeatures)); // Default feature bagging size

        trees.clear();
        for (int t = 0; t < numTrees; t++) {
            DecisionTree tree = new DecisionTree(maxDepth, minSamplesSplit, maxFeatures);
            
            // Generate Bootstrap Sample
            double[][] X_bootstrap = new double[X.length][X[0].length];
            int[] y_bootstrap = new int[y.length];
            for (int i = 0; i < X.length; i++) {
                int randIdx = rand.nextInt(X.length);
                X_bootstrap[i] = X[randIdx].clone();
                y_bootstrap[i] = y[randIdx];
            }

            tree.train(X_bootstrap, y_bootstrap, rand);
            trees.add(tree);
        }
    }

    public double predictProbability(double[] sample) {
        double sum = 0.0;
        for (DecisionTree tree : trees) {
            sum += tree.predict(sample);
        }
        return sum / trees.size();
    }

    public int predict(double[] sample) {
        return predictProbability(sample) >= 0.5 ? 1 : 0;
    }

    /**
     * Tree Interpreter: Calculates the local feature contribution for a specific sample.
     * The prediction decomposes to: prediction = bias + sum(feature_contributions)
     */
    public Map<String, Double> explainPrediction(double[] sample, List<String> featureNames) {
        double[] avgContributions = new double[sample.length];
        double avgBias = 0.0;

        for (DecisionTree tree : trees) {
            avgBias += tree.getBias();
            tree.computeContributions(sample, avgContributions);
        }

        avgBias /= trees.size();
        Map<String, Double> importance = new LinkedHashMap<>();
        for (int j = 0; j < sample.length; j++) {
            double featureAvgContribution = avgContributions[j] / trees.size();
            importance.put(featureNames.get(j), featureAvgContribution);
        }

        return importance;
    }
}
