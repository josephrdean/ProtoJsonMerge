package protojsonmerge;

import com.google.protobuf.*;
import com.google.protobuf.util.JsonFormat;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class ProtobufMultiFileParser {

    /**
     * @param baseMessageType The name of the root message type.
     * @param descriptorPath  The protoc generated Descriptor File that holds definitions of the message
     *                        classes we're parsing.
     * @param contentRoot     The URL of the root object to parse.
     */
    public static Message Parse(String baseMessageType,
                                Path descriptorPath,
                                Path contentRoot) throws IOException, Descriptors.DescriptorValidationException {
        InputStream descriptorStream = new FileInputStream(descriptorPath.toFile());
        DescriptorProtos.FileDescriptorSet descriptorSet = DescriptorProtos.FileDescriptorSet.parseFrom(descriptorStream);

        TypeRegistry.Builder typeRegistryBuilder = TypeRegistry.newBuilder();
        for (var e : descriptorSet.getFileList()) {
            typeRegistryBuilder.add(
                    Descriptors.FileDescriptor.buildFrom(e, new Descriptors.FileDescriptor[]{}).getMessageTypes());
        }
        TypeRegistry typeRegistry = typeRegistryBuilder.build();

        Descriptors.Descriptor descriptor = typeRegistry.find(baseMessageType);
        DynamicMessage.Builder messageBuilder = DynamicMessage.newBuilder(descriptor);

        String content = Files.readString(contentRoot);
        JsonFormat.parser().merge(content, messageBuilder);

        Path contentDir = Paths.get(contentRoot.getParent().toString(),
                FilenameUtils.getBaseName(contentRoot.getFileName().toString()));
        parseDirectoryAsMessage(contentDir, messageBuilder);

        return messageBuilder.build();
    }

    private static void parseDirectoryAsMessage(Path directoryPath, Message.Builder builder) {

        // Can't return null as we get this from a known file
        //noinspection DataFlowIssue
        for (File file : directoryPath.toFile().listFiles()) {
            // Skip hidden files, including filesystem pointers '.' and '..'
            if (file.getName().startsWith(".")) {
                continue;
            }

            String fieldName = FilenameUtils.getBaseName(file.getName());
            Descriptors.FieldDescriptor fieldDescriptor = builder.getDescriptorForType().findFieldByName(fieldName);
            if (fieldDescriptor == null) {
                throw new RuntimeException("Could not find field " + fieldName);
            }

            // If we have a file foo.json, we expect a field foo in the message
            if (file.isFile()) {

                if (!fieldDescriptor.getType().equals(Descriptors.FieldDescriptor.Type.MESSAGE)) {
                    throw new RuntimeException("Expected " + fieldName +
                            " to be of type message, but was " + fieldDescriptor.getType().name());
                }

                try {
                    Message.Builder fieldBuilder = builder.getFieldBuilder(fieldDescriptor);
                    JsonFormat.parser().merge(Files.readString(file.toPath()), fieldBuilder);
                } catch (IOException e) {
                    throw new RuntimeException("IOException when attempting to open file " + file.getPath());
                }
            } else { // This is a directory
                if (fieldDescriptor.isMapField()) {
                    parseDirectoryAsMap(file, fieldDescriptor, builder);
                } else if (fieldDescriptor.isRepeated()) {
                    parseDirectoryAsRepeated(file, fieldDescriptor, builder);
                } else {
                    Message.Builder fieldBuilder = builder.getFieldBuilder(fieldDescriptor);
                    parseDirectoryAsMessage(file.toPath(), fieldBuilder);
                }
            }
        }
    }

    private static void parseDirectoryAsRepeated(File directory,
                                                 Descriptors.FieldDescriptor descriptor,
                                                 Message.Builder parentBuilder) {

        if (descriptor.getType() != Descriptors.FieldDescriptor.Type.MESSAGE) {
            throw new RuntimeException("Repeated field " + descriptor.getName() + " value type was not Message.");
        }

        for (File file : Objects.requireNonNull(directory.listFiles())) {
            // Skip hidden files, including filesystem pointers '.' and '..'
            if (file.getName().startsWith(".")) {
                continue;
            }

            Message.Builder fieldBuilder = parentBuilder.newBuilderForField(descriptor);
            try {
                JsonFormat.parser().merge(Files.readString(file.toPath()), fieldBuilder);
            } catch (IOException e) {
                throw new RuntimeException("IOException when attempting to open file " + file.getPath());
            }

            parentBuilder.addRepeatedField(descriptor, fieldBuilder.build());
        }
    }

    private static void parseDirectoryAsMap(File directory,
                                            Descriptors.FieldDescriptor descriptor,
                                            Message.Builder parentBuilder) {

        Descriptors.FieldDescriptor keyDescriptor = descriptor.getMessageType().findFieldByName("key");
        if (keyDescriptor.getType() != Descriptors.FieldDescriptor.Type.STRING) {
            throw new RuntimeException(
                    "Map field " + descriptor.getName() +
                    " had key type " + keyDescriptor.getMessageType().toString() +
                    " but only String is supported");
        }

        Descriptors.FieldDescriptor valueDescriptor = descriptor.getMessageType().findFieldByName("value");
        if (valueDescriptor.getType() != Descriptors.FieldDescriptor.Type.MESSAGE) {
            throw new RuntimeException("Map field " + descriptor.getName() + " value type was not Message.");
        }

        for (File file : Objects.requireNonNull(directory.listFiles())) {
            // Skip hidden files, including filesystem pointers '.' and '..'
            if (file.getName().startsWith(".")) {
                continue;
            }

            Message.Builder mapEntryBuilder = parentBuilder.newBuilderForField(descriptor);
            String fileName = FilenameUtils.getBaseName(file.getName());
            mapEntryBuilder.setField(keyDescriptor, fileName);
            Message.Builder valueBuilder = mapEntryBuilder.getFieldBuilder(valueDescriptor);
            try {
                JsonFormat.parser().merge(Files.readString(file.toPath()), valueBuilder);
            } catch (IOException e) {
                throw new RuntimeException("IOException when attempting to open file " + file.getPath());
            }
            parentBuilder.addRepeatedField(descriptor, mapEntryBuilder.build());
        }
    }
}