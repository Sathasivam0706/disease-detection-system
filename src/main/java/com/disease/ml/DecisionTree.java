package com.disease.ml;

import java.io.Serializable;
import java.util.*;

public class DecisionTree implements Serializable {
    private static final long serialVersionUID = 1L;

    public static class Node implements Serializable {
        private static final long serialVersionUID = 1L;

        public boolean isLeaf;
        public double prediction; // Probability of class 1 in this node
        public int featureIndex = -1;
        public double splitValue = 0.0;
        public Node left;
        public Node right;
        public int numSamples;

        public Node(double prediction, int numSamples) {
            this.prediction = prediction;
            this.numSamples = numSamples;
            this.isLeaf = true;
        }
    }

    private Node root;
    private int maxDepth;
    private int minSamplesSplit;
    private int maxFeatures; // For feature bagging in RF

    public DecisionTree(int maxDepth, int minSamplesSplit, int maxFeatures) {
        this.maxDepth = maxDepth;
        this.minSamplesSplit = minSamplesSplit;
        this.maxFeatures = maxFeatures;
    }

    public void train(double[][] X, int[] y, Random rand) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < X.length; i++) {
            indices.add(i);
        }
        this.root = buildTree(X, y, indices, 0, rand);
    }

    private Node buildTree(double[][] X, int[] y, List<Integer> indices, int depth, Random rand) {
        int n = indices.size();
        if (n == 0) return null;

        // Calculate prediction at this node
        int count1 = 0;
        for (int idx : indices) {
            if (y[idx] == 1) count1++;
        }
        double prediction = (double) count1 / n;

        Node node = new Node(prediction, n);

        // Stopping criteria
        if (count1 == 0 || count1 == n || depth >= maxDepth || n < minSamplesSplit) {
            return node;
        }

        // Feature Bagging: select random subset of features to search for split
        int numAllFeatures = X[0].length;
        List<Integer> featuresToTry = new ArrayList<>();
        for (int i = 0; i < numAllFeatures; i++) {
            featuresToTry.add(i);
        }
        Collections.shuffle(featuresToTry, rand);
        int m = Math.min(maxFeatures, numAllFeatures);
        featuresToTry = featuresToTry.subList(0, m);

        int bestFeature = -1;
        double bestSplitVal = 0.0;
        double bestGiniGain = -1.0;
        List<Integer> bestLeftIndices = null;
        List<Integer> bestRightIndices = null;

        double currentGini = calculateGini(y, indices);

        for (int f : featuresToTry) {
            // Get unique sorted values for this feature in the node
            List<Double> fValues = new ArrayList<>();
            for (int idx : indices) {
                fValues.add(X[idx][f]);
            }
            Collections.sort(fValues);

            // Test midpoints
            for (int i = 0; i < fValues.size() - 1; i++) {
                double val1 = fValues.get(i);
                double val2 = fValues.get(i + 1);
                if (Math.abs(val1 - val2) < 1e-9) continue;

                double splitVal = (val1 + val2) / 2.0;

                List<Integer> leftIndices = new ArrayList<>();
                List<Integer> rightIndices = new ArrayList<>();

                for (int idx : indices) {
                    if (X[idx][f] <= splitVal) {
                        leftIndices.add(idx);
                    } else {
                        rightIndices.add(idx);
                    }
                }

                if (leftIndices.isEmpty() || rightIndices.isEmpty()) continue;

                double giniL = calculateGini(y, leftIndices);
                double giniR = calculateGini(y, rightIndices);
                double gain = currentGini - (((double) leftIndices.size() / n) * giniL 
                                            + ((double) rightIndices.size() / n) * giniR);

                if (gain > bestGiniGain) {
                    bestGiniGain = gain;
                    bestFeature = f;
                    bestSplitVal = splitVal;
                    bestLeftIndices = leftIndices;
                    bestRightIndices = rightIndices;
                }
            }
        }

        // If split improves impurity, create children
        if (bestGiniGain > 1e-9 && bestFeature != -1) {
            node.isLeaf = false;
            node.featureIndex = bestFeature;
            node.splitValue = bestSplitVal;
            node.left = buildTree(X, y, bestLeftIndices, depth + 1, rand);
            node.right = buildTree(X, y, bestRightIndices, depth + 1, rand);
        }

        return node;
    }

    private double calculateGini(int[] y, List<Integer> indices) {
        int n = indices.size();
        if (n == 0) return 0.0;
        int count1 = 0;
        for (int idx : indices) {
            if (y[idx] == 1) count1++;
        }
        double p1 = (double) count1 / n;
        double p0 = 1.0 - p1;
        return 1.0 - p0 * p0 - p1 * p1;
    }

    public double predict(double[] sample) {
        Node curr = root;
        while (!curr.isLeaf) {
            if (sample[curr.featureIndex] <= curr.splitValue) {
                curr = curr.left;
            } else {
                curr = curr.right;
            }
        }
        return curr.prediction;
    }

    /**
     * Compute contribution of features along the path for a given sample.
     * This is the Local Feature Importance (Tree Interpreter algorithm).
     * For each step from node A to node B, the difference B.prediction - A.prediction
     * is attributed to the feature that was used to split A.
     */
    public void computeContributions(double[] sample, double[] contributions) {
        Node curr = root;
        while (curr != null && !curr.isLeaf) {
            Node next = (sample[curr.featureIndex] <= curr.splitValue) ? curr.left : curr.right;
            if (next == null) break;
            
            // Contribution of feature used in split: P(child) - P(parent)
            contributions[curr.featureIndex] += (next.prediction - curr.prediction);
            curr = next;
        }
    }

    public double getBias() {
        return root != null ? root.prediction : 0.0;
    }
}
