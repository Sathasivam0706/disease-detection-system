package com.disease.dto;

public class PredictionRequest {
    private String diseaseType; // "diabetes", "heart", "kidney"

    // Diabetes fields
    private double pregnancies;
    private double glucose;
    private double bloodPressure;
    private double skinThickness;
    private double insulin;
    private double bmi;
    private double diabetesPedigreeFunction;
    private double age;

    // Heart fields
    private double age_h;
    private double sex;
    private double cp;
    private double trestbps;
    private double chol;
    private double fbs;
    private double restecg;
    private double thalach;
    private double exang;
    private double oldpeak;
    private double slope;
    private double ca;
    private double thal;

    // Kidney fields
    private double k_age;
    private double k_bp;
    private double k_sg;
    private double k_al;
    private double k_su;
    private double k_rbc;
    private double k_pc;
    private double k_pcc;
    private double k_ba;
    private double k_bgr;
    private double k_bu;
    private double k_sc;
    private double k_sod;
    private double k_pot;
    private double k_hemo;
    private double k_pcv;
    private double k_wc;
    private double k_rc;
    private double k_htn;
    private double k_dm;
    private double k_cad;
    private double k_appet;
    private double k_pe;
    private double k_ane;

    // Convert request fields to double array matching model expectation
    public double[] toDoubleArray() {
        if ("diabetes".equalsIgnoreCase(diseaseType)) {
            return new double[]{
                pregnancies, glucose, bloodPressure, skinThickness, insulin, bmi, diabetesPedigreeFunction, age
            };
        } else if ("heart".equalsIgnoreCase(diseaseType)) {
            return new double[]{
                age_h, sex, cp, trestbps, chol, fbs, restecg, thalach, exang, oldpeak, slope, ca, thal
            };
        } else if ("kidney".equalsIgnoreCase(diseaseType)) {
            return new double[]{
                k_age, k_bp, k_sg, k_al, k_su, k_rbc, k_pc, k_pcc, k_ba, k_bgr, k_bu, k_sc, k_sod, k_pot, k_hemo, k_pcv, k_wc, k_rc, k_htn, k_dm, k_cad, k_appet, k_pe, k_ane
            };
        }
        return new double[0];
    }

    // Getters and Setters
    public String getDiseaseType() { return diseaseType; }
    public void setDiseaseType(String diseaseType) { this.diseaseType = diseaseType; }

    public double getPregnancies() { return pregnancies; }
    public void setPregnancies(double pregnancies) { this.pregnancies = pregnancies; }

    public double getGlucose() { return glucose; }
    public void setGlucose(double glucose) { this.glucose = glucose; }

    public double getBloodPressure() { return bloodPressure; }
    public void setBloodPressure(double bloodPressure) { this.bloodPressure = bloodPressure; }

    public double getSkinThickness() { return skinThickness; }
    public void setSkinThickness(double skinThickness) { this.skinThickness = skinThickness; }

    public double getInsulin() { return insulin; }
    public void setInsulin(double insulin) { this.insulin = insulin; }

    public double getBmi() { return bmi; }
    public void setBmi(double bmi) { this.bmi = bmi; }

    public double getDiabetesPedigreeFunction() { return diabetesPedigreeFunction; }
    public void setDiabetesPedigreeFunction(double diabetesPedigreeFunction) { this.diabetesPedigreeFunction = diabetesPedigreeFunction; }

    public double getAge() { return age; }
    public void setAge(double age) { this.age = age; }

    public double getAge_h() { return age_h; }
    public void setAge_h(double age_h) { this.age_h = age_h; }

    public double getSex() { return sex; }
    public void setSex(double sex) { this.sex = sex; }

    public double getCp() { return cp; }
    public void setCp(double cp) { this.cp = cp; }

    public double getTrestbps() { return trestbps; }
    public void setTrestbps(double trestbps) { this.trestbps = trestbps; }

    public double getChol() { return chol; }
    public void setChol(double chol) { this.chol = chol; }

    public double getFbs() { return fbs; }
    public void setFbs(double fbs) { this.fbs = fbs; }

    public double getRestecg() { return restecg; }
    public void setRestecg(double restecg) { this.restecg = restecg; }

    public double getThalach() { return thalach; }
    public void setThalach(double thalach) { this.thalach = thalach; }

    public double getExang() { return exang; }
    public void setExang(double exang) { this.exang = exang; }

    public double getOldpeak() { return oldpeak; }
    public void setOldpeak(double oldpeak) { this.oldpeak = oldpeak; }

    public double getSlope() { return slope; }
    public void setSlope(double slope) { this.slope = slope; }

    public double getCa() { return ca; }
    public void setCa(double ca) { this.ca = ca; }

    public double getThal() { return thal; }
    public void setThal(double thal) { this.thal = thal; }

    public double getK_age() { return k_age; }
    public void setK_age(double k_age) { this.k_age = k_age; }

    public double getK_bp() { return k_bp; }
    public void setK_bp(double k_bp) { this.k_bp = k_bp; }

    public double getK_sg() { return k_sg; }
    public void setK_sg(double k_sg) { this.k_sg = k_sg; }

    public double getK_al() { return k_al; }
    public void setK_al(double k_al) { this.k_al = k_al; }

    public double getK_su() { return k_su; }
    public void setK_su(double k_su) { this.k_su = k_su; }

    public double getK_rbc() { return k_rbc; }
    public void setK_rbc(double k_rbc) { this.k_rbc = k_rbc; }

    public double getK_pc() { return k_pc; }
    public void setK_pc(double k_pc) { this.k_pc = k_pc; }

    public double getK_pcc() { return k_pcc; }
    public void setK_pcc(double k_pcc) { this.k_pcc = k_pcc; }

    public double getK_ba() { return k_ba; }
    public void setK_ba(double k_ba) { this.k_ba = k_ba; }

    public double getK_bgr() { return k_bgr; }
    public void setK_bgr(double k_bgr) { this.k_bgr = k_bgr; }

    public double getK_bu() { return k_bu; }
    public void setK_bu(double k_bu) { this.k_bu = k_bu; }

    public double getK_sc() { return k_sc; }
    public void setK_sc(double k_sc) { this.k_sc = k_sc; }

    public double getK_sod() { return k_sod; }
    public void setK_sod(double k_sod) { this.k_sod = k_sod; }

    public double getK_pot() { return k_pot; }
    public void setK_pot(double k_pot) { this.k_pot = k_pot; }

    public double getK_hemo() { return k_hemo; }
    public void setK_hemo(double k_hemo) { this.k_hemo = k_hemo; }

    public double getK_pcv() { return k_pcv; }
    public void setK_pcv(double k_pcv) { this.k_pcv = k_pcv; }

    public double getK_wc() { return k_wc; }
    public void setK_wc(double k_wc) { this.k_wc = k_wc; }

    public double getK_rc() { return k_rc; }
    public void setK_rc(double k_rc) { this.k_rc = k_rc; }

    public double getK_htn() { return k_htn; }
    public void setK_htn(double k_htn) { this.k_htn = k_htn; }

    public double getK_dm() { return k_dm; }
    public void setK_dm(double k_dm) { this.k_dm = k_dm; }

    public double getK_cad() { return k_cad; }
    public void setK_cad(double k_cad) { this.k_cad = k_cad; }

    public double getK_appet() { return k_appet; }
    public void setK_appet(double k_appet) { this.k_appet = k_appet; }

    public double getK_pe() { return k_pe; }
    public void setK_pe(double k_pe) { this.k_pe = k_pe; }

    public double getK_ane() { return k_ane; }
    public void setK_ane(double k_ane) { this.k_ane = k_ane; }
}
