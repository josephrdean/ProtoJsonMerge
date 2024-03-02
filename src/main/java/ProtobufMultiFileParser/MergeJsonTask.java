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
import java.net.URL;
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
        System.out.println("Invoked with " + getDescriptorFilePath().get());
    }

    private void applyInternal() {
        URL descriptorFile = getClass().getClassLoader().getResource(getDescriptorFilePath().get());
        assert descriptorFile != null;
        URL contentRoot = getClass().getClassLoader().getResource(getContentRoot().get());
        assert contentRoot != null;

        Message content;
        try {
            content = ProtobufMultiFileParser.Parse(getMessageType().get(), descriptorFile, contentRoot);
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
