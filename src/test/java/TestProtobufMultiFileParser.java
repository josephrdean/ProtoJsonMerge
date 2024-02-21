import com.google.common.io.Resources;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class TestProtobufMultiFileParser {

    @Test
    void testParseFile() throws IOException {
        URL url = Resources.getResource("base_message.json");
        String jsonFile = Resources.toString(url, StandardCharsets.UTF_8);

        InputStream descriptor = getClass().getClassLoader().getResourceAsStream("main.dsc");
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
}
