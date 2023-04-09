cmake_minimum_required(VERSION 3.22)

set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED True)
set(CMAKE_BUILD_TYPE Debug)
set(CMAKE_COLOR_DIAGNOSTICS True)

project(clox)

add_executable(${PROJECT_NAME}
    src/clox.c
    src/memory.c
    src/chunk.c
    src/debug.c
    src/value.c
    src/vm.c)

target_compile_options(${PROJECT_NAME} PRIVATE 
    -Wall -Wpedantic -Wextra -Wswitch-enum -fdiagnostics-color=always)
