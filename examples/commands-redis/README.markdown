# Testing Redis

This example project tests the [Redis](http://redis.io) database with
ScalaCheck's `Commands`
[API](http://www.scalacheck.org/files/scalacheck_2.11-1.12.0-SNAPSHOT-api/org/scalacheck/commands/Commands.html)
for property-based stateful testing.

## Running the tests

Start a local Redis server:

```
$ redis-server
```

The redis server listens on port `6379` by default, and that port is also
hard-coded in the example code. You can of course change both the redis host
and port used in [CommandsRedis](./src/test/scala/CommandsRedis.scala), if you want.

Then fire up this project in `sbt`:

```
$ sbt
```

Run the tests with:

```
> test
```

By default, ScalaCheck runs 100 tests, with a test size varying between 0 and
100. For the `Commands` API, this means that 100 distinct command sequences
will be run against the Redis server. Each sequence will consist of 0 to 100
commands. You can tweak these parameters:

```
> testOnly CommandsRedis -- -minSuccessfulTests 500 -minSize 50 -maxSize 100
```

The above example will run 500 distinct command sequences, with lengths between
50 and 100.

## What is tested?

Look at the `Command` implementations in
[CommandRedis](./src/test/scala/CommandsRedis.scala). At the time of writing,
the commands tested was `DBSize`, `Set`, `Del`, `FlushDB`, `Disconnect`,
`Connect`, `Get`.

Also take a look at the `State` type to see how Redis's state is modeled in
order to verify postconditions.
