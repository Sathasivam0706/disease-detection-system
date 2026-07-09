package com.disease.ml;

import java.io.*;
import java.util.*;

public class ModelManager {

    public static class ModelContainer {
        public EnsembleModel model;
        public Scaler scaler;
        public List<String> featureNames;

        public ModelContainer(EnsembleModel model, Scaler scaler, List<String> featureNames) {
            this.model = model;
            this.scaler = scaler;
            this.featureNames = featureNames;
        }
    }

    private static final Map<String, List<String>> diseaseFeatures = new HashMap<>();

    static {
        diseaseFeatures.put("diabetes", Arrays.asList(
            "Pregnancies", "Glucose", "BloodPressure", "SkinThickness", "Insulin", "BMI", "DiabetesPedigreeFunction", "Age"
        ));
        diseaseFeatures.put("heart", Arrays.asList(
            "Age", "Sex", "CP", "Trestbps", "Chol", "FBS", "RestECG", "Thalach", "Exang", "Oldpeak", "Slope", "CA", "Thal"
        ));
        diseaseFeatures.put("kidney", Arrays.asList(
            "age", "bp", "sg", "al", "su", "rbc", "pc", "pcc", "ba", "bgr", "bu", "sc", "sod", "pot", "hemo", "pcv", "wc", "rc", "htn", "dm", "cad", "appet", "pe", "ane"
        ));
    }

    public static List<String> getFeatureNames(String disease) {
        return diseaseFeatures.get(disease.toLowerCase());
    }

    /**
     * Train ensemble model for a disease, print report, and save to files.
     */
    public static void trainAndSave(String disease, String csvPath, String modelPath, String scalerPath) throws Exception {
        System.out.println("--------------------------------------------------");
        System.out.println("Training Ensemble Model for: " + disease.toUpperCase());
        System.out.println("Source Data: " + csvPath);

        Dataset dataset;
        String lowercaseDisease = disease.toLowerCase();
        if (lowercaseDisease.contains("diabetes")) {
            dataset = Dataset.preprocessDiabetes(csvPath);
        } else if (lowercaseDisease.contains("heart")) {
            dataset = Dataset.preprocessHeart(csvPath);
        } else if (lowercaseDisease.contains("kidney")) {
            dataset = Dataset.preprocessKidney(csvPath);
        } else {
            throw new IllegalArgumentException("Unknown disease: " + disease);
        }

        // Initialize RF: 100 trees, max depth 10, min split 2
        RandomForest rf = new RandomForest(100, 10, 2);
        // Initialize LR: learning rate 0.1, epochs 200
        LogisticRegression lr = new LogisticRegression(0.1, 200);

        EnsembleModel ensemble = new EnsembleModel(rf, lr);
        ensemble.train(dataset.X_train, dataset.y_train, 42);

        // Evaluate model
        int[] y_pred = new int[dataset.X_test.length];
        double[] probs = new double[dataset.X_test.length];
        int tp = 0, fp = 0, tn = 0, fn = 0;

        for (int i = 0; i < dataset.X_test.length; i++) {
            probs[i] = ensemble.predictProbability(dataset.X_test[i]);
            y_pred[i] = probs[i] >= 0.5 ? 1 : 0;

            if (y_pred[i] == 1 && dataset.y_test[i] == 1) tp++;
            else if (y_pred[i] == 1 && dataset.y_test[i] == 0) fp++;
            else if (y_pred[i] == 0 && dataset.y_test[i] == 0) tn++;
            else if (y_pred[i] == 0 && dataset.y_test[i] == 1) fn++;
        }

        double accuracy = (double) (tp + tn) / dataset.X_test.length;
        System.out.printf("Test Set Accuracy: %.2f%%\n", accuracy * 100);

        // Precision, Recall, F1
        double p0 = tn + fn > 0 ? (double) tn / (tn + fn) : 0.0;
        double r0 = tn + fp > 0 ? (double) tn / (tn + fp) : 0.0;
        double f1_0 = p0 + r0 > 0 ? 2 * (p0 * r0) / (p0 + r0) : 0.0;
        int support0 = tn + fp;

        double p1 = tp + fp > 0 ? (double) tp / (tp + fp) : 0.0;
        double r1 = tp + fn > 0 ? (double) tp / (tp + fn) : 0.0;
        double f1_1 = p1 + r1 > 0 ? 2 * (p1 * r1) / (p1 + r1) : 0.0;
        int support1 = tp + fn;

        System.out.println("Classification Report:");
        System.out.printf("              precision    recall  f1-score   support\n");
        System.out.printf("           0       %.2f      %.2f      %.2f      %d\n", p0, r0, f1_0, support0);
        System.out.printf("           1       %.2f      %.2f      %.2f      %d\n", p1, r1, f1_1, support1);
        System.out.printf("    accuracy                           %.2f      %d\n", accuracy, dataset.y_test.length);

        // Serialize and save model
        File modelFile = new File(modelPath);
        if (!modelFile.getParentFile().exists()) {
            modelFile.getParentFile().mkdirs();
        }
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(modelFile))) {
            oos.writeObject(ensemble);
        }
        System.out.println("Model saved successfully to: " + modelPath);

        // Serialize and save scaler
        Scaler scaler = new Scaler(dataset.means, dataset.stds);
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(scalerPath))) {
            oos.writeObject(scaler);
        }
        System.out.println("Scaler saved successfully to: " + scalerPath);
        System.out.println("--------------------------------------------------\n");
    }

    /**
     * Deserialize EnsembleModel
     */
    public static EnsembleModel loadModel(String modelPath) throws Exception {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(modelPath))) {
            return (EnsembleModel) ois.readObject();
        }
    }

    /**
     * Deserialize Scaler
     */
    public static Scaler loadScaler(String scalerPath) throws Exception {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(scalerPath))) {
            return (Scaler) ois.readObject();
        }
    }
}
