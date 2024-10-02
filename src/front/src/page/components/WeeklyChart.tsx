import React from 'react';
import './WeeklyChart.css'; // 스타일 파일

interface WeeklyChartProps {
    top10WeeklySongs: {
        id: string;
        title: string;
        artist: string;
        videoId: string;  // videoId를 추가
    }[];
}

const WeeklyChart: React.FC<WeeklyChartProps> = ({ top10WeeklySongs }) => {

    // 유튜브 링크로 이동하는 함수
    const handleSongClick = (videoId: string) => {
        const youtubeUrl = `https://www.youtube.com/watch?v=${videoId}`;
        window.open(youtubeUrl, '_blank'); // 새 탭에서 유튜브 페이지로 이동
    };

    return (
        <div className="weekly-chart">
            <h3>주간 인기 차트</h3>
            <ul className="chart-list">
                {top10WeeklySongs.length > 0 ? (
                    top10WeeklySongs.map((song, index) => (
                        <li
                            key={song.id}
                            className="chart-item"
                            onClick={() => handleSongClick(song.videoId)}  // 클릭 이벤트 추가
                            style={{ cursor: 'pointer', color: 'black' }}  // 스타일 수정
                        >
                            <h4>{index + 1}. {song.title}</h4>
                            <span>{song.artist}</span>
                        </li>
                    ))
                ) : (
                    <p>주간 인기 차트가 없습니다.</p>
                )}
            </ul>
            <div className="chart-footer">월요일 00시 기준</div>
        </div>
    );
};

export default WeeklyChart;
