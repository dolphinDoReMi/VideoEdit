#ifndef FAISS_INDEX_FLAT_H
#define FAISS_INDEX_FLAT_H

namespace faiss {

enum MetricType {
    METRIC_INNER_PRODUCT = 0,
    METRIC_L2 = 1
};

class IndexFlat {
public:
    int d;  // dimension
    MetricType metric_type;
    
    IndexFlat(int d, MetricType metric_type);
    virtual ~IndexFlat();
};

} // namespace faiss

#endif // FAISS_INDEX_FLAT_H
