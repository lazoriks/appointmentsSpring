package com.example.appointments.controller;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Lets the salon manager replace the photos shown on the WordPress home page
 * without touching WordPress. Each slot has a fixed, permanent public URL, so
 * the home page references it once and never has to be edited again — a new
 * upload simply overwrites the object behind that URL.
 *
 * Sits under /api/admin/** so AdminApiKeyFilter already protects it.
 */
@RestController
@RequestMapping("/api/admin/site-images")
public class SiteImageController {

    /** Slot ids the home page knows about: 7 category sections x 2 photos. */
    private static final List<String> SLOTS = List.of(
            "nails-1", "nails-2",
            "hair-1", "hair-2",
            "brows-1", "brows-2",
            "permanent-1", "permanent-2",
            "waxing-1", "waxing-2",
            "treatments-1", "treatments-2",
            "makeup-1", "makeup-2"
    );

    private static final Set<String> ALLOWED_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp");

    private static final long MAX_BYTES = 5L * 1024 * 1024; // 5 MB

    /** Short TTL so a replaced photo shows up on the site within minutes. */
    private static final String CACHE_CONTROL = "public, max-age=300";

    @Value("${app.site-images.bucket:glamlimerick-site-images}")
    private String bucket;

    private final Storage storage = StorageOptions.getDefaultInstance().getService();

    @GetMapping
    public List<Map<String, Object>> list() {
        return SLOTS.stream().map(slot -> {
            var blob = storage.get(BlobId.of(bucket, objectName(slot)));
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("slot", slot);
            item.put("url", publicUrl(slot));
            item.put("uploaded", blob != null && blob.exists());
            item.put("updated", blob != null ? blob.getUpdateTimeOffsetDateTime() : null);
            return item;
        }).toList();
    }

    @PostMapping("/{slot}")
    public Map<String, Object> upload(@PathVariable String slot,
                                      @RequestParam("file") MultipartFile file) {
        if (!SLOTS.contains(slot)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown slot: " + slot);
        }
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Max file size is 5 MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Only JPEG, PNG and WebP images are allowed");
        }

        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucket, objectName(slot)))
                .setContentType(contentType)
                .setCacheControl(CACHE_CONTROL)
                .build();

        try {
            storage.create(blobInfo, file.getBytes());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not store the image", e);
        }

        return Map.of("slot", slot, "url", publicUrl(slot));
    }

    private String objectName(String slot) {
        return "categories/" + slot;
    }

    private String publicUrl(String slot) {
        return "https://storage.googleapis.com/" + bucket + "/" + objectName(slot);
    }
}
