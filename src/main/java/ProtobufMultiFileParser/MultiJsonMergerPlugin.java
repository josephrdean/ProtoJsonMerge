package ProtobufMultiFileParser;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;

// Code analysis thinks this is unused, but it being a Gradle Plugin gets picked up.
@SuppressWarnings("unused")
public class MultiJsonMergerPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        Task mergeTask = project.getTasks().create("mergeJson", MergeJsonTask.class);
        mergeTask.setDescription("Merge a collection of json files defined by a protobuf schema");

        // Most likely this plugin will be used in concert with the protobuf plugin to generate the descriptor file.
        // If it is present, configure ourselves to run after that plugin.
        // It also makes sense for projects to receive the descriptor file from another project in which case this
        // will not be present.
        Task protobufTask = project.getTasks().findByName("generateProto");
        if (protobufTask != null) {
            mergeTask.dependsOn(protobufTask);
        }

        // When assembling the jar we first want to perform the merging. This feels like it should potentially
        // be part of processResources or processTestResources, but how would we distinguish which of the two?
        Task assembleTask = project.getTasks().findByName("assemble");
        if (assembleTask != null) {
            assembleTask.dependsOn(mergeTask);
        }
    }
}