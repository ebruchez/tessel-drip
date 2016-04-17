# Drip irrigation controller for Tessel 2 in Scala

## Compiling and running

- `sbt fullOptJS::webpack`
- `t2 run --name MyTessel --compress=false target/scala-2.11/tessel-drip-opt-bundle.js`

## Status

- turn valve on/off once a day at fixed time
- use Dark Sky weather service and skip if rain
- Use BuildInfo to externalize API keys
- use `ScalaJSBundlerPlugin`
- use `async` / `await`
- no external configuration of the schedule yet

## License

MIT license.