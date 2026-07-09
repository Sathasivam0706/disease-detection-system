package com.disease.util;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

public class ReportGenerator {

    public static byte[] generateReport(
            String disease, 
            int prediction, 
            double probability, 
            Map<String, Double> importance, 
            double[] inputData, 
            List<String> featureNames) {
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Set up fonts
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, new Color(30, 136, 229));
            
            Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA, 12, Color.GRAY);
            Font sectionHeadingFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(55, 71, 79));
            Font boldTextFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
            Font regularTextFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
            Font listTextFont = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(33, 33, 33));

            // Title
            Paragraph title = new Paragraph("Satha0706AI Disease Detection Clinical Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(5);
            document.add(title);

            // Subtitle
            Paragraph subtitle = new Paragraph("System: Satha0706AI Ensemble Machine Learning + Explainable AI (XAI)", subTitleFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(20);
            document.add(subtitle);

            // Metadata: Disease and Date
            Paragraph meta = new Paragraph();
            meta.add(new Chunk("Disease Analyzed: ", boldTextFont));
            meta.add(new Chunk(disease.toUpperCase() + "\n", regularTextFont));
            meta.add(new Chunk("Date Generated: ", boldTextFont));
            meta.add(new Chunk(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n", regularTextFont));
            meta.setSpacingAfter(20);
            document.add(meta);

            // Results Section
            Paragraph resultHeader = new Paragraph("DIAGNOSTIC ASSESSMENT", sectionHeadingFont);
            resultHeader.setSpacingAfter(10);
            document.add(resultHeader);

            Paragraph resultPara = new Paragraph();
            resultPara.add(new Chunk("Risk Level: ", boldTextFont));
            
            Font riskFont;
            String riskText;
            if (prediction == 1) {
                riskText = "HIGH RISK";
                riskFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(231, 76, 60)); // Soft Red
            } else {
                riskText = "LOW RISK";
                riskFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(46, 204, 113)); // Soft Green
            }
            resultPara.add(new Chunk(riskText + "\n", riskFont));
            
            resultPara.add(new Chunk("Risk Probability: ", boldTextFont));
            resultPara.add(new Chunk(String.format(Locale.US, "%.1f%%\n", probability * 100), regularTextFont));
            resultPara.setSpacingAfter(20);
            document.add(resultPara);

            // Top Contributing Factors (XAI)
            Paragraph xaiHeader = new Paragraph("Top Contributing Risk Factors (XAI Analysis):", sectionHeadingFont);
            xaiHeader.setSpacingAfter(10);
            document.add(xaiHeader);

            // Sort factors by magnitude (absolute values)
            List<Map.Entry<String, Double>> factors = new ArrayList<>(importance.entrySet());
            factors.sort((a, b) -> Double.compare(Math.abs(b.getValue()), Math.abs(a.getValue())));

            com.lowagie.text.List list = new com.lowagie.text.List(com.lowagie.text.List.UNORDERED);
            int count = 0;
            for (Map.Entry<String, Double> entry : factors) {
                if (count >= 5) break;
                double val = entry.getValue();
                String direction = val > 0 ? "increases risk" : "decreases risk";
                String bulletText = String.format("%s (Impact: %s%.3f, which %s)", 
                        entry.getKey(), val > 0 ? "+" : "", val, direction);
                ListItem item = new ListItem(bulletText, listTextFont);
                list.add(item);
                count++;
            }
           // list.setSpacingAfter(25);
            document.add(list);

            // Patient Data Table
            Paragraph tableHeader = new Paragraph("SUBMITTED PATIENT PARAMETERS", sectionHeadingFont);
            tableHeader.setSpacingAfter(10);
            document.add(tableHeader);

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setSpacingBefore(5);
            table.setSpacingAfter(15);
            
            // Set widths
            float[] columnWidths = {50f, 50f};
            table.setWidths(columnWidths);

            // Table Header Cells
            PdfPCell cell1 = new PdfPCell(new Paragraph("Clinical Metric", boldTextFont));
            cell1.setBackgroundColor(new Color(227, 242, 253)); // Soft blue
            cell1.setPadding(6);
            cell1.setHorizontalAlignment(Element.ALIGN_CENTER);
            
            PdfPCell cell2 = new PdfPCell(new Paragraph("Submitted Value", boldTextFont));
            cell2.setBackgroundColor(new Color(227, 242, 253)); // Soft blue
            cell2.setPadding(6);
            cell2.setHorizontalAlignment(Element.ALIGN_CENTER);

            table.addCell(cell1);
            table.addCell(cell2);

            // Add clinical data
            Color altRowColor = new Color(245, 249, 255); // Alternating light blue
            Color whiteRowColor = Color.WHITE;

            for (int i = 0; i < featureNames.size(); i++) {
                String featureName = featureNames.get(i);
                double val = inputData[i];
                
                String valStr;
                // Format nicely (integers as int, decimals as decimal, map kidney categoricals back to labels)
                if ("kidney".equalsIgnoreCase(disease)) {
                    valStr = formatKidneyFeatureValue(featureName, val);
                } else if ("heart".equalsIgnoreCase(disease)) {
                    valStr = formatHeartFeatureValue(featureName, val);
                } else {
                    valStr = (val == (long) val) ? String.format("%d", (long) val) : String.format(Locale.US, "%.2f", val);
                }

                PdfPCell nameCell = new PdfPCell(new Paragraph(featureName, regularTextFont));
                nameCell.setPadding(5);
                
                PdfPCell valCell = new PdfPCell(new Paragraph(valStr, regularTextFont));
                valCell.setPadding(5);
                valCell.setHorizontalAlignment(Element.ALIGN_CENTER);

                // Alternating row background
                Color rowBg = (i % 2 == 1) ? altRowColor : whiteRowColor;
                nameCell.setBackgroundColor(rowBg);
                valCell.setBackgroundColor(rowBg);

                table.addCell(nameCell);
                table.addCell(valCell);
            }

            document.add(table);

            // Footer / Disclaimer
            Paragraph disclaimer = new Paragraph(
                "\nDisclaimer: This clinical report was generated by the Satha0706AI diagnostic support system. " +
                "It is designed to serve as an analytical reference. Predictions are based on statistical probability distributions and " +
                "must be reviewed by qualified medical professionals for final diagnosis.",
                FontFactory.getFont(FontFactory.HELVETICA, 8, Font.ITALIC, Color.DARK_GRAY)
            );
            disclaimer.setAlignment(Element.ALIGN_JUSTIFIED);
            document.add(disclaimer);

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return out.toByteArray();
    }

    private static String formatKidneyFeatureValue(String col, double val) {
        switch (col.toLowerCase()) {
            case "rbc":
            case "pc":
                return val == 1.0 ? "Normal" : (val == 0.0 ? "Abnormal" : "N/A");
            case "pcc":
            case "ba":
                return val == 1.0 ? "Present" : (val == 0.0 ? "Not Present" : "N/A");
            case "htn":
            case "dm":
            case "cad":
            case "pe":
            case "ane":
                return val == 1.0 ? "Yes" : (val == 0.0 ? "No" : "N/A");
            case "appet":
                return val == 1.0 ? "Good" : (val == 0.0 ? "Poor" : "N/A");
            case "sg":
                return String.format(Locale.US, "%.3f", val);
            default:
                return (val == (long) val) ? String.format("%d", (long) val) : String.format(Locale.US, "%.1f", val);
        }
    }

    private static String formatHeartFeatureValue(String col, double val) {
        switch (col.toLowerCase()) {
            case "sex":
                return val == 1.0 ? "Male" : "Female";
            case "cp":
                int cpVal = (int) val;
                if (cpVal == 0) return "Typical Angina";
                if (cpVal == 1) return "Atypical Angina";
                if (cpVal == 2) return "Non-Anginal";
                return "Asymptomatic";
            case "fbs":
                return val == 1.0 ? "Yes (>120 mg/dL)" : "No";
            case "restecg":
                int ecgVal = (int) val;
                if (ecgVal == 0) return "Normal";
                if (ecgVal == 1) return "ST-T Abnormality";
                return "LV Hypertrophy";
            case "exang":
                return val == 1.0 ? "Yes" : "No";
            case "slope":
                int slopeVal = (int) val;
                if (slopeVal == 0) return "Upsloping";
                if (slopeVal == 1) return "Flat";
                return "Downsloping";
            case "thal":
                int thalVal = (int) val;
                if (thalVal == 1) return "Fixed Defect";
                if (thalVal == 2) return "Normal";
                if (thalVal == 3) return "Reversible Defect";
                return "Unknown";
            default:
                return (val == (long) val) ? String.format("%d", (long) val) : String.format(Locale.US, "%.1f", val);
        }
    }
}
