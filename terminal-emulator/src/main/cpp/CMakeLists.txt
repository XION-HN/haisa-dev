cmake_minimum_required(VERSION 3.18.1)

project("jackpal-termexec2")

add_library(jackpal-termexec2 SHARED
    termexec.c
)

target_link_libraries(jackpal-termexec2
    android
    log
)
