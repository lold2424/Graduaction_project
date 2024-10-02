package com.example.Graduatcion_project.service;

import com.example.Graduatcion_project.entity.VtuberEntity;
import com.example.Graduatcion_project.entity.VtuberSongsEntity;
import com.example.Graduatcion_project.repository.VtuberRepository;
import com.example.Graduatcion_project.repository.VtuberSongsRepository;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.*;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

@Service
public class UpdateVtuberSongsService {

    private final YouTube youTube;
    private final VtuberRepository vtuberRepository;
    private final VtuberSongsRepository vtuberSongsRepository;
    private final List<String> apiKeys;
    private int currentKeyIndex = 0;
    private static final Logger logger = Logger.getLogger(UpdateVtuberSongsService.class.getName());

    // 노래 관련 키워드 리스트 (제목만 필터링)
    private static final List<String> SONG_KEYWORDS = List.of("music", "song", "cover", "original", "official", "mv", "뮤직", "노래", "커버");

    public UpdateVtuberSongsService(YouTube youTube, VtuberRepository vtuberRepository, VtuberSongsRepository vtuberSongsRepository, @Value("${youtube.api.keys}") List<String> apiKeys) {
        this.youTube = youTube;
        this.vtuberRepository = vtuberRepository;
        this.vtuberSongsRepository = vtuberSongsRepository;
        this.apiKeys = apiKeys;
    }

    @Scheduled(cron = "0 26 15 * * ?", zone = "Asia/Seoul")
    public void fetchRecentVtuberSongs() {
        List<VtuberEntity> vtubers = vtuberRepository.findAll();
        LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);

        for (VtuberEntity vtuber : vtubers) {
            fetchRecentSongsFromSearch(vtuber.getChannelId(), vtuber.getName(), threeDaysAgo);
        }
    }

    private void fetchRecentSongsFromSearch(String channelId, String channelName, LocalDateTime threeDaysAgo) {
        String combinedQuery = "music|cover|original|official";
        String pageToken = null;

        do {
            try {
                YouTube.Search.List search = youTube.search().list("id,snippet");
                search.setChannelId(channelId);
                search.setQ(combinedQuery);
                search.setType("video");
                search.setOrder("date");
                search.setFields("nextPageToken,items(id/videoId,snippet/title,snippet/description,snippet/publishedAt,snippet/channelId)");
                search.setMaxResults(20L);
                search.setKey(apiKeys.get(currentKeyIndex));
                search.setPageToken(pageToken);

                SearchListResponse searchResponse = search.execute();
                List<SearchResult> searchResults = searchResponse.getItems();
                handleSearchResults(searchResults, channelName, threeDaysAgo);

                pageToken = searchResponse.getNextPageToken();
            } catch (IOException e) {
                logger.severe("API 호출 중 오류 발생: " + e.getMessage());
                switchApiKey();
            }
        } while (pageToken != null);
    }

    private void handleSearchResults(List<SearchResult> searchResults, String channelName, LocalDateTime threeDaysAgo) {
        for (SearchResult result : searchResults) {
            String videoId = result.getId().getVideoId();
            LocalDateTime publishedAt = Instant.ofEpochMilli(result.getSnippet().getPublishedAt().getValue())
                    .atZone(ZoneId.systemDefault()).toLocalDateTime();

            // 최근 3일 이내에 게시된 동영상인지 확인
            if (publishedAt.isAfter(threeDaysAgo)) {
                String title = result.getSnippet().getTitle().toLowerCase();

                // 제목에 노래와 관련된 키워드가 포함되어 있는지 확인
                if (isSongRelated(title)) {
                    Optional<VtuberSongsEntity> existingSongOpt = vtuberSongsRepository.findByVideoId(videoId);
                    if (!existingSongOpt.isPresent()) {
                        saveNewSong(result, channelName, videoId); // videoId 전달
                    }
                } else {
                    logger.info("노래와 관련 없는 동영상 필터링: " + title);
                }
            }
        }
    }

    // 제목에 노래 관련 키워드가 포함되어 있는지 확인하는 함수
    private boolean isSongRelated(String title) {
        // 제목에 SONG_KEYWORDS에 해당하는 단어가 포함되어 있는지 확인
        return SONG_KEYWORDS.stream().anyMatch(title::contains);
    }

    private void saveNewSong(SearchResult result, String channelName, String videoId) {
        VtuberSongsEntity song = new VtuberSongsEntity();
        song.setChannelId(result.getSnippet().getChannelId());
        song.setVideoId(result.getId().getVideoId());
        song.setTitle(result.getSnippet().getTitle());
        song.setDescription(truncateDescription(result.getSnippet().getDescription()));
        song.setPublishedAt(Instant.ofEpochMilli(result.getSnippet().getPublishedAt().getValue())
                .atZone(ZoneId.systemDefault()).toLocalDateTime());
        song.setAddedTime(LocalDateTime.now());
        song.setUpdateDayTime(LocalDateTime.now());
        song.setUpdateWeekTime(LocalDateTime.now());
        song.setVtuberName(channelName);
        song.setViewCount(0L);  // 초기 조회수를 0으로 설정
        song.setViewsIncreaseDay(0L);  // 초기 일간 조회수 증가량을 0으로 설정
        song.setViewsIncreaseWeek(0L);  // 초기 주간 조회수 증가량을 0으로 설정
        song.setLastWeekViewCount(0L);  // 초기 주간 조회수
        song.setStatus("new");

        // 쇼츠 또는 비디오를 구분하여 classification 설정
        song.setClassification(determineClassification(videoId, result.getSnippet().getTitle()));

        vtuberSongsRepository.save(song);
    }

    private String determineClassification(String videoId, String title) {
        // 비디오의 길이를 기준으로 쇼츠 여부 판단
        try {
            YouTube.Videos.List request = youTube.videos().list("contentDetails");
            request.setId(videoId);
            request.setKey(apiKeys.get(currentKeyIndex));

            var response = request.execute();
            if (!response.getItems().isEmpty()) {
                var videoDetails = response.getItems().get(0).getContentDetails();
                if (videoDetails != null && videoDetails.getDuration() != null) {
                    Duration videoDuration = Duration.parse(videoDetails.getDuration());
                    if (videoDuration.compareTo(Duration.ofMinutes(1)) <= 0) {
                        return "shorts"; // 1분 이하면 쇼츠로 분류
                    }
                }
            }
        } catch (IOException e) {
            logger.severe("Error fetching video details for classification: " + e.getMessage());
            switchApiKey();
        }

        // 비디오 제목에 "short"가 포함되어 있는 경우도 쇼츠로 간주
        if (title.toLowerCase().contains("short")) {
            return "shorts";
        }

        // 기본적으로는 일반 비디오로 분류
        return "videos";
    }

    private long fetchViewCount(String videoId) {
        try {
            YouTube.Videos.List request = youTube.videos().list("statistics");
            request.setId(videoId);
            request.setKey(apiKeys.get(currentKeyIndex));
            var response = request.execute();
            if (!response.getItems().isEmpty()) {
                return response.getItems().get(0).getStatistics().getViewCount().longValue();  // Long 타입으로 반환
            }
        } catch (IOException e) {
            logger.severe("Failed to fetch view counts: " + e.getMessage());
            switchApiKey();
        }
        return 0;  // 오류 발생시 0 반환
    }

    private String truncateDescription(String description) {
        return description.length() > 255 ? description.substring(0, 255) : description;
    }

    private void switchApiKey() {
        currentKeyIndex = (currentKeyIndex + 1) % apiKeys.size();
        logger.info("Switched to next API key: " + apiKeys.get(currentKeyIndex));
    }

    @Scheduled(cron = "0 11 16 * * MON", zone = "Asia/Seoul")
    public void updateSongStatusToExisting() {
        // "new" 상태인 모든 곡을 찾아서 "existing"으로 상태 업데이트
        List<VtuberSongsEntity> newSongs = vtuberSongsRepository.findByStatus("new");

        newSongs.forEach(song -> {
            song.setStatus("existing");
            song.setUpdateDayTime(LocalDateTime.now()); // 상태 변경 시간을 기록
            vtuberSongsRepository.save(song);
        });

        logger.info("Updated " + newSongs.size() + " songs from 'new' to 'existing'.");
    }

    @Scheduled(cron = "0 33 15 * * ?", zone = "Asia/Seoul") // 매일 특정 시간에 실행
    public void updateViewCounts() {
        List<VtuberSongsEntity> songs = vtuberSongsRepository.findAll(); // 모든 곡 조회
        for (VtuberSongsEntity song : songs) {
            long newViewCount = fetchViewCount(song.getVideoId()); // 유튜브 API로 조회수 가져오기
            if (newViewCount > 0) {
                // null 체크를 위해 Long 타입 사용
                Long currentViewCount = (song.getViewCount() != null) ? song.getViewCount() : 0L;

                long viewIncreaseDay = newViewCount - currentViewCount; // 일간 조회수 증가량 계산
                song.setViewCount(newViewCount); // 새로운 조회수 저장
                song.setViewsIncreaseDay(viewIncreaseDay); // 일간 조회수 증가량 저장
                song.setUpdateDayTime(LocalDateTime.now()); // 조회수 갱신 시간 저장

                // 주간 조회수 갱신은 월요일에만 수행
                if (LocalDateTime.now().getDayOfWeek() == DayOfWeek.TUESDAY) {
                    // null 체크 후 기본값 0L로 설정
                    Long lastWeekViewCount = (song.getLastWeekViewCount() != null) ? song.getLastWeekViewCount() : 0L;

                    long viewIncreaseWeek = newViewCount - lastWeekViewCount; // 주간 조회수 증가량 계산
                    song.setViewsIncreaseWeek(viewIncreaseWeek); // 주간 조회수 증가량 저장
                    song.setLastWeekViewCount(newViewCount); // 현재 조회수를 last_week_view_count로 저장
                    song.setUpdateWeekTime(LocalDateTime.now()); // 주간 조회수 갱신 시간 기록
                }

                vtuberSongsRepository.save(song); // DB에 저장
            }
        }
        logger.info("조회수 업데이트 완료");
    }

}
