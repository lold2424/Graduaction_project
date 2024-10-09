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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

@Service
public class UpdateVtuberSongsService {

    private final YouTube youTube;
    private final VtuberRepository vtuberRepository;
    private final VtuberSongsRepository vtuberSongsRepository;
    private final List<String> apiKeys;
    private final List<Boolean> keyUsage;
    private int currentKeyIndex = 0;
    private static final Logger logger = Logger.getLogger(UpdateVtuberSongsService.class.getName());

    // 노래 관련 키워드 리스트 (제목만 필터링)
    private static final List<String> SONG_KEYWORDS = List.of("music", "song", "cover", "original", "official", "mv", "뮤직", "노래", "커버");

    public UpdateVtuberSongsService(YouTube youTube, VtuberRepository vtuberRepository, VtuberSongsRepository vtuberSongsRepository, @Value("${youtube.api.keys}") List<String> apiKeys) {
        this.youTube = youTube;
        this.vtuberRepository = vtuberRepository;
        this.vtuberSongsRepository = vtuberSongsRepository;
        this.apiKeys = new ArrayList<>(apiKeys);
        this.keyUsage = new ArrayList<>(apiKeys.size());
        // keyUsage 리스트의 크기와 값 초기화
        for (int i = 0; i < apiKeys.size(); i++) {
            this.keyUsage.add(true); // 모든 키를 처음엔 사용 가능으로 설정
        }
    }


    @Scheduled(cron = "0 5 0 * * ?", zone = "Asia/Seoul")
    public void fetchRecentVtuberSongs() {
        List<VtuberEntity> vtubers = vtuberRepository.findAll();
        Instant oneDayAgoInstant = LocalDateTime.now().minusDays(1).toInstant(ZoneOffset.UTC); // 현재 시간에서 하루를 빼고 Instant로 변환

        for (VtuberEntity vtuber : vtubers) {
            fetchRecentSongsFromSearch(vtuber.getChannelId(), vtuber.getName(), oneDayAgoInstant);
        }
    }

    private void fetchRecentSongsFromSearch(String channelId, String channelName, Instant oneDayAgoInstant) {
        String combinedQuery = "music|cover|original|official";
        String pageToken = null;
        int keyAttempt = 0;

        do {
            try {
                YouTube.Search.List search = youTube.search().list("id,snippet");
                search.setChannelId(channelId);
                search.setQ(combinedQuery);
                search.setType("video");
                search.setOrder("date");
                search.setFields("nextPageToken,items(id/videoId,snippet/title,snippet/publishedAt,snippet/channelId)");
                search.setMaxResults(30L);
                search.setKey(apiKeys.get(currentKeyIndex));
                search.setPageToken(pageToken);

                // publishedAfter 파라미터를 설정하여 검색 범위를 최근 1일 이내로 제한
                search.setPublishedAfter(new com.google.api.client.util.DateTime(oneDayAgoInstant.toEpochMilli()));

                SearchListResponse searchResponse = search.execute();
                List<SearchResult> searchResults = searchResponse.getItems();
                handleSearchResults(searchResults, channelName);  // publishedAt 확인 필요 없음

                pageToken = searchResponse.getNextPageToken();
            } catch (IOException e) {
                logger.severe("API 호출 중 오류 발생 - 채널명: " + channelName + ", 채널 ID: " + channelId + " - 오류 메시지: " + e.getMessage());
                keyUsage.set(currentKeyIndex, false);  // 현재 키가 사용 불가능하다고 표시
                keyAttempt++;
                if (keyAttempt >= apiKeys.size()) {
                    logger.severe("모든 API 키의 할당량 소진됨. 작업 종료. 마지막 작업 버튜버: " + channelName + " (" + channelId + ")");
                    return;  // 모든 키의 할당량 소진 시 종료
                }
                switchApiKey();
            }
        } while (pageToken != null);
    }

    private void handleSearchResults(List<SearchResult> searchResults, String channelName) {
        for (SearchResult result : searchResults) {
            String videoId = result.getId().getVideoId();
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


    private void switchApiKey() {
        int initialKeyIndex = currentKeyIndex;
        do {
            currentKeyIndex = (currentKeyIndex + 1) % apiKeys.size();
            if (keyUsage.get(currentKeyIndex)) {
                logger.info("API 키 전환: " + apiKeys.get(currentKeyIndex));
                return;  // 사용 가능한 API 키를 찾으면 바로 반환
            }
        } while (currentKeyIndex != initialKeyIndex); // 모든 키를 순환했는지 확인

        // 모든 키가 할당량을 초과한 경우
        logger.severe("모든 API 키의 할당량이 소진됨. 작업을 중단합니다.");
        throw new RuntimeException("모든 API 키의 할당량이 소진되었습니다.");
    }

    private void resetKeyUsage() {
        for (int i = 0; i < keyUsage.size(); i++) {
            keyUsage.set(i, true); // 모든 키를 사용 가능하게 설정
        }
    }

    @Scheduled(cron = "30 2 0 * * MON", zone = "Asia/Seoul")
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

    @Scheduled(cron = "0 0 0 * * ?", zone = "Asia/Seoul") // 매일 특정 시간에 실행
    public void updateViewCounts() {
        resetKeyUsage(); // API 키 사용 상태를 리셋
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
                if (LocalDateTime.now().getDayOfWeek() == DayOfWeek.MONDAY) {
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
