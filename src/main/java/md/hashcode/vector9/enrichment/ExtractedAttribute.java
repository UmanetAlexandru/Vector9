package md.hashcode.vector9.enrichment;

public record ExtractedAttribute(
        String title,
        String controlType,
        Long featureId,
        String featureType,
        String translatedValue,
        Object rawValue
) {
}
