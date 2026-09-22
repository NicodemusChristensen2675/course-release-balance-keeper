package education.devtools;

public record ReleaseOperation(String releaseId, String buildId, String courseSlug) {
    public ReleaseOperation {
        if (releaseId == null || releaseId.isBlank()) throw new IllegalArgumentException("releaseId is required");
        if (buildId == null || buildId.isBlank()) throw new IllegalArgumentException("buildId is required");
        if (courseSlug == null || courseSlug.isBlank()) throw new IllegalArgumentException("courseSlug is required");
    }
}
