package com.disease.ml;

import java.io.Serializable;

public class Scaler implements Serializable {
    private static final long serialVersionUID = 1L;

    public double[] means;
    public double[] stds;

    public Scaler(double[] means, double[] stds) {
        this.means = means;
        this.stds = stds;
    }

    public double[] transform(double[] input) {
        double[] scaled = new double[input.length];
        for (int i = 0; i < input.length; i++) {
            if (stds[i] > 1e-9) {
                scaled[i] = (input[i] - means[i]) / stds[i];
            } else {
                scaled[i] = input[i] - means[i];
            }
        }
        return scaled;
    }
}
