import com.google.common.io.Resources;
import com.google.protobuf.*;
import com.google.protobuf.util.JsonFormat;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class TestProtobufMultiFileParser {

    @Test
    void testParseFile() throws IOException {
        URL url = Resources.getResource("base_message.json");
        String jsonFile = Resources.toString(url, StandardCharsets.UTF_8);

        InputStream descriptor = getClass().getClassLoader().getResourceAsStream("main.dsc");
        assert descriptor != null;
        ProtobufMultiFileParser.ProtobufMultiFileParser.Parse("base_message", descriptor, jsonFile);
    }

    @Test
    void messWithProtobufDynamicMessage() {
//        Descriptors.Descriptor descriptor = ExampleMessage.base_message.getDefaultInstance().getDescriptorForType().findNestedTypeByName("nested_field");
        Descriptors.Descriptor descriptor1 = ExampleMessage.BaseMessage.getDescriptor();
        Descriptors.Descriptor fieldDescriptor = descriptor1.findNestedTypeByName("nested_field");


        DynamicMessage dynamicMessage = DynamicMessage.newBuilder(descriptor1).getDefaultInstanceForType();
//        DynamicMessage nested_message = DynamicMessage.newBuilder(descriptor).getDefaultInstanceForType();

//        DynamicMessage nested_message2 = DynamicMessage.newBuilder(descriptor).getDefaultInstanceForType();
        System.out.println("foo");
    }

    @Test
    void parseFromJsonFails() throws IOException {
        URL url = Resources.getResource("base_message.json");
        String jsonFile = Resources.toString(url, StandardCharsets.UTF_8);

        InputStream descriptorFile = getClass().getClassLoader().getResourceAsStream("main.dsc");
        assert descriptorFile != null;
        byte[] descriptorBytes = descriptorFile.readAllBytes();

        DescriptorProtos.FileDescriptorSet descriptorSet = DescriptorProtos.FileDescriptorSet.parseFrom(descriptorBytes);
        List<DescriptorProtos.DescriptorProto> descriptorProtos = descriptorSet.getFileList().stream().flatMap(e -> e.getMessageTypeList().stream()).toList();
        List<DescriptorProtos.DescriptorProto> candidates = descriptorProtos.stream().filter(e -> e.getName().equals("BaseMessage")).toList();

        // Strangely the constructor for DynamicMessage.Builder accepts both DescriptorProto and Descriptor.
        // DescriptorProto seems more correct upon inspecting the runtime object... it actually contains the fields...
        DescriptorProtos.DescriptorProto descriptorProto = candidates.getFirst();
        DynamicMessage.Builder dynBuilder = DynamicMessage.newBuilder(descriptorProto);

        // This is the version for the underlying descriptor, which seems less likely to work.
//        Descriptors.Descriptor dynamicDescriptor = descriptorProto.getDescriptorForType();
//        DynamicMessage.Builder dynBuilder = DynamicMessage.newBuilder(dynamicDescriptor);

        JsonFormat.parser().merge(jsonFile, dynBuilder);
        System.out.println("foo");
    }

    @Test
    void parseFromBinary() throws IOException {
        InputStream descriptorFile = getClass().getClassLoader().getResourceAsStream("main.dsc");
        byte[] descriptorBytes;
        descriptorBytes = descriptorFile.readAllBytes();

        DescriptorProtos.FileDescriptorSet descriptorSet = DescriptorProtos.FileDescriptorSet.parseFrom(descriptorBytes);
        List<DescriptorProtos.DescriptorProto> descriptorProtos = descriptorSet.getFileList().stream().flatMap(e -> e.getMessageTypeList().stream()).toList();
        List<DescriptorProtos.DescriptorProto> candidates = descriptorProtos.stream().filter(e -> e.getName().equals("BaseMessage")).toList();

        DynamicMessage.Builder dynBuilder = DynamicMessage.newBuilder(candidates.getFirst());

        byte[] exampleBytes = ExampleMessage.BaseMessage.newBuilder()
                .setBase(7).build().toByteArray();

        dynBuilder.mergeFrom(exampleBytes);

        Message dynMessage = dynBuilder.build();

        System.out.println("foo");
    }

    @Test
    void parseWithStaticDescriptorWorks() throws IOException {
        URL url = Resources.getResource("base_message.json");
        String jsonFile = Resources.toString(url, StandardCharsets.UTF_8);

        Descriptors.Descriptor staticDescriptor = ExampleMessage.BaseMessage.getDescriptor();
        DynamicMessage.Builder builder = DynamicMessage.newBuilder(staticDescriptor);

        JsonFormat.parser().merge(jsonFile, builder);
        System.out.println("foo");
    }

//    @Test
//    void testTypeRegistryApi() throws IOException {
//        TypeRegistry typeRegistry = TypeRegistry.newBuilder().build();
//
//        InputStream descriptorFile = getClass().getClassLoader().getResourceAsStream("main.dsc");
//        byte[] descriptorBytes;
//        descriptorBytes = descriptorFile.readAllBytes();
//        DescriptorProtos.FileDescriptorSet descriptorSet = DescriptorProtos.FileDescriptorSet.parseFrom(descriptorBytes);
//
//        descriptorSet.getFileList().stream().flatMap(
//
//
//        TypeRegistry.Builder typeRegistryBuilder = TypeRegistry.newBuilder();
//        descriptorSet.getFileList().getFirst().getMessageTypeList().getFirst();
//        typeRegistryBuilder.add
//        descriptorSet.getFileList().stream().forEach(e -> typeRegistryBuilder.add(e.getMessageTypeList()));
//        typeRegistryBuilder.add()
//    }
}
