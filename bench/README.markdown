# Benchmarks

To run ScalaCheck's benchmarks, run the following command from SBT:

```
bench/jmh:run -wi 5 -i 5 -f1 -t1 org.scalacheck.bench.GenBench.*
```

The required parameters are:

 * `-wi` the number of warmup intervals to run
 * `-i` the number of benchmarking intervals to run
 * `-f` the number of forked processes to use during benchmarking
 * `-t` the number of threads to use during benchmarking

Smaller numbers will run faster but with less accuracy.

For more information about how we benchmark ScalaCheck, please see the
comments in `GenBench.scala`.
