package rd.alzada.storage;

public record StoredFile(byte[] content, String contentType, String extension) {}
