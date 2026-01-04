package org.waitlight.codememo.proxy;


import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class LoomThreadMain {

    public static void main(String[] args) throws Exception {
        //访问 google，由于你懂得，会比较慢
        retrieveURLs(
                new URL("https://www.google.com/"),
                new URL("https://www.google.com/"),
                new URL("https://www.google.com/")
        );
    }

    private static void retrieveURLs(URL... urls) throws Exception {
        //创建虚拟线程线程池
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            //生成读取对每个 url 执行 getURL 方法的任务
            List<Callable<String>> tasks = Arrays.stream(urls)
                    .map(url -> (Callable<String>) () -> getURL(url))
                    .toList();
            //提交任务，等待并返回所有结果
            executor.invokeAll(tasks)
                    .stream()
                    .filter(future -> Future.State.SUCCESS.equals(future.state()))
                    .map(future -> {
                        try {
                            return future.get();
                        } catch (InterruptedException | ExecutionException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .forEach(System.out::println);
        }
    }

    // 读取url的内容
    private static String getURL(URL url) throws IOException {
        try (InputStream in = url.openStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
