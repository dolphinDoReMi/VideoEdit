cmake_minimum_required(VERSION 3.22.1)
project(ml_core)
add_library(ml_core SHARED
  # Add ML core sources here when implemented
)
find_library(log-lib log)
target_link_libraries(ml_core ${log-lib})
