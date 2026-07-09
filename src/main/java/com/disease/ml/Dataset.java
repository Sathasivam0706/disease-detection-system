package com.disease.ml;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

public class Dataset implements Serializable {
    private static final long serialVersionUID = 1L;

    public double[][] X_train;
    public double[][] X_test;
    public int[] y_train;
    public int[] y_test;
    public List<String> featureNames;
    public double[] means;
    public double[] stds;

    public Dataset() {}

    public Dataset(double[][] X_train, double[][] X_test, int[] y_train, int[] y_test, List<String> featureNames, double[] means, double[] stds) {
        this.X_train = X_train;
        this.X_test = X_test;
        this.y_train = y_train;
        this.y_test = y_test;
        this.featureNames = featureNames;
        this.means = means;
        this.stds = stds;
    }

    /**
     * Replaces target zeros in specified columns with their medians.
     * Replaces any empty/NaN values with medians.
     */
    public static double[][] preprocessZerosAndNaNs(double[][] data, List<String> colsToImputeZeros, List<String> allCols, double[] medians) {
        double[][] cleaned = new double[data.length][data[0].length];
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[i].length; j++) {
                double val = data[i][j];
                String colName = allCols.get(j);
                
                // If it's a NaN or a zero in a column where zero is a missing value, replace with median
                if (Double.isNaN(val) || (colsToImputeZeros.contains(colName) && Math.abs(val) < 1e-9)) {
                    cleaned[i][j] = medians[j];
                } else {
                    cleaned[i][j] = val;
                }
            }
        }
        return cleaned;
    }

    /**
     * Compute column-wise medians for a 2D array, ignoring NaNs and optionally zeros.
     */
    public static double[] calculateMedians(double[][] data, List<String> colsToImputeZeros, List<String> allCols) {
        int numCols = data[0].length;
        double[] medians = new double[numCols];
        for (int j = 0; j < numCols; j++) {
            List<Double> validValues = new ArrayList<>();
            String colName = allCols.get(j);
            boolean checkZero = colsToImputeZeros.contains(colName);
            for (int i = 0; i < data.length; i++) {
                double val = data[i][j];
                if (!Double.isNaN(val)) {
                    if (checkZero && Math.abs(val) < 1e-9) {
                        continue;
                    }
                    validValues.add(val);
                }
            }
            if (validValues.isEmpty()) {
                medians[j] = 0.0;
            } else {
                Collections.sort(validValues);
                int size = validValues.size();
                if (size % 2 == 1) {
                    medians[j] = validValues.get(size / 2);
                } else {
                    medians[j] = (validValues.get(size / 2 - 1) + validValues.get(size / 2)) / 2.0;
                }
            }
        }
        return medians;
    }

    /**
     * Standardizes a 2D array using the provided means and standard deviations.
     */
    public static double[][] scaleData(double[][] data, double[] means, double[] stds) {
        double[][] scaled = new double[data.length][data[0].length];
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[i].length; j++) {
                if (stds[j] > 1e-9) {
                    scaled[i][j] = (data[i][j] - means[j]) / stds[j];
                } else {
                    scaled[i][j] = data[i][j] - means[j];
                }
            }
        }
        return scaled;
    }

    /**
     * Fits a standard scaler by calculating mean and standard deviation of columns.
     */
    public static void fitScaler(double[][] data, double[] outMeans, double[] outStds) {
        int numCols = data[0].length;
        int numRows = data.length;
        for (int j = 0; j < numCols; j++) {
            double sum = 0;
            for (int i = 0; i < numRows; i++) {
                sum += data[i][j];
            }
            double mean = sum / numRows;
            outMeans[j] = mean;

            double varianceSum = 0;
            for (int i = 0; i < numRows; i++) {
                varianceSum += Math.pow(data[i][j] - mean, 2);
            }
            double std = Math.sqrt(varianceSum / numRows);
            outStds[j] = std;
        }
    }

    /**
     * Deterministic train/test split (80/20) with random_state=42.
     */
    public static Dataset splitAndScale(double[][] X, int[] y, List<String> featureNames, long seed) {
        int n = X.length;
        List<Integer> indices = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            indices.add(i);
        }
        // Deterministic shuffle with seed
        Collections.shuffle(indices, new Random(seed));

        int trainSize = (int) (n * 0.8);
        int testSize = n - trainSize;

        double[][] X_train_raw = new double[trainSize][X[0].length];
        int[] y_train = new int[trainSize];
        double[][] X_test_raw = new double[testSize][X[0].length];
        int[] y_test = new int[testSize];

        for (int i = 0; i < trainSize; i++) {
            int idx = indices.get(i);
            X_train_raw[i] = X[idx].clone();
            y_train[i] = y[idx];
        }
        for (int i = 0; i < testSize; i++) {
            int idx = indices.get(trainSize + i);
            X_test_raw[i] = X[idx].clone();
            y_test[i] = y[idx];
        }

        // Fit scaler on train data
        double[] means = new double[X[0].length];
        double[] stds = new double[X[0].length];
        fitScaler(X_train_raw, means, stds);

        // Scale both train and test data
        double[][] X_train = scaleData(X_train_raw, means, stds);
        double[][] X_test = scaleData(X_test_raw, means, stds);

        return new Dataset(X_train, X_test, y_train, y_test, featureNames, means, stds);
    }

    /**
     * Helper to read a CSV file into lines.
     */
    private static List<String[]> readCsvLines(String filepath) throws IOException {
        List<String[]> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filepath))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                // Simple CSV parsing that handles quotes if necessary
                lines.add(line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)"));
            }
        }
        return lines;
    }

    // -------------------------------------------------------------
    // PREPROCESS DIABETES
    // -------------------------------------------------------------
    public static Dataset preprocessDiabetes(String filepath) throws IOException {
        List<String[]> lines = readCsvLines(filepath);
        if (lines.size() < 2) throw new IOException("Invalid diabetes dataset");

        String[] headers = lines.get(0);
        List<String> featureNames = new ArrayList<>();
        for (int i = 0; i < headers.length - 1; i++) {
            featureNames.add(headers[i].trim());
        }

        int rows = lines.size() - 1;
        int cols = featureNames.size();
        double[][] X = new double[rows][cols];
        int[] y = new int[rows];

        for (int i = 0; i < rows; i++) {
            String[] tokens = lines.get(i + 1);
            for (int j = 0; j < cols; j++) {
                String tok = tokens[j].trim();
                X[i][j] = (tok.isEmpty() || tok.equalsIgnoreCase("nan") || tok.equals("?")) ? Double.NaN : Double.parseDouble(tok);
            }
            String label = tokens[cols].trim();
            y[i] = Integer.parseInt(label);
        }

        // Medians calculated ignoring zeros in specific columns
        List<String> colsToImputeZeros = Arrays.asList("Glucose", "BloodPressure", "SkinThickness", "Insulin", "BMI");
        double[] medians = calculateMedians(X, colsToImputeZeros, featureNames);
        double[][] X_cleaned = preprocessZerosAndNaNs(X, colsToImputeZeros, featureNames, medians);

        return splitAndScale(X_cleaned, y, featureNames, 42);
    }

    // -------------------------------------------------------------
    // PREPROCESS HEART
    // -------------------------------------------------------------
    public static Dataset preprocessHeart(String filepath) throws IOException {
        List<String[]> lines = readCsvLines(filepath);
        if (lines.size() < 2) throw new IOException("Invalid heart dataset");

        String[] headers = lines.get(0);
        List<String> featureNames = new ArrayList<>();
        for (int i = 0; i < headers.length - 1; i++) {
            featureNames.add(headers[i].trim());
        }

        int rows = lines.size() - 1;
        int cols = featureNames.size();
        double[][] X = new double[rows][cols];
        int[] y = new int[rows];

        for (int i = 0; i < rows; i++) {
            String[] tokens = lines.get(i + 1);
            for (int j = 0; j < cols; j++) {
                String tok = tokens[j].trim();
                X[i][j] = (tok.isEmpty() || tok.equalsIgnoreCase("nan") || tok.equals("?")) ? Double.NaN : Double.parseDouble(tok);
            }
            String label = tokens[cols].trim();
            y[i] = Integer.parseInt(label);
        }

        // Fill remaining NaNs with median (no zero imputation for heart)
        double[] medians = calculateMedians(X, new ArrayList<>(), featureNames);
        double[][] X_cleaned = preprocessZerosAndNaNs(X, new ArrayList<>(), featureNames, medians);

        return splitAndScale(X_cleaned, y, featureNames, 42);
    }

    // -------------------------------------------------------------
    // PREPROCESS KIDNEY
    // -------------------------------------------------------------
    public static Dataset preprocessKidney(String filepath) throws IOException {
        List<String[]> lines = readCsvLines(filepath);
        if (lines.size() < 2) throw new IOException("Invalid kidney dataset");

        String[] headers = lines.get(0);
        // Find indices
        int idIdx = -1;
        int classIdx = -1;
        for (int i = 0; i < headers.length; i++) {
            String h = headers[i].trim().toLowerCase();
            if (h.equals("id")) idIdx = i;
            if (h.equals("classification")) classIdx = i;
        }

        if (classIdx == -1) throw new IOException("Classification column not found in kidney dataset");

        List<String> featureNames = new ArrayList<>();
        List<Integer> featureIndices = new ArrayList<>();
        for (int i = 0; i < headers.length; i++) {
            if (i != idIdx && i != classIdx) {
                featureNames.add(headers[i].trim());
                featureIndices.add(i);
            }
        }

        int rows = lines.size() - 1;
        int cols = featureNames.size();
        double[][] X = new double[rows][cols];
        int[] y = new int[rows];

        for (int i = 0; i < rows; i++) {
            String[] tokens = lines.get(i + 1);
            
            // Map features
            for (int j = 0; j < cols; j++) {
                int originalIdx = featureIndices.get(j);
                String tok = tokens[originalIdx].trim().toLowerCase();
                X[i][j] = parseKidneyFeature(featureNames.get(j), tok);
            }
            
            // Map classification
            String label = tokens[classIdx].trim().toLowerCase();
            if (label.contains("notckd")) {
                y[i] = 0;
            } else if (label.contains("ckd")) {
                y[i] = 1;
            } else {
                y[i] = 0; // Default fallback
            }
        }

        // Fill remaining NaNs with median (numeric only)
        double[] medians = calculateMedians(X, new ArrayList<>(), featureNames);
        double[][] X_cleaned = preprocessZerosAndNaNs(X, new ArrayList<>(), featureNames, medians);

        return splitAndScale(X_cleaned, y, featureNames, 42);
    }

    private static double parseKidneyFeature(String col, String val) {
        if (val.isEmpty() || val.equals("?") || val.equals("nan") || val.equals("null")) {
            return Double.NaN;
        }
        // Strip quotes if present
        if (val.startsWith("\"") && val.endsWith("\"")) {
            val = val.substring(1, val.length() - 1).trim();
        }
        // Map categorical features
        switch (col.toLowerCase()) {
            case "rbc":
            case "pc":
                return val.equals("normal") ? 1.0 : (val.equals("abnormal") ? 0.0 : Double.NaN);
            case "pcc":
            case "ba":
                return val.equals("present") ? 1.0 : (val.equals("notpresent") ? 0.0 : Double.NaN);
            case "htn":
            case "dm":
            case "cad":
            case "pe":
            case "ane":
                return val.contains("yes") ? 1.0 : (val.contains("no") ? 0.0 : Double.NaN);
            case "appet":
                return val.equals("good") ? 1.0 : (val.equals("poor") ? 0.0 : Double.NaN);
            default:
                try {
                    return Double.parseDouble(val);
                } catch (NumberFormatException e) {
                    return Double.NaN;
                }
        }
    }

    // -------------------------------------------------------------
    // SYNTHETIC DATA GENERATOR
    // -------------------------------------------------------------
    public static void generateDatasetsIfNotExist(String dataDir) throws IOException {
        File dir = new File(dataDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File diabetesFile = new File(dir, "diabetes.csv");
        if (!diabetesFile.exists()) {
            generateDiabetesCsv(diabetesFile);
        }

        File heartFile = new File(dir, "heart.csv");
        if (!heartFile.exists()) {
            generateHeartCsv(heartFile);
        }

        File kidneyFile = new File(dir, "kidney.csv");
        if (!kidneyFile.exists()) {
            generateKidneyCsv(kidneyFile);
        }
    }

    private static void generateDiabetesCsv(File file) throws IOException {
        Random r = new Random(42);
        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(file)))) {
            pw.println("Pregnancies,Glucose,BloodPressure,SkinThickness,Insulin,BMI,DiabetesPedigreeFunction,Age,Outcome");
            int rows = 768;
            int numPos = (int) (rows * 0.35); // 35% positive

            for (int i = 0; i < rows; i++) {
                int outcome = (i < numPos) ? 1 : 0;
                
                int pregnancies;
                double glucose, bp, skin, insulin, bmi, dpf, age;

                if (outcome == 1) {
                    pregnancies = Math.max(0, (int) (r.nextGaussian() * 3 + 5.2));
                    glucose = r.nextGaussian() * 25 + 142;
                    bp = r.nextGaussian() * 11 + 75;
                    skin = r.nextGaussian() * 9 + 29;
                    insulin = r.nextGaussian() * 90 + 175;
                    bmi = r.nextGaussian() * 5.5 + 35.1;
                    dpf = r.nextDouble() * 1.5 + 0.3;
                    age = r.nextGaussian() * 11 + 38;
                } else {
                    pregnancies = Math.max(0, (int) (r.nextGaussian() * 2.2 + 2.7));
                    glucose = r.nextGaussian() * 18 + 110;
                    bp = r.nextGaussian() * 9 + 68;
                    skin = r.nextGaussian() * 8 + 19;
                    insulin = r.nextGaussian() * 35 + 78;
                    bmi = r.nextGaussian() * 4.5 + 28.2;
                    dpf = r.nextDouble() * 0.8 + 0.15;
                    age = r.nextGaussian() * 9 + 31;
                }

                // Bound features
                pregnancies = Math.min(17, pregnancies);
                glucose = Math.max(70, Math.min(200, glucose));
                bp = Math.max(50, Math.min(120, bp));
                skin = Math.max(10, Math.min(50, skin));
                insulin = Math.max(15, Math.min(600, insulin));
                bmi = Math.max(15, Math.min(50, bmi));
                age = Math.max(21, Math.min(81, age));

                // Introduce zeros (missing value tests) in 12% of records for some variables
                if (r.nextDouble() < 0.12) glucose = 0;
                if (r.nextDouble() < 0.12) bp = 0;
                if (r.nextDouble() < 0.12) skin = 0;
                if (r.nextDouble() < 0.12) insulin = 0;
                if (r.nextDouble() < 0.12) bmi = 0;

                pw.printf(Locale.US, "%d,%.1f,%.1f,%.1f,%.1f,%.1f,%.3f,%.1f,%d\n",
                        pregnancies, glucose, bp, skin, insulin, bmi, dpf, age, outcome);
            }
        }
    }

    private static void generateHeartCsv(File file) throws IOException {
        Random r = new Random(42);
        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(file)))) {
            pw.println("Age,Sex,CP,Trestbps,Chol,FBS,RestECG,Thalach,Exang,Oldpeak,Slope,CA,Thal,Target");
            int rows = 303;
            int numPos = (int) (rows * 0.45); // 45% positive

            for (int i = 0; i < rows; i++) {
                int target = (i < numPos) ? 1 : 0;
                
                double age, sex, cp, trestbps, chol, fbs, restecg, thalach, exang, oldpeak, slope, ca, thal;

                if (target == 1) {
                    age = r.nextGaussian() * 8 + 58;
                    sex = (r.nextDouble() < 0.7) ? 1 : 0;
                    cp = (r.nextDouble() < 0.8) ? (r.nextInt(3) + 1) : 0; // mostly atypical/non-anginal/asymptomatic
                    trestbps = r.nextGaussian() * 18 + 138;
                    chol = r.nextGaussian() * 50 + 260;
                    fbs = (r.nextDouble() < 0.18) ? 1 : 0;
                    restecg = (r.nextDouble() < 0.6) ? 1 : (r.nextDouble() < 0.8 ? 2 : 0);
                    thalach = r.nextGaussian() * 20 + 135;
                    exang = (r.nextDouble() < 0.5) ? 1 : 0;
                    oldpeak = Math.max(0.0, r.nextGaussian() * 1.2 + 1.8);
                    slope = (r.nextDouble() < 0.6) ? 1 : (r.nextDouble() < 0.8 ? 2 : 0);
                    ca = r.nextInt(3) + 1;
                    thal = (r.nextDouble() < 0.7) ? 3 : 2;
                } else {
                    age = r.nextGaussian() * 9 + 52;
                    sex = (r.nextDouble() < 0.58) ? 1 : 0;
                    cp = (r.nextDouble() < 0.15) ? (r.nextInt(3) + 1) : 0; // mostly typical
                    trestbps = r.nextGaussian() * 16 + 128;
                    chol = r.nextGaussian() * 45 + 230;
                    fbs = (r.nextDouble() < 0.08) ? 1 : 0;
                    restecg = (r.nextDouble() < 0.3) ? 1 : 0;
                    thalach = r.nextGaussian() * 18 + 155;
                    exang = (r.nextDouble() < 0.14) ? 1 : 0;
                    oldpeak = Math.max(0.0, r.nextGaussian() * 0.6 + 0.5);
                    slope = (r.nextDouble() < 0.8) ? 2 : 1;
                    ca = (r.nextDouble() < 0.85) ? 0 : 1;
                    thal = (r.nextDouble() < 0.8) ? 2 : 3;
                }

                // Bounds
                age = Math.max(29, Math.min(77, age));
                trestbps = Math.max(94, Math.min(200, trestbps));
                chol = Math.max(126, Math.min(564, chol));
                thalach = Math.max(71, Math.min(202, thalach));
                oldpeak = Math.min(6.2, oldpeak);

                pw.printf(Locale.US, "%d,%d,%d,%d,%d,%d,%d,%d,%d,%.1f,%d,%d,%d,%d\n",
                        (int)age, (int)sex, (int)cp, (int)trestbps, (int)chol, (int)fbs, 
                        (int)restecg, (int)thalach, (int)exang, oldpeak, (int)slope, 
                        (int)ca, (int)thal, target);
            }
        }
    }

    private static void generateKidneyCsv(File file) throws IOException {
        Random r = new Random(42);
        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(file)))) {
            pw.println("id,age,bp,sg,al,su,rbc,pc,pcc,ba,bgr,bu,sc,sod,pot,hemo,pcv,wc,rc,htn,dm,cad,appet,pe,ane,classification");
            int rows = 400;
            int numPos = (int) (rows * 0.40); // 40% positive

            for (int i = 0; i < rows; i++) {
                boolean isCkd = (i < numPos);
                String classification = isCkd ? "ckd" : "notckd";
                
                int id = i + 1;
                double age, bp, sg, al, su, bgr, bu, sc, sod, pot, hemo, pcv, wc, rc;
                String rbc, pc, pcc, ba, htn, dm, cad, appet, pe, ane;

                if (isCkd) {
                    age = r.nextGaussian() * 12 + 55;
                    bp = r.nextGaussian() * 15 + 83;
                    sg = 1.005 + 0.005 * r.nextInt(3); // 1.005, 1.010, 1.015
                    al = r.nextInt(4) + 1; // Albumin 1-4
                    su = r.nextDouble() < 0.3 ? r.nextInt(3) : 0;
                    rbc = (r.nextDouble() < 0.6) ? "normal" : "abnormal";
                    pc = (r.nextDouble() < 0.45) ? "normal" : "abnormal";
                    pcc = (r.nextDouble() < 0.28) ? "present" : "notpresent";
                    ba = (r.nextDouble() < 0.18) ? "present" : "notpresent";
                    bgr = r.nextGaussian() * 70 + 175;
                    bu = r.nextGaussian() * 65 + 98;
                    sc = r.nextGaussian() * 4.5 + 4.8;
                    sod = r.nextGaussian() * 8 + 130;
                    pot = r.nextGaussian() * 1.5 + 4.8;
                    hemo = r.nextGaussian() * 2.2 + 9.5;
                    pcv = r.nextGaussian() * 7 + 29;
                    wc = r.nextGaussian() * 3200 + 9800;
                    rc = r.nextGaussian() * 0.9 + 3.6;
                    htn = (r.nextDouble() < 0.72) ? "yes" : "no";
                    dm = (r.nextDouble() < 0.65) ? "yes" : "no";
                    cad = (r.nextDouble() < 0.18) ? "yes" : "no";
                    appet = (r.nextDouble() < 0.55) ? "poor" : "good";
                    pe = (r.nextDouble() < 0.35) ? "yes" : "no";
                    ane = (r.nextDouble() < 0.28) ? "yes" : "no";
                } else {
                    age = r.nextGaussian() * 10 + 46;
                    bp = r.nextGaussian() * 8 + 74;
                    sg = 1.020 + 0.005 * r.nextInt(2); // 1.020, 1.025
                    al = 0; // Albumin 0
                    su = 0; // Sugar 0
                    rbc = "normal";
                    pc = "normal";
                    pcc = "notpresent";
                    ba = "notpresent";
                    bgr = r.nextGaussian() * 20 + 105;
                    bu = r.nextGaussian() * 12 + 33;
                    sc = r.nextGaussian() * 0.3 + 0.9;
                    sod = r.nextGaussian() * 4 + 141;
                    pot = r.nextGaussian() * 0.5 + 4.1;
                    hemo = r.nextGaussian() * 1.5 + 15.2;
                    pcv = r.nextGaussian() * 4 + 46;
                    wc = r.nextGaussian() * 1800 + 7200;
                    rc = r.nextGaussian() * 0.6 + 5.2;
                    htn = "no";
                    dm = "no";
                    cad = "no";
                    appet = "good";
                    pe = "no";
                    ane = "no";
                }

                // Bounds & NaNs
                age = Math.max(2, Math.min(90, age));
                bp = Math.max(50, Math.min(180, bp));
                bgr = Math.max(22, Math.min(490, bgr));
                bu = Math.max(1.0, Math.min(391.0, bu));
                sc = Math.max(0.4, Math.min(76.0, sc));
                sod = Math.max(4.5, Math.min(163.0, sod));
                pot = Math.max(2.5, Math.min(47.0, pot));
                hemo = Math.max(3.1, Math.min(17.8, hemo));
                pcv = Math.max(9, Math.min(54, pcv));
                wc = Math.max(2200, Math.min(26400, wc));
                rc = Math.max(2.1, Math.min(8.0, rc));

                // Write with occasional missing values represented as empty values
                String s_age = (r.nextDouble() < 0.08) ? "" : String.format(Locale.US, "%.0f", age);
                String s_bp = (r.nextDouble() < 0.08) ? "" : String.format(Locale.US, "%.0f", bp);
                String s_sg = (r.nextDouble() < 0.08) ? "" : String.format(Locale.US, "%.3f", sg);
                String s_al = (r.nextDouble() < 0.08) ? "" : String.format(Locale.US, "%.0f", al);
                String s_su = (r.nextDouble() < 0.08) ? "" : String.format(Locale.US, "%.0f", su);
                String s_rbc = (r.nextDouble() < 0.08) ? "" : rbc;
                String s_pc = (r.nextDouble() < 0.08) ? "" : pc;
                String s_pcc = (r.nextDouble() < 0.08) ? "" : pcc;
                String s_ba = (r.nextDouble() < 0.08) ? "" : ba;
                String s_bgr = (r.nextDouble() < 0.08) ? "" : String.format(Locale.US, "%.0f", bgr);
                String s_bu = (r.nextDouble() < 0.08) ? "" : String.format(Locale.US, "%.1f", bu);
                String s_sc = (r.nextDouble() < 0.08) ? "" : String.format(Locale.US, "%.1f", sc);
                String s_sod = (r.nextDouble() < 0.08) ? "" : String.format(Locale.US, "%.1f", sod);
                String s_pot = (r.nextDouble() < 0.08) ? "" : String.format(Locale.US, "%.1f", pot);
                String s_hemo = (r.nextDouble() < 0.08) ? "" : String.format(Locale.US, "%.1f", hemo);
                String s_pcv = (r.nextDouble() < 0.08) ? "" : String.format(Locale.US, "%.0f", pcv);
                String s_wc = (r.nextDouble() < 0.08) ? "" : String.format(Locale.US, "%.0f", wc);
                String s_rc = (r.nextDouble() < 0.08) ? "" : String.format(Locale.US, "%.1f", rc);
                String s_htn = (r.nextDouble() < 0.08) ? "" : htn;
                String s_dm = (r.nextDouble() < 0.08) ? "" : dm;
                String s_cad = (r.nextDouble() < 0.08) ? "" : cad;
                String s_appet = (r.nextDouble() < 0.08) ? "" : appet;
                String s_pe = (r.nextDouble() < 0.08) ? "" : pe;
                String s_ane = (r.nextDouble() < 0.08) ? "" : ane;

                pw.printf("%d,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n",
                        id, s_age, s_bp, s_sg, s_al, s_su, s_rbc, s_pc, s_pcc, s_ba, s_bgr, s_bu, s_sc, 
                        s_sod, s_pot, s_hemo, s_pcv, s_wc, s_rc, s_htn, s_dm, s_cad, s_appet, s_pe, s_ane, 
                        classification);
            }
        }
    }
}
