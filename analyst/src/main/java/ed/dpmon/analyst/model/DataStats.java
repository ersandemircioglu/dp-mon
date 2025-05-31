package ed.dpmon.analyst.model;

import lombok.Data;

@Data
public class DataStats {

    public static final int MINUS_2_SD = 0;
    public static final int MINUS_1_SD = 1;
    public static final int MEAN = 2;
    public static final int PLUS_1_SD = 3;
    public static final int PLUS_2_SD = 4;

    private final long mean;
    private final long standartDeviation;

    private long[] distribution = new long[5];
    private QualityLabel[] qualityLabels = new QualityLabel[5];

    public DataStats(long mean, long standartDeviation) {
        this.mean = mean;
        this.standartDeviation = standartDeviation;
        distribution[MINUS_2_SD] = mean - (2 * standartDeviation);
        distribution[MINUS_1_SD] = mean - (1 * standartDeviation);
        distribution[MEAN] = mean;
        distribution[PLUS_1_SD] = mean + (1 * standartDeviation);
        distribution[PLUS_2_SD] = mean + (2 * standartDeviation);
    }

}
