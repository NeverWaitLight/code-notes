# Deployment Architecture

## Deployment Strategy

**Frontend Deployment:**

- **Platform:** Local host
- **Build Command:** `npm run build`
- **Output Directory:** `dist`
- **CDN/Edge:** N/A

**Backend Deployment:**

- **Platform:** Local host
- **Build Command:** `mvn package`
- **Deployment Method:** `java -jar target/*.jar`

## CI/CD Pipeline

```yaml
# 演示项目不要求 CI/CD
```

## Environments

| Environment | Frontend URL          | Backend URL           | Purpose           |
| ----------- | --------------------- | --------------------- | ----------------- |
| Development | http://localhost:5173 | http://localhost:8080 | Local development |
| Staging     | N/A                   | N/A                   | Not used          |
| Production  | N/A                   | N/A                   | Not used          |
