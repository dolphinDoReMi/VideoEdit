#ifndef FAISS_INDEX_HNSW_H
#define FAISS_INDEX_HNSW_H

#include "IndexFlat.h"

namespace faiss {

class IndexHNSW : public IndexFlat {
public:
    int M;
    int efConstruction;
    int efSearch;
    
    IndexHNSW(int d, int M);
    virtual ~IndexHNSW();
};

} // namespace faiss

#endif // FAISS_INDEX_HNSW_H
