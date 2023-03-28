cmake_minimum_required(VERSION 3.22)

set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED True)

project(clox)

add_executable(${PROJECT_NAME}
    src/clox.c
    src/memory.c
    src/chunk.c
    src/debug.c
    src/value.c)

target_compile_options(${PROJECT_NAME} PRIVATE -Wall -Wpedantic -Wextra)
