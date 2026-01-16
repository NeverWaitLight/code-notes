# Security and Performance

## Security Requirements

**Frontend Security:**

- CSP Headers: 本地演示不强制，但建议开启基础 CSP
- XSS Prevention: 模板输出默认转义；禁止 `v-html` 直插用户输入
- Secure Storage: 不存储敏感信息

**Backend Security:**

- Input Validation: 上传文件类型与大小校验
- Rate Limiting: 演示环境不启用
- CORS Policy: 允许 `http://localhost:5173` 访问 `http://localhost:8080`

**Authentication Security:**

- Token Storage: N/A
- Session Management: N/A
- Password Policy: N/A

## Performance Optimization

**Frontend Performance:**

- Bundle Size Target: 小于 500KB（gzip，演示级）
- Loading Strategy: 路由按需加载
- Caching Strategy: 浏览器默认缓存即可

**Backend Performance:**

- Response Time Target: 列表/详情 < 500ms（本机）
- Database Optimization: 基于 `status` 索引
- Caching Strategy: 不使用缓存
