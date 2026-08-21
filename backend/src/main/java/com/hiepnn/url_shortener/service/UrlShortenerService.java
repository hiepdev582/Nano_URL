package com.hiepnn.url_shortener.service;

import com.hiepnn.url_shortener.model.UrlMapping;
import com.hiepnn.url_shortener.repository.UrlMappingRepository;
import com.hiepnn.url_shortener.util.Base62;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class UrlShortenerService {

    private final UrlMappingRepository repository;
    private final StringRedisTemplate redisTemplate;

    private static final String CACHE_PREFIX = "url:shortcode:";
    private static final long CACHE_TTL_DAYS = 1;

    public UrlShortenerService(UrlMappingRepository repository, StringRedisTemplate redisTemplate) {
        this.repository = repository;
        this.redisTemplate = redisTemplate;
    }

    @Transactional
    public UrlMapping shortenUrl(String originalUrl) {
        // Chuẩn hóa URL gốc (xóa khoảng trắng thừa)
        String cleanedUrl = originalUrl.trim();

        // 1. Kiểm tra xem URL gốc đã được rút gọn trước đó chưa
        Optional<UrlMapping> existing = repository.findByOriginalUrl(cleanedUrl);
        if (existing.isPresent()) {
            UrlMapping mapping = existing.get();
            // Lưu vào Redis cache phòng trường hợp bị evicted
            cacheUrl(mapping.getShortCode(), cleanedUrl);
            return mapping;
        }

        // 2. Lưu bản ghi rỗng/chưa có shortCode để lấy ID tự tăng
        UrlMapping mapping = new UrlMapping();
        mapping.setOriginalUrl(cleanedUrl);
        mapping = repository.save(mapping);

        // 3. Sử dụng ID tự tăng sinh ra mã ngắn Base62
        String shortCode = Base62.encode(mapping.getId());
        mapping.setShortCode(shortCode);

        // 4. Lưu lại bản ghi đã có shortCode
        mapping = repository.save(mapping);

        // 5. Lưu vào Redis Cache (Cache-Aside)
        cacheUrl(shortCode, cleanedUrl);

        return mapping;
    }

    public Optional<String> getOriginalUrl(String shortCode) {
        String cacheKey = CACHE_PREFIX + shortCode;

        // 1. Check Redis Cache
        String cachedUrl = redisTemplate.opsForValue().get(cacheKey);
        if (cachedUrl != null) {
            return Optional.of(cachedUrl);
        }

        // 2. Cache Miss: Giải mã shortCode về ID
        try {
            long id = Base62.decode(shortCode);

            // 3. Truy vấn DB theo ID (Query tối ưu bằng PK index)
            Optional<UrlMapping> mapping = repository.findById(id);
            if (mapping.isPresent()) {
                String originalUrl = mapping.get().getOriginalUrl();

                // 4. Write back to Redis Cache với TTL
                cacheUrl(shortCode, originalUrl);

                return Optional.of(originalUrl);
            }
        } catch (IllegalArgumentException e) {
            // Trường hợp shortCode chứa ký tự lạ không hợp lệ cho Base62
            return Optional.empty();
        }

        return Optional.empty();
    }

    private void cacheUrl(String shortCode, String originalUrl) {
        String cacheKey = CACHE_PREFIX + shortCode;
        redisTemplate.opsForValue().set(cacheKey, originalUrl, CACHE_TTL_DAYS, TimeUnit.DAYS);
    }
}
