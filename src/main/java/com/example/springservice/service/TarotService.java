package com.example.springservice.service;

import com.example.springservice.config.AppProperties;
import com.example.springservice.dto.TarotRequest;
import com.example.springservice.dto.TarotResponse;
import com.example.springservice.dto.VersionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TarotService {

    private static final Logger log = LoggerFactory.getLogger(TarotService.class);

    private final TarotRequestValidator tarotRequestValidator;
    private final GradioSpaceService gradioSpaceService;
    private final AppProperties appProperties;

    public TarotService(
        TarotRequestValidator tarotRequestValidator,
        GradioSpaceService gradioSpaceService,
        AppProperties appProperties
    ) {
        this.tarotRequestValidator = tarotRequestValidator;
        this.gradioSpaceService = gradioSpaceService;
        this.appProperties = appProperties;
    }

    public TarotResponse createReading(TarotRequest request) {
        TarotRequestValidator.ValidatedTarotRequest validatedRequest = tarotRequestValidator.validate(request);
        return gradioSpaceService.generateReading(validatedRequest);
    }

    public VersionResponse getBackendVersion() {
        try {
            return gradioSpaceService.getBackendVersion();
        } catch (Exception exception) {
            log.warn("Falling back to Spring app version", exception);
            return new VersionResponse(appProperties.getVersion());
        }
    }
}
