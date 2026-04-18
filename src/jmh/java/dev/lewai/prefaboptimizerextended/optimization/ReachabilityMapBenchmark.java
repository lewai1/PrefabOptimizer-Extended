package dev.lewai.prefaboptimizerextended.optimization;

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
public class ReachabilityMapBenchmark {

    @Param({"32", "64", "128"})
    public int size;

    private ReachabilityMap map;

    @Setup(Level.Invocation)
    public void setup() {
        this.map = new ReachabilityMap(0, this.size - 1, 0, this.size - 1, 0, this.size - 1);
    }

    @Benchmark
    public void fillAll() {
        for (int x = 0; x < this.size; x++) {
            for (int y = 0; y < this.size; y++) {
                for (int z = 0; z < this.size; z++) {
                    this.map.add(x, y, z);
                }
            }
        }
    }

    @Benchmark
    public void checkContainsOnEmpty(Blackhole bh) {
        for (int x = 0; x < this.size; x++) {
            for (int y = 0; y < this.size; y++) {
                for (int z = 0; z < this.size; z++) {
                    bh.consume(this.map.contains(x, y, z));
                }
            }
        }
    }
}
