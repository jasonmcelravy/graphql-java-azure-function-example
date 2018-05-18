# graphql-java-azure-function-example

An example of using graphql-java in an Azure function.

This project is a migration of the GraphQL Java HTTP example (https://github.com/graphql-java/graphql-java-http-example) to run in a Java Azure Function.

Learn more about developing Azure functions in Java using Visual Studio Code at https://code.visualstudio.com/docs/java/java-serverless 

Create an Azure function app in VS Code using the package name 'com.graphql.example.function' and replace the "Hello World" function with the code for this function as well as the other Java files and pom.xml. 

Once you get it to build and deploy locally, point GraphiQL or your favorite GraphQL query tool to this endpoint 

    http://localhost:7071/api/graphql    


Some example graphql queries might be

     {
       hero {
         name
         friends {
           name
           friends {
             id
             name
           }
           
         }
       }
     }


or maybe

    {
      luke: human(id: "1000") {
        ...HumanFragment
      }
      leia: human(id: "1003") {
        ...HumanFragment
      }
    }
    
    fragment HumanFragment on Human {
      name
      homePlanet
      friends {
        name
        __typename
      }
    }