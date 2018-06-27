package com.graphql.example.function.util;

import com.microsoft.azure.functions.HttpRequestMessage;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Graphql clients can send GET or POST HTTP requests.  The spec does not make an explicit
 * distinction.  So you may need to handle both.  The following was tested using
 * a graphiql client tool found here : https://github.com/skevy/graphiql-app
 *
 * You should consider bundling graphiql in your application
 *
 * https://github.com/graphql/graphiql
 *
 * This outlines more information on how to handle parameters over http
 *
 * http://graphql.org/learn/serving-over-http/
 */
public class QueryParameters {

    String query;
    String operationName;
    Map<String, Object> variables = Collections.emptyMap();

    public String getQuery() {
        return query;
    }

    public String getOperationName() {
        return operationName;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public static QueryParameters from(HttpRequestMessage<Optional<String>> request) {
        QueryParameters parameters = new QueryParameters();
        if ("POST".equalsIgnoreCase(request.getMethod())) {
            Map<String, Object> json = readJSON(request);
            parameters.query = (String) json.get("query");
            parameters.operationName = (String) json.get("operationName");
            parameters.variables = getVariables(json.get("variables"));
        } else {
            parameters.query = (String)request.getQueryParameters().get("query");
            parameters.operationName = (String)request.getQueryParameters().get("operationName");
            parameters.variables = getVariables(request.getQueryParameters().get("variables"));
        }
        return parameters;
    }


    private static Map<String, Object> getVariables(Object variables) {
        if (variables instanceof Map) {
            Map<?, ?> inputVars = (Map) variables;
            Map<String, Object> vars = new HashMap<>();
            inputVars.forEach((k, v) -> vars.put(String.valueOf(k), v));
            return vars;
        }
        return JsonKit.toMap(String.valueOf(variables));
    }

    private static Map<String, Object> readJSON(HttpRequestMessage<Optional<String>> request) {
        String s = readPostBody(request);
        return JsonKit.toMap(s);
    }

    private static String readPostBody(HttpRequestMessage<Optional<String>> request) {        
        return request.getBody().map(String::toString).orElse("{}");
    }

}