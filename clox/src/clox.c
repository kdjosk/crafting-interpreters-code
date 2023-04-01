#include "common.h"
#include "chunk.h"
#include "debug.h"

int main(int argc, const char* argv[]) {
    Chunk chunk;
    initChunk(&chunk);

    for (int i = 0; i < 300; i++) {
        writeConstant(&chunk, (Value)i, i / 10);
    }

    writeChunk(&chunk, OP_RETURN, 300);

    disassembleChunk(&chunk, "test chunk");

    freeChunk(&chunk);
    return 0;
}
