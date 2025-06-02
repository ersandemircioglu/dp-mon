package ed.dpmon.analyst.model;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import lombok.Data;

@Data
public class AnalysisResult {

    private final AnnotatedFeature annotatedFeature;

    private final Map<String, Long> snapshot = new HashMap<String, Long>();

    /**
     * Quality class between 0 (N/A) 1 (low) - AnalystService.NUM_OF_QUALITY_CLASS
     * (high)
     */
    private int qualityClass;

    /**
     * Quality class between 0 (N/A) 1 (low) - AnalystService.NUM_OF_QUALITY_CLASS
     * (high)
     */
    private int estimatedQualityClass;

    public static final String getHeaderStr(LinkedHashSet<String> featureOrder) {
        StringBuilder sb = new StringBuilder()
                .append("Product Name").append("\t");
        for (String featureName : featureOrder) {
            sb.append(featureName).append("\t");
        }
        sb.append("Actual Quality").append("\t");
        sb.append("Actual Quality Class").append("\t");
        sb.append("Estimated Quality Class").append("\t");
        sb.append("Result");
        return sb.toString();
    }

    public String getPrintStr(LinkedHashSet<String> featureOrder) {
        StringBuilder sb = new StringBuilder()
                .append(annotatedFeature.getFeature().getName()).append("\t");
        for (String featureName : featureOrder) {
            Long diff = snapshot.get(featureName);
            if (diff != null && diff > 0) {
                sb.append(diff).append("\t");
            } else {
                sb.append("N/A").append("\t");
            }
        }
        sb.append(annotatedFeature.getFeature().getQuality()).append("\t");
        sb.append(qualityClass).append("\t");
        sb.append(estimatedQualityClass).append("\t");
        sb.append(qualityClass == estimatedQualityClass);
        return sb.toString();
    }

}
