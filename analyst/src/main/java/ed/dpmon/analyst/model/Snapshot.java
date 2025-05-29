package ed.dpmon.analyst.model;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;

@Data
public class Snapshot {
    private final AnalysisResult analysisResult;
    private Map<String, Long> features = new HashMap<String, Long>();

    public Snapshot(AnalysisResult analysisResult, int numOfQualityClass) {
        this.analysisResult = analysisResult;
    }

}
