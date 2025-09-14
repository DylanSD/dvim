package com.dksd.dvim.model;

import com.dksd.dvim.buffer.Buf;
import com.dksd.dvim.view.Line;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.sun.jdi.ArrayReference;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ChatModel {

    private Logger logger = LoggerFactory.getLogger(ChatModel.class);
    private static final String INCEPTION_API_KEY = System.getenv("INCEPTION_API_KEY");
    private ModelName modelName;
    OpenAiStreamingChatModel model;

    public ChatModel(ModelName modelName) {
        this.modelName = modelName;
        model = OpenAiStreamingChatModel.builder()
                .apiKey(INCEPTION_API_KEY)
                .baseUrl("https://api.inceptionlabs.ai/v1")
                .modelName(modelName.getModelName())
                .timeout(Duration.ofSeconds(10))
                .maxTokens(512)
                .build();
    }

    public void chat(String prompt, Buf destBuf) {
        destBuf.addRow("Sending LLM request to model: " + modelName);
        model.chat(prompt, new StreamingChatResponseHandler() {

            @Override
            public void onPartialResponse(String partialResponse) {
                try {
                    destBuf.addRow(partialResponse);
                } catch (Exception e) {
                    onError(e);
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                destBuf.addRow("====Completed response=========");
                String[] ls = completeResponse.aiMessage().text().split("\n");
                for (int i = 0; i < ls.length; i++) {
                    destBuf.addRow(ls[i]);
                }
                //int inTokens = completeResponse.tokenUsage().inputTokenCount();
                //int outTokens = completeResponse.tokenUsage().outputTokenCount();
                //long storageUsed = completeResponse.aiMessage().toString().length();
            }

            @Override
            public void onError(Throwable error) {
                logger.error("Error during chat completion", error);
            }
        });
    }
}
