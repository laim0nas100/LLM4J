package com.github.laim0nas100.lmstudio4j.mcp;

import com.github.laim0nas100.dbstore.filestore.FileStoreDB;
import com.github.laim0nas100.dbstore.filestore.ResourceFull;
import com.github.laim0nas100.dbstore.filestore.ResourceMetadata;
import com.github.laim0nas100.dbstore.keystore.KeyValueStore;
import com.github.laim0nas100.lmstudio4j.AttachmentTextDetector;
import com.github.laim0nas100.uncheckedutils.SafeOpt;
import com.github.laim0nas100.uncheckedutils.func.UncheckedFunction;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.TypeRef;
import io.modelcontextprotocol.json.schema.JsonSchemaValidator;
import io.modelcontextprotocol.spec.McpSchema;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.apache.tika.io.TikaInputStream;

/**
 *
 * @author Lemmin
 */
public class ResourcePersistentMcpServer extends GenericMcpServer {

    public static class ResourcePersistanntBuilder<B extends ResourcePersistanntBuilder<B>> extends GenericMcpServer.Builder<B> {

        protected FileStoreDB resourceStore;
        protected KeyValueStore keyValueStore;

        public ResourcePersistanntBuilder() {
        }

        public B withPersistantResources(FileStoreDB resourceStore) {
            this.autoCapabilities.resources(true, true);
            this.resourceStore = resourceStore;
            return me();
        }

        public B withKeyValueStore(KeyValueStore keyValueStore) {
            this.keyValueStore = keyValueStore;
            this.autoCapabilities.tools(true);
            return me();
        }

        @Override
        public ResourcePersistentMcpServer build() {
            return new ResourcePersistentMcpServer(this);
        }
        
        

    }

    public ResourcePersistentMcpServer(ResourcePersistanntBuilder builder) {
        this.fileStore = builder.resourceStore;
        this.keyValueStore = builder.keyValueStore;
        super(builder);

    }

    public ResourcePersistentMcpServer(String serverName,
            String serverVersion,
            boolean sync,
            boolean immediateExecution,
            Duration requestTimeout,
            String mcpEndpoint,
            String instructions,
            McpJsonMapper jsonMapper,
            JsonSchemaValidator jsonSchemaValidator,
            McpSchema.ServerCapabilities capabilities,
            List<ToolDefinition> toolDefinitions,
            List<CompletionDefinition> completions,
            List<PromptDefinition> prompts,
            List<ResourceTemplateDefinition> templates,
            List<ResourceDefinition> resources,
            FileStoreDB fileStore,
            KeyValueStore keyValueStore
    ) {
        this.fileStore = fileStore;
        this.keyValueStore = keyValueStore;
        super(serverName,
                serverVersion,
                sync,
                immediateExecution,
                requestTimeout,
                mcpEndpoint,
                instructions,
                jsonMapper,
                jsonSchemaValidator,
                capabilities,
                toolDefinitions,
                completions,
                prompts,
                templates,
                resources
        );

    }

    @Override
    protected void init() {
        super.init();
        // pupolate server with resources
        if (fileStore != null) {
            fileStore.getAll().map(list -> {
                ArrayList<ResourceDefinition> arrayList = new ArrayList<ResourceDefinition>();
                for (ResourceMetadata res : list) {
                    arrayList.add(getResourceDefinition(res));
                }
                return arrayList;
            }).ifPresent(list -> {
                for (ResourceDefinition res : list) {
                    addResource(res);
                }
            });
        }
    }

    protected FileStoreDB fileStore;
    protected KeyValueStore keyValueStore;

    protected <T> SafeOpt<T> usingFileStore(UncheckedFunction<FileStoreDB, T> functor, Object... nullable) {
        for (Object ob : nullable) {
            if (ob == null) {
                return SafeOpt.empty();
            }
        }
        return SafeOpt.ofNullable(fileStore).map(functor);
    }

    protected <T> SafeOpt<T> usingFileStoreFlat(UncheckedFunction<FileStoreDB, SafeOpt<T>> functor, Object... nullable) {
        for (Object ob : nullable) {
            if (ob == null) {
                return SafeOpt.empty();
            }
        }
        return SafeOpt.ofNullable(fileStore).flatMap(functor);
    }

    protected McpSchema.ReadResourceResult resourceReadAction(Object exchange, McpSchema.ReadResourceRequest request) {
        String uri = request.uri();
        return fileStore.find(uri).flatMap(meta -> {
            McpSchema.Resource resource = getResourceDefinition(meta).resource();
            if (meta.isText()) {
                return fileStore.openString(uri)
                        .mapClosing(reader -> reader.readAllAsString())
                        .map(content -> {
                            return McpSchema.ReadResourceResult.builder(
                                    List.of(
                                            McpSchema.TextResourceContents.builder(resource.uri(), content)
                                                    .mimeType(resource.mimeType())
                                                    .meta(resource.meta())
                                                    .build()
                                    )
                            ).build();
                        });
            } else {// binary
                return fileStore.open(uri)
                        .mapClosing(stream -> stream.readAllBytes())
                        .map(bytes -> Base64.getEncoder().encodeToString(bytes))
                        .map(content -> {
                            return McpSchema.ReadResourceResult.builder(
                                    List.of(
                                            McpSchema.BlobResourceContents.builder(resource.uri(), content)
                                                    .mimeType(resource.mimeType())
                                                    .meta(resource.meta())
                                                    .build()
                                    )
                            ).build();
                        });
            }

        }).throwAnyGet();
    }

    protected ResourceDefinition getResourceDefinition(ResourceMetadata metadata) throws IOException {
        McpSchema.Resource.Builder meta = McpSchema.Resource.builder(metadata.getUri(), metadata.getName())
                .description(metadata.getDescription())
                .mimeType(metadata.getMimeType())
                .size(metadata.getSize())
                .meta(jsonMapper.readValue(metadata.getAdditional_info(), new TypeRef<Map<String, Object>>() {
                }));

        return new ResourceDefinition(meta.build(), this::resourceReadAction);
    }
    
    public SafeOpt putResource(ResourceFull resource){
        return putResource(resource, TikaInputStream.get(resource.getContent()));
    }

    public SafeOpt putResource(ResourceMetadata meta, InputStream content) {
        return usingFileStoreFlat(fs -> {
            TikaInputStream stream = TikaInputStream.get(content);
            AttachmentTextDetector.TextDetectionResult detected = AttachmentTextDetector.detectText(
                    stream, meta.getName(), meta.getMimeType()
            );
            meta.setMimeType(detected.detectedMimeType());
            meta.setText(detected.isText());
            return fs.put(meta, stream).peek(any -> {
                addResource(getResourceDefinition(meta));
            });
        }, meta, content);
    }

    public SafeOpt putResourceString(ResourceMetadata meta, String content) {
        return usingFileStoreFlat(fs -> {
            meta.setMimeType("text/plain");
            meta.setText(true);
            byte[] bytes = content.getBytes();
            meta.setSize(bytes.length);
            return fs.put(meta, TikaInputStream.get(bytes)).peek(any -> {
                addResource(getResourceDefinition(meta));
            });
        }, meta, content);
    }

    public SafeOpt deleteResource(String uri) {
        return usingFileStoreFlat(fs -> {
            return fs.delete(uri).peek(id -> {
                removeResource(uri);
            });
        }, uri);
    }

}
