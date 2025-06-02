package ed.dpmon.analyst.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import ed.dpmon.analyst.AnalystApplication;
import ed.dpmon.analyst.model.AnnotatedFeature;
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

    @GetMapping("/fetch")
    public List<AnnotatedFeature> fetch() {
        return analystService.fetch();
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
