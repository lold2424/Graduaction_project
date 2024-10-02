import React, { useState, useEffect } from 'react';
import './App.css';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Header from './page/components/Header';
import MainPage from './page/MainPage';
import SearchResultsPage from './page/SearchResultsPage';
import WeeklyChart from './page/components/WeeklyChart';
import axios from 'axios';

const App: React.FC = () => {
    const [top10WeeklySongs, setTop10WeeklySongs] = useState<any[]>([]);
    const [isLoading, setIsLoading] = useState(true); // 로딩 상태 추가
    const [error, setError] = useState<string | null>(null); // 에러 상태 추가

    useEffect(() => {
        // 동일한 /main 경로에서 데이터 가져오기
        axios.get('/main') // 메인 페이지와 동일한 경로 사용
            .then((response) => {
                // 주간 인기 차트 데이터 추출
                setTop10WeeklySongs(response.data.top10WeeklySongs || []); // 데이터가 없을 경우 빈 배열 설정
                setIsLoading(false); // 데이터 로드 완료
            })
            .catch((error) => {
                console.error('주간 인기 차트 데이터를 가져오는 중 오류 발생:', error);
                setError('주간 인기 차트를 불러오는 중 오류가 발생했습니다.'); // 에러 상태 설정
                setIsLoading(false); // 로딩 완료
            });
    }, []);

    return (
        <Router>
            <div className="app-container">
                <Header />
                <div className="main-layout">
                    <div className="content">
                        <Routes>
                            <Route path="/" element={<MainPage />} />
                            <Route path="/search" element={<SearchResultsPage />} />
                        </Routes>
                    </div>
                    <div className="sidebar">
                        {/* 주간 인기 차트 렌더링: 로딩 중이거나 에러일 경우 처리 */}
                        {isLoading ? (
                            <p>주간 인기 차트를 불러오는 중입니다...</p>
                        ) : error ? (
                            <p>{error}</p>
                        ) : (
                            <WeeklyChart top10WeeklySongs={top10WeeklySongs} />
                        )}
                    </div>
                </div>
            </div>
        </Router>
    );
};

export default App;
