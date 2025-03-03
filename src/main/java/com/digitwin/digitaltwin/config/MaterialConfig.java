package com.digitwin.digitaltwin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConfigurationProperties(prefix = "error-detection")
public class MaterialConfig {

    private Map<String, MaterialProfile> materialProfiles;

    public Map<String, MaterialProfile> getMaterialProfiles() {
        return materialProfiles;
    }

    public void setMaterialProfiles(Map<String, MaterialProfile> materialProfiles) {
        this.materialProfiles = materialProfiles;
    }

    public static class MaterialProfile {
        private int minTemp;
        private int maxTemp;

        public int getMinTemp() {
            return minTemp;
        }

        public void setMinTemp(int minTemp) {
            this.minTemp = minTemp;
        }

        public int getMaxTemp() {
            return maxTemp;
        }

        public void setMaxTemp(int maxTemp) {
            this.maxTemp = maxTemp;
        }
    }
}
