import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';  // 페이지 이동을 위한 useNavigate 훅 사용
import './Header.css';  // 스타일 정의

interface HeaderProps {
    onSearch?: (searchTerm: string) => void; // 선택적으로 받도록 설정
}

const Header: React.FC<HeaderProps> = ({ onSearch }) => {
    const [searchTerm, setSearchTerm] = useState('');
    const navigate = useNavigate();

    const handleSearch = () => {
        if (!searchTerm.trim()) return;
        if (onSearch) {
            onSearch(searchTerm);  // onSearch가 있는 경우 실행
        } else {
            navigate(`/search?query=${searchTerm}`); // 기본 동작으로 페이지 이동
        }
    };

    const handleKeyPress = (event: React.KeyboardEvent<HTMLInputElement>) => {
        if (event.key === 'Enter') {
            handleSearch();
        }
    };

    const handleClear = () => {
        setSearchTerm('');  // 검색어 초기화
    };

    const handleGoHome = () => {
        navigate('/');  // 홈으로 이동
    };

    return (
        <header className="header">
            <button className="home-btn" onClick={handleGoHome}>
                홈
            </button>

            <div className="search-bar">
                <input
                    type="text"
                    placeholder="검색"
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    onKeyPress={handleKeyPress}  // Enter 키 입력 시 검색
                />
                {searchTerm && (
                    <button type="button" className="clear-btn" onClick={handleClear}>
                        &#x2715; {/* X 아이콘 */}
                    </button>
                )}
                <button type="submit" className="search-btn" onClick={handleSearch}>
                    <img src="/images/SearchBar.png" alt="Search" />
                </button>
            </div>
        </header>
    );
};

export default Header;
