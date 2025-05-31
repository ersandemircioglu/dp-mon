package ed.dpmon.analyst.model;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;

@Data
public class Snapshot {
    private Long productGenerationTime;
    private int qualityClass;

    private final Map<String, Long> features = new HashMap<String, Long>();
}
