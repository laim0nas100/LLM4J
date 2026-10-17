package com.github.laim0nas100.llm4j;

import com.github.laim0nas100.commonslb.Java;
import com.github.laim0nas100.commonslb.io.autopath.AutoPath;
import com.github.laim0nas100.dbstore.filestore.ResourceFull;
import com.github.laim0nas100.dbstore.filestore.ResourceMetadata;
import com.github.laim0nas100.llm4j.mcp.McpServletBind;
import com.github.laim0nas100.llm4j.mcp.ResourcePersistentMcpServer;
import com.github.laim0nas100.llm4j.mcp.ToolDefinition;
import com.github.laim0nas100.llm4j.store.SqlService;
import com.github.laim0nas100.uncheckedutils.SafeOpt;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;
import java.util.Map;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

/**
 *
 * @author Lemmin
 */
public class LMStudio4j {
    
    public static final AutoPath WORK_DIR = AutoPath.fs(Java.getUserHome(), "lb-soft", "LMStudio4J");
    public static final AutoPath DB_FILE = WORK_DIR.concat("FileDatabase.db");
    private static McpSchema.Tool createReadAttachmentTool() {

        /*
         * JSON Schema describing:
         *
         * {
         *     "file_id": "..."
         * }
         *
         * The SDK validates incoming tool arguments against
         * this schema by default.
         */
        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "file_uri", Map.of(
                                "type", "string",
                                "description",
                                "uri of the file attached to the current chat session."
                        )
                ),
                "required", List.of("file_uri")
        );

        return McpSchema.Tool.builder(
                "read_attachment",
                schema
        )
                .description(
                        """
                        Reads the contents of a file that was attached \
                        by the user to the current chat session.

                        Use this tool when you need to inspect the \
                        contents of an attached file.

                        The file_uri is supplied by the application in \
                        the conversation context.
                        """
                )
                .build();
    }
    
    
    public static void main(String[] args) throws Exception {
        System.out.println("Hello World!");
        
        SqlService db = new SqlService(DB_FILE.toPath());
        var builder = new ResourcePersistentMcpServer.ResourcePersistanntBuilder<>();
        ResourcePersistentMcpServer mcpServer = builder
                .withKeyValueStore(db.keyStore)
                .withPersistantResources(db.fileStore)
                .withTool(createReadAttachmentTool(), ToolDefinition.simpleText(request ->{
                    return SafeOpt.of(request).map(r ->{
                        return r.arguments().get("file_uri");
                    }).flatMap(uri ->{
                        return db.fileStore.openString(String.valueOf(uri));
                    }).mapClosing(reader -> reader.readAllAsString());
                }))
                .withSync(true).build();
        
        var jetty = new Server();
        
        ServerConnector connector
                = new ServerConnector(jetty);
        
        connector.setHost("127.0.0.1");
        connector.setPort(1337);
        
        jetty.addConnector(connector);
        
        mcpServer.bind(McpServletBind.jetty(jetty));
        jetty.start();
        
        List<ResourceMetadata> get = db.fileStore.getAll().get();
        
        ResourceFull meta = new ResourceFull();
        meta.setId(1);
        meta.setUri("attachments/1/text_file.txt");
        meta.setMimeType("text");
        meta.setName("test_file.txt");
        meta.setText(true);
        meta.setDescription("Just a simple text file");
        meta.setContentAndSize("The password is: carrots".getBytes());
        
        SafeOpt putResource = mcpServer.putResource(meta);
        
        List<ResourceMetadata> get2 = db.fileStore.getAll().get();
        System.out.println(get2.size());
//        builder.
    }
}
