#ifndef FAISS_INDEX_IVFPQ_H
#define FAISS_INDEX_IVFPQ_H

#include "IndexFlat.h"

namespace faiss {

class IndexIVFPQ : public IndexFlat {
public:
    int nlist;
    int m;
    int nbits;
    bool own_fields;
    
    IndexIVFPQ(IndexFlat* quantizer, int d, int nlist, int m, int nbits);
    virtual ~IndexIVFPQ();
};

} // namespace faiss

#endif // FAISS_INDEX_IVFPQ_H
