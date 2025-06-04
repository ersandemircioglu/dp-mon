package ed.dpmon.analyst.controller;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import ed.dpmon.analyst.AnalystApplication;
import ed.dpmon.analyst.model.AnalysisResult;
import ed.dpmon.analyst.model.Feature;
import ed.dpmon.analyst.service.AnalystService;

@RestController
public class AnalystController {

    private final AnalystApplication analystApplication;

    @Autowired
    private AnalystService analystService;

    AnalystController(AnalystApplication analystApplication) {
        this.analystApplication = analystApplication;
    }

    @GetMapping("/get/products")
    public Set<String> getProductNames() {
        return analystService.getProductNames();
    }

    @GetMapping("/get/hist/{productName}")
    public List<AnalysisResult> getHistoricalData(@PathVariable("productName") String productName) {
        return analystService.getHistoricalData(productName);
    }

    @GetMapping("/get/live/{productName}")
    public List<AnalysisResult> getLiveData(@PathVariable("productName") String productName) {
        return analystService.getLiveData(productName);
    }

    @PostMapping("/inject")
    public int inject(@RequestBody List<Feature> features) {
        return analystService.inject(features);
    }

    @PatchMapping("/append")
    public int append(@RequestBody Feature feature) {
        return analystService.append(feature);
    }

}
