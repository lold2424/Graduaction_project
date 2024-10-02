package com.example.Graduatcion_project.entity;

import javax.persistence.*;
import java.math.BigInteger;
import java.time.LocalDateTime;

@Entity
@Table(name = "vtubers")
public class VtuberEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String channelId;
    private String name;
    private String title;
    @Column(length = 255)
    private String description;
    private BigInteger subscribers;
    private LocalDateTime addedTime; // 추가된 시간 필드
    private String channelImg;

    // Getter와 Setter
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public BigInteger getSubscribers() {
        return subscribers;
    }

    public void setSubscribers(BigInteger subscribers) {
        this.subscribers = subscribers;
    }

    public LocalDateTime getAddedTime() {
        return addedTime;
    }

    public void setAddedTime(LocalDateTime addedTime) {
        this.addedTime = addedTime;
    }

    public String getChannelImg() {
        return channelImg;
    }

    public void setChannelImg(String channelImg) {
        this.channelImg = channelImg;
    }
}
