package com.disease;

import com.disease.ml.Dataset;
import com.disease.ml.ModelManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.io.File;

@SpringBootApplication
public class DiseaseDetectionApplication {

    private static final Logger logger = LoggerFactory.getLogger(DiseaseDetectionApplication.class);

    public static void main(String[] args) {
        try {
            logger.info("Initializing Early Disease Detection System...");
            
            // 1. Generate synthetic datasets if not present
            logger.info("Checking datasets...");
            Dataset.generateDatasetsIfNotExist("data");
            
            // 2. Check and train models if they are missing
            logger.info("Checking models...");
            File modelsDir = new File("models");
            if (!modelsDir.exists()) {
                modelsDir.mkdirs();
            }

            String[] diseases = {"diabetes", "heart", "kidney"};
            for (String disease : diseases) {
                String modelPath = "models/" + disease + "_model.pkl";
                String scalerPath = "models/" + disease + "_scaler.pkl";
                String csvPath = "data/" + disease + ".csv";
                
                File modelFile = new File(modelPath);
                File scalerFile = new File(scalerPath);
                
                if (!modelFile.exists() || !scalerFile.exists()) {
                    logger.info("{} model or scaler missing. Training now...", disease.toUpperCase());
                    ModelManager.trainAndSave(disease, csvPath, modelPath, scalerPath);
                } else {
                    logger.info("{} model and scaler already exist.", disease.toUpperCase());
                }
            }

            logger.info("Model initialization check complete.");
        } catch (Exception e) {
            logger.error("Fatal error during application startup initialization", e);
        }

        // 3. Boot Spring application
        SpringApplication.run(DiseaseDetectionApplication.class, args);
    }
}
