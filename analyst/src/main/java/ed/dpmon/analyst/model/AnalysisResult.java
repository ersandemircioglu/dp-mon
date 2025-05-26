package ed.dpmon.analyst.model;

import lombok.Data;

@Data
public class AnalysisResult {

    String processing_time;
    String name;
    String filename;
    Float quality;

    Long relativeTime;

    Long timeliness;
    boolean isActual;
}
