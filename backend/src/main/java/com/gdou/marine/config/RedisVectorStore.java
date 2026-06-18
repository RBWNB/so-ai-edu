package com.gdou.marine.config;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.search.IndexDefinition;
import redis.clients.jedis.search.IndexOptions;
import redis.clients.jedis.search.Query;
import redis.clients.jedis.search.Schema;
import redis.clients.jedis.search.SearchResult;
import redis.clients.jedis.search.Document;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author FlnyXx
 * @version 1.0
 * @date 2026/6/17
 * @Description 基于 Jedis + Redis Stack 的 EmbeddingStore 实现。
 * 使用 Redis Stack 的 FT.SEARCH + KNN 实现向量存储与语义检索。
 * 避免 langchain4j-redis 0.36.2 与 langchain4j-core 1.0.0-beta2 的接口不兼容问题。
 */
public class RedisVectorStore implements EmbeddingStore<TextSegment> {

    private static final Logger log = LoggerFactory.getLogger(RedisVectorStore.class);

    private static final byte[] FIELD_EMBEDDING = "embedding".getBytes(StandardCharsets.UTF_8);
    private static final byte[] FIELD_TEXT = "text".getBytes(StandardCharsets.UTF_8);
    private static final byte[] FIELD_DOCUMENT_ID = "documentId".getBytes(StandardCharsets.UTF_8);

    private final JedisPooled jedis;
    private final String indexName;
    private final String prefix;
    private final int dimension;

    public RedisVectorStore(JedisPooled jedis, String indexName, String prefix, int dimension) {
        this.jedis = jedis;
        this.indexName = indexName;
        this.prefix = prefix;
        this.dimension = dimension;
        initIndex();
    }

    // ==================== 存储 ====================

    @Override
    public String add(Embedding embedding) {
        String id = UUID.randomUUID().toString().replace("-", "");
        add(id, embedding);
        return id;
    }

    @Override
    public void add(String id, Embedding embedding) {
        Map<byte[], byte[]> fields = new HashMap<>();
        fields.put(FIELD_EMBEDDING, floatsToBytes(embedding.vector()));
        fields.put(FIELD_TEXT, new byte[0]);
        jedis.hset(keyBytes(id), fields);
    }

    @Override
    public String add(Embedding embedding, TextSegment segment) {
        String id = UUID.randomUUID().toString().replace("-", "");
        Map<byte[], byte[]> fields = new HashMap<>();
        fields.put(FIELD_EMBEDDING, floatsToBytes(embedding.vector()));
        fields.put(FIELD_TEXT, segment.text() != null
                ? segment.text().getBytes(StandardCharsets.UTF_8)
                : new byte[0]);
        if (segment.metadata() != null) {
            Object docId = segment.metadata().toMap().get("documentId");
            if (docId != null) {
                fields.put(FIELD_DOCUMENT_ID, docId.toString().getBytes(StandardCharsets.UTF_8));
            }
        }
        jedis.hset(keyBytes(id), fields);
        return id;
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings) {
        List<String> ids = new ArrayList<>();
        for (Embedding e : embeddings) {
            ids.add(add(e));
        }
        return ids;
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings, List<TextSegment> segments) {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < embeddings.size(); i++) {
            TextSegment seg = i < segments.size() ? segments.get(i) : null;
            ids.add(add(embeddings.get(i), seg));
        }
        return ids;
    }

    // ==================== 检索 ====================

    @Override
    public EmbeddingSearchResult<TextSegment> search(EmbeddingSearchRequest request) {
        int maxResults = Math.max(1, Math.min(request.maxResults(), 100));
        double minScore = Math.max(0.0, Math.min(request.minScore(), 1.0));

        String queryStr = "*=>[KNN " + maxResults + " @embedding $vec AS score]";
        Query q = new Query(queryStr)
                .addParam("vec", floatsToBytes(request.queryEmbedding().vector()))
                .setSortBy("score", true)
                .returnFields("text", "documentId", "score")
                .limit(0, maxResults)
                .dialect(2);

        try {
            SearchResult sr = jedis.ftSearch(indexName, q);
            List<Document> docs = sr.getDocuments();
            List<EmbeddingMatch<TextSegment>> matches = new ArrayList<>();

            for (Document doc : docs) {
                double rawScore = Double.parseDouble(doc.getString("score"));
                // COSINE distance ∈ [0,2], 转换为 similarity ∈ [0,1]
                double similarity = 1.0 - (rawScore / 2.0);
                if (similarity < minScore) {
                    continue;
                }

                String fullId = doc.getId();
                String docId = fullId.startsWith(prefix + ":")
                        ? fullId.substring(prefix.length() + 1)
                        : fullId;
                String text = doc.getString("text");
                String documentId = doc.getString("documentId");

                TextSegment segment = TextSegment.from(text != null ? text : "");
                if (documentId != null) {
                    segment.metadata().put("documentId", documentId);
                }

                matches.add(new EmbeddingMatch<>(similarity, docId, null, segment));
            }
            return new EmbeddingSearchResult<>(matches);
        } catch (Exception e) {
            log.warn("Redis vector search error: {}", e.getMessage());
            return new EmbeddingSearchResult<>(List.of());
        }
    }

    // ==================== 删除 ====================

    @Override
    public void remove(String id) {
        jedis.del(keyBytes(id));
    }

    @Override
    public void removeAll(Collection<String> ids) {
        if (ids != null && !ids.isEmpty()) {
            byte[][] keys = ids.stream()
                    .filter(s -> s != null && !s.isBlank())
                    .map(this::keyBytes)
                    .toArray(byte[][]::new);
            if (keys.length > 0) {
                jedis.del(keys);
            }
        }
    }

    @Override
    public void removeAll() {
        try {
            // 使用 SCAN 安全地遍历删除
            String cursor = "0";
            do {
                var scanResult = jedis.scan(cursor.getBytes(StandardCharsets.UTF_8),
                        new redis.clients.jedis.params.ScanParams()
                                .match(prefix + ":*")
                                .count(100));
                cursor = scanResult.getCursor();
                List<byte[]> keys = scanResult.getResult();
                if (!keys.isEmpty()) {
                    jedis.del(keys.toArray(new byte[0][]));
                }
            } while (!"0".equals(cursor));
        } catch (Exception e) {
            log.warn("Redis removeAll error: {}", e.getMessage());
        }
    }

    // ==================== 辅助方法 ====================

    private void initIndex() {
        try {
            try {
                jedis.ftInfo(indexName);
                log.info("Redis vector index '{}' already exists", indexName);
                return;
            } catch (Exception ignored) {
                // 索引不存在
            }

            Schema schema = new Schema()
                    .addField(new Schema.VectorField("embedding",
                            Schema.VectorField.VectorAlgo.FLAT,
                            Map.of(
                                    "TYPE", "FLOAT32",
                                    "DIM", String.valueOf(dimension),
                                    "DISTANCE_METRIC", "COSINE"
                            )))
                    .addField(new Schema.TextField("text"))
                    .addField(new Schema.TagField("documentId"));

            IndexDefinition def = new IndexDefinition()
                    .setPrefixes(prefix + ":");

            jedis.ftCreate(indexName, IndexOptions.defaultOptions().setDefinition(def), schema);
            log.info("Redis vector index '{}' created (dim={}, prefix={})", indexName, dimension, prefix);
        } catch (Exception e) {
            log.warn("Failed to init Redis vector index '{}': {}", indexName, e.getMessage());
        }
    }

    private byte[] keyBytes(String id) {
        return (prefix + ":" + id).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * float[] → 小端字节序（Redis FLOAT32 格式）
     */
    static byte[] floatsToBytes(float[] vector) {
        ByteBuffer buf = ByteBuffer.allocate(vector.length * Float.BYTES)
                .order(ByteOrder.LITTLE_ENDIAN);
        for (float v : vector) {
            buf.putFloat(v);
        }
        return buf.array();
    }
}
