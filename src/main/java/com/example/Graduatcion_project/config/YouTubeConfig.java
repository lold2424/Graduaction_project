package com.example.Graduatcion_project.config;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.security.GeneralSecurityException;

@Configuration
public class YouTubeConfig {

    @Bean
    public YouTube youTube() throws GeneralSecurityException, IOException {
        return new YouTube.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JacksonFactory.getDefaultInstance(),
                request -> {})
                .setApplicationName("vtuber-songs")
                .build();
    }
}
