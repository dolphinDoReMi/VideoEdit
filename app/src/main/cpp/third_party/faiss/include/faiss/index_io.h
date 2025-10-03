#ifndef FAISS_INDEX_IO_H
#define FAISS_INDEX_IO_H

#include <string>

namespace faiss {

class Index;

// Stub declarations for index I/O
Index* read_index(const std::string& filename);
void write_index(const Index* index, const std::string& filename);

} // namespace faiss

#endif // FAISS_INDEX_IO_H
