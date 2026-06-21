package sk.mkrajcovic.springai.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import sk.mkrajcovic.springai.model.Flight;

@Service
@RequiredArgsConstructor
public class FlightChatService {

	private final ChatClient chatClient;
	private final VectorStore milvusVectorStore;

	/**
	 * Answers a user question by invoking the configured Spring AI chat client with
	 * Retrieval-Augmented Generation (RAG) which allows responses to be grounded in
	 * application-specific knowledge stored in Milvus rather than relying solely on
	 * the model's training data.
	 *
	 * <p>
	 * A {@link QuestionAnswerAdvisor} is attached to the request pipeline. Before
	 * the question reaches the underlying language model, the advisor:
	 *
	 * <ol>
	 * 	<li>Generates an embedding from the user's question.</li>
	 * 	<li>Performs a similarity search against the configured Milvus vector store.</li>
	 * 	<li>Retrieves the most relevant documents.</li>
	 * 	<li>Injects the retrieved content into the prompt as additional context.</li>
	 * 	<li>Invokes the chat model with the enriched prompt.</li>
	 * </ol>
	 *
	 * <p>
	 * The advisor participates in Spring AI's request/response pipeline. Advisors
	 * can be combined with other implementations such as:
	 *
	 * <ul>
	 * 	<li>{@link QuestionAnswerAdvisor} for RAG-based document retrieval.</li>
	 * 	<li>{@code MessageChatMemoryAdvisor} for conversational memory.</li>
	 * 	<li>{@code PromptChatMemoryAdvisor} for prompt-based memory injection.</li>
	 * 	<li>{@code VectorStoreChatMemoryAdvisor} for semantic memory retrieval.</li>
	 * 	<li>{@code SimpleLoggerAdvisor} for request/response logging.</li>
	 * </ul>
	 *
	 * <p>
	 * This service does <strong>not</strong> retain chat memory. Each invocation of
	 * this method is independent and only includes:
	 * <ul>
	 * 	<li>The current user question.</li>
	 * 	<li>The contextual documents retrieved from the vector store via RAG.</li>
	 * </ul>
	 *
	 * <p>
	 * Previous user messages and model responses are not automatically included in
	 * subsequent requests. For example:
	 *
	 * <pre>{@code
	 * User: Show me flights to Tokyo.
	 * AI: Flight JL402 departs at 10:30.
	 *
	 * User: What time does it depart?
	 * }</pre>
	 *
	 * <p>
	 * Without a configured chat memory advisor, the model does not know what
	 * {@code "it"} refers to because the previous conversation is not part of the
	 * prompt sent to the model.
	 *
	 * <p>
	 * To enable conversational context across requests, a memory advisor such as
	 * {@code MessageChatMemoryAdvisor}, {@code PromptChatMemoryAdvisor}, or
	 * {@code VectorStoreChatMemoryAdvisor} must be added alongside the
	 * {@link QuestionAnswerAdvisor}.
	 *
	 * @param userQuestion the user's natural-language question
	 * @return the generated {@link Flight} entity extracted from the model response
	 */
	public Flight answerQuestion(String userQuestion) {
		/*
		 * Retrieval-Augmented Generation (RAG) advisor backed by Milvus.
		 * For each question it retrieves semantically similar documents from the vector
		 * store and injects them into the prompt before the ChatModel is invoked.
		 */
		var ragAdvisor = QuestionAnswerAdvisor.builder(milvusVectorStore).build();
		return chatClient
				.prompt()
				.advisors(ragAdvisor)
				.user(userQuestion)
				.call()
				.entity(Flight.class);
	}
}
