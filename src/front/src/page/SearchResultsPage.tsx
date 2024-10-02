import React, { useEffect, useState } from 'react';
import axios from 'axios';
import './SearchResultsPage.css'; // SearchResultsPage 전용 CSS 파일
import { useLocation, useNavigate } from 'react-router-dom'; // useNavigate 사용
import VideoCard from './components/VideoCard'; // VideoCard 컴포넌트 가져오기

const SearchResultsPage: React.FC = () => {
    const [searchResults, setSearchResults] = useState<{ songs: any[], vtubers: any[] } | null>(null);
    const [visibleSongs, setVisibleSongs] = useState<any[]>([]);
    const [songPage, setSongPage] = useState(1);
    const [hasMoreSongs, setHasMoreSongs] = useState(true);

    const location = useLocation();
    const navigate = useNavigate();
    const query = new URLSearchParams(location.search).get('query');
    const channelId = new URLSearchParams(location.search).get('channelId'); // channelId 가져옴

    useEffect(() => {
        // channelId 또는 query가 있을 때 API 호출
        if (query || channelId) {
            axios.get('http://localhost:8080/api/v1/vtubers/search', {
                params: { query, channelId } // query 또는 channelId 전달
            })
                .then((response) => {
                    console.log('Search results:', response.data); // 결과 로그 확인
                    if (response.data) {
                        setSearchResults(response.data);
                        setVisibleSongs(response.data.songs.slice(0, 10)); // 처음 10개 노출
                    }
                })
                .catch((error) => {
                    console.error('검색 중 오류 발생:', error);
                });
        }
    }, [query, channelId]); // query와 channelId가 변경될 때마다 호출

    const loadMoreSongs = () => {
        if (!searchResults) return; // searchResults가 null인 경우 처리

        const nextPage = songPage + 1;
        const newSongs = searchResults.songs.slice(visibleSongs.length, visibleSongs.length + 10);
        setVisibleSongs([...visibleSongs, ...newSongs]); // 10개씩 추가로 보이기
        setSongPage(nextPage);

        if (newSongs.length < 10) {
            setHasMoreSongs(false); // 더 이상 노래가 없으면 더보기 버튼 숨김
        }
    };

    return (
        <div className="search-results-page">
            <h1>검색 결과</h1>

            {/* 버튜버 채널 결과 */}
            {searchResults?.vtubers && searchResults.vtubers.length > 0 && (
                <div>
                    <h2>버튜버 채널</h2>
                    <div className="vtuber-grid">
                        {searchResults.vtubers.map((vtuber: any) => (
                            <div className="vtuber-card" key={vtuber.channelId}>
                                <img src={vtuber.channelImg} alt={vtuber.name} />
                                <div className="vtuber-info">
                                    <h3>{vtuber.name}</h3>
                                    <p>구독자 수: {vtuber.subscribers}</p>
                                    <button onClick={() => window.open(`https://www.youtube.com/channel/${vtuber.channelId}`, '_blank')}>
                                        채널로 이동
                                    </button>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            )}

            {/* 노래 결과 */}
            <div>
                <div className="video-grid">
                    {visibleSongs.length > 0 ? (
                        visibleSongs.map((song: any) => (
                            <VideoCard key={song.id} song={song} />
                        ))
                    ) : (
                        <p>검색된 노래가 없습니다.</p>
                    )}
                </div>

                {/* 더보기 버튼 */}
                {hasMoreSongs && (
                    <button onClick={loadMoreSongs}>더보기</button>
                )}
            </div>
        </div>
    );
};

export default SearchResultsPage;
