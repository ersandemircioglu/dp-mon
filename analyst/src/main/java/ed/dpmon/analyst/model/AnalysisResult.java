package ed.dpmon.analyst.model;

import lombok.Data;

@Data
public class AnalysisResult {

    private final ProductSummary productSummary;

    /**
     * Quality class between 0 (N/A) 1 (low) - AnalystService.NUM_OF_QUALITY_CLASS
     * (high)
     */
    private int qualityClass;

    /**
     * Processing time relative to T0 in minutes
     */
    private Long relativeTime;

    private boolean isActual;
}
