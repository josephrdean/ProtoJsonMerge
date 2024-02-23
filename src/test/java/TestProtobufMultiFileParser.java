import com.google.common.io.Resources;
import com.google.protobuf.*;
import com.google.protobuf.util.JsonFormat;
import org.junit.jupiter.api.Test;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Assertions;

public class TestProtobufMultiFileParser {

//    @Test
//    void testParseFile() throws IOException {
//        URL url = Resources.getResource("base_message.json");
//        String jsonFile = Resources.toString(url, StandardCharsets.UTF_8);
//
//        InputStream descriptor = getClass().getClassLoader().getResourceAsStream("main.dsc");
//        assert descriptor != null;
//        ProtobufMultiFileParser.ProtobufMultiFileParser.Parse("base_message", descriptor, jsonFile);
//    }

    @Test
    void testMakeTypeRegistry() throws IOException, Descriptors.DescriptorValidationException {
        InputStream descriptorFile = getClass().getClassLoader().getResourceAsStream("main.dsc");
        assert descriptorFile != null;
        byte[] descriptorBytes = descriptorFile.readAllBytes();

        DescriptorProtos.FileDescriptorSet descriptorSet = DescriptorProtos.FileDescriptorSet.parseFrom(descriptorBytes);

        Descriptors.FileDescriptor fileDescriptor = Descriptors.FileDescriptor.buildFrom(descriptorSet.getFileList().getFirst(), new Descriptors.FileDescriptor[]{});

        TypeRegistry.Builder typeRegistryBuilder = TypeRegistry.newBuilder().add(fileDescriptor.getMessageTypes());
        TypeRegistry typeRegistry = typeRegistryBuilder.build();

        Descriptors.Descriptor descriptor1 = typeRegistry.find("BaseMessage");

        ExampleMessage.BaseMessage baseMessage = ExampleMessage.BaseMessage.newBuilder()
                .setBase(7).build();
        DynamicMessage.Builder dynamicBuilder = DynamicMessage.newBuilder(descriptor1);
        dynamicBuilder.mergeFrom(baseMessage.toByteArray());
        DynamicMessage dynamicMessage = dynamicBuilder.build();
        Object obj = dynamicMessage.getField(descriptor1.findFieldByName("base"));
        Assertions.assertEquals(7, (Integer)obj);

        System.out.println("hi");
    }

    @Test
    void testJson() throws IOException, Descriptors.DescriptorValidationException {
        InputStream descriptorFile = getClass().getClassLoader().getResourceAsStream("main.dsc");
        assert descriptorFile != null;
        byte[] descriptorBytes = descriptorFile.readAllBytes();

        DescriptorProtos.FileDescriptorSet descriptorSet = DescriptorProtos.FileDescriptorSet.parseFrom(descriptorBytes);

        Descriptors.FileDescriptor fileDescriptor = Descriptors.FileDescriptor.buildFrom(descriptorSet.getFileList().getFirst(), new Descriptors.FileDescriptor[]{});

        TypeRegistry.Builder typeRegistryBuilder = TypeRegistry.newBuilder().add(fileDescriptor.getMessageTypes());
        TypeRegistry typeRegistry = typeRegistryBuilder.build();

        Descriptors.Descriptor descriptor1 = typeRegistry.find("BaseMessage");

        ExampleMessage.BaseMessage baseMessage = ExampleMessage.BaseMessage.newBuilder()
                .setBase(7).build();
        String json = JsonFormat.printer().print(baseMessage);

        DynamicMessage.Builder dynamicBuilder = DynamicMessage.newBuilder(descriptor1);

        JsonFormat.parser().merge(json, dynamicBuilder);

        DynamicMessage dynamicMessage = dynamicBuilder.build();
        Object obj = dynamicMessage.getField(descriptor1.findFieldByName("base"));
        Assertions.assertEquals(7, (Integer)obj);

        System.out.println("hi");
    }
}
