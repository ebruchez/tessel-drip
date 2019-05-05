# Drip irrigation controller for Tessel 2 in Scala

## Compiling and running

- `sbt fullOptJS::webpack`
- `cp target/scala-2.11/scalajs-bundler/main/tessel-drip-opt-bundle.js . && t2 push --loglevel debug --name Vega --full --compress=false tessel-drip-opt-bundle.js`

## Status

- turn valve on/off once a day at fixed time
- use Dark Sky weather service and skip if rain
- use BuildInfo to externalize API keys
- use `ScalaJSBundlerPlugin`
- use `async` / `await`
- no external configuration of the schedule yet

## License

MIT license.