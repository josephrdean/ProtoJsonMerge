package ProtobufMultiFileParser;

import com.google.protobuf.Descriptors;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class MergeJsonTask extends DefaultTask {

    @Input
    public abstract Property<String> getDescriptorFilePath();

    @Input
    public abstract Property<String> getContentRoot();

    @Input
    public abstract Property<String> getMessageType();

    @Input
    public abstract Property<String> getOutputJson();

    @Input
    public abstract Property<String> getOutputPb();

    @TaskAction
    public void mergeJson() {
        applyInternal();
    }

    private void applyInternal() {
        Path descriptorPath = Path.of(getDescriptorFilePath().get());
        if (!descriptorPath.toFile().exists()) {
            throw new RuntimeException("Descriptor file does not exist. Target path: " + descriptorPath);
        }

        Path contentPath = Path.of(getContentRoot().get());
        if (!contentPath.toFile().exists()) {
            throw new RuntimeException("Content Root does not exist. Target path: " + contentPath);
        }

        Message content;
        try {
            content = ProtobufMultiFileParser.Parse(getMessageType().get(), descriptorPath, contentPath);
        } catch (IOException | Descriptors.DescriptorValidationException e) {
            throw new RuntimeException(e);
        }

        if (getOutputJson().isPresent()) {
            String jsonContent;
            try {
                jsonContent = JsonFormat.printer().print(content);
            } catch (InvalidProtocolBufferException e) {
                throw new RuntimeException(e);
            }
            try {
                Files.write(Path.of(getOutputJson().get()), jsonContent.getBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        if (getOutputPb().isPresent()) {
            try {
                Files.write(Path.of(getOutputPb().get()), content.toByteArray());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
