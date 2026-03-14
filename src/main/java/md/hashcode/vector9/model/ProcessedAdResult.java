package md.hashcode.vector9.model;

public record ProcessedAdResult(
        boolean created,
        boolean materiallyChanged,
        boolean priceChanged,
        int insertedImages
) {
}