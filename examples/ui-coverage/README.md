# Karate UI Test Coverage
This project extends [the ui-test example](https://github.com/karatelabs/karate/tree/master/examples/ui-test) to demo how to collect coverage of the Javascript code of UI Tests.

## Running

Run the following commands from the root of the project:
- npm install
- npm run coverage:instrument
- mvn test
- npm run coverage:report

Code coverage reports should be available under the configured directory (see below)

## Instrumentalisation

This project uses a very Java/Maven oriented html code inspired from the `ui-test` project. 
The `target/test-classes/ui/html` directory will be the root served by `MockRunner`and will contain:
- an instrumented version of `src/test/java/ui/html/karate.js` file installed by `npm run coverage:instrument`
- the default html files installed by Maven. Make sure that you run `mvn test` without the `clean` goal 
so that the instrumented karate.js previously generated does not get overwritten


When the js/html code is built by tools such as Vite or Babel, the corresponding istanbul plugins should be used instead.
In other cases, the nyc command line used in this project is also suitable. Just make sure that the instrumented files are packaged.

## Output files

Output files are internally used by nyc. They are generated after each scenario, and all existing files are taken into account 
when generating the reports.

Note that by default, existing output files are deleted when the suite starts.

## Reports

This project only generates HTML reports, however, more advanced tools such as Sonar will probably require the lcov format which 
is natively supported by nyc.

## Configuration

The `karate.coverage.output-dir` system property may be used to specify where output files will be generated (default: `.nyc_output`).
If changed, make sure to adapt the `coverage:report` script accordingly with the `--temp-dir` parameter.

The `report-dir` parameter in `coverage:report` may be used to specify where coverage reports will be generated (default: `coverage`)