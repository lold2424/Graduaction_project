package com.example.Graduatcion_project.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "except_vtubers")
public class ExceptVtuberEntity {
    @Id
    private String channelId;

    // Getter and Setter
    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }
}
