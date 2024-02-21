package ProtobufMultiFileParser;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ProtobufMultiFileParser {

    /**
     *
     * @param baseMessageType The name of the root message type.
     * @param descriptorFile The protoc generated Descriptor File that holds definitions of the message
     *                       classes we're parsing.
     * @param baseFile The root of the file hierarchy to parse.
     */
    public static Message Parse(String baseMessageType, InputStream descriptorFile, String jsonContent) throws IOException {

        byte[] descriptorBytes;
        try {
            descriptorBytes = descriptorFile.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        DescriptorProtos.FileDescriptorSet descriptorSet = DescriptorProtos.FileDescriptorSet.parseFrom(descriptorBytes);
        List<DescriptorProtos.DescriptorProto> descriptorProtos = descriptorSet.getFileList().stream().flatMap(e -> e.getMessageTypeList().stream()).toList();
        List<DescriptorProtos.DescriptorProto> candidates = descriptorProtos.stream().filter(e -> e.getName().equals(baseMessageType)).toList();

        if (candidates.stream().findAny().isEmpty()) {
            throw new IllegalArgumentException("Message type not found in descriptor set. Candidates: "
                    + String.join(", ", descriptorProtos.stream().map(DescriptorProtos.DescriptorProto::getName).toList()));
        } else if (candidates.size() > 1) {
            throw new IllegalArgumentException("Found multiple candidate matches.");
        }
        DescriptorProtos.DescriptorProto descriptorProto = candidates.getFirst();


//        Message.Builder builder = descriptorProto.newBuilderForType();
//        com.google.protobuf.util.JsonFormat;
//        JsonFormat.parser().ignoringUnknownFields().merge(json, structBuilder);
//        return structBuilder.build();
//
        Descriptors.Descriptor descriptor = descriptorProto.getDescriptorForType();
        DynamicMessage.Builder dynamicBuilder = DynamicMessage.newBuilder(descriptor);


//        dynamicBuilder.getDefaultInstanceForType().buil


        com.google.protobuf.util.JsonFormat.parser().merge(jsonContent, dynamicBuilder);
        Message message = dynamicBuilder.build();
        return message;
    }
}