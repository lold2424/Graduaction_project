package com.example.Graduatcion_project.service;

import com.example.Graduatcion_project.entity.VtuberSongsEntity;
import com.example.Graduatcion_project.repository.VtuberRepository;
import com.example.Graduatcion_project.repository.VtuberSongsRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SongService {

    private final VtuberSongsRepository vtuberSongsRepository;
    private final VtuberRepository vtuberRepository;

    public SongService(VtuberSongsRepository vtuberSongsRepository, VtuberRepository vtuberRepository) {
        this.vtuberSongsRepository = vtuberSongsRepository;
        this.vtuberRepository = vtuberRepository;
    }

    // 완전히 랜덤한 비디오 노래 목록을 가져오는 메서드
    public List<VtuberSongsEntity> getRandomVideoSongs(int limit) {
        List<VtuberSongsEntity> randomSongs = vtuberSongsRepository.findRandomVideoSongs(limit);
        System.out.println("Fetched random songs: " + randomSongs.size());
        return randomSongs;
    }

    // 완전히 랜덤한 쇼츠 목록을 가져오는 메서드
    public List<VtuberSongsEntity> getRandomShortsSongs(int limit) {
        return vtuberSongsRepository.findRandomShortsSongs(limit);
    }


}
