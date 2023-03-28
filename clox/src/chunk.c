#include <stdlib.h>

#include "chunk.h"
#include "memory.h"

void initChunk(Chunk* chunk) {
    chunk->count = 0;
    chunk->countLines = 0;
    chunk->capacity = 0;
    chunk->capacityLines = 0;
    chunk->code = NULL;
    chunk->lines = NULL;
    initValueArray(&chunk->constants);
}

void writeChunk(Chunk* chunk, uint8_t byte, int line) {
    if (chunk->capacity < chunk->count + 1) {
        int oldCapacity = chunk->capacity;
        chunk->capacity = GROW_CAPACITY(oldCapacity);
        chunk->code = GROW_ARRAY(uint8_t, chunk->code,
            oldCapacity, chunk->capacity);

    }

    if (chunk->capacityLines < chunk->countLines + 2) {
        int oldCapacity = chunk->capacityLines;
        chunk->capacityLines = GROW_CAPACITY(oldCapacity);
        chunk->lines = GROW_ARRAY(int, chunk->lines, 
            oldCapacity, chunk->capacityLines);
    } 

    chunk->code[chunk->count] = byte;
    chunk->count++;

    /* For storing lines I used a run-length encoding
        n_bytes, line, n_bytes, line, .., e.g
        4, 1, 3, 2, ..
        for 4 bytes on line 1, 3 bytes on line 2, etc
    */
    if (chunk->countLines >= 2 && chunk->lines[chunk->countLines - 1] == line) {
        chunk->lines[chunk->countLines - 2]++;
    } else {
        chunk->lines[chunk->countLines] = 1;
        chunk->lines[chunk->countLines + 1] = line;
        chunk->countLines += 2;
    }
}

int getLine(Chunk *chunk, int offset) {
    /* For storing lines I used a run-length encoding
        n_bytes, line, n_bytes, line, .., e.g
        4, 1, 3, 2, ..
        for 4 bytes on line 1, 3 bytes on line 2, etc
    */
    for (int i = 0; i < chunk->countLines; i += 2) {
        if (offset <= chunk->lines[i]) {
            return chunk->lines[i + 1];
        } else {
            offset -= chunk->lines[i];
        }
    }

    // this should never happen, but if it does it means there's a bug
    exit(1);
}

void freeChunk(Chunk *chunk) {
    FREE_ARRAY(uint8_t, chunk->code, chunk->capacity);
    FREE_ARRAY(int, chunk->lines, chunk->capacityLines);
    freeValueArray(&chunk->constants);
    initChunk(chunk);
}

int addConstant(Chunk *chunk, Value value) {
    writeValueArray(&chunk->constants, value);
    return chunk->constants.count - 1;
}