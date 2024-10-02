import React, { useEffect, useState } from 'react';
import axios from 'axios';
import './MainPage.css'; // MainPage 전용 CSS 파일
import VideoCard from './components/VideoCard'; // 비디오 카드 컴포넌트

const MainPage: React.FC = () => {
    const [data, setData] = useState<any>({
        randomSongs: [],
        top10RecentSongs: [],
        top10DailySongs: [],
        top10WeeklySongs: [], // 주간 인기 차트 데이터
    });
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        axios.get('/main') // 백엔드에서 데이터 가져오기
            .then((response) => {
                console.log(response.data);
                setData({
                    randomSongs: response.data.randomSongs || [],
                    top10RecentSongs: response.data.top10RecentSongs || [],
                    top10DailySongs: response.data.top10DailySongs || [],
                    top10WeeklySongs: response.data.top10WeeklySongs || [], // 주간 인기 차트 데이터 설정
                });
                setIsLoading(false);
            })
            .catch((error) => {
                console.error('백엔드에서 데이터 가져오기 오류:', error);
                setIsLoading(false);
            });
    }, []);

    if (isLoading) {
        return <p>로딩 중...</p>;
    }

    if (!data.randomSongs.length && !data.top10RecentSongs.length && !data.top10DailySongs.length && !data.top10WeeklySongs.length) {
        return <p>데이터가 없습니다.</p>;
    }

    return (
        <div className="main-layout">
            {/* 왼쪽 8 부분: 메인 콘텐츠 */}
            <div className="content">
                {/* 랜덤 노래 섹션 */}
                <section>
                    <h2>랜덤 노래</h2>
                    <div className="video-grid">
                        {data.randomSongs.length > 0 ? (
                            data.randomSongs.map((song: any) => (
                                <VideoCard key={song.id} song={song} />
                            ))
                        ) : (
                            <p>랜덤 노래가 없습니다.</p>
                        )}
                    </div>
                </section>

                {/* 최신 노래 섹션 */}
                <section>
                    <h2>최신 노래</h2>
                    <div className="video-grid">
                        {data.top10RecentSongs.length > 0 ? (
                            data.top10RecentSongs.map((song: any) => (
                                <VideoCard key={song.id} song={song} />
                            ))
                        ) : (
                            <p>최신 노래가 없습니다.</p>
                        )}
                    </div>
                </section>

                {/* 일간 인기 노래 섹션 */}
                <section>
                    <h2>인기 있어요 (일간)</h2>
                    <div className="video-grid">
                        {data.top10DailySongs.length > 0 ? (
                            data.top10DailySongs.map((song: any) => (
                                <VideoCard key={song.id} song={song} />
                            ))
                        ) : (
                            <p>인기 있는 노래가 없습니다.</p>
                        )}
                    </div>
                </section>
            </div>
        </div>
    );
};

export default MainPage;
