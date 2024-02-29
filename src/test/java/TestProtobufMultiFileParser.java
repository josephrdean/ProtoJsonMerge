import ProtobufMultiFileParser.ProtobufMultiFileParser;
import com.google.protobuf.*;
import com.google.protobuf.util.JsonFormat;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class TestProtobufMultiFileParser {


    /**
     * Dynamic parsing with protobuf took me a little effort to get right, so I'm leaving this test case.
     * 1.) Output the descriptor file main.dsc during the build process
     * 2.) Parse this as a FileDescriptorSet
     * 3.) Create a FileDescriptor for each File in the DescriptorSet. This is the step that threw me off. You already
     * get a list of FileDescriptorProtos that contain DescriptorProtos for each message. These worked for parsing
     * protobuf binary, but failed for protobuf Json.
     * 4.) Create a TypeRegistry containing all of these. It gives nice lookup methods for types.
     */
    @Test
    void testProtobufDynamicParsing() throws IOException, Descriptors.DescriptorValidationException {
        DescriptorProtos.FileDescriptorSet descriptorSet =
                DescriptorProtos.FileDescriptorSet.parseFrom(
                        getClass().getClassLoader().getResourceAsStream("main.dsc"));

        TypeRegistry.Builder typeRegistryBuilder = TypeRegistry.newBuilder();
        for (var e : descriptorSet.getFileList()) {
            typeRegistryBuilder.add(
                    Descriptors.FileDescriptor.buildFrom(e, new Descriptors.FileDescriptor[]{}).getMessageTypes());
        }
        TypeRegistry typeRegistry = typeRegistryBuilder.build();

        Descriptors.Descriptor descriptor = typeRegistry.find("BaseMessage");

        ExampleMessage.BaseMessage baseMessage = ExampleMessage.BaseMessage.newBuilder()
                .setBase(7).build();

        // Test binary parsing
        {
            DynamicMessage.Builder dynamicBuilder = DynamicMessage.newBuilder(descriptor);
            dynamicBuilder.mergeFrom(baseMessage.toByteArray());
            DynamicMessage dynamicMessage = dynamicBuilder.build();
            Object obj = dynamicMessage.getField(descriptor.findFieldByName("base"));
            Assertions.assertEquals(7, (Integer) obj);
        }

        // Test json parsing
        {
            DynamicMessage.Builder dynamicBuilder = DynamicMessage.newBuilder(descriptor);
            String json = JsonFormat.printer().print(baseMessage);
            JsonFormat.parser().merge(json, dynamicBuilder);
            DynamicMessage dynamicMessage = dynamicBuilder.build();
            Object obj = dynamicMessage.getField(descriptor.findFieldByName("base"));
            Assertions.assertEquals(7, (Integer) obj);
        }
    }

    @Test
    void testBasicTestCase() throws Descriptors.DescriptorValidationException, IOException {
        URL descriptorFile = getClass().getClassLoader().getResource("main.dsc");
        URL contentRoot = getClass().getClassLoader().getResource("base_message.json");
        Message message = ProtobufMultiFileParser.Parse("BaseMessage", descriptorFile, contentRoot);

        Integer baseField = (Integer) message.getField(message.getDescriptorForType().findFieldByName("base"));
        Assertions.assertEquals(1, baseField);

        Message nestedMessage = (Message)message.getField(message.getDescriptorForType().findFieldByName("nested"));
        Integer nestedInt = (Integer)nestedMessage.getField(nestedMessage.getDescriptorForType().findFieldByName("int"));
        Assertions.assertEquals(7, nestedInt);
    }

    void testRepeated() {

    }

    void testMap() {

    }
}
