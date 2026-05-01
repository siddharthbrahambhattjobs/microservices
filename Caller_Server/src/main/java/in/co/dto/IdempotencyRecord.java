package in.co.dto;
public record IdempotencyRecord(
        String idempotencyKey,
        String correlationId,
        String status, // PROCESSING, COMPLETED, FAILED
        String requestHash,
        String responseBody
) {}