package ProtobufMultiFileParser;

import com.google.protobuf.Descriptors;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

public class MultiJsonMergerPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        MultiJsonMergerExtension config = project.getExtensions().create("proto_json_merge", MultiJsonMergerExtension.class);

        project.task("mergeJson").doLast(task -> applyInternal(config));
    }

    private void applyInternal(MultiJsonMergerExtension config) {
        URL descriptorFile = getClass().getClassLoader().getResource(config.descriptorFilePath);
        assert descriptorFile != null;
        URL contentRoot = getClass().getClassLoader().getResource(config.contentRoot);
        assert contentRoot != null;

        Message content;
        try {
            content = ProtobufMultiFileParser.Parse(config.messageType, descriptorFile, contentRoot);
        } catch (IOException | Descriptors.DescriptorValidationException e) {
            throw new RuntimeException(e);
        }

        if (config.outputJson != null) {
            String jsonContent;
            try {
                jsonContent = JsonFormat.printer().print(content);
            } catch (InvalidProtocolBufferException e) {
                throw new RuntimeException(e);
            }
            try {
                Files.write(Path.of(config.outputJson), jsonContent.getBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        if (config.outputPb != null) {
            try {
                Files.write(Path.of(config.outputPb), content.toByteArray());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}