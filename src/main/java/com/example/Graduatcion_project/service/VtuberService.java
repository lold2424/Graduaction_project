package com.example.Graduatcion_project.service;

import com.example.Graduatcion_project.entity.VtuberEntity;
import com.example.Graduatcion_project.entity.VtuberSongsEntity;
import com.example.Graduatcion_project.repository.VtuberRepository;
import com.example.Graduatcion_project.repository.VtuberSongsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class VtuberService {
    private final VtuberRepository vtuberRepository;
    private final VtuberSongsRepository vtuberSongsRepository;
    private static final Logger logger = LoggerFactory.getLogger(VtuberService.class);

    public VtuberService(VtuberRepository vtuberRepository, VtuberSongsRepository vtuberSongsRepository) {
        this.vtuberRepository = vtuberRepository;
        this.vtuberSongsRepository = vtuberSongsRepository;
    }

    public VtuberEntity createVtuber(String description) {
        VtuberEntity newVtuber = new VtuberEntity();
        newVtuber.setDescription(description);
        return vtuberRepository.save(newVtuber); // DB에 저장
    }

    public Map<String, Object> searchVtubersAndSongs(String query, String channelId) {
        Map<String, Object> result = new HashMap<>();

        // 채널 ID가 제공된 경우 해당 버튜버와 그 버튜버의 노래만 검색
        if (channelId != null && !channelId.isEmpty()) {
            Optional<VtuberEntity> vtuber = vtuberRepository.findByChannelId(channelId);
            if (vtuber.isPresent()) {
                List<VtuberEntity> vtubers = List.of(vtuber.get()); // 단일 버튜버를 리스트에 담음
                result.put("vtubers", vtubers);

                // 해당 버튜버의 노래 검색
                List<VtuberSongsEntity> songs = vtuberSongsRepository.findAllByVtuberNameOrderByViewCountDesc(vtuber.get().getName());
                result.put("songs", songs);
            } else {
                result.put("vtubers", Collections.emptyList());
                result.put("songs", Collections.emptyList());
            }
        } else if (query != null && !query.isEmpty()) {
            // 제목에 검색어가 포함된 노래 목록을 조회수 순으로 정렬하여 가져옴
            List<VtuberSongsEntity> songs = vtuberSongsRepository.findAllByTitleContainingAndClassificationOrderByViewCountDesc(query, "videos");
            List<VtuberEntity> vtubers = vtuberRepository.findAllByNameContaining(query);
            result.put("songs", songs);
            result.put("vtubers", vtubers);
        } else {
            // 검색어와 채널 ID가 모두 없는 경우 빈 값 반환
            result.put("vtubers", Collections.emptyList());
            result.put("songs", Collections.emptyList());
        }

        return result;
    }
}
