package ed.dpmon.analyst.model;

public enum QualityLabel {
    NA(0, 1, "#C0C0C0"),
    LOW(1, 4, "#FF0000"),
    MID(4, 8, "#FFFF00"),
    HIGH(8, 11, "#00FF00");

    private int minQuality;
    private int maxQuality;
    private String colorCode;

    private QualityLabel(int minQuality, int maxQuality, String colorCode) {
        this.minQuality = minQuality;
        this.maxQuality = maxQuality;
        this.colorCode = colorCode;
    }

    public String getColorCode() {
        return colorCode;
    }

    public static QualityLabel getInstance(int qualityClass) {
        if (qualityClass < 0) {
            return NA;
        }
        for (QualityLabel qualityLabel : values()) {
            if (qualityLabel.minQuality <= qualityClass && qualityClass < qualityLabel.maxQuality) {
                return qualityLabel;
            }
        }
        return HIGH; // (qualityClass > 10)
    }
}
