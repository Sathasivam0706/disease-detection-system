package com.disease.controller;

import com.disease.dto.PredictionRequest;
import com.disease.dto.PredictionResponse;
import com.disease.ml.EnsembleModel;
import com.disease.ml.ModelManager;
import com.disease.ml.Scaler;
import com.disease.util.ReportGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ApiController {

    private static final Logger logger = LoggerFactory.getLogger(ApiController.class);

    @PostMapping("/predict")
    public ResponseEntity<?> predict(@RequestBody PredictionRequest request) {
        try {
            String disease = request.getDiseaseType();
            if (disease == null || disease.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Disease type is required."));
            }

            // Load model and scaler
            String modelPath = "models/" + disease.toLowerCase() + "_model.pkl";
            String scalerPath = "models/" + disease.toLowerCase() + "_scaler.pkl";

            EnsembleModel model = ModelManager.loadModel(modelPath);
            Scaler scaler = ModelManager.loadScaler(scalerPath);
            List<String> featureNames = ModelManager.getFeatureNames(disease);

            double[] rawInput = request.toDoubleArray();
            if (rawInput.length == 0) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Invalid inputs for disease type: " + disease));
            }

            // Normalize
            double[] scaledInput = scaler.transform(rawInput);

            // Predict
            int prediction = model.predict(scaledInput);
            double probability = model.predictProbability(scaledInput);

            // Explain (Tree Interpreter values on the Random Forest component)
            Map<String, Double> importance = model.explainPrediction(scaledInput, featureNames);

            PredictionResponse response = new PredictionResponse(prediction, probability, importance);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Prediction endpoint failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Prediction failed: " + e.getMessage()));
        }
    }

    @PostMapping("/report")
    public ResponseEntity<byte[]> downloadReport(@RequestBody PredictionRequest request) {
        try {
            String disease = request.getDiseaseType();
            
            String modelPath = "models/" + disease.toLowerCase() + "_model.pkl";
            String scalerPath = "models/" + disease.toLowerCase() + "_scaler.pkl";

            EnsembleModel model = ModelManager.loadModel(modelPath);
            Scaler scaler = ModelManager.loadScaler(scalerPath);
            List<String> featureNames = ModelManager.getFeatureNames(disease);

            double[] rawInput = request.toDoubleArray();
            double[] scaledInput = scaler.transform(rawInput);

            int prediction = model.predict(scaledInput);
            double probability = model.predictProbability(scaledInput);
            Map<String, Double> importance = model.explainPrediction(scaledInput, featureNames);

            byte[] pdfBytes = ReportGenerator.generateReport(
                    disease, prediction, probability, importance, rawInput, featureNames
            );

            String filename = String.format("%s_report_%s.pdf", 
                    disease.toLowerCase(), 
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            logger.error("Report generation endpoint failed", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboardStats() {
        try {
            Map<String, Object> stats = new LinkedHashMap<>();
            String[] diseases = {"diabetes", "heart", "kidney"};

            for (String disease : diseases) {
                String modelPath = "models/" + disease + "_model.pkl";
                String scalerPath = "models/" + disease + "_scaler.pkl";
                String csvPath = "data/" + disease + ".csv";

                File modelFile = new File(modelPath);
                if (!modelFile.exists()) {
                    continue;
                }

                // Dynamically evaluate test accuracy
                com.disease.ml.Dataset dataset;
                if ("diabetes".equals(disease)) {
                    dataset = com.disease.ml.Dataset.preprocessDiabetes(csvPath);
                } else if ("heart".equals(disease)) {
                    dataset = com.disease.ml.Dataset.preprocessHeart(csvPath);
                } else {
                    dataset = com.disease.ml.Dataset.preprocessKidney(csvPath);
                }

                EnsembleModel ensemble = ModelManager.loadModel(modelPath);
                int correct = 0;
                for (int i = 0; i < dataset.X_test.length; i++) {
                    double prob = ensemble.predictProbability(dataset.X_test[i]);
                    int pred = prob >= 0.5 ? 1 : 0;
                    if (pred == dataset.y_test[i]) {
                        correct++;
                    }
                }
                double accuracy = (double) correct / dataset.X_test.length;
                stats.put(disease + "Accuracy", String.format(Locale.US, "%.1f%%", accuracy * 100));
            }

            stats.put("modelType", "Voting Ensemble (RF + LR)");
            stats.put("totalModels", 3);
            stats.put("xaiFramework", "SHAP / Tree Interpreter");

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            logger.error("Dashboard stats endpoint failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Failed to fetch stats: " + e.getMessage()));
        }
    }
}
