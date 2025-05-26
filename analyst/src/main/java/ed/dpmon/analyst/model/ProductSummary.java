package ed.dpmon.analyst.model;

import java.util.List;

import lombok.Data;

@Data
public class ProductSummary {
    String processing_time;
    String name;
    String filename;
    Float quality;
    List<ProductSummary> inputs;
}
