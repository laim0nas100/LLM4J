package com.github.laim0nas100.llm4jllm4j;

import static com.github.laim0nas100.llm4jllm4j.Global.jsonMapper;
import com.github.laim0nas100.llm4j.lmstudio.v1.ChatRequest;
import com.github.laim0nas100.llm4j.lmstudio.v1.ChatResponse;
import com.github.laim0nas100.llm4j.lmstudio.v1.LLMEvent;
import com.github.mizosoft.methanol.AdapterCodec;
import com.github.mizosoft.methanol.MediaType;
import com.github.mizosoft.methanol.Methanol;
import com.github.mizosoft.methanol.MutableRequest;
import com.github.mizosoft.methanol.adapter.jackson.JacksonAdapterFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpResponse;
import java.util.Objects;
import java.util.stream.Stream;

/**
 *
 * @author Lemmin
 */
public class LLMChattingClientV1 {

    
    public static final String ENDPOINT = "/api/v1/chat";
    protected final AdapterCodec adapterCodec = AdapterCodec.newBuilder()
            .encoder(JacksonAdapterFactory.createJsonEncoder(jsonMapper))
            .decoder(JacksonAdapterFactory.createJsonDecoder(jsonMapper))
            .build();

    protected final Methanol methanol;
    protected final String bearerToken;

    public LLMChattingClientV1(String bearerToken) {
        this("http://localhost:1234", bearerToken);
    }

    public LLMChattingClientV1(String baseURI, String bearerToken) {
        Objects.requireNonNull(baseURI);
        this.bearerToken = Objects.requireNonNull(bearerToken);
        methanol = Methanol.newBuilder()
                .baseUri(baseURI)
                .adapterCodec(adapterCodec)
                .build();
    }

    public ChatResponse sendSync(ChatRequest request) throws IOException, InterruptedException {
        request.setStream(Boolean.FALSE);
        MutableRequest POST = MutableRequest.POST(
                ENDPOINT,
                request,
                MediaType.APPLICATION_JSON
        );
        POST.setHeader("Authorization", "Bearer " + bearerToken);
        HttpResponse<String> response = methanol.send(
                POST, HttpResponse.BodyHandlers.ofString()
        );

        return jsonMapper.readValue(response.body(), ChatResponse.class);
    }

    public InputStream sendAsyncRaw(ChatRequest request) throws IOException, InterruptedException {
        request.setStream(Boolean.TRUE);
        MutableRequest POST = MutableRequest.POST(
                ENDPOINT,
                request,
                MediaType.APPLICATION_JSON
        );
        POST.setHeader("Authorization", "Bearer " + bearerToken);
        HttpResponse<InputStream> response = methanol.send(
                POST, HttpResponse.BodyHandlers.ofInputStream()
        );

        return response.body();
    }

    public Stream<LLMEvent> sendAsyncStream(ChatRequest request) throws IOException, InterruptedException {
        request.setStream(Boolean.TRUE);
        MutableRequest POST = MutableRequest.POST(
                ENDPOINT,
                request,
                MediaType.APPLICATION_JSON
        );
        POST.setHeader("Authorization", "Bearer " + bearerToken);

        HttpResponse<Stream<String>> response = methanol.send(
                POST, HttpResponse.BodyHandlers.ofLines()
        );

        return response.body().map(this::parseEvent).filter(event -> event != null);
    }

    protected LLMEvent parseEvent(String msg) {
        // mangle the msg
        int dataIndex = msg.indexOf("data:");
        if (dataIndex < 0) {
            return null;
        }
        String data = msg.substring(dataIndex + 6).trim();
        try {
            return jsonMapper.readValue(data, LLMEvent.class);
        } catch (IOException ex) {
            try {
                return jsonMapper.readValue(data, LLMEvent.OtherLLMEvent.class);
            } catch (IOException ex1) {
                ex1.addSuppressed(ex);
                return new LLMEvent.APIError(ex1);
            }
        }
    }

    public String getBearerToken() {
        return bearerToken;
    }

}
