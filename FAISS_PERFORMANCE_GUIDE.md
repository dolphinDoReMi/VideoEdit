# FAISS Performance Tuning Guide

## Overview

This guide provides recommendations for tuning FAISS performance parameters based on your video indexing requirements.

## Index Types and Use Cases

### 1. FlatIP (Exact Search)
- **Use Case**: Small datasets (< 10K vectors), highest recall required
- **Pros**: Perfect recall, simple setup
- **Cons**: Slow for large datasets, high memory usage
- **Parameters**: None (exact search)

### 2. IVF+PQ (Approximate Search)
- **Use Case**: Large datasets (10K-1M vectors), balanced speed/recall
- **Pros**: Fast search, reasonable memory usage, good compression
- **Cons**: Requires training, some recall loss
- **Key Parameters**:
  - `nlist`: Number of clusters (higher = better recall, slower search)
  - `nprobe`: Number of clusters to search (higher = better recall, slower search)
  - `pqM`: Number of subvectors for product quantization
  - `pqBits`: Bits per subvector (higher = better quality, larger index)

### 3. HNSW (Hierarchical Navigable Small World)
- **Use Case**: Large datasets, very fast search required
- **Pros**: Very fast search, no training required
- **Cons**: High memory usage, complex parameter tuning
- **Key Parameters**:
  - `M`: Number of connections per node (higher = better recall, more memory)
  - `efConstruction`: Size of dynamic candidate list during construction
  - `efSearch`: Size of dynamic candidate list during search

## Performance Tuning Recommendations

### For Video Frame Embeddings (512-dimensional)

#### Small Dataset (< 1K videos)
```kotlin
val config = FaissDesignConfig(
    indexType = FaissIndexType.FLAT_IP,
    dim = 512,
    segmentTargetN = 512
)
```

#### Medium Dataset (1K-10K videos)
```kotlin
val config = FaissDesignConfig(
    indexType = FaissIndexType.IVF_PQ,
    dim = 512,
    nlist = 1024,
    nprobe = 16,
    pqM = 64,
    pqBits = 8,
    segmentTargetN = 512
)
```

#### Large Dataset (10K+ videos)
```kotlin
val config = FaissDesignConfig(
    indexType = FaissIndexType.HNSW_IP,
    dim = 512,
    hnswM = 32,
    efConstruction = 200,
    efSearch = 64,
    segmentTargetN = 1024
)
```

## Parameter Tuning Guidelines

### IVF+PQ Parameters

#### nlist (Number of Clusters)
- **Rule of thumb**: `nlist = sqrt(total_vectors)`
- **Range**: 256 to 4096
- **Trade-off**: Higher nlist = better recall, slower search

#### nprobe (Number of Clusters to Search)
- **Rule of thumb**: `nprobe = nlist / 4` to `nlist / 2`
- **Range**: 1 to nlist
- **Trade-off**: Higher nprobe = better recall, slower search

#### pqM (Product Quantization Subvectors)
- **Rule of thumb**: `pqM = dim / 8` (for 512-dim: 64)
- **Range**: 1 to dim
- **Trade-off**: Higher pqM = better quality, larger index

#### pqBits (Bits per Subvector)
- **Rule of thumb**: 8 bits (256 centroids per subvector)
- **Range**: 1 to 8
- **Trade-off**: Higher pqBits = better quality, larger index

### HNSW Parameters

#### M (Connections per Node)
- **Rule of thumb**: 16-32 for 512-dimensional vectors
- **Range**: 4 to 64
- **Trade-off**: Higher M = better recall, more memory

#### efConstruction (Construction Search Width)
- **Rule of thumb**: 200-400
- **Range**: 50 to 1000
- **Trade-off**: Higher efConstruction = better quality, slower construction

#### efSearch (Search Width)
- **Rule of thumb**: 64-128
- **Range**: 16 to 512
- **Trade-off**: Higher efSearch = better recall, slower search

## Memory Usage Estimates

### FlatIP
- **Memory**: `4 * dim * num_vectors` bytes
- **Example**: 512-dim, 10K vectors = 20MB

### IVF+PQ
- **Memory**: `(pqBits * pqM * num_vectors) / 8 + overhead` bytes
- **Example**: 8-bit PQ, 64 subvectors, 10K vectors = 640KB + overhead

### HNSW
- **Memory**: `4 * dim * num_vectors + M * num_vectors * 4` bytes
- **Example**: 512-dim, M=32, 10K vectors = 20MB + 1.28MB = 21.28MB

## Performance Benchmarks

### Search Speed (queries per second)
- **FlatIP**: 100-1000 QPS (depends on dataset size)
- **IVF+PQ**: 1000-10000 QPS
- **HNSW**: 10000+ QPS

### Recall@10 (top-10 accuracy)
- **FlatIP**: 100%
- **IVF+PQ**: 90-99% (depends on parameters)
- **HNSW**: 95-99% (depends on parameters)

## Segment and Compaction Strategy

### Segment Size
- **Small segments**: Faster ingestion, more segments to manage
- **Large segments**: Slower ingestion, fewer segments to manage
- **Recommended**: 512-1024 vectors per segment

### Compaction
- **Enable for**: Large datasets with many segments
- **Threshold**: 16+ segments
- **Strategy**: Merge segments into shards for faster querying

## Monitoring and Optimization

### Key Metrics to Monitor
1. **Search latency**: Target < 10ms per query
2. **Recall@10**: Target > 95%
3. **Memory usage**: Monitor growth over time
4. **Index size**: Balance quality vs. storage

### Optimization Strategies
1. **Start with conservative parameters**
2. **Measure performance on representative data**
3. **Gradually increase parameters for better recall**
4. **Monitor memory usage and adjust accordingly**
5. **Use compaction for large datasets**

## Example Configuration for Video Search

```kotlin
// Production configuration for video frame search
val productionConfig = FaissDesignConfig(
    indexType = FaissIndexType.IVF_PQ,
    dim = 512,
    nlist = 2048,
    nprobe = 32,
    pqM = 64,
    pqBits = 8,
    segmentTargetN = 1024,
    compactionEnabled = true,
    compactionMinSegments = 16
)
```

This configuration provides:
- **Good recall** (95%+ recall@10)
- **Fast search** (< 5ms per query)
- **Reasonable memory usage** (~1MB per 10K vectors)
- **Automatic compaction** for large datasets
