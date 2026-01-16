# Testing Strategy

## Testing Pyramid

```text
E2E Tests
/        \
Integration Tests
/            \
Frontend Unit  Backend Unit
```

## Test Organization

**Frontend Tests**

```
frontend/
  tests/
    components/
    pages/
```

**Backend Tests**

```
backend/
  src/test/java/com/example/videodemo/
    controller/
    service/
    repository/
```

**E2E Tests**

```
N/A (手工冒烟)
```

## Test Examples

**Frontend Component Test**

```typescript
// VideoListItem.spec.ts
import { mount } from "@vue/test-utils";
import VideoListItem from "@/components/VideoListItem.vue";

test("renders title", () => {
  const wrapper = mount(VideoListItem, {
    props: { video: { id: 1, title: "demo", status: "READY" } },
  });
  expect(wrapper.text()).toContain("demo");
});
```

**Backend API Test**

```java
@SpringBootTest
@AutoConfigureMockMvc
class VideoControllerTest {
  @Autowired MockMvc mvc;

  @Test
  void listShouldReturnOk() throws Exception {
    mvc.perform(get("/api/videos"))
       .andExpect(status().isOk());
  }
}
```

**E2E Test**

```
N/A (手工冒烟)
```
