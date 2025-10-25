cmake_minimum_required(VERSION 3.31)

include(FetchContent)
include(GoogleTest)

if(NOT TARGET GTest::gtest_main)
    find_package(GTest CONFIG QUIET)

    if(NOT GTest_FOUND)
        set(gtest_force_shared_crt ON CACHE BOOL "" FORCE)

        FetchContent_Declare(
            GTest
            GIT_REPOSITORY https://github.com/google/googletest.git
            GIT_TAG v1.17.0
        )

        FetchContent_MakeAvailable(GTest)
    endif()
endif()

add_executable(
    poorcraft_tests
    basic_test.cpp
    window_test.cpp
    renderer_test.cpp
    gpu_detection_test.cpp
    timer_test.cpp
    input_test.cpp
    camera_test.cpp
    block_test.cpp
    chunk_test.cpp
    chunk_mesher_test.cpp
    terrain_generator_test.cpp
    chunk_manager_test.cpp
    player_test.cpp
    collision_test.cpp
    raycaster_test.cpp
    inventory_test.cpp
    block_interaction_test.cpp
)

target_compile_features(poorcraft_tests PRIVATE cxx_std_17)

target_include_directories(
    poorcraft_tests
    PRIVATE
        ${PROJECT_SOURCE_DIR}/include
)

target_link_libraries(
    poorcraft_tests
    PRIVATE
        GTest::gmock
        GTest::gtest_main
        SDL2::SDL2
        glm::glm-header-only
        FastNoise
)

if(POORCRAFT_ENABLE_VULKAN AND _poorcraft_vulkan_available)
    target_link_libraries(poorcraft_tests PRIVATE Vulkan::Vulkan)
endif()

if(POORCRAFT_ENABLE_OPENGL)
    if(UNIX AND NOT APPLE)
        find_package(OpenGL REQUIRED)
        target_link_libraries(poorcraft_tests PRIVATE OpenGL::GL ${CMAKE_DL_LIBS})
    elseif(WIN32)
        find_package(OpenGL REQUIRED)
        target_link_libraries(poorcraft_tests PRIVATE OpenGL::GL)
    endif()
endif()

target_compile_definitions(
    poorcraft_tests
    PRIVATE
        $<$<AND:$<BOOL:${POORCRAFT_ENABLE_VULKAN}>,$<BOOL:${_poorcraft_vulkan_available}>>:POORCRAFT_VULKAN_ENABLED>
        $<$<AND:$<BOOL:${POORCRAFT_ENABLE_VULKAN}>,$<BOOL:${_poorcraft_vulkan_available}>>:GLM_FORCE_DEPTH_ZERO_TO_ONE>
        $<$<BOOL:${POORCRAFT_ENABLE_OPENGL}>:POORCRAFT_OPENGL_ENABLED>
)

gtest_discover_tests(
    poorcraft_tests
    DISCOVERY_MODE PRE_TEST
)
