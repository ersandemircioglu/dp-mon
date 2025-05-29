package ed.dpmon.analyst.model;

import lombok.Data;

@Data
public class ProductSummary {
    private String processing_time;
    private String name;
    private String filename;
    private Boolean isProduct;
    private float quality;
    // private List<ProductSummary> inputs;

    public boolean isProduct() {
        return this.isProduct.booleanValue();
    }
}
