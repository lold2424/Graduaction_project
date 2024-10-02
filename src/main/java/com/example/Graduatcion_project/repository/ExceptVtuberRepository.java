package com.example.Graduatcion_project.repository;

import com.example.Graduatcion_project.entity.ExceptVtuberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExceptVtuberRepository extends JpaRepository<ExceptVtuberEntity, Long> {

    @Query("SELECT e.channelId FROM ExceptVtuberEntity e")
    List<String> findAllChannelIds();
}
