/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.github.laim0nas100.llm4j.demo;

import com.github.laim0nas100.llm4j.lmstudio.v1.Integration;
import com.github.laim0nas100.llm4j.lmstudio.v1.Reasoning;
import com.github.laim0nas100.llm4j.lmstudio.v1.LLMEvent;
import com.github.laim0nas100.llm4j.lmstudio.v1.ChatResponse;
import com.github.laim0nas100.llm4j.lmstudio.v1.ChatRequest;
import com.github.laim0nas100.llm4j.lmstudio.v1.Integration.PluginIntegration;
import com.github.laim0nas100.llm4j.lmstudio.v1.LLMToolEvent;
import com.github.laim0nas100.llm4j.lmstudio.v1.ChatSession;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.github.laim0nas100.llm4j.lmstudio.v1.LLMToolEvent.ToolCallContent;
import com.github.mizosoft.methanol.AdapterCodec;
import com.github.mizosoft.methanol.MediaType;
import com.github.mizosoft.methanol.Methanol;
import com.github.mizosoft.methanol.MutableRequest;
import com.github.mizosoft.methanol.adapter.jackson.JacksonAdapterFactory;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.TypeRef;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 *
 * @author Lemmin
 */
public class Test {

    // basic
    //sk-lm-wonZWJhd:3ebNgG9Tifz4OENM4DSk
    public static void main(String... args) throws Exception {
        ChatSession session = new ChatSession();
        session.setReasoning(Reasoning.OFF);
        session.setModel("qwen/qwen3.5-9b");
        List<Integration> integrations = new ArrayList<>();
//        integrations.add(new PluginIntegration("mcp/brightdata"));
        integrations.add(new PluginIntegration("mcp/local"));
        session.setIntegrations(integrations);
        SimpleChatting simpleChatting = new SimpleChatting(session);
        String token = System.getenv("BEARER_TOKEN");
        System.out.println(token);
        simpleChatting.setBearerToken(token);

        ChatRequest chatRequest = new ChatRequest(session);
        chatRequest.setInput(" Access the resource with URI: attachments/1/text_file.txt\n"
                + "exposed in the mcp/local. Use the tool \"read_attachment\"\n"
                + "Tell me what the password in the text file is");
//        chatRequest.setInput("Hello, what is current temperature in Vilnius, you can use the web search");

//        Iterator<LLMEvent> sendAsync = simpleChatting.sendAsync2(chatRequest);
        Stream<LLMEvent> stream = simpleChatting.sendAsyncStream(chatRequest);
        Thread.sleep(3000); // even with delay, no tokens are skipped
        List<LLMEvent.OtherLLMEvent> others = new ArrayList<>();
        List<LLMToolEvent> toolEvents = new ArrayList<>();
        List<LLMEvent.LLMError> errors = new ArrayList<>();

        stream.forEach(event -> {
            if (event instanceof LLMEvent.OtherLLMEvent ev) {
                Objects.requireNonNull(ev);
                others.add(ev);

            }

            if (event instanceof LLMEvent.LLMError ev) {
                Objects.requireNonNull(ev);
                errors.add(ev);
            }

            if (event instanceof LLMToolEvent ev) {
                Objects.requireNonNull(ev);
                toolEvents.add(ev);
                if (ev instanceof LLMToolEvent.ToolCallSuccess toolCall) {
                    
                    McpJsonMapper mapper = McpJsonDefaults.getMapper();
                    
                   toolCall.tryParse(mapper).ifPresent(list ->{
                        for(ToolCallContent content:list){
                            System.out.println(content.text());
                        }
                   });
//                    try {
//                        mapper.readValue(toolCall.output(), new TypeRef<List<ToolCallContent>>(){});
//                    } catch (IOException ex) {
//                        ex.printStackTrace();
//                    }
                    
                }
            }
            System.out.print(event.printable());
        });
        stream.close();
//        while(sendAsync.hasNext()){
//            System.out.println(sendAsync.next());
//        }
//        BufferedInputStream stream = new BufferedInputStream(simpleChatting.sendAsync(chatRequest));
//        InputStreamReader reader = new InputStreamReader(stream);
//
//        StringBuilder builder = new StringBuilder();
//        while (true) {
//            char[] buffer = new char[2048];
//            int read = reader.read(buffer);
//            if (read < 0) {// EOL
//                break;
//            }
//            String valueOf = String.valueOf(buffer, 0, read);
//            builder.append(valueOf);
//            DLog.print(valueOf);
//        }
//        
//        System.out.println();
//        System.out.print(builder);
//        simpleChatting.chat("What is the current temperature in Vilnius, use the web search?").ifPresent(System.out::println);
    }

    public static void main2(String[] args) throws Exception {

        JsonMapper mapper = new JsonMapper();
        AdapterCodec adapterCodec
                = AdapterCodec.newBuilder()
                        .encoder(JacksonAdapterFactory.createJsonEncoder(mapper))
                        .decoder(JacksonAdapterFactory.createJsonDecoder(mapper))
                        .build();

        Methanol methanol = Methanol.newBuilder()
                .baseUri("http://localhost:1234")
                .adapterCodec(adapterCodec)
                .build();
        ChatRequest chat = new ChatRequest();
        chat.setModel("qwen/qwen3.5-9b");
        chat.setInput("Hello, what is your name?");
        chat.setReasoning(Reasoning.OFF);

        HttpResponse<String> response = methanol.send(
                MutableRequest.POST(
                        "/api/v1/chat",
                        chat,
                        MediaType.APPLICATION_JSON
                ),
                BodyHandlers.ofString()
        );

        ChatResponse readValue = mapper.readValue(response.body(), ChatResponse.class);

        System.out.println(readValue.toString());

        ChatRequest chat2 = new ChatRequest();
        chat2.setModel("qwen/qwen3.5-9b");
        chat2.setInput("Nice to meet you, my name is Lemmin");
        chat2.setReasoning(Reasoning.OFF);
        chat2.setPreviousResponseId(readValue.getResponseId());

        HttpResponse<String> response2 = methanol.send(
                MutableRequest.POST(
                        "/api/v1/chat",
                        chat2,
                        MediaType.APPLICATION_JSON
                ),
                BodyHandlers.ofString()
        );

        ChatResponse readValue2 = mapper.readValue(response2.body(), ChatResponse.class);

        System.out.println(readValue2.toString());

    }
}
