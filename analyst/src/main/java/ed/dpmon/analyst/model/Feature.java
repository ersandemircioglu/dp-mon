package ed.dpmon.analyst.model;

import lombok.Data;

@Data
public class Feature {
    private String processing_time;
    private String name;
    private String filename;
    private Boolean isProduct;
    private float quality;
    // private List<Feature> inputs;

    public boolean isProduct() {
        return this.isProduct.booleanValue();
    }
}
