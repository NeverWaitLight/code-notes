package org.waitlight.bridgeway.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;


@Slf4j
@RestController
@RequestMapping("/files")
public class FileProxyController {

    @PostMapping(value = "/upload")
    public void upload(@RequestPart("file") MultipartFile file,
                       @RequestParam("token") String token) throws IOException {
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        CloseableHttpResponse response = null;
        try {
            HttpPost httpPost = new HttpPost("https://cloud.yiyangxxkj.com/ic/v1/aliyun/oss/upload?token" + token);
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addBinaryBody("file", file.getBytes(), ContentType.DEFAULT_BINARY, file.getName());
            HttpEntity multipart = builder.build();
            httpPost.setEntity(multipart);
            response = httpClient.execute(httpPost);

            HttpEntity responseEntity = response.getEntity();
            String responseBody = EntityUtils.toString(responseEntity, StandardCharsets.UTF_8);
            log.info("Response {}", responseBody);
        } finally {
            if (Objects.nonNull(response)) response.close();
            if (Objects.nonNull(httpClient)) httpClient.close();
        }
    }
}
