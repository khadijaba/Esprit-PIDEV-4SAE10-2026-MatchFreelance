const PROXY_CONFIG = [
  {
    context: ['/api'],
    target: 'http://localhost:8081',
    secure: false,
    changeOrigin: true,
  },
];

module.exports = PROXY_CONFIG;
