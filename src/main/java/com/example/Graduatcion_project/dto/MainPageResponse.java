package com.example.Graduatcion_project.dto;

import com.example.Graduatcion_project.entity.VtuberSongsEntity;
import java.util.List;

public class MainPageResponse {
    private List<VtuberSongsEntity> randomSongs; // 랜덤한 노래 추가
    private List<VtuberSongsEntity> top10WeeklySongs; //
    private List<VtuberSongsEntity> top10DailySongs; //
    private List<VtuberSongsEntity> top10RecentSongs; // 최신순 10개 필드 추가
    public MainPageResponse(List<VtuberSongsEntity> randomSongs, List<VtuberSongsEntity> top10WeeklySongs, List<VtuberSongsEntity> top10DailySongs, List<VtuberSongsEntity> top10RecentSongs) {
        this.randomSongs = randomSongs;
        this.top10WeeklySongs = top10WeeklySongs;
        this.top10DailySongs = top10DailySongs;
        this.top10RecentSongs = top10RecentSongs;
    }

    public List<VtuberSongsEntity> getRandomSongs() {
        return randomSongs;
    }

    public void setRandomSongs(List<VtuberSongsEntity> randomSongs) {
        this.randomSongs = randomSongs;
    }

    public List<VtuberSongsEntity> getTop10WeeklySongs() {
        return top10WeeklySongs;
    }

    public void setTop10WeeklySongs(List<VtuberSongsEntity> top10WeeklySongs) {
        this.top10WeeklySongs = top10WeeklySongs;
    }

    public List<VtuberSongsEntity> getTop10DailySongs() {
        return top10DailySongs;
    }

    public void setTop10DailySongs(List<VtuberSongsEntity> top10DailySongs) {
        this.top10DailySongs = top10DailySongs;
    }

    public List<VtuberSongsEntity> getTop10RecentSongs() {
        return top10RecentSongs;
    }

    public void setTop10RecentSongs(List<VtuberSongsEntity> top10RecentSongs) {
        this.top10RecentSongs = top10RecentSongs;
    }
}
