package com.example.Graduatcion_project.controller;

import com.example.Graduatcion_project.dto.MainPageResponse;
import com.example.Graduatcion_project.entity.VtuberSongsEntity;
import com.example.Graduatcion_project.service.SongService;
import com.example.Graduatcion_project.service.SortSongsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/main")
public class MainController {

    private final SongService SongService;
    private final SortSongsService sortSongsService;

    public MainController(SongService SongService , SortSongsService sortSongsService) {
        this.SongService = SongService;
        this.sortSongsService = sortSongsService;
    }

    @GetMapping
    public MainPageResponse getMainPage() {
        List<VtuberSongsEntity> RandomVideoSongs = SongService.getRandomVideoSongs(6);
        List<VtuberSongsEntity> top10WeeklySongs = sortSongsService.getTop10SongsByViewsIncreaseWeek();
        List<VtuberSongsEntity> top10DailySongs = sortSongsService.getTop10SongsByViewsIncreaseDay();
        List<VtuberSongsEntity> top10RecentSongs = sortSongsService.getTop10SongsByPublishedAt();
        return new MainPageResponse(RandomVideoSongs, top10WeeklySongs, top10DailySongs, top10RecentSongs);
    }
}
