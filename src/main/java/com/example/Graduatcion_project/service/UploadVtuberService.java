package com.example.Graduatcion_project.service;

import com.example.Graduatcion_project.entity.VtuberEntity;
import com.example.Graduatcion_project.repository.VtuberRepository;
import com.example.Graduatcion_project.repository.ExceptVtuberRepository;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.ChannelListResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.io.IOException;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class UploadVtuberService {

    private final YouTube youTube;
    private final VtuberRepository vtuberRepository;
    private final ExceptVtuberRepository exceptVtuberRepository;
    private final List<String> apiKeys;
    private int currentKeyIndex = 0;
    private final List<String> queries = Arrays.asList(
            "버튜버", "Vtuber", "버츄얼 유튜버", "버츄버", "v-youtuber", "스텔라이브", "V-LUP", "이세계아이돌", "VRECORD",
            "일루전 라이브", "버츄얼 헤르츠", "싸이코드", "PLAVE", "츠라이컴퍼니", "러브다이아", "스타데이즈", "에스더",
            "라이브루리", "HeartSoniC"
    );
    private int currentQueryIndex = 0;
    private static final Logger logger = Logger.getLogger(VtuberService.class.getName());
    private static final int MIN_SUBSCRIBERS = 3000;

    private final List<String> excludeKeywordsInTitle = Arrays.asList("응원", "통계", "번역", "다시보기", "게임", "저장", "번역", "일상", "브이로그", "보관", "잼민", "TV", "코인", "주식", "Tj", "tv", "팬계정", "창고", "박스", "팬", "클립", "키리누키", "vlog", "유튜버", "youtube", "YOUTUBE", "유튜브", "코딩", "코드", "로블록스", "덕질", "음식", "기도", "교회", "여행", "VOD", "풀영상");
    private final List<String> excludeKeywordsInDescription = Arrays.asList("팬클립", "팬영상", "팬채널", "저장소", "브이로그", "학년", "초등학", "중학", "고등학", "코딩", "로블록스", "덕질", "음식" ,"지식", "협찬", "비지니스", "광고", "병맛", "종합게임", "종겜", "기도", "교회", "목사", "여행", "애니메이션", "개그", "코미디", "뷰티", "fashion", "패션", "vlog", "게임채널", "연주", "강의", "더빙", "일상", "다시보기", "풀영상", "쇼츠", "서브채널", "그림", "팬페이지", "개그맨", "다이어트", "4K", "커뮤니티", "악세사리", "쇼핑몰", "경제", "키리누키", "채널 옮겼습니다", "클립", "넷플릭스", "게이머", "팬 채널", "게임방송", "리뷰", "예술", "Fashion", "자기관리", "예능", "희극인", "장애인", "실물");

    private final List<String> priorityKeywords = Arrays.asList("버츄얼 유튜버", "버튜버", "V-Youtuber", "버튜얼 유튜버");

    public UploadVtuberService(YouTube youTube, VtuberRepository vtuberRepository, ExceptVtuberRepository exceptVtuberRepository, @Value("${youtube.api.keys}") String apiKeys) {
        this.youTube = youTube;
        this.vtuberRepository = vtuberRepository;
        this.exceptVtuberRepository = exceptVtuberRepository;
        this.apiKeys = Arrays.asList(apiKeys.split(","));
    }

    public void fetchAndSaveVtuberChannels() {
        Set<String> exceptChannelIds = new HashSet<>(exceptVtuberRepository.findAllChannelIds());
        logger.info("Except Channel IDs: " + exceptChannelIds);

        Set<String> processedChannelIds = new HashSet<>(vtuberRepository.findAllChannelIds());
        logger.info("Processed Channel IDs: " + processedChannelIds);

        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(5);

        for (String query : queries) {
            String pageToken = null;

            do {
                try {
                    YouTube.Search.List search = youTube.search().list("id,snippet");
                    search.setQ(query);
                    search.setType("channel");
                    search.setFields("nextPageToken,items(id/channelId,snippet/title,snippet/description)");
                    search.setMaxResults(50L);
                    search.setKey(apiKeys.get(currentKeyIndex));
                    search.setPageToken(pageToken);

                    SearchListResponse searchResponse = search.execute();
                    List<SearchResult> searchResultList = searchResponse.getItems();

                    List<String> channelIds = searchResultList.stream()
                            .map(result -> result.getId().getChannelId())
                            .filter(channelId -> !exceptChannelIds.contains(channelId)) // 필터링 추가
                            .filter(channelId -> !processedChannelIds.contains(channelId))
                            .collect(Collectors.toList());

                    logger.info("Filtered Channel IDs: " + channelIds);

                    if (!channelIds.isEmpty()) {
                        List<List<String>> partitions = partitionList(channelIds, 10);
                        for (List<String> partition : partitions) {
                            executor.submit(() -> processChannels(partition, exceptChannelIds, processedChannelIds));
                        }
                    }

                    pageToken = searchResponse.getNextPageToken();
                } catch (IOException e) {
                    System.err.println("API 호출 중 오류 발생: " + e.getMessage());
                    logger.severe("API 호출 중 오류 발생: " + e.getMessage());
                    currentKeyIndex = (currentKeyIndex + 1) % apiKeys.size();
                    System.err.println("다음 API 키로 전환: " + apiKeys.get(currentKeyIndex));
                    logger.warning("다음 API 키로 전환: " + apiKeys.get(currentKeyIndex));
                    if (currentKeyIndex == 0) {
                        System.err.println("모든 API 키가 사용되었습니다. 할당량이 리셋될 때까지 기다려야 합니다.");
                        logger.severe("모든 API 키가 사용되었습니다. 할당량이 리셋될 때까지 기다려야 합니다.");
                        return;
                    }
                }
            } while (pageToken != null);

            currentQueryIndex = (currentQueryIndex + 1) % queries.size();
        }

        executor.shutdown();
    }

    private void processChannels(List<String> channelIds, Set<String> exceptChannelIds, Set<String> processedChannelIds) {
        try {
            YouTube.Channels.List channelRequest = youTube.channels().list("snippet,statistics");
            channelRequest.setId(String.join(",", channelIds));
            channelRequest.setKey(apiKeys.get(currentKeyIndex));
            channelRequest.setFields("items(id,snippet/title,snippet/description,snippet/thumbnails/default/url,statistics/subscriberCount)");

            ChannelListResponse channelResponse = channelRequest.execute();
            List<Channel> channels = channelResponse.getItems();

            if (channels == null || channels.isEmpty()) {
                System.err.println("채널 정보를 가져오지 못했습니다.");
                logger.warning("채널 정보를 가져오지 못했습니다.");
                return;
            }

            for (Channel channel : channels) {
                String channelId = channel.getId();

                if (exceptChannelIds.contains(channelId) || processedChannelIds.contains(channelId)) {
                    logger.info("제외된 채널: " + channel.getSnippet().getTitle() + " - " + channelId);
                    continue;
                }

                if (isKoreanVtuber(channel) && channel.getStatistics().getSubscriberCount().compareTo(BigInteger.valueOf(MIN_SUBSCRIBERS)) >= 0) {
                    if (vtuberRepository.findByChannelId(channelId).isEmpty()) {
                        VtuberEntity vtuber = new VtuberEntity();
                        vtuber.setChannelId(channelId);
                        vtuber.setTitle(channel.getSnippet().getTitle());

                        String description = channel.getSnippet().getDescription();
                        if (description != null && description.length() > 255) {
                            description = description.substring(0, 255);
                        }
                        vtuber.setDescription(description);

                        vtuber.setName(channel.getSnippet().getTitle());
                        vtuber.setSubscribers(BigInteger.valueOf(channel.getStatistics().getSubscriberCount().longValue()));
                        vtuber.setAddedTime(LocalDateTime.now());

                        // 프로필 이미지 (썸네일 기본 사이즈) 저장
                        vtuber.setChannelImg(channel.getSnippet().getThumbnails().getDefault().getUrl());

                        vtuberRepository.save(vtuber);
                        processedChannelIds.add(channelId);
                    }
                } else {
                    logger.info("걸러진 채널: " + channel.getSnippet().getTitle() + " - " + channelId);
                }
            }
        } catch (IOException e) {
            System.err.println("API 호출 중 오류 발생: " + e.getMessage());
            logger.severe("API 호출 중 오류 발생: " + e.getMessage());
            currentKeyIndex = (currentKeyIndex + 1) % apiKeys.size();
            System.err.println("다음 API 키로 전환: " + apiKeys.get(currentKeyIndex));
            logger.warning("다음 API 키로 전환: " + apiKeys.get(currentKeyIndex));
        }
    }


    private boolean isKoreanVtuber(Channel channel) {
        String title = channel.getSnippet().getTitle();
        String description = channel.getSnippet().getDescription();

        logger.info("Checking channel: " + title + " - " + description);

        boolean containsKoreanInTitle = title.matches(".*[ㄱ-ㅎㅏ-ㅣ가-힣]+.*");
        boolean containsKoreanInDescription = description != null && description.matches(".*[ㄱ-ㅎㅏ-ㅣ가-힣]+.*");
        boolean containsVtuberInTitleOrDescription = title.contains("버튜버") || title.contains("Vtuber") ||
                (description != null && (description.contains("버튜버") || description.contains("Vtuber")));

        boolean containsPriorityKeyword = priorityKeywords.stream().anyMatch(keyword -> title.contains(keyword) || (description != null && description.contains(keyword)));

        boolean containsExcludeKeywordsInTitle = excludeKeywordsInTitle.stream().anyMatch(keyword -> title.contains(keyword));
        boolean containsExcludeKeywordsInDescription = excludeKeywordsInDescription.stream().anyMatch(keyword -> description != null && description.contains(keyword));

        logger.info("containsKoreanInTitle: " + containsKoreanInTitle);
        logger.info("containsKoreanInDescription: " + containsKoreanInDescription);
        logger.info("containsVtuberInTitleOrDescription: " + containsVtuberInTitleOrDescription);
        logger.info("containsPriorityKeyword: " + containsPriorityKeyword);
        logger.info("containsExcludeKeywordsInTitle: " + containsExcludeKeywordsInTitle);
        logger.info("containsExcludeKeywordsInDescription: " + containsExcludeKeywordsInDescription);

        // 제외 단어가 포함되어 있고, 우선순위 키워드가 포함되어 있지 않으면 제외
        if ((containsExcludeKeywordsInTitle || containsExcludeKeywordsInDescription) && !containsPriorityKeyword) {
            logger.info("제외된 이유 - 제목/설명에 제외 키워드 포함: " + title);
            return false;
        }

        boolean result = (containsKoreanInTitle || (containsKoreanInDescription && containsVtuberInTitleOrDescription)) || containsPriorityKeyword;
        logger.info("채널 필터링 결과: " + title + " - " + result);
        return result;
    }

    private List<List<String>> partitionList(List<String> list, int size) {
        List<List<String>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }

    // 유튜브 프로필 사진 미적용된거 일괄 db에 적용하는 코드
    @Transactional
    public void updateMissingProfileImages() {
        List<VtuberEntity> vtubersWithMissingImages = vtuberRepository.findByChannelImgIsNull();
        logger.info("Found " + vtubersWithMissingImages.size() + " vtubers with missing profile images.");

        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(5); // 멀티스레딩을 사용하여 API 호출

        for (VtuberEntity vtuber : vtubersWithMissingImages) {
            executor.submit(() -> {
                String imageUrl = fetchChannelProfileImage(vtuber.getChannelId());
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    vtuber.setChannelImg(imageUrl);
                    vtuberRepository.save(vtuber);
                    logger.info("Updated profile image for channel: " + vtuber.getName());
                } else {
                    logger.warning("Failed to fetch image for channel: " + vtuber.getName());
                }
            });
        }

        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.severe("Thread interrupted: " + e.getMessage());
        }
    }

    private String fetchChannelProfileImage(String channelId) {
        try {
            YouTube.Channels.List channelsList = youTube.channels().list("snippet");
            channelsList.setId(channelId);
            channelsList.setKey(apiKeys.get(currentKeyIndex));
            channelsList.setFields("items(snippet/thumbnails/default/url)");
            ChannelListResponse response = channelsList.execute();

            List<Channel> channels = response.getItems();
            if (!channels.isEmpty() && channels.get(0).getSnippet().getThumbnails() != null) {
                return channels.get(0).getSnippet().getThumbnails().getDefault().getUrl();
            }
        } catch (Exception e) {
            logger.warning("Failed to fetch channel image for channel ID: " + channelId + " - " + e.getMessage());
            currentKeyIndex = (currentKeyIndex + 1) % apiKeys.size(); // Rotate API key if there's an issue
        }
        return null;
    }

    @Scheduled(cron = "0 33 15 * * ?")  // 매일 새벽 1시에 실행
    public void scheduledUpdateMissingProfileImages() {
        updateMissingProfileImages();
    }

    /* 최초 버튜버 업데이트시만 사용
    @Scheduled(cron = "0 0 0 * * ?", zone = "Asia/Seoul")
    public void scheduledFetchAndSaveVtuberChannels() {
        fetchAndSaveVtuberChannels();
    }

    public VtuberEntity createVtuber(String description) {
        VtuberEntity vtuberEntity = new VtuberEntity();
        vtuberEntity.setDescription(description);
        vtuberEntity.setAddedTime(LocalDateTime.now());
        return vtuberRepository.save(vtuberEntity);
    }
     */

}
