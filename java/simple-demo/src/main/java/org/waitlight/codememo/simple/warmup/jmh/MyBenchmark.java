package org.waitlight.codememo.simple.warmup.jmh;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

@State(Scope.Thread)
public class MyBenchmark {

    @Benchmark
    public void testMethod() {
        // place your benchmarked code here
    }

    @Setup
    public void warmup() {
    }
}

