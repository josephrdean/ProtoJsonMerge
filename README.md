# Protobuf Json Merge Gradle Plugin
Protobuf messages are often used as configuration objects with json content stored in source control. For projects that have significant content it can be cumbersome to store this in a single file. The motivation for this plugin was a previous project I worked on that had over 50k lines of JSON configuration data.

This gradle plugin uses a supplied protobuf schema to collect a series of json content files into a single protobuf message, and writes content back as JSON and/or protobuf-binary. Smaller and more encapsulated files promote readability and reduce merge conflicts.

This plugin recurses through a target directory applying the following rules:
* Files will be matched to corresponding fields in the given protobuf schema and parsed as the corresponding `Message` type
* Directories that match to a field with type `Message` will recurse into
* Directories that match to a field with type `repeated Message` will collect all files within the directory into the repeated field
* Directories that match to a field with type `map<string,Message>` will collect all files within the directory into the map field with the filename as the key

## Example
Example protobuf schema
```protobuf
syntax = "proto3";

message NestedMessage {
  int32 int = 1;
  string string = 2;
}

message BaseMessage {
  int32 base = 1;
  NestedMessage nested = 2;
  repeated NestedMessage repeated = 3;
  map<string, NestedMessage> map = 4;
}
```
Example directory structure
```text
base_message.json
|- base_message
| |- nested.json
| |- repeated
| | |- first_repeated.json
| | |- second_repeated.json
| | |- third_repeated.json
| |- map
| | |- first.json
| | |- second.json
```
Invoking this plugin with the following schema and content would yield a single protobuf message of type BaseMessage.
* The content of `base_message.json` would be parsed into the root object as an instance of `BaseMessage`
* The content of `nested.json` would be read into the field `BaseMessage::nested` as an instance of `NestedMessage`
* The content of the files in the directory `repeated` would be read into the field `BaseMessage::repeated`, each of type `NestedMessage`, resulting in a list with three elements
* The content of the files in the directory `map` would be read into the field `BaseMessage::map`, each of type `NestedMessage`, resulting in a map with two elements with keys `[first, second]`

# Applying the plugin
## Plugin configuration
```groovy
plugins {
    id("org.jdean.proto-json-merge") version "0.1.0"
}

// Configuration parameters are project directory relative
mergeJson {
    // The descriptor file from protoc
    descriptorFilePath = "build/resources/main/main.dsc"
    // Which file to start our parsing process on. It is implied a directory exists at the same location.
    contentRoot = "src/main/resources/base_message.json"
    // Which protobuf message type contained in the .dsc file to start our processing with.
    messageType = "BaseMessage"
    // Optional: Where to write the merged output file in json format.
    // This is especially useful as a debug artifact to analyze the merge process.
    outputJson = "build/resources/merged.json"
    // Optional: Where to write the merged output file in .pb format
    // It's likely this merged content will be used for your end project at runtime,
    //   and you may as well use .pb for superior performance over json.
    outputPb = "build/resources/merged.pb"
}
```
## protobuf-gradle-plugin configuration
It is a common use case that this plugin is paired with the [protobuf-gradle-plugin](https://github.com/google/protobuf-gradle-plugin). When both plugins are utilized in a project, `mergeJson` will automatically depend on the `generateProto` task to allow the descriptor file to be generated and up-to-date. To configure it to output the necessary descriptor file required by this plugin you can use the following:
```groovy
protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${protobufVersion}"
    }
    
    generateProtoTasks {
        all().configureEach{ task ->
            // This is the descriptor file you'll need to pass to proto-json-merge
            task.descriptorSetOptions.path =
                    "${projectDir}/build/resources/main/${task.sourceSet.name}.dsc"
            task.generateDescriptorSet = true
            task.descriptorSetOptions.includeSourceInfo = true
            task.descriptorSetOptions.includeImports = true
        }
    }
}
```