package md.hashcode.vector9.enrichment;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ExtractedAdDetails(
        long adId,
        String url,
        String title,
        String body,
        String sourceType,
        List<ExtractedAttributeGroup> groupedAttributes,
        Map<String, Object> normalizedAttributes,
        Map<String, Object> location,
        Map<String, Object> contactInfo,
        long scrapeDurationMs
) {

    public Map<String, Object> characteristicsPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sourceType", sourceType);
        payload.put("adId", adId);
        payload.put("url", url);
        payload.put("title", title);
        payload.put("body", body);
        payload.put("groupedAttributes", groupedAttributes);
        payload.put("normalized", normalizedAttributes);
        return payload;
    }
}
