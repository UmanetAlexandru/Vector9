package md.hashcode.vector9.model;

public record ProcessedAdResult(
        boolean created,
        boolean priceChanged,
        int insertedImages
) {
}