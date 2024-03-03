import ProtobufMultiFileParser.ProtobufMultiFileParser;
import com.google.protobuf.*;
import com.google.protobuf.util.JsonFormat;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

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

    // Tests the success case of merging all file types (message as file, list of files, map of files)
    @Test
    void testMultiFileParsing() throws Descriptors.DescriptorValidationException, IOException, URISyntaxException {
        URL descriptorFile = getClass().getClassLoader().getResource("main.dsc");
        URL contentRoot = getClass().getClassLoader().getResource("base_message.json");
        assert descriptorFile != null;
        assert contentRoot != null;
        Path descriptorPath = Paths.get(descriptorFile.toURI());
        Path contentPath = Paths.get(contentRoot.toURI());
        Message message = ProtobufMultiFileParser.Parse("BaseMessage", descriptorPath, contentPath);

        byte[] messageBytes = message.toByteArray();
        ExampleMessage.BaseMessage baseMessage = ExampleMessage.BaseMessage.parseFrom(messageBytes);

        Assertions.assertEquals(1, baseMessage.getBase());
        Assertions.assertEquals(7, baseMessage.getNested().getInt());
        Assertions.assertEquals("repeated", baseMessage.getRepeatedList().getFirst().getString());
        Assertions.assertEquals("first_map", baseMessage.getMapMap().get("first").getString());
        Assertions.assertEquals("second_map", baseMessage.getMapMap().get("second").getString());
    }

    @Test
    void testPlugin() {
        Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply("org.jdean.proto-json-merge");
    }
}
