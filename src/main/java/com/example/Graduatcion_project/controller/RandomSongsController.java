package com.example.Graduatcion_project.controller;

import com.example.Graduatcion_project.entity.VtuberSongsEntity;
import com.example.Graduatcion_project.service.SongService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/random-songs")
public class RandomSongsController {

    private final SongService SongService;

    public RandomSongsController(SongService SongService) {
        this.SongService = SongService;
    }

    @GetMapping("/random")
    public List<VtuberSongsEntity> getRandomVideoSongs(@RequestParam(defaultValue = "6") int limit) {
        return SongService.getRandomVideoSongs(limit);
    }
    @GetMapping("/random-shorts")
    public List<VtuberSongsEntity> getRandomShortsSongs(@RequestParam(defaultValue = "6") int limit) {
        return SongService.getRandomShortsSongs(limit);
    }
}
