cmake_minimum_required(VERSION 3.5)

project(clox LANGUAGES C)

add_executable(clox main.c)

install(TARGETS clox
    LIBRARY DESTINATION ${CMAKE_INSTALL_LIBDIR})
