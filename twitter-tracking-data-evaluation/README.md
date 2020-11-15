# twitter-tracking-data-evaluation
A tool to extract and analyse user events from ElasticSearch.

## Local Development

To run the Elasticsearch docker container, use: `docker run -d -p 9200:9200 -p 9300:9300 -e "discovery.type=single-node" docker.elastic.co/elasticsearch/elasticsearch:7.9.0`.

If you want to connect to an ES instance running on AWS, make sure to [set envvars](https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-envvars.html#envvars-set) correctly.

## Running a task

You can simply execute the `main` function in `de.alxgrk.data.MainKt` from your IDE to run the program. 

However, since this is a CLI application, the simplest would be to first install it using `./gradlew :installDist` 
and the run it using `plot.sh` (which is a symbolic link to `./build/install/twitter-tracking-data-evaluation/bin/twitter-tracking-data-evaluation`).

Use `plot.sh --help` to print all possible options. A note to the required `--host` option: omit any protocol 
information like `http://`, only specify the `host` part.

In the end, the command could look like this: `./plot.sh --host=localhost TweetsPerSessionPlot`