package com.example.Graduatcion_project.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "vtuber_songs")
public class VtuberSongsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String channelId;

    private String videoId;

    private String title;

    private String description;

    private LocalDateTime publishedAt;

    private LocalDateTime addedTime;

    private String vtuberName;

    @Column(length = 50)
    private String classification;

    private Long viewCount; // 조회수

    private Long lastWeekViewCount; // 저번 주 조회수

    private Long viewsIncreaseDay;  // 1일 후 조회수 상승량

    private Long viewsIncreaseWeek; // 7일 후 조회수 상승량

    private LocalDateTime updateDayTime;   // 1일 업데이트 시간

    private LocalDateTime updateWeekTime;  // 주간 업데이트 시간

    private String status;


    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getVideoId() {
        return videoId;
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public LocalDateTime getAddedTime() {
        return addedTime;
    }

    public void setAddedTime(LocalDateTime addedTime) {
        this.addedTime = addedTime;
    }

    public String getVtuberName() {
        return vtuberName;
    }

    public void setVtuberName(String vtuberName) {
        this.vtuberName = vtuberName;
    }

    public String getClassification() {
        return classification;
    }

    public void setClassification(String classification) {
        this.classification = classification;
    }

    public Long getViewCount() {
        return viewCount;
    }

    public void setViewCount(Long viewCount) {
        this.viewCount = viewCount;
    }

    public Long getViewsIncreaseDay() {
        return viewsIncreaseDay;
    }

    public void setViewsIncreaseDay(Long viewsIncreaseDay) {
        this.viewsIncreaseDay = viewsIncreaseDay;
    }

    public Long getViewsIncreaseWeek() {
        return viewsIncreaseWeek;
    }

    public void setViewsIncreaseWeek(Long viewsIncreaseWeek) {
        this.viewsIncreaseWeek = viewsIncreaseWeek;
    }

    public LocalDateTime getUpdateDayTime() {
        return updateDayTime;
    }

    public void setUpdateDayTime(LocalDateTime updateDayTime) {
        this.updateDayTime = updateDayTime;
    }

    public LocalDateTime getUpdateWeekTime() {
        return updateWeekTime;
    }

    public void setUpdateWeekTime(LocalDateTime updateWeekTime) {
        this.updateWeekTime = updateWeekTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }


    public Long getLastWeekViewCount() {
        return lastWeekViewCount;
    }

    public void setLastWeekViewCount(Long lastWeekViewCount) {
        this.lastWeekViewCount = lastWeekViewCount;
    }
}