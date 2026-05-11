cmake_minimum_required(VERSION 3.18.1)

project("haisa-termexec")

add_library(haisa-termexec SHARED
termexec.c
)

target_link_libraries(haisa-termexec
    android
    log
)
