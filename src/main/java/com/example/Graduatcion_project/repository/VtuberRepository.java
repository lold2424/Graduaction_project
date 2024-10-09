package com.example.Graduatcion_project.repository;

import com.example.Graduatcion_project.entity.VtuberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VtuberRepository extends JpaRepository<VtuberEntity, Long> {
    // Channel ID로 버튜버 찾기
    Optional<VtuberEntity> findByChannelId(String channelId);

    // 모든 채널 ID를 가져오는 쿼리
    @Query("SELECT v.channelId FROM VtuberEntity v")
    List<String> findAllChannelIds();

    // channelImg 필드가 null인 VtuberEntity 리스트를 반환하는 메소드
    List<VtuberEntity> findByChannelImgIsNull();

    // 버튜버 이름으로 검색하기 (대소문자 무시)
    List<VtuberEntity> findAllByNameContaining(String name);
}
