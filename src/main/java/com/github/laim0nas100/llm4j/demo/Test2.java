package com.github.laim0nas100.llm4j.demo;

import com.github.laim0nas100.llm4j.lmstudio.v1.ChatSession;
import com.github.laim0nas100.llm4j.lmstudio.v1.Integration;
import com.github.laim0nas100.llm4j.lmstudio.v1.LLMEvent;
import com.github.laim0nas100.llm4j.lmstudio.v1.Reasoning;
import com.github.laim0nas100.llm4j.lmstudio.v1.impl.LLMEventAccumulator;
import com.github.laim0nas100.llm4j.lmstudio.v1.impl.LMStudioChatStore;
import com.github.laim0nas100.llm4j.lmstudio.v1.impl.LMStudioChattingClient;
import com.github.laim0nas100.llm4j.lmstudio.v1.impl.LMStudioPersistentClient;
import com.github.laim0nas100.llm4j.store.Helpers;
import com.github.laim0nas100.llm4j.store.TableCreation;
import com.github.laim0nas100.uncheckedutils.SafeOpt;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Lemmin
 */
public class Test2 {

    public static void main(String... args) throws Exception {
        TableCreation tableCreation = new TableCreation();
        String token = System.getenv("BEARER_TOKEN");

        LMStudioChattingClient chattingClient = new LMStudioChattingClient(token);
        LMStudioPersistentClient fullClient = new LMStudioPersistentClient(tableCreation, chattingClient);
        
         ChatSession session = new ChatSession();
        session.setReasoning(Reasoning.OFF);
        session.setModel("qwen/qwen3.5-9b");
        List<Integration> integrations = new ArrayList<>();
//        integrations.add(new PluginIntegration("mcp/brightdata"));
//        integrations.add(new Integration.PluginIntegration("mcp/local"));
        session.setIntegrations(integrations);
//        fullClient.initializeTemplate("QWEN", session).throwAnyGet();
        SafeOpt<LMStudioChatStore.LMStudioResponse> continueChat = fullClient.continueChat("61", "61", "Ok, thanks. That will be all.", List.of());
//        SafeOpt<LMStudioChatStore.LMStudioResponse> newChat = fullClient.startNewChat("QWEN", "Hello, please respond in a short sentence", List.of());
        
        LMStudioChatStore.LMStudioResponse response = continueChat.get();
        
        LLMEventAccumulator acc = response.accumulator();
        
        while(acc.hasNext()){
            LLMEvent next = acc.next();
            if(next instanceof LLMEvent.LLMError error){
                System.out.println(error.type()+" "+error.error());
            }
            System.out.println(next.type()+": "+next.printable());
//            System.out.print(next.printable());
        }
        
        
        
        
    }
}
