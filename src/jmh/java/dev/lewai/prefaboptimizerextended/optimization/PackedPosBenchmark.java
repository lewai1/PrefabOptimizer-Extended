package dev.lewai.prefaboptimizerextended.optimization;

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class PackedPosBenchmark {

    @Param({"32", "128"})
    public int size;

    private int[] xs;
    private int[] ys;
    private int[] zs;

    @Setup
    public void setup() {
        int n = this.size * this.size * this.size;
        this.xs = new int[n];
        this.ys = new int[n];
        this.zs = new int[n];
        int i = 0;
        for (int x = 0; x < this.size; x++) {
            for (int y = 0; y < this.size; y++) {
                for (int z = 0; z < this.size; z++) {
                    this.xs[i] = x;
                    this.ys[i] = y;
                    this.zs[i] = z;
                    i++;
                }
            }
        }
    }

    @Benchmark
    public void packAll(Blackhole bh) {
        for (int i = 0; i < this.xs.length; i++) {
            bh.consume(PackedPos.pack(this.xs[i], this.ys[i], this.zs[i]));
        }
    }

    @Benchmark
    public void packAndUnpack(Blackhole bh) {
        for (int i = 0; i < this.xs.length; i++) {
            long key = PackedPos.pack(this.xs[i], this.ys[i], this.zs[i]);
            bh.consume(PackedPos.unpackX(key));
            bh.consume(PackedPos.unpackY(key));
            bh.consume(PackedPos.unpackZ(key));
        }
    }
}
