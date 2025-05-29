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

import ed.dpmon.analyst.WebSocketConfig;
import ed.dpmon.analyst.model.AnalysisResult;
import ed.dpmon.analyst.model.ProductSummary;
import ed.dpmon.analyst.model.Snapshot;
import ed.dpmon.analyst.util.NeuralNetwork;

@Service
public class AnalystService {

    private final WebSocketConfig webSocketConfig;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private static final int NUM_OF_QUALITY_CLASS = 10;

    private Long time_t0 = null;
    private List<AnalysisResult> analysisResults = new ArrayList<AnalysisResult>();
    private Map<String, AnalysisResult> productCache = new HashMap<String, AnalysisResult>();

    private LinkedHashSet<String> productInsertionOrder = new LinkedHashSet<String>();
    private Map<String, List<Snapshot>> trainingData = new HashMap<String, List<Snapshot>>();
    private Map<String, NeuralNetwork> nnMap = new HashMap<String, NeuralNetwork>();

    AnalystService(WebSocketConfig webSocketConfig) {
        this.webSocketConfig = webSocketConfig;
    }

    public List<AnalysisResult> fetch() {
        return analysisResults;
    }

    public int inject(List<ProductSummary> productSummaries) {
        for (ProductSummary productSummary : productSummaries) {
            AnalysisResult analysisResult = analyse(productSummary);
            analysisResults.add(analysisResult);
            productInsertionOrder.add(analysisResult.getProductSummary().getName());

            if (analysisResult.getProductSummary().isProduct()) {
                Snapshot snapshot = createSnapshot(analysisResult);
                if (!trainingData.containsKey(analysisResult.getProductSummary().getName())) {
                    trainingData.put(analysisResult.getProductSummary().getName(), new ArrayList<Snapshot>());
                }
                trainingData.get(analysisResult.getProductSummary().getName()).add(snapshot);
            }
        }
        generateNNs();
        return productSummaries.size();
    }

    private Snapshot createSnapshot(AnalysisResult analysisResult) {
        Snapshot snapshot = new Snapshot(analysisResult, NUM_OF_QUALITY_CLASS);
        for (AnalysisResult analysisResultFromCache : productCache.values()) {
            long diff = analysisResult.getRelativeTime() - analysisResultFromCache.getRelativeTime();
            if (diff > 0) { // the product from cache was generated before this product and may affect this
                            // product
                snapshot.getFeatures().put(analysisResultFromCache.getProductSummary().getName(), diff);
            }
        }
        return snapshot;
    }

    private void generateNNs() {
        for (String productName : trainingData.keySet()) {
            NeuralNetwork nn = new NeuralNetwork(productInsertionOrder, NUM_OF_QUALITY_CLASS + 1);
            nn.train(trainingData.get(productName));
            nnMap.put(productName, nn);
        }
    }

    public int append(ProductSummary productSummary) {
        AnalysisResult analysisResult = analyse(productSummary);
        analysisResults.add(analysisResult);
        if (analysisResult.getProductSummary().isProduct()) {
            Snapshot snapshot = createSnapshot(analysisResult);
            nnMap.get(analysisResult.getProductSummary().getName()).test(snapshot);
        }
        messagingTemplate.convertAndSend("/topic/data", analysisResult);
        return 1;
    }

    private AnalysisResult analyse(ProductSummary productSummary) {
        AnalysisResult analysisResult = createAnalysisResult(productSummary);
        productCache.put(analysisResult.getProductSummary().getName(), analysisResult);
        return analysisResult;
    }

    private AnalysisResult createAnalysisResult(ProductSummary productSummary) {
        AnalysisResult analysisResult = new AnalysisResult(productSummary);

        int qualityClass = calculateQualityClass(productSummary.getQuality());
        analysisResult.setQualityClass(qualityClass);

        analysisResult.setActual(true);

        LocalDateTime processingDateTime = LocalDateTime.parse(productSummary.getProcessing_time());
        long relativeTimeInMinutes = calculateRelativeTime(processingDateTime);
        analysisResult.setRelativeTime(relativeTimeInMinutes);
        return analysisResult;
    }

    /**
     * Adds the Analysis Result to the cache which stores latest AnalysisResult
     * instance of each product type
     * 
     * @param analysisResult
     * 
     */
    private void addToCache(AnalysisResult analysisResult) {

    }

    /**
     * Converts quality value to quality class
     * 
     * @param Quality value between 0.0 (low or N/A) - 1.0 (high)
     * @return Quality class between 0 (low or N/A) - NUM_OF_QUALITY_CLASS (high)
     */
    private int calculateQualityClass(float quality) {
        int qualityClass = (int) (quality * NUM_OF_QUALITY_CLASS);
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
