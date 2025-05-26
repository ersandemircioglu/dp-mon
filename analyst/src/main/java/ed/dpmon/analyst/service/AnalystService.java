package ed.dpmon.analyst.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import ed.dpmon.analyst.model.AnalysisResult;
import ed.dpmon.analyst.model.ProductSummary;

@Service
public class AnalystService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private Long time_t0 = null;
    private List<AnalysisResult> list = new ArrayList<AnalysisResult>();

    private Map<String, AnalysisResult> currentProducts = new HashMap<String, AnalysisResult>();

    public List<AnalysisResult> fetch() {
        return list;
    }

    public int inject(List<ProductSummary> productSummaries) {
        list.addAll(productSummaries.stream().map(p -> analyse(p)).toList());
        return productSummaries.size();
    }

    public int append(ProductSummary productSummary) {
        AnalysisResult analysisResult = analyse(productSummary);
        list.add(analysisResult);
        messagingTemplate.convertAndSend("/topic/data", analysisResult);
        return 1;
    }

    private AnalysisResult analyse(ProductSummary productSummary) {
        AnalysisResult analysisResult = new AnalysisResult();
        analysisResult.setProcessing_time(productSummary.getProcessing_time());
        analysisResult.setName(productSummary.getName());
        analysisResult.setFilename(productSummary.getFilename());
        analysisResult.setQuality(productSummary.getQuality());
        analysisResult.setActual(true);

        LocalDateTime processingDateTime = LocalDateTime.parse(productSummary.getProcessing_time());

        Long processingDateTimeInSeconds = (processingDateTime.toEpochSecond(ZoneOffset.UTC));

        if (time_t0 == null) {
            time_t0 = processingDateTimeInSeconds;
        }

        long relativeTime = (processingDateTimeInSeconds - time_t0) / 60;
        analysisResult.setRelativeTime(relativeTime);

        if (productSummary.getInputs() != null) {
            Long timeliness = relativeTime
                    - currentProducts.get(productSummary.getInputs().getFirst().getName()).getRelativeTime();
            analysisResult.setTimeliness(timeliness);
        }

        currentProducts.put(productSummary.getName(), analysisResult);
        return analysisResult;
    }
}
