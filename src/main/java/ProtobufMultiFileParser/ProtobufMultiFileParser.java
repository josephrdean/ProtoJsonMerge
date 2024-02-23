package ProtobufMultiFileParser;

import com.google.protobuf.*;
import com.google.protobuf.util.JsonFormat;

import java.io.IOException;
import java.net.URL;

public class ProtobufMultiFileParser {

    /**
     *
     * @param baseMessageType The name of the root message type.
     * @param descriptorFile The protoc generated Descriptor File that holds definitions of the message
     *                       classes we're parsing.
     * @param contentRoot The URL of the root object to parse.
     */
    public static Message Parse(String baseMessageType, URL descriptorFile, URL contentRoot) throws IOException, Descriptors.DescriptorValidationException {
        DescriptorProtos.FileDescriptorSet descriptorSet = DescriptorProtos.FileDescriptorSet.parseFrom(descriptorFile.openStream());

        TypeRegistry.Builder typeRegistryBuilder = TypeRegistry.newBuilder();
        for (var e : descriptorSet.getFileList()) {
            typeRegistryBuilder.add(
                    Descriptors.FileDescriptor.buildFrom(e, new Descriptors.FileDescriptor[]{}).getMessageTypes());
        }
        TypeRegistry typeRegistry = typeRegistryBuilder.build();

        Descriptors.Descriptor descriptor = typeRegistry.find(baseMessageType);
        DynamicMessage.Builder messageBuilder = DynamicMessage.newBuilder(descriptor);

        String content = new String(contentRoot.openStream().readAllBytes());
        JsonFormat.parser().merge(content, messageBuilder);
        return messageBuilder.build();
    }
}