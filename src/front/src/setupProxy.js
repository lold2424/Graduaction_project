const { createProxyMiddleware } = require('http-proxy-middleware');

module.exports = function (app) {
    app.use(
        '/main', // 프록시할 경로 (예: "/api" 경로)
        createProxyMiddleware({
            target: 'http://localhost:8080', // 백엔드 서버 주소
            changeOrigin: true,
        })
    );
};
