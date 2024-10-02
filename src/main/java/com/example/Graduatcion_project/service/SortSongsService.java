package com.example.Graduatcion_project.service;

import com.example.Graduatcion_project.entity.VtuberSongsEntity;
import com.example.Graduatcion_project.repository.VtuberSongsRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SortSongsService {

    private final VtuberSongsRepository vtuberSongsRepository;

    public SortSongsService(VtuberSongsRepository vtuberSongsRepository) {
        this.vtuberSongsRepository = vtuberSongsRepository;
    }

    public List<VtuberSongsEntity> getTop10SongsByViewsIncreaseWeek() {
        return vtuberSongsRepository.findTop10ByStatusOrderByViewsIncreaseWeekDesc("existing");
    }

    public List<VtuberSongsEntity> getTop10SongsByViewsIncreaseDay() {
        return vtuberSongsRepository.findTop10ByStatusOrderByViewsIncreaseDayDesc();
    }

    public List<VtuberSongsEntity> getTop10SongsByPublishedAt() {
        return vtuberSongsRepository.findTop10ByPublishedAtDesc();
    }
}
