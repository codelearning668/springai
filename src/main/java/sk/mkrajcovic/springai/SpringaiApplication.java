package sk.mkrajcovic.springai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.ai.vectorstore.milvus.autoconfigure.MilvusVectorStoreAutoConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.testcontainers.milvus.MilvusContainer;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;

@SpringBootApplication(exclude = MilvusVectorStoreAutoConfiguration.class)
public class SpringaiApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringaiApplication.class, args);
	}

	/**
	 * ChatClient is intended as a fluent facade over a ChatModel, and the builder
	 * allows per-client customization:
	 *
	 * builder.defaultSystem("You are a pirate").defaultOptions(...).build();
	 *
	 * Because different applications may want multiple differently configured
	 * clients, Spring AI auto-configures the builder rather than a single global
	 * ChatClient.
	 */
	@Bean
	ChatClient chatClient(ChatClient.Builder chatClientBuilder) {
		return chatClientBuilder.build();
	}

	/**
	 * Loads and configures a MarkdownDocumentReader for flights.md.
	 *
	 * Converts markdown into Documents for later embedding in Milvus.
	 *
	 * Config: - Horizontal rules split documents into chunks - Blockquotes and code
	 * blocks are excluded to reduce embedding noise
	 *
	 * Note: Calling reader.get() parses the file immediately (used here only for
	 * debug output). Actual embedding happens later in VectorStore ingestion.
	 */
	@Bean
	MarkdownDocumentReader markdownDocumentReader(@Value("classpath:flights.md") Resource flightsResource) {
		var config = MarkdownDocumentReaderConfig.builder()
			.withHorizontalRuleCreateDocument(true)
			.withIncludeBlockquote(false)
			.withIncludeCodeBlock(false)
			.build();

		// NOTE: metadata are not being embedded, only the content
		//       make sure the content contains header information

		return new MarkdownDocumentReader(flightsResource, config);
	}

	/**
	 * Starts a disposable Milvus instance using Testcontainers.
	 *
	 * CAUTION:
	 *
	 * This is appropriate for development and testing only.
	 *
	 * Each application startup launches a new container and any data stored in
	 * Milvus is ephemeral unless Docker volumes are configured.
	 *
	 * DEPLOY_MODE=STANDALONE is set explicitly due to a Milvus 2.6 startup
	 * regression where standalone mode is not always inferred correctly.
	 */
	@Bean
	MilvusContainer milvusContainer() {
		/*
		 * Milvus v2.6 moved the paramtable.Init() call earlier in the startup sequence
		 * (before the role sets its own deploy mode), breaking the implicit assumption
		 * that milvus run standalone implies standalone mode.
		 * The fix is to make it explicit via the env var.
		 *
		 * This is a known Milvus bug (#43760), fixed in v2.6.1, but milvusdb/milvus:latest
		 * may still be affected depending on the exact tag pulled.
		 */
		@SuppressWarnings("resource")
		var container = new MilvusContainer("milvusdb/milvus:latest")
			.withEnv("DEPLOY_MODE", "STANDALONE");

		container.start();
		return container;
	}

	/**
	 * Creates and configures the MilvusServiceClient.
	 *
	 * Connects to a Testcontainers-managed Milvus instance using the container
	 * endpoint and default credentials.
	 *
	 * Note: This client is used by Spring AI MilvusVectorStore for collection
	 * management and vector insert/query operations.
	 */
	@Bean
	MilvusServiceClient milvusClient(MilvusContainer container) {
		return new MilvusServiceClient(ConnectParam.newBuilder()
			.withAuthorization("minioadmin", "minioadmin")
			.withUri(container.getEndpoint())
			.build());
	}

	/**
	 * Creates the Milvus-backed VectorStore.
	 *
	 * IMPORTANT:
	 *
	 * Do not call vectorStore.add(...) inside this bean factory method.
	 *
	 * Although initializeSchema(true) is configured, collection creation is tied to
	 * the Spring bean lifecycle. During execution of a @Bean factory method the
	 * bean has not yet completed initialization and lifecycle callbacks may not
	 * have run.
	 *
	 * Calling add(...) here can therefore fail with:
	 *
	 * can't find collection[database=default][collection=vector_store]
	 *
	 * because the collection has not been created yet.
	 *
	 * Document ingestion is performed later via ApplicationRunner after the
	 * application context has completed startup.
	 *
	 * The embedding dimension must match the embedding model used to generate
	 * vectors. Using embeddingModel.dimensions() avoids hardcoding
	 * provider-specific dimensions such as 1536 for text-embedding-3-small.
	 */
	@Bean
	VectorStore vectorStore(MilvusServiceClient milvusClient, EmbeddingModel embeddingModel) {
		return MilvusVectorStore.builder(milvusClient, embeddingModel)
			.collectionName("vector_store") // exists after loadDocs()
			.databaseName("default")
			.indexType(IndexType.IVF_FLAT)
			.metricType(MetricType.COSINE)
			.batchingStrategy(new TokenCountBatchingStrategy())
			.initializeSchema(true)
			// this method will call the remote Embedding endpoint
			// to get the dimensions of the embedded vectors
			.embeddingDimension(embeddingModel.dimensions())
			.build();
	}

	/**
	 * Loads documents after the Spring context has fully initialized.
	 *
	 * This runner executes after all singleton beans have been created and
	 * initialized. At this point the Milvus collection created by
	 * initializeSchema(true) is available and vector insertion can proceed safely.
	 *
	 * Performing ingestion here avoids startup failures caused by attempting to
	 * insert vectors before the collection exists.
	 */
	@Bean
	ApplicationRunner loadDocs(VectorStore vectorStore, MarkdownDocumentReader reader) {
		return args -> vectorStore.add(reader.read());
	}

}
