package md.hashcode.vector9.enrichment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class AdDetailExtractor {

    private static final List<String> AD_VIEW_MARKERS = List.of("\\\"adView\\\":", "\"adView\":");
    private static final Map<Long, String> FEATURE_KEYS = Map.ofEntries(
            Map.entry(1L, "offer_type"),
            Map.entry(7L, "region"),
            Map.entry(593L, "condition"),
            Map.entry(675L, "cpu_series"),
            Map.entry(681L, "operating_system"),
            Map.entry(795L, "seller_type"),
            Map.entry(1244L, "ram"),
            Map.entry(2285L, "cpu_model"),
            Map.entry(2289L, "power_watts"),
            Map.entry(2295L, "storage_capacity")
    );
    private static final Map<String, String> CONTROL_KEYS = Map.ofEntries(
            Map.entry("Tipul ofertei", "offer_type"),
            Map.entry("Regiune", "region"),
            Map.entry("Stare", "condition"),
            Map.entry("Autorul anun\u021Bulu", "seller_type"),
            Map.entry("Autorul anun\u021Bului", "seller_type"),
            Map.entry("Serie procesor", "cpu_series"),
            Map.entry("Model procesor", "cpu_model"),
            Map.entry("Memorie RAM", "ram"),
            Map.entry("Capacitate de stocare", "storage_capacity"),
            Map.entry("Sistem de operare", "operating_system"),
            Map.entry("Sursa de alimentare", "power_watts"),
            Map.entry("Surs\u0103 de alimentare", "power_watts")
    );

    private final ObjectMapper objectMapper;

    public AdDetailExtractor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ExtractedAdDetails extract(String url, String html, long scrapeDurationMs) {
        Document document = Jsoup.parse(html, url);
        JsonNode adView = extractEmbeddedAdView(html);

        long adId = adView.path("id").asLong();
        String title = textOrFallback(adView.path("title"), document.title());
        String body = extractTranslatedText(adView.path("body").path("value"));
        List<ExtractedAttributeGroup> groupedAttributes = extractGroups(adView.path("groups"));
        Map<String, Object> normalizedAttributes = normalizeAttributes(groupedAttributes, adView);
        Map<String, Object> location = buildLocation(adView);
        Map<String, Object> contactInfo = buildContactInfo(adView);

        return new ExtractedAdDetails(
                adId,
                url,
                title,
                body,
                "embedded_page_state",
                groupedAttributes,
                normalizedAttributes,
                location,
                contactInfo,
                scrapeDurationMs
        );
    }

    private JsonNode extractEmbeddedAdView(String html) {
        int markerIndex = findMarkerIndex(html);
        if (markerIndex < 0) {
            throw new EnrichmentException("Embedded adView payload was not found in ad HTML", false);
        }

        int markerLength = markerLengthAt(html, markerIndex);
        int objectStart = html.indexOf('{', markerIndex + markerLength);
        if (objectStart < 0) {
            throw new EnrichmentException("Embedded adView payload start was not found in ad HTML", false);
        }

        String escapedObject = extractBalancedObject(html, objectStart);
        String decodedObject = decodeJavascriptEscapes(escapedObject);

        try {
            return objectMapper.readTree(decodedObject);
        } catch (Exception exception) {
            throw new EnrichmentException("Embedded adView payload could not be parsed", exception, false);
        }
    }

    private int findMarkerIndex(String html) {
        for (String marker : AD_VIEW_MARKERS) {
            int markerIndex = html.indexOf(marker);
            if (markerIndex >= 0) {
                return markerIndex;
            }
        }
        return -1;
    }

    private int markerLengthAt(String html, int markerIndex) {
        for (String marker : AD_VIEW_MARKERS) {
            if (html.startsWith(marker, markerIndex)) {
                return marker.length();
            }
        }
        throw new EnrichmentException("Embedded adView payload marker could not be resolved", false);
    }

    private String extractBalancedObject(String input, int objectStart) {
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;

        for (int index = objectStart; index < input.length(); index++) {
            char current = input.charAt(index);

            if (escaped) {
                escaped = false;
                continue;
            }

            if (current == '\\') {
                escaped = true;
                continue;
            }

            if (current == '"') {
                inString = !inString;
                continue;
            }

            if (inString) {
                continue;
            }

            if (current == '{') {
                depth++;
            } else if (current == '}') {
                depth--;
                if (depth == 0) {
                    return input.substring(objectStart, index + 1);
                }
            }
        }

        throw new EnrichmentException("Embedded adView payload end was not found in ad HTML", false);
    }

    private String decodeJavascriptEscapes(String input) {
        StringBuilder decoded = new StringBuilder(input.length());
        for (int index = 0; index < input.length(); index++) {
            char current = input.charAt(index);
            if (current != '\\') {
                decoded.append(current);
                continue;
            }

            if (index + 1 >= input.length()) {
                decoded.append(current);
                continue;
            }

            char escaped = input.charAt(++index);
            switch (escaped) {
                case '"' -> decoded.append('"');
                case '\\' -> decoded.append('\\');
                case '/' -> decoded.append('/');
                case 'b' -> decoded.append('\b');
                case 'f' -> decoded.append('\f');
                case 'n' -> decoded.append('\n');
                case 'r' -> decoded.append('\r');
                case 't' -> decoded.append('\t');
                case 'u' -> {
                    if (index + 4 >= input.length()) {
                        throw new EnrichmentException("Embedded adView payload contains invalid unicode escape", false);
                    }
                    String hex = input.substring(index + 1, index + 5);
                    decoded.append((char) Integer.parseInt(hex, 16));
                    index += 4;
                }
                default -> decoded.append(escaped);
            }
        }
        return decoded.toString();
    }

    private List<ExtractedAttributeGroup> extractGroups(JsonNode groupsNode) {
        List<ExtractedAttributeGroup> groups = new ArrayList<>();
        if (!groupsNode.isArray()) {
            return groups;
        }

        for (JsonNode groupNode : groupsNode) {
            String groupTitle = groupNode.path("title").asText();
            List<ExtractedAttribute> attributes = new ArrayList<>();
            for (JsonNode controlNode : groupNode.path("controls")) {
                JsonNode featureNode = controlNode.path("feature");
                attributes.add(new ExtractedAttribute(
                        controlNode.path("title").asText(),
                        controlNode.path("type").asText(),
                        featureNode.path("id").isMissingNode() ? null : featureNode.path("id").asLong(),
                        emptyToNull(featureNode.path("type").asText()),
                        extractDisplayValue(featureNode.path("value")),
                        toPlainValue(featureNode.path("value"))
                ));
            }
            groups.add(new ExtractedAttributeGroup(groupTitle, List.copyOf(attributes)));
        }

        return List.copyOf(groups);
    }

    private Map<String, Object> normalizeAttributes(List<ExtractedAttributeGroup> groups, JsonNode adView) {
        Map<String, Object> normalized = new LinkedHashMap<>();

        for (ExtractedAttributeGroup group : groups) {
            for (ExtractedAttribute attribute : group.attributes()) {
                String key = normalizeKey(attribute);
                if (key == null) {
                    continue;
                }

                if ("power_watts".equals(key) && attribute.rawValue() instanceof Map<?, ?> rawMap) {
                    Object numericValue = rawMap.get("value");
                    if (numericValue != null) {
                        normalized.put(key, numericValue);
                    }
                    continue;
                }

                if (attribute.translatedValue() != null && !attribute.translatedValue().isBlank()) {
                    normalized.put(key, attribute.translatedValue());
                }
            }
        }

        String region = extractDisplayValue(adView.path("region").path("value"));
        if (region != null) {
            normalized.putIfAbsent("region", region);
        }

        Set<String> phones = extractPhoneNumbers(adView.path("phoneNumbers").path("value").path("phone_numbers"));
        if (!phones.isEmpty()) {
            normalized.put("phone_numbers", List.copyOf(phones));
        }

        return normalized;
    }

    private Map<String, Object> buildLocation(JsonNode adView) {
        Map<String, Object> location = new LinkedHashMap<>();
        location.put("region", extractDisplayValue(adView.path("region").path("value")));
        location.put("city", extractDisplayValue(adView.path("city").path("value")));
        location.put("district", extractDisplayValue(adView.path("district").path("value")));
        location.put("street", extractDisplayValue(adView.path("street").path("value")));
        location.put("apartment", extractDisplayValue(adView.path("appartment").path("value")));
        location.put("mapPoint", toPlainValue(adView.path("mapPoint")));
        return location;
    }

    private Map<String, Object> buildContactInfo(JsonNode adView) {
        Map<String, Object> contactInfo = new LinkedHashMap<>();
        contactInfo.put("phoneNumbers", List.copyOf(extractPhoneNumbers(adView.path("phoneNumbers").path("value").path("phone_numbers"))));
        contactInfo.put("email", textOrNull(adView.path("email")));
        contactInfo.put("owner", buildOwnerSummary(adView.path("owner")));
        contactInfo.put("quickReplies", buildQuickReplies(adView.path("subCategory").path("quickReplies")));
        return contactInfo;
    }

    private Map<String, Object> buildOwnerSummary(JsonNode ownerNode) {
        Map<String, Object> owner = new LinkedHashMap<>();
        owner.put("id", textOrNull(ownerNode.path("id")));
        owner.put("login", textOrNull(ownerNode.path("login")));
        owner.put("avatar", textOrNull(ownerNode.path("avatar")));
        owner.put("createdDate", textOrNull(ownerNode.path("createdDate")));
        owner.put("businessPlan", textOrNull(ownerNode.path("business").path("plan")));
        owner.put("businessId", textOrNull(ownerNode.path("business").path("id")));
        owner.put("hasDelivery", ownerNode.path("business").path("hasDelivery").asBoolean(false));
        owner.put("isVerified", ownerNode.path("verification").path("isVerified").asBoolean(false));
        owner.put("verificationDate", textOrNull(ownerNode.path("verification").path("date")));
        return owner;
    }

    private List<String> buildQuickReplies(JsonNode quickRepliesNode) {
        List<String> replies = new ArrayList<>();
        if (!quickRepliesNode.isArray()) {
            return replies;
        }

        for (JsonNode replyNode : quickRepliesNode) {
            String translated = textOrNull(replyNode.path("title").path("translated"));
            if (translated != null) {
                replies.add(translated);
            }
        }
        return List.copyOf(replies);
    }

    private Set<String> extractPhoneNumbers(JsonNode phoneNumbersNode) {
        Set<String> phones = new LinkedHashSet<>();
        if (!phoneNumbersNode.isArray()) {
            return phones;
        }

        for (JsonNode phoneNode : phoneNumbersNode) {
            if (phoneNode.isTextual() && !phoneNode.asText().isBlank()) {
                phones.add(phoneNode.asText());
            }
        }
        return phones;
    }

    private String normalizeKey(ExtractedAttribute attribute) {
        if (attribute.featureId() != null && FEATURE_KEYS.containsKey(attribute.featureId())) {
            return FEATURE_KEYS.get(attribute.featureId());
        }
        return CONTROL_KEYS.get(attribute.title());
    }

    private String extractDisplayValue(JsonNode valueNode) {
        if (valueNode == null || valueNode.isMissingNode() || valueNode.isNull()) {
            return null;
        }
        if (valueNode.isTextual()) {
            return valueNode.asText();
        }
        if (valueNode.hasNonNull("translated")) {
            return valueNode.path("translated").asText();
        }
        if (valueNode.hasNonNull("value") && valueNode.hasNonNull("unit")) {
            String unit = switch (valueNode.path("unit").asText()) {
                case "UNIT_WATT" -> "W";
                default -> valueNode.path("unit").asText();
            };
            return valueNode.path("value").asText() + " " + unit;
        }
        if (valueNode.hasNonNull("value") && valueNode.path("value").isValueNode()) {
            return valueNode.path("value").asText();
        }
        return null;
    }

    private Object toPlainValue(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        return objectMapper.convertValue(node, Object.class);
    }

    private String extractTranslatedText(JsonNode valueNode) {
        if (valueNode == null || valueNode.isMissingNode() || valueNode.isNull()) {
            return null;
        }
        if (valueNode.hasNonNull("translated")) {
            return valueNode.path("translated").asText();
        }
        if (valueNode.hasNonNull("ro")) {
            return valueNode.path("ro").asText();
        }
        if (valueNode.isTextual()) {
            return valueNode.asText();
        }
        return null;
    }

    private String textOrNull(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String value = node.asText();
        return value == null || value.isBlank() ? null : value;
    }

    private String textOrFallback(JsonNode node, String fallback) {
        String value = textOrNull(node);
        return value != null ? value : fallback;
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
