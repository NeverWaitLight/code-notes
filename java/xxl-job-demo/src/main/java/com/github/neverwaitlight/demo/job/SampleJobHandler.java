package com.github.neverwaitlight.demo.job;

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;

@Component
public class SampleJobHandler {

    private static final Logger logger = LoggerFactory.getLogger(SampleJobHandler.class);

    /**
     * 任务需要在admin页面中配置才会运行
     */
    @XxlJob("addRandomNumbersJob")
    public void addRandomNumbers() throws Exception {
        XxlJobHelper.log("开始执行随机数相加任务");

        Random random = new Random();
        int a = random.nextInt(100);
        int b = random.nextInt(100);
        int sum = a + b;

        XxlJobHelper.log("随机数a: {}, 随机数b: {}, 相加结果: {}", a, b, sum);
        logger.info("随机数相加结果: {}", sum);
    }
}
