# ktor-lambda-twitter-tracking
The receiving part of Twitter click tracking - done with Ktor, ready for AWS Lambda.

## Deploy Function

Make sure to have [serverless-cli](https://www.serverless.com/framework/docs/getting-started/) installed and correctly [configured for AWS](https://www.serverless.com/framework/docs/providers/aws/cli-reference/config-credentials/).
Before you're ready, create a file named `.env.yml` pointing to you AWS Elasticsearch cluster like this:
```yaml
esUrl: search-xxxxxxx.eu-central-1.es.amazonaws.com
```
Now you can run `sls deploy` and everything should be set up

## Local Development

To run the Elasticsearch docker container, use: `docker run -d -p 9200:9200 -p 9300:9300 -e "discovery.type=single-node" docker.elastic.co/elasticsearch/elasticsearch:7.9.0`.
