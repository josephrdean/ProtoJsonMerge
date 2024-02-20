package ProtobufMultiFileParser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.DescriptorProtos;

/**
 *
 * @param baseMessageType The name of the root message type.
 * @param descriptorFile The protoc generated Descriptor File that holds definitions of the message
 *                       classes we're parsing.
 * @param baseFile The root of the file hierarchy to parse.
 */
public class ProtobufMultiFileParser {

    // Parses a set
    public static Message Parse(String baseMessageType, File descriptorFile, File baseFile) throws InvalidProtocolBufferException {

        byte[] descriptorBytes;
        try {
            descriptorBytes = Files.readAllBytes(descriptorFile.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        DescriptorProtos.FileDescriptorProto descriptorProto = DescriptorProtos.FileDescriptorProto.parseFrom(descriptorBytes);

        // Just make it compile for now. We'll need to find the matching message, then parse the contents of
        // baseFile into it.
        return descriptorProto.getMessageTypeList().getFirst().newBuilderForType().build();
    }
}