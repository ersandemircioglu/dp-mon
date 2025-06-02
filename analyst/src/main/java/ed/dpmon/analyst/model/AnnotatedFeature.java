package ed.dpmon.analyst.model;

import lombok.Data;

@Data
public class AnnotatedFeature {

    private final Feature feature;

    /**
     * Processing time relative to T0 in minutes
     */
    private Long relativeTime;

}
