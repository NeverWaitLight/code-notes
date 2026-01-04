package org.waitlight.codememo.simple;

import java.util.concurrent.RecursiveTask;
import java.util.stream.IntStream;

public class MyRecursiveTask extends RecursiveTask<Integer> {
    static final int THRESHOLD = 10;
    private final int[] data;  // 存储数据的数组
    private final int start;  // 数据的起始位置
    private final int end;  // 数据的结束位置


    public MyRecursiveTask(int[] data, int start, int end) {
        this.data = data;
        this.start = start;
        this.end = end;
    }

    @Override
    protected Integer compute() {
        // 如果任务大小小于等于阈值则直接计算
        if (end - start <= THRESHOLD) {
            int sum = 0;
            // 遍历任务，求和
            for (int i = start; i < end; i++) {
                sum += data[i];
            }
            System.out.println(">> calc");
            return sum;
        } else {
            // 将任务划分为两部分
            int mid = (start + end) / 2;
            MyRecursiveTask left = new MyRecursiveTask(data, start, mid);
            MyRecursiveTask right = new MyRecursiveTask(data, mid, end);
            left.fork();
            right.fork();
            System.out.println(">> fork");
            // 等待两个子任务执行完毕，并返回结果相加
            return left.join() + right.join();
        }
    }

    public static void main(String[] args) {
        int[] data = IntStream.range(0, 1000).toArray();
        System.out.println(new MyRecursiveTask(data, 0, data.length).compute());
    }

}