// Frontend controller - SathaAI early disease prediction XAI dashboard

const API_BASE = '/api';

// Initialize on page load
document.addEventListener('DOMContentLoaded', () => {
    // Load initial stats
    updateDashboardCounters();
    loadDashboardStats();
});

// Switch Tabbed Sections
function switchPage(pageId) {
    // Hide all sections
    const sections = document.querySelectorAll('.page-section');
    sections.forEach(s => s.classList.remove('active'));

    // Deactivate all buttons
    const navButtons = document.querySelectorAll('.nav-btn');
    navButtons.forEach(btn => btn.classList.remove('active'));

    // Activate selected
    const activeSection = document.getElementById(`page-${pageId}`);
    if (activeSection) activeSection.classList.add('active');

    const activeBtn = document.getElementById(`btn-${pageId}`);
    if (activeBtn) activeBtn.classList.add('active');

    // Update Header Text
    const title = document.getElementById('page-title');
    const subtitle = document.getElementById('page-subtitle');

    if (pageId === 'home') {
        title.innerText = "Satha's System Overview";
        subtitle.innerText = 'SathaAI Ensemble Machine Learning & Explainable AI Diagnosis';
    } else if (pageId === 'diabetes') {
        title.innerText = "Satha's Diabetes Risk Diagnostic";
        subtitle.innerText = 'SathaAI analysis of physiological indicators for insulin resistance and diabetic risk';
    } else if (pageId === 'heart') {
        title.innerText = "Satha's Cardiovascular Risk Diagnostic";
        subtitle.innerText = 'SathaAI analysis of electrocardiogram, blood chemistry, and vital cardiac signs';
    } else if (pageId === 'kidney') {
        title.innerText = "Satha's Renal Function Diagnostic";
        subtitle.innerText = 'SathaAI analysis of glomerular filtration, electrolytes, and comorbidity factors';
    } else if (pageId === 'dashboard') {
        title.innerText = "Satha's Diagnostic Operations Center";
        subtitle.innerText = 'Track SathaAI model accuracies, pipeline telemetry, and local execution logs';
        loadDashboardStats();
    }
}

// Gather Form inputs into PredictionRequest JSON structure
function serializeForm(formId, diseaseType) {
    const form = document.getElementById(formId);
    const formData = new FormData(form);
    const requestData = { diseaseType: diseaseType };

    for (let [key, val] of formData.entries()) {
        requestData[key] = parseFloat(val);
    }

    return requestData;
}

// Submit Diagnostic form to API
async function submitPrediction(event, diseaseType) {
    event.preventDefault();
    showLoader(true);

    const formId = `form-${diseaseType}`;
    const requestBody = serializeForm(formId, diseaseType);

    try {
        const response = await fetch(`${API_BASE}/predict`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(requestBody)
        });

        if (!response.ok) {
            throw new Error(`API Error: ${response.statusText}`);
        }

        const data = await response.json();
        displayPredictionResults(diseaseType, data, requestBody);
        
        // Log in local storage history
        logPredictionToHistory(diseaseType, data.prediction, data.probability);

    } catch (err) {
        console.error("Diagnostic failure:", err);
        alert(`Diagnostic analysis failed. Please verify that the backend is running. Details: ${err.message}`);
    } finally {
        showLoader(false);
    }
}

// Display results and render Plotly XAI Horizontal Bar Chart
function displayPredictionResults(disease, responseData, inputData) {
    // Hide placeholder, show card
    const placeholder = document.getElementById(`result-placeholder-${disease}`);
    const resultCard = document.getElementById(`result-card-${disease}`);
    
    if (placeholder) placeholder.classList.add('hidden');
    if (resultCard) resultCard.classList.remove('hidden');

    // Risk Banner Configuration
    const banner = document.getElementById(`risk-banner-${disease}`);
    const bannerText = document.getElementById(`risk-text-${disease}`);
    const isHighRisk = responseData.prediction === 1;

    if (banner) {
        banner.className = `risk-banner ${isHighRisk ? 'high' : 'low'}`;
        bannerText.innerHTML = isHighRisk 
            ? '<i class="fa-solid fa-triangle-exclamation"></i> HIGH RISK DETECTED' 
            : '<i class="fa-solid fa-circle-check"></i> LOW RISK ASSESSMENT';
    }

    // Probability bar
    const probBar = document.getElementById(`prob-bar-${disease}`);
    const probVal = document.getElementById(`prob-val-${disease}`);
    const percentage = responseData.probability * 100;

    if (probBar) {
        probBar.style.width = `${percentage}%`;
        probBar.className = `prob-bar ${isHighRisk ? 'danger' : 'success'}`;
    }
    if (probVal) {
        probVal.innerText = `${percentage.toFixed(1)}%`;
    }

    // Render XAI Chart
    renderXaiChart(`chart-${disease}`, responseData.importance, disease);
}

// Render Plotly bar chart
function renderXaiChart(containerId, importance, diseaseName) {
    // Convert map to arrays, sort by magnitude
    const items = Object.entries(importance).map(([feature, val]) => ({ feature, val }));
    items.sort((a, b) => Math.abs(a.val) - Math.abs(b.val)); // Ascending for horizontal rendering (Plotly draws from bottom up)

    const featureNames = items.map(item => item.feature);
    const contributions = items.map(item => item.val);
    
    // RED (#ef4444) for risk enhancers (positive change), GREEN (#10b981) for risk reducers (negative change)
    const barColors = contributions.map(v => v > 0 ? '#ef4444' : '#10b981');

    const trace = {
        type: 'bar',
        x: contributions,
        y: featureNames,
        orientation: 'h',
        marker: {
            color: barColors,
            line: { width: 0.5, color: '#e2e8f0' }
        },
        hovertemplate: 'Feature: %{y}<br>Impact: %{x:+.3f}<extra></extra>'
    };

    const layout = {
        title: {
            text: `SathaAI Path Impact Decomposition (SHAP / Tree Interpreter)`,
            font: { family: 'Outfit, sans-serif', size: 12, color: '#64748b' }
        },
        xaxis: {
            title: 'Path Probability Change',
            zeroline: true,
            zerolinecolor: '#94a3b8',
            zerolinewidth: 1,
            gridcolor: '#f1f5f9',
            tickfont: { size: 10 }
        },
        yaxis: {
            automargin: true,
            tickfont: { size: 10 }
        },
        margin: { l: 120, r: 20, t: 35, b: 40 },
        height: 320,
        plot_bgcolor: '#ffffff',
        paper_bgcolor: '#ffffff'
    };

    const config = {
        displayModeBar: false,
        responsive: true
    };

    Plotly.newPlot(containerId, [trace], layout, config);
}

// Download PDF Report by submitting form parameters directly
async function downloadPdf(diseaseType) {
    showLoader(true);
    const formId = `form-${diseaseType}`;
    const requestBody = serializeForm(formId, diseaseType);

    try {
        const response = await fetch(`${API_BASE}/report`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(requestBody)
        });

        if (!response.ok) throw new Error("Could not generate report");

        const blob = await response.blob();
        
        // Generate download click
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        
        const timestamp = new Date().toISOString().slice(0, 19).replace(/[-T:]/g, "");
        a.download = `${diseaseType}_report_${timestamp}.pdf`;
        document.body.appendChild(a);
        a.click();
        
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);

    } catch (err) {
        console.error("PDF generation failed:", err);
        alert(`Failed to download report: ${err.message}`);
    } finally {
        showLoader(false);
    }
}

// Fetch dashboard telemetry from Spring Boot backend
async function loadDashboardStats() {
    try {
        const response = await fetch(`${API_BASE}/dashboard`);
        if (!response.ok) throw new Error("Dashboard API unavailable");

        const stats = await response.json();
        
        // Update model accuracy bars
        const diabetesAcc = stats.diabetesAccuracy || '0%';
        const heartAcc = stats.heartAccuracy || '0%';
        const kidneyAcc = stats.kidneyAccuracy || '0%';

        document.getElementById('acc-val-diabetes').innerText = diabetesAcc;
        document.getElementById('acc-bar-diabetes').style.width = diabetesAcc;

        document.getElementById('acc-val-heart').innerText = heartAcc;
        document.getElementById('acc-bar-heart').style.width = heartAcc;

        document.getElementById('acc-val-kidney').innerText = kidneyAcc;
        document.getElementById('acc-bar-kidney').style.width = kidneyAcc;

    } catch (err) {
        console.warn("Failed to load backend stats:", err);
    }
    
    // Update local history lists
    renderHistoryLog();
}

// Logging history in local storage
function logPredictionToHistory(disease, prediction, probability) {
    const history = JSON.parse(localStorage.getItem('pulse_history')) || [];
    const item = {
        timestamp: new Date().toLocaleString(),
        disease: disease.charAt(0).toUpperCase() + disease.slice(1),
        prediction: prediction,
        probability: probability
    };
    history.unshift(item); // Add to start
    localStorage.setItem('pulse_history', JSON.stringify(history));

    updateDashboardCounters();
}

function updateDashboardCounters() {
    const history = JSON.parse(localStorage.getItem('pulse_history')) || [];
    
    let highRiskCount = 0;
    history.forEach(item => {
        if (item.prediction === 1) highRiskCount++;
    });

    document.getElementById('stat-total-runs').innerText = history.length;
    document.getElementById('stat-high-risk').innerText = highRiskCount;
}

function renderHistoryLog() {
    const history = JSON.parse(localStorage.getItem('pulse_history')) || [];
    const tbody = document.getElementById('history-log-body');
    
    if (!tbody) return;
    
    if (history.length === 0) {
        tbody.innerHTML = `<tr><td colspan="4" class="text-center text-gray">No assessments performed in this session.</td></tr>`;
        return;
    }

    tbody.innerHTML = history.map(item => {
        const badgeClass = item.prediction === 1 ? 'high' : 'low';
        const label = item.prediction === 1 ? 'High Risk' : 'Low Risk';
        return `
            <tr>
                <td>${item.timestamp}</td>
                <td><strong>${item.disease}</strong></td>
                <td>${(item.probability * 100).toFixed(1)}%</td>
                <td><span class="history-badge ${badgeClass}">${label}</span></td>
            </tr>
        `;
    }).join('');
}

function clearHistory() {
    localStorage.removeItem('pulse_history');
    updateDashboardCounters();
    renderHistoryLog();
}

// Utility Loader Toggle
function showLoader(show) {
    const overlay = document.getElementById('loader-overlay');
    if (overlay) {
        if (show) overlay.classList.add('active');
        else overlay.classList.remove('active');
    }
}
