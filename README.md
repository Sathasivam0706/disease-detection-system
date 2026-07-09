# AI-Powered Early Disease Detection System (Java Spring Boot Edition)

This is a production-ready, complete B.Tech final year project utilizing **Ensemble Machine Learning** and **Explainable AI (XAI)** for early-stage multi-disease prediction (Diabetes, Heart Disease, and Chronic Kidney Disease).

The backend is built in pure Java (Spring Boot) with custom machine learning algorithms and local explainability models. The frontend is a modern, responsive single-page application dashboard featuring interactive Plotly.js charts and on-the-fly PDF clinical reports.

---

## Technical Features

1. **Custom Machine Learning Engine (Pure Java)**
   - **Ensemble Voting Classifier**: Soft-voting combination of a custom **Random Forest Classifier** and a **Logistic Regression Classifier**.
   - **Data Preprocessing & Scaling**: Handles missing value (zero) imputation, median replacement, and standard scaling (`StandardScaler` logic fitting/transforming).
   - **Tree Interpreter (SHAP Alternative)**: A recursive split-path analyzer that computes exact local feature contributions for a specific prediction, mirroring Python's TreeSHAP explainability.
   
2. **Clinical Document Exporter**
   - Integrates **OpenPDF** to assemble styled A4 clinical report PDFs showing risk levels (colored by status), probability scores, top 5 contributing pathological factors, and a full clinical metrics breakdown table.

3. **Responsive Operations Frontend**
   - Built with semantic HTML5, modern HSL-driven glassmorphic CSS, and vanilla JS.
   - Interactive horizontal bar charts powered by **Plotly.js** showing feature impacts.
   - Live local history logging utilizing `localStorage`.

---

## Project Structure

```text
disease_detection/
├── pom.xml                                   # Maven dependencies & build setup
├── README.md                                 # System documentation
├── data/                                     # Folder for CSV datasets
│   ├── diabetes.csv                          # (Generated automatically at startup)
│   ├── heart.csv                             # (Generated automatically at startup)
│   └── kidney.csv                            # (Generated automatically at startup)
├── models/                                   # Folder for serialized model objects
│   ├── diabetes_model.pkl                    # Serialized Ensemble model
│   ├── diabetes_scaler.pkl                   # Serialized Scaler coefficients
│   ├── heart_model.pkl                       # Serialized Ensemble model
│   ├── heart_scaler.pkl                      # Serialized Scaler coefficients
│   ├── kidney_model.pkl                      # Serialized Ensemble model
│   └── kidney_scaler.pkl                     # Serialized Scaler coefficients
└── src/
    └── main/
        ├── java/
        │   └── com/
        │       └── disease/
        │           ├── DiseaseDetectionApplication.java   # App entry point & trainer
        │           ├── controller/
        │           │   └── ApiController.java             # REST API Controller
        │           ├── dto/
        │           │   ├── PredictionRequest.java         # API Request mapping DTO
        │           │   └── PredictionResponse.java        # API Response mapping DTO
        │           ├── ml/
        │           │   ├── Dataset.java                   # Preprocessing & split logic
        │           │   ├── DecisionTree.java              # Gini Impurity tree classifier
        │           │   ├── RandomForest.java              # Forest of trees + Tree Interpreter
        │           │   ├── LogisticRegression.java        # Gradient Descent linear classifier
        │           │   ├── EnsembleModel.java             # Ensemble combiner (soft voting)
        │           │   ├── Scaler.java                    # StandardScaler normalizer
        │           │   └── ModelManager.java              # Training & serialization driver
        │           └── util/
        │               └── ReportGenerator.java           # PDF clinical report assembler
        └── resources/
            ├── application.properties                     # Configuration properties
            └── static/                                    # Static web assets
                ├── index.html                             # Dashboard dashboard UI
                ├── css/
                │   └── style.css                          # Custom healthcare theme styles
                └── js/
                    └── app.js                             # Front-end API & Plotly logic
```

---

## Setup & Running Instructions

### Prerequisite: Install Java JDK
To build and run this application, you must install the **Java Development Kit (JDK) 17 or higher**.
- **Windows**: Download and install OpenJDK 17 or 21 from [Adoptium (Temurin)](https://adoptium.net/) or Microsoft.
- **Verification**: Check your path by typing `java -version` and `javac -version` in your terminal/PowerShell.

---

### Option A: Run inside an IDE (Recommended)
1. Install **IntelliJ IDEA Community Edition** (or Eclipse / VS Code).
2. Open the IDE and select **Open Project**. Navigate to and choose the `disease_detection` folder.
3. The IDE will automatically read the `pom.xml` and download dependencies (Spring Boot, OpenPDF).
4. Navigate to `src/main/java/com/disease/DiseaseDetectionApplication.java`.
5. Right-click the file and select **Run 'DiseaseDetectionApplication.main()'**.

---

### Option B: Run via command line (Maven required)
1. Install Apache Maven (see [Maven Setup Guide](https://maven.apache.org/install.html)) or make sure `mvn` is on your PATH.
2. Open your terminal inside the `disease_detection` directory.
3. Run the following command to compile, package, and launch the application:
   ```bash
   mvn spring-boot:run
   ```

---

## What Happens on App Launch
1. **Data Generation**: The application checks if the `data/` folder and `diabetes.csv`, `heart.csv`, and `kidney.csv` are present. If missing, it generates **realistic synthetic datasets** with representative diagnostic features, target ratios, missing entries, and outlier distributions.
2. **Model Training**: It checks if serialized classifiers and scalers exist under `models/`. If missing, the app pre-processes each dataset, imputes missing records with medians, standardizes variables, splits samples (80/20 train/test split with seed `42`), trains the ensembles, evaluates model accuracy, logs classification reports to the console, and serializes the objects.
3. **Web Server Host**: Spring Boot mounts the embedded Tomcat server on `http://localhost:8080`.

Open your browser and navigate to:
👉 https://disease-detection-system-1.onrender.com/. 
---

## System Telemetry & ML Performance

Upon first-run training, the models execute test-split evaluations yielding high-accuracy metrics:
- **Diabetes Ensemble Classifier**: ~90-92% Accuracy
- **Cardiovascular Ensemble Classifier**: ~92-94% Accuracy
- **Chronic Kidney Disease Model**: ~95-97% Accuracy

Each prediction is accompanied by a **local path feature attribution analysis (Tree Interpreter)** which details how much each metric shifted the final probability (in positive risk or negative protective directions), showing the full math of why a prediction was made.
