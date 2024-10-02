package com.example.Graduatcion_project.service;

import com.example.Graduatcion_project.entity.VtuberEntity;
import com.example.Graduatcion_project.entity.VtuberSongsEntity;
import com.example.Graduatcion_project.repository.VtuberRepository;
import com.example.Graduatcion_project.repository.VtuberSongsRepository;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

@Service
public class VtuberSongsService {

    private final YouTube youTube;
    private final VtuberRepository vtuberRepository;
    private final VtuberSongsRepository vtuberSongsRepository;
    private final List<String> apiKeys;
    private int currentKeyIndex = 0;
    private static final Logger logger = Logger.getLogger(VtuberSongsService.class.getName());

    private int startProcessingFromIndex = 115;

    public VtuberSongsService(YouTube youTube, VtuberRepository vtuberRepository, VtuberSongsRepository vtuberSongsRepository, @Value("${youtube.api.keys}") List<String> apiKeys) {
        this.youTube = youTube;
        this.vtuberRepository = vtuberRepository;
        this.vtuberSongsRepository = vtuberSongsRepository;
        this.apiKeys = apiKeys;
    }

    // 스케줄러가 실행되는 메서드 (매일 버튜버 노래를 수집)
    @Scheduled(cron = "0 0 0 * * ?", zone = "Asia/Seoul")
    public void fetchAndSaveVtuberSongs() {
        // API 호출 및 노래 데이터 수집 로직
        logger.info("Starting VTuber song data fetch.");
        List<VtuberEntity> vtubers = vtuberRepository.findAll();

        for (int i = startProcessingFromIndex; i < vtubers.size(); i++) {
            VtuberEntity vtuber = vtubers.get(i);
            fetchSongsFromSearch(vtuber.getChannelId());
        }

        logger.info("Completed processing VTuber song data.");
    }

    // 캐시를 삭제하는 메서드
    @CacheEvict(value = "songSearchCache", allEntries = true)
    public void clearSongCache() {
        logger.info("Clearing song search cache after data fetch.");
    }

    // 스케줄러가 종료된 후 캐시를 삭제하도록 설정
    @Scheduled(cron = "0 40 20 * * ?", zone = "Asia/Seoul") // 노래 수집 후 10분 뒤 캐시 삭제 예시
    public void scheduleCacheEviction() {
        clearSongCache(); // 캐시 삭제 메서드 호출
    }

    // 삭제된 메서드: fetchSongsFromPlaylists

    @Cacheable(value = "songSearchCache", key = "#channelId + '-' + #query", unless = "#result.isEmpty()")
    public void fetchSongsFromSearch(String channelId) {
        List<String> searchQueries = List.of("music", "cover", "original", "official");
        searchQueries.forEach(query -> {
            String pageToken = null;
            do {
                try {
                    YouTube.Search.List request = youTube.search().list("id,snippet");
                    request.setChannelId(channelId).setQ(query).setType("video")
                            .setFields("nextPageToken,items(id/videoId,snippet/title,snippet/description,snippet/publishedAt,snippet/channelId)")
                            .setMaxResults(50L).setKey(apiKeys.get(currentKeyIndex)).setPageToken(pageToken);

                    SearchListResponse response = request.execute();
                    response.getItems().forEach(result -> {
                        String videoId = result.getId().getVideoId();
                        String videoChannelId = result.getSnippet().getChannelId();
                        processVideo(videoId, videoChannelId, result.getSnippet().getTitle(),
                                result.getSnippet().getDescription(), result.getSnippet().getPublishedAt().toString(), channelId);
                    });
                    pageToken = response.getNextPageToken();
                } catch (IOException e) {
                    logger.severe("Error calling YouTube API: " + e.getMessage());
                    switchApiKey();
                }
            } while (pageToken != null);
        });
    }


    private void processVideo(String videoId, String videoChannelId, String title, String description, String publishedAt, String vtuberChannelId) {
        // 로그 추가: 비교하려는 값들을 로그로 출력해 확인
        logger.info("Processing Video - Video ID: " + videoId + ", Video Channel ID: " + videoChannelId + ", VTuber Channel ID: " + vtuberChannelId);

        // videoChannelId나 vtuberChannelId가 null인 경우
        if (videoChannelId == null || vtuberChannelId == null) {
            logger.warning("Channel ID 중 하나가 null입니다. Video ID: " + videoId);
            return;
        }

        // 동영상이 현재 처리 중인 VTuber의 채널에서 업로드된 것이 맞는지 확인
        if (!videoChannelId.equals(vtuberChannelId)) {
            logger.info("동영상이 다른 채널에서 업로드되었습니다. 건너뜁니다. Video ID: " + videoId);
            return;  // 현재 VTuber의 채널과 다르면 저장하지 않고 건너뜁니다.
        }

        // 제목이 키워드를 포함하지 않거나, 동영상 길이가 유효하지 않으면 처리하지 않음
        if (!isTitleContainsKeywords(title.toLowerCase()) || !isVideoDurationValid(videoId)) {
            return;
        }

        long currentViewCount = fetchViewCount(videoId);
        Optional<VtuberSongsEntity> existingSongOpt = vtuberSongsRepository.findByVideoId(videoId);
        VtuberSongsEntity song = existingSongOpt.orElseGet(() -> new VtuberSongsEntity());

        // VtuberEntity에서 channelId를 찾아서 name을 가져옴
        Optional<VtuberEntity> vtuberOpt = vtuberRepository.findByChannelId(vtuberChannelId);
        String vtuberName = vtuberOpt.map(VtuberEntity::getName).orElse(vtuberChannelId); // 매칭된 이름이 없을 경우, channelId 저장

        if (!existingSongOpt.isPresent()) {
            song.setChannelId(vtuberChannelId);  // 올바른 VTuber 채널 ID 설정
            song.setVideoId(videoId);
            song.setTitle(title);
            song.setDescription(truncateDescription(description));
            song.setPublishedAt(Instant.parse(publishedAt).atZone(ZoneId.systemDefault()).toLocalDateTime());
            song.setAddedTime(LocalDateTime.now());
            song.setUpdateDayTime(LocalDateTime.now());
            song.setUpdateWeekTime(LocalDateTime.now());
            song.setVtuberName(vtuberName); // VTuber 이름 설정
            song.setClassification(determineClassification(videoId, title, description));
            song.setStatus("new");
        }

        updateViewIncreases(song, currentViewCount);
        song.setViewCount(currentViewCount);
        vtuberSongsRepository.save(song);
    }

    private void updateViewIncreases(VtuberSongsEntity song, long currentViewCount) {
        LocalDateTime now = LocalDateTime.now();
        if (song.getUpdateDayTime().isBefore(now.minusDays(1))) {
            song.setViewsIncreaseDay(currentViewCount - song.getViewCount());
            song.setUpdateDayTime(now);
        }
        if (now.getDayOfWeek() == java.time.DayOfWeek.MONDAY) {
            song.setViewsIncreaseWeek(currentViewCount - song.getViewCount());
            song.setUpdateWeekTime(now);
        }
    }

    private long fetchViewCount(String videoId) {
        try {
            YouTube.Videos.List request = youTube.videos().list("statistics");
            request.setId(videoId).setKey(apiKeys.get(currentKeyIndex));

            VideoListResponse response = request.execute();
            return response.getItems().stream()
                    .filter(v -> v.getStatistics() != null && v.getStatistics().getViewCount() != null)
                    .findFirst()
                    .map(v -> v.getStatistics().getViewCount().longValue())
                    .orElse(0L);
        } catch (IOException e) {
            logger.warning("View count is null for video ID: " + videoId);
            return 0L;
        }
    }

    private boolean isTitleContainsKeywords(String title) {
        List<String> keywords = List.of("music", "cover", "original", "official");
        return keywords.stream().anyMatch(title::contains);
    }

    private boolean isVideoDurationValid(String videoId) {
        try {
            YouTube.Videos.List request = youTube.videos().list("contentDetails");
            request.setId(videoId).setKey(apiKeys.get(currentKeyIndex));

            VideoListResponse response = request.execute();
            return response.getItems().stream()
                    .map(Video::getContentDetails)
                    .filter(details -> details != null && details.getDuration() != null)
                    .anyMatch(details -> Duration.parse(details.getDuration()).compareTo(Duration.ofMinutes(10)) <= 0);
        } catch (IOException e) {
            logger.severe("Error validating video duration: " + e.getMessage());
            switchApiKey();
            return false;
        }
    }

    private String truncateDescription(String description) {
        return (description != null && description.length() > 255) ? description.substring(0, 255) : description;
    }

    private String determineClassification(String videoId, String title, String description) {
        if (title.contains("short") || description.contains("short")) {
            return "shorts";
        } else {
            return "videos";
        }
    }

    private void switchApiKey() {
        currentKeyIndex = (currentKeyIndex + 1) % apiKeys.size();
        logger.warning("Switching to next API key: " + apiKeys.get(currentKeyIndex));
        if (currentKeyIndex == 0) {
            logger.severe("All API keys used. Waiting for quota reset.");
        }
    }
}
