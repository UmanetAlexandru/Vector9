package md.hashcode.vector9.client;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import md.hashcode.vector9.model.AdImageInput;
import md.hashcode.vector9.model.AdUpsertCommand;
import md.hashcode.vector9.model.OwnerUpsertCommand;
import md.hashcode.vector9.model.ProcessedAdCommand;
import md.hashcode.vector9.model.graphql.GraphqlAd;
import md.hashcode.vector9.model.graphql.GraphqlFeatureValue;
import md.hashcode.vector9.model.graphql.GraphqlOwner;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
public class GraphqlAdMapper {

    public List<ProcessedAdCommand> toProcessedAds(List<GraphqlAd> ads) {
        if (ads == null || ads.isEmpty()) {
            return List.of();
        }

        return ads.stream()
                .filter(Objects::nonNull)
                .map(this::toProcessedAd)
                .toList();
    }

    public ProcessedAdCommand toProcessedAd(GraphqlAd ad) {
        List<String> imageNames = extractImageNames(ad.images());
        List<AdImageInput> images = new ArrayList<>(imageNames.size());
        for (int i = 0; i < imageNames.size(); i++) {
            images.add(new AdImageInput(imageNames.get(i), i, i == 0));
        }

        return new ProcessedAdCommand(
                new AdUpsertCommand(
                        parseLong(ad.id()),
                        ad.title(),
                        ad.subCategory() != null ? ad.subCategory().id() : 0L,
                        extractNumeric(ad.price(), "value"),
                        extractText(ad.price(), "unit"),
                        extractText(ad.price(), "measurement"),
                        extractText(ad.price(), "mode"),
                        extractNumeric(ad.pricePerMeter(), null),
                        extractNumeric(ad.oldPrice(), null),
                        extractBody(ad.body()),
                        null,
                        null,
                        null,
                        null,
                        null,
                        extractInteger(ad.transportYear()),
                        extractTranslatedOption(ad.realEstate()),
                        "active",
                        LocalDateTime.now()
                ),
                toOwnerCommand(ad.owner()),
                images
        );
    }

    private OwnerUpsertCommand toOwnerCommand(GraphqlOwner owner) {
        if (owner == null || owner.id() == null || owner.id().isBlank()) {
            return null;
        }

        try {
            return new OwnerUpsertCommand(
                    UUID.fromString(owner.id()),
                    owner.login(),
                    owner.avatar(),
                    owner.createdDate(),
                    owner.business() != null ? owner.business().plan() : null,
                    owner.business() != null ? owner.business().id() : null,
                    owner.verification() != null ? owner.verification().isVerified() : null,
                    owner.verification() != null ? owner.verification().date() : null
            );
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private long parseLong(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0L;
        }
        return Long.parseLong(raw);
    }

    private Integer extractInteger(GraphqlFeatureValue featureValue) {
        BigDecimal numeric = extractNumeric(featureValue, null);
        return numeric != null ? numeric.intValue() : null;
    }

    private String extractTranslatedOption(GraphqlFeatureValue featureValue) {
        if (featureValue == null || featureValue.value() == null || featureValue.value().isNull()) {
            return null;
        }

        JsonNode translated = featureValue.value().get("translated");
        return translated != null && translated.isTextual() ? translated.asText() : null;
    }

    private String extractBody(GraphqlFeatureValue featureValue) {
        if (featureValue == null || featureValue.value() == null || featureValue.value().isNull()) {
            return null;
        }
        if (featureValue.value().isTextual()) {
            return featureValue.value().asText();
        }

        JsonNode translated = featureValue.value().get("translated");
        return translated != null && translated.isTextual() ? translated.asText() : null;
    }

    private BigDecimal extractNumeric(GraphqlFeatureValue featureValue, String nestedField) {
        if (featureValue == null || featureValue.value() == null || featureValue.value().isNull()) {
            return null;
        }

        JsonNode node = featureValue.value();
        if (nestedField != null && node.isObject()) {
            node = node.get(nestedField);
        }

        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.decimalValue();
        }
        if (node.isTextual()) {
            try {
                return new BigDecimal(node.asText());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String extractText(GraphqlFeatureValue featureValue, String nestedField) {
        if (featureValue == null || featureValue.value() == null || featureValue.value().isNull()) {
            return null;
        }

        JsonNode node = featureValue.value();
        if (nestedField != null && node.isObject()) {
            node = node.get(nestedField);
        }

        return node != null && node.isValueNode() ? node.asText() : null;
    }

    private List<String> extractImageNames(GraphqlFeatureValue images) {
        if (images == null || images.value() == null || !images.value().isArray()) {
            return List.of();
        }

        List<String> result = new ArrayList<>();
        for (JsonNode entry : images.value()) {
            if (entry != null && entry.isTextual()) {
                result.add(stripQuery(entry.asText()));
            }
        }
        return result;
    }

    private String stripQuery(String raw) {
        int index = raw.indexOf('?');
        return index >= 0 ? raw.substring(0, index) : raw;
    }
}
