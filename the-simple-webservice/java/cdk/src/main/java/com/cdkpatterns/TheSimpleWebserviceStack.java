package com.cdkpatterns;

import software.amazon.awscdk.core.*;
import software.amazon.awscdk.services.apigatewayv2.HttpApi;
import software.amazon.awscdk.services.apigatewayv2.LambdaProxyIntegration;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.dynamodb.TableProps;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;

import java.util.Map;

public class TheSimpleWebserviceStack extends Stack {
    public TheSimpleWebserviceStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public TheSimpleWebserviceStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        Table dynamoDbTable = createDynamoDBTable();
        Function lambda = createLambda(dynamoDbTable.getTableName());
        HttpApi api = createHttpApi(lambda);

        dynamoDbTable.grantReadWriteData(lambda);

        CfnOutput.Builder.create(this, "ApiUrl")
                .description("HTTP API Url")
                .value(api.getUrl())
                .build();
    }

    private HttpApi createHttpApi(Function dynamoLambda) {
        HttpApi httpApi = HttpApi.Builder.create(this, "Endpoint")
                .defaultIntegration(
                        LambdaProxyIntegration.Builder.create()
                                .handler(dynamoLambda)
                                .build())
                .build();
        return httpApi;
    }

    private Function createLambda(String tableName) {
        Function lambdaFunction =
                Function.Builder.create(this, "DynamoLambdaHandler")
                        .code(Code.fromAsset(this.getLambdaCodePath()))
                        .handler("com.cdkpatterns.LambdaHandler::handleRequest")
                        .runtime(Runtime.JAVA_11)
                        .memorySize(1538)
                        .environment(Map.of(
                                "HITS_TABLE_NAME", tableName,
                                "REGION", this.getRegion()))
                        .build();
        return lambdaFunction;
    }

    private String getLambdaCodePath() {
        String userDir = System.getProperty("user.dir");
        return userDir + "/../lambda/target/lambda.zip";
    }

    private Table createDynamoDBTable() {
        return new Table(this, "Hits", TableProps.builder()
                .partitionKey(Attribute.builder()
                        .name("path")
                        .type(AttributeType.STRING)
                        .build())
                .build());
    }
}
