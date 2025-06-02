package ed.dpmon.analyst.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import ed.dpmon.analyst.model.AnalysisResult;
import ed.dpmon.analyst.model.AnnotatedFeature;
import ed.dpmon.analyst.model.Feature;
import ed.dpmon.analyst.util.NeuralNetwork;

@Service
public class AnalystService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private static final int NUM_OF_QUALITY_CLASS = 10;

    private Long time_t0 = null;
    private List<AnnotatedFeature> annotatedFeatures = new ArrayList<AnnotatedFeature>();
    private Map<String, AnnotatedFeature> productCache = new HashMap<String, AnnotatedFeature>();

    private LinkedHashSet<String> productInsertionOrder = new LinkedHashSet<String>();
    private Map<String, List<AnalysisResult>> trainingData = new HashMap<String, List<AnalysisResult>>();
    private Map<String, NeuralNetwork> nnMap = new HashMap<String, NeuralNetwork>();

    public List<AnnotatedFeature> fetch() {
        return annotatedFeatures;
    }

    public int inject(List<Feature> features) {
        for (Feature feature : features) {
            AnnotatedFeature annotatedFeature = annotate(feature);
            productInsertionOrder.add(annotatedFeature.getFeature().getName());
            if (feature.isProduct()) {
                AnalysisResult analysisResult = createAnalysisResult(annotatedFeature);
                if (!trainingData.containsKey(feature.getName())) {
                    trainingData.put(feature.getName(), new ArrayList<AnalysisResult>());
                }
                trainingData.get(feature.getName()).add(analysisResult);
            }
        }
        generateNNs();
        System.out.println(AnalysisResult.getHeaderStr(productInsertionOrder));

        return features.size();
    }

    private void generateNNs() {
        for (String productName : trainingData.keySet()) {
            NeuralNetwork nn = new NeuralNetwork(productInsertionOrder, NUM_OF_QUALITY_CLASS + 1);
            nn.train(trainingData.get(productName));
            nnMap.put(productName, nn);
        }
    }

    public int append(Feature feature) {
        AnnotatedFeature annotatedFeature = annotate(feature);
        annotatedFeatures.add(annotatedFeature);
        if (feature.isProduct()) {
            AnalysisResult analysisResult = createAnalysisResult(annotatedFeature);
            nnMap.get(feature.getName()).test(analysisResult);
            System.out.println(analysisResult.getPrintStr(productInsertionOrder));
        }
        messagingTemplate.convertAndSend("/topic/data", annotatedFeature);
        return 1;
    }

    private AnnotatedFeature annotate(Feature feature) {
        AnnotatedFeature output = new AnnotatedFeature(feature);

        LocalDateTime processingDateTime = LocalDateTime.parse(feature.getProcessing_time());
        long relativeTimeInMinutes = calculateRelativeTime(processingDateTime);
        output.setRelativeTime(relativeTimeInMinutes);

        annotatedFeatures.add(output);
        productCache.put(output.getFeature().getName(), output);
        return output;
    }

    private AnalysisResult createAnalysisResult(AnnotatedFeature annotatedFeature) {
        AnalysisResult output = new AnalysisResult(annotatedFeature);
        AnalysisResult analysisResult = (AnalysisResult) output;
        int qualityClass = calculateQualityClass(annotatedFeature.getFeature().getQuality());
        analysisResult.setQualityClass(qualityClass);
        for (AnnotatedFeature cachedAnnotatedFeature : productCache.values()) {
            long diff = analysisResult.getAnnotatedFeature().getRelativeTime()
                    - cachedAnnotatedFeature.getRelativeTime();
            if (diff > 0) { // the product from cache was generated before this product and may affect this
                            // product
                analysisResult.getSnapshot().put(cachedAnnotatedFeature.getFeature().getName(), diff);
            }
        }
        return output;
    }

    /**
     * Converts quality value to quality class
     * 
     * @param Quality value between 0.0 (low and/or N/A) - 1.0 (high)
     * @return Quality class between 0 (N/A) 1 (low) - NUM_OF_QUALITY_CLASS (high)
     */
    private int calculateQualityClass(float quality) {
        int qualityClass = (int) (quality * NUM_OF_QUALITY_CLASS);
        if (qualityClass == 0 && quality > 0) {
            qualityClass = 1;
        }
        if (qualityClass > 10) {
            qualityClass = 10;
        }
        return qualityClass;
    }

    /**
     * Converts datetime object into minutes since T0. It also assignes T0 during
     * the first call
     * 
     * @param processingDateTime
     * @return Time relative to T0 in minutes
     */
    private long calculateRelativeTime(LocalDateTime processingDateTime) {
        Long processingDateTimeInSeconds = (processingDateTime.toEpochSecond(ZoneOffset.UTC));
        if (time_t0 == null) {
            time_t0 = processingDateTimeInSeconds;
        }
        return (processingDateTimeInSeconds - time_t0) / 60;
    }
}
