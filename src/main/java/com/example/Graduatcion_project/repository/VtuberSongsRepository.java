package com.example.Graduatcion_project.repository;

import com.example.Graduatcion_project.entity.VtuberSongsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VtuberSongsRepository extends JpaRepository<VtuberSongsEntity, Long> {
    Optional<VtuberSongsEntity> findByVideoId(String videoId);

    List<VtuberSongsEntity> findByStatus(String status);

    // 여러 개의 완전히 랜덤한 노래를 가져오는 쿼리 (classification이 video인 경우)
    @Query(value = "SELECT * FROM vtuber_songs WHERE classification = 'videos' ORDER BY RAND() LIMIT ?1", nativeQuery = true)
    List<VtuberSongsEntity> findRandomVideoSongs(int limit);

    // 여러 개의 완전히 랜덤한 쇼츠를 가져오는 쿼리 (classification이 shorts인 경우)
    @Query(value = "SELECT * FROM vtuber_songs WHERE classification = 'shorts' ORDER BY RAND() LIMIT ?1", nativeQuery = true)
    List<VtuberSongsEntity> findRandomShortsSongs(int limit);

    // 7일 기준 조회수 최상위 정렬
    @Query(value = "SELECT * FROM vtuber_songs WHERE status = ?1 ORDER BY views_increase_week DESC LIMIT 10", nativeQuery = true)
    List<VtuberSongsEntity> findTop10ByStatusOrderByViewsIncreaseWeekDesc(String status);

    // 1일 기준 조회수 최상위 정렬
    @Query(value = "SELECT * FROM vtuber_songs ORDER BY views_increase_day DESC LIMIT 10", nativeQuery = true)
    List<VtuberSongsEntity> findTop10ByStatusOrderByViewsIncreaseDayDesc();

    // 최신순 정렬
    @Query(value = "SELECT * FROM vtuber_songs ORDER BY published_at DESC LIMIT 10", nativeQuery = true)
    List<VtuberSongsEntity> findTop10ByPublishedAtDesc();

    // 페이징을 통한 검색과 분류를 위한 메서드
    List<VtuberSongsEntity> findAllByTitleContainingAndClassificationOrderByViewCountDesc(String title, String classification);

    List<VtuberSongsEntity> findAllByVtuberNameOrderByViewCountDesc(String vtuberName);

}
