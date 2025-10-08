cmake_minimum_required(VERSION 3.22.1)
project(ml_core)

# TODO: Add ML core sources when implemented
# For now, create a dummy library to satisfy build requirements
add_library(ml_core SHARED
  dummy.cpp
)

find_library(log-lib log)
target_link_libraries(ml_core ${log-lib})
