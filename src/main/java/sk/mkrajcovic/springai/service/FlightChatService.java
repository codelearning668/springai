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

	public Flight answerQuestion(String userQuestion) {
		
		/**
		 * Creates a {@link org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor}
		 * configured to use the provided Milvus vector store for Retrieval-Augmented
		 * Generation (RAG).
		 *
		 * <p>When a user submits a question, the advisor:
		 * <ol>
		 *   <li>Generates an embedding for the question.</li>
		 *   <li>Performs a similarity search against the vector store.</li>
		 *   <li>Retrieves the most relevant documents.</li>
		 *   <li>Injects the retrieved content into the prompt as context.</li>
		 *   <li>Invokes the language model with the enriched prompt.</li>
		 * </ol>
		 *
		 * <p>This enables the model to generate responses grounded in the contents
		 * of the vector store rather than relying solely on its training data.
		 *
		 * @return a {@code QuestionAnswerAdvisor} backed by the configured Milvus
		 *         vector store
		 */
		
		
		
		
		/**
		 * *
		 * <p>
		 * Common Spring AI advisors:
		 * <ul>
		 * <li>{@code QuestionAnswerAdvisor} <br>
		 * Performs Retrieval-Augmented Generation (RAG) by retrieving relevant
		 * documents from a {@code VectorStore} and injecting them into the prompt as
		 * context.</li>
		 *
		 * <li>{@code MessageChatMemoryAdvisor} <br>
		 * Adds previous conversation messages from a {@code ChatMemory} implementation
		 * to maintain conversational context across requests.</li>
		 *
		 * <li>{@code PromptChatMemoryAdvisor} <br>
		 * Injects chat history directly into the prompt template, providing memory
		 * without relying on message-based APIs.</li>
		 *
		 * <li>{@code VectorStoreChatMemoryAdvisor} <br>
		 * Stores and retrieves conversation history from a vector store, enabling
		 * semantic memory retrieval across long conversations.</li>
		 *
		 * <li>{@code SimpleLoggerAdvisor} <br>
		 * Logs prompts and responses for debugging and development purposes.</li>
		 * </ul>
		 *
		 * <p>
		 * Multiple advisors can be chained together. Each advisor participates in the
		 * request/response lifecycle before control reaches the underlying
		 * {@code ChatModel}.
		 */	
		
		
		
		/**
		 * Creates a Retrieval-Augmented Generation (RAG) advisor backed by the
		 * configured Milvus vector store.
		 *
		 * <p>For each user question, the advisor intercepts the request before it
		 * reaches the language model and performs the following steps:
		 *
		 * <ol>
		 *   <li>Generates an embedding from the user's question.</li>
		 *   <li>Executes a similarity search against the Milvus vector store.</li>
		 *   <li>Retrieves the most relevant documents based on vector similarity.</li>
		 *   <li>Injects the retrieved document content into the prompt as context.</li>
		 *   <li>Sends the enriched prompt to the configured chat model.</li>
		 * </ol>
		 *
		 * <p>Conceptually, Spring AI performs logic similar to:
		 *
		 * <pre>{@code
		 * String question = "What is our refund policy?";
		 *
		 * List<Document> documents = vectorStore.similaritySearch(
		 *     SearchRequest.builder()
		 *         .query(question)
		 *         .topK(4)
		 *         .build()
		 * );
		 *
		 * String context = documents.stream()
		 *     .map(Document::getText)
		 *     .collect(Collectors.joining("\n"));
		 *
		 * String prompt = """
		 *     Use the following context to answer the question:
		 *
		 *     %s
		 *
		 *     Question: %s
		 *     """.formatted(context, question);
		 *
		 * chatModel.call(prompt);
		 * }</pre>
		 *
		 * <p>This allows the model to generate answers grounded in the indexed
		 * documents stored in Milvus rather than relying solely on its training data.
		 *
		 * @return a {@link org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor}
		 *         configured to retrieve context from Milvus
		 */
//		QuestionAnswerAdvisor.builder(milvusVectorStore).build();

		return chatClient//ChatClient.builder(openAiChatModel).build()
				// RAG
				.prompt()
				.advisors(QuestionAnswerAdvisor.builder(milvusVectorStore).build())
				.user(userQuestion)
				.call().entity(Flight.class);
	}
}

//Educational code should model the habits we want developers to carry into production systems.
