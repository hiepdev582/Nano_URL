package com.hiepnn.url_shortener.controller;

import com.hiepnn.url_shortener.dto.ShortenRequest;
import com.hiepnn.url_shortener.dto.ShortenResponse;
import com.hiepnn.url_shortener.model.UrlMapping;
import com.hiepnn.url_shortener.service.UrlShortenerService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Optional;

@RestController
public class UrlShortenerController {

    private final UrlShortenerService service;

    public UrlShortenerController(UrlShortenerService service) {
        this.service = service;
    }

    @PostMapping("/api/v1/shorten")
    public ResponseEntity<?> shortenUrl(@RequestBody ShortenRequest request, HttpServletRequest servletRequest) {
        String originalUrl = request.getOriginalUrl();

        // Kiểm tra URL hợp lệ đơn giản
        if (originalUrl == null || originalUrl.trim().isEmpty() || 
            (!originalUrl.startsWith("http://") && !originalUrl.startsWith("https://"))) {
            return ResponseEntity.badRequest().body("URL không hợp lệ. URL phải bắt đầu bằng http:// hoặc https://");
        }

        UrlMapping mapping = service.shortenUrl(originalUrl);

        // Tạo full URL rút gọn dựa trên request thực tế gửi tới Nginx/Backend
        String scheme = servletRequest.getHeader("X-Forwarded-Proto");
        if (scheme == null) {
            scheme = servletRequest.getScheme();
        }
        
        String host = servletRequest.getHeader("Host");
        if (host == null) {
            host = servletRequest.getServerName() + ":" + servletRequest.getServerPort();
        }

        String shortUrl = scheme + "://" + host + "/" + mapping.getShortCode();

        ShortenResponse response = new ShortenResponse(
                mapping.getOriginalUrl(),
                shortUrl,
                mapping.getShortCode(),
                mapping.getCreatedAt()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirectToOriginalUrl(@PathVariable String shortCode) {
        Optional<String> originalUrlOpt = service.getOriginalUrl(shortCode);

        if (originalUrlOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(originalUrlOpt.get()))
                    .build();
        }

        return ResponseEntity.notFound().build();
    }
}
