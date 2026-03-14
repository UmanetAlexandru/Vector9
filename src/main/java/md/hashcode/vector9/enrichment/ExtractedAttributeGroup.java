package md.hashcode.vector9.enrichment;

import java.util.List;

public record ExtractedAttributeGroup(
        String title,
        List<ExtractedAttribute> attributes
) {
}
