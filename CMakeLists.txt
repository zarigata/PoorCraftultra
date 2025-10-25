cmake_minimum_required(VERSION 3.31)

project(
    PoorCraft
    VERSION 0.1.0
    DESCRIPTION "PoorCraft - Open Source Voxel Game Engine"
    LANGUAGES C CXX
)

if(CMAKE_SOURCE_DIR STREQUAL CMAKE_BINARY_DIR)
    message(
        FATAL_ERROR
        "In-source builds are not supported. Please specify a separate build directory."
    )
endif()

option(POORCRAFT_BUILD_TESTS "Build the PoorCraft test suite" ON)
option(POORCRAFT_UNITY_BUILD "Enable unity builds for compilation speed" OFF)
option(POORCRAFT_ENABLE_VULKAN "Enable the Vulkan renderer backend" ON)
option(POORCRAFT_ENABLE_OPENGL "Enable the OpenGL renderer backend" ON)
option(POORCRAFT_BUILD_IMGUI_DEMO "Build the Dear ImGui demo window" OFF)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

set_property(GLOBAL PROPERTY USE_FOLDERS ON)

include(FetchContent)

set(_poorcraft_vulkan_available FALSE CACHE BOOL "Vulkan SDK available for PoorCraft" FORCE)
set(_poorcraft_opengl_available TRUE)

if(POORCRAFT_ENABLE_VULKAN)
    find_package(Vulkan QUIET)
    if(Vulkan_FOUND)
        set(_poorcraft_vulkan_available TRUE CACHE BOOL "Vulkan SDK available for PoorCraft" FORCE)
    else()
        set(_poorcraft_vulkan_available FALSE CACHE BOOL "Vulkan SDK available for PoorCraft" FORCE)
    endif()
else()
    set(Vulkan_FOUND FALSE)
endif()

if(NOT POORCRAFT_ENABLE_OPENGL)
    set(_poorcraft_opengl_available FALSE)
endif()

find_package(SDL2 CONFIG QUIET)
if(NOT SDL2_FOUND)
    message(STATUS "SDL2 package not found, fetching from source...")
    set(SDL2_DISABLE_INSTALL ON CACHE BOOL "" FORCE)
    FetchContent_Declare(
        SDL2
        GIT_REPOSITORY https://github.com/libsdl-org/SDL.git
        GIT_TAG release-2.32.6
        OVERRIDE_FIND_PACKAGE
        SYSTEM
        EXCLUDE_FROM_ALL
    )
    FetchContent_MakeAvailable(SDL2)
endif()

set(FETCHCONTENT_TRY_FIND_PACKAGE_MODE ALWAYS)

find_package(glm CONFIG QUIET)
if(glm_FOUND)
    message(STATUS "GLM package found: ${glm_DIR}")
else()
    message(STATUS "GLM package not found, fetching from source...")
    FetchContent_Declare(
        glm
        GIT_REPOSITORY https://github.com/g-truc/glm.git
        GIT_TAG 1.0.2
        GIT_SHALLOW TRUE
        SYSTEM
        FIND_PACKAGE_ARGS NAMES glm CONFIG
    )
    FetchContent_MakeAvailable(glm)
    message(STATUS "GLM fetched via FetchContent")
endif()

find_package(FastNoise2 CONFIG QUIET)
if(FastNoise2_FOUND)
    message(STATUS "FastNoise2 package found: ${FastNoise2_DIR}")
else()
    message(STATUS "FastNoise2 package not found, fetching from source...")
    FetchContent_Declare(
        FastNoise2
        GIT_REPOSITORY https://github.com/Auburn/FastNoise2.git
        GIT_TAG v0.10.0-alpha
        GIT_SHALLOW TRUE
        SYSTEM
    )
    set(FASTNOISE2_NOISETOOL OFF CACHE BOOL "" FORCE)
    FetchContent_MakeAvailable(FastNoise2)
    message(STATUS "FastNoise2 fetched via FetchContent")
endif()

find_package(imgui CONFIG QUIET)
if(imgui_FOUND)
    message(STATUS "Dear ImGui package found: ${imgui_DIR}")
    if(TARGET imgui::imgui AND NOT TARGET imgui)
        add_library(imgui ALIAS imgui::imgui)
    endif()
else()
    message(STATUS "Dear ImGui package not found, fetching from source...")
    FetchContent_Declare(
        imgui
        GIT_REPOSITORY https://github.com/ocornut/imgui.git
        GIT_TAG v1.91.9
        GIT_SHALLOW TRUE
        SYSTEM
    )
    FetchContent_MakeAvailable(imgui)
    message(STATUS "Dear ImGui fetched via FetchContent")
endif()

if(NOT TARGET imgui)
    if(NOT DEFINED imgui_SOURCE_DIR)
        message(FATAL_ERROR "Dear ImGui source directory not available for building bundled target.")
    endif()

    add_library(imgui STATIC)

    target_sources(
        imgui
        PRIVATE
            ${imgui_SOURCE_DIR}/imgui.cpp
            ${imgui_SOURCE_DIR}/imgui_draw.cpp
            ${imgui_SOURCE_DIR}/imgui_tables.cpp
            ${imgui_SOURCE_DIR}/imgui_widgets.cpp
    )

    if(POORCRAFT_BUILD_IMGUI_DEMO)
        target_sources(imgui PRIVATE ${imgui_SOURCE_DIR}/imgui_demo.cpp)
    endif()

    if(POORCRAFT_ENABLE_VULKAN AND Vulkan_FOUND)
        target_sources(imgui PRIVATE ${imgui_SOURCE_DIR}/backends/imgui_impl_vulkan.cpp)
        target_compile_definitions(imgui PUBLIC IMGUI_IMPL_VULKAN_NO_PROTOTYPES)
    endif()

    if(POORCRAFT_ENABLE_OPENGL)
        target_sources(imgui PRIVATE ${imgui_SOURCE_DIR}/backends/imgui_impl_opengl3.cpp)
        target_compile_definitions(imgui PUBLIC IMGUI_IMPL_OPENGL_LOADER_CUSTOM)
    endif()

    target_sources(imgui PRIVATE ${imgui_SOURCE_DIR}/backends/imgui_impl_sdl2.cpp)

    target_include_directories(
        imgui
        PUBLIC
            ${imgui_SOURCE_DIR}
            ${imgui_SOURCE_DIR}/backends
    )
endif()

if(POORCRAFT_ENABLE_VULKAN AND _poorcraft_vulkan_available AND _poorcraft_opengl_available)
    message(STATUS "PoorCraft renderers enabled: Vulkan, OpenGL")
elseif(POORCRAFT_ENABLE_VULKAN AND _poorcraft_vulkan_available)
    message(STATUS "PoorCraft renderer enabled: Vulkan")
elseif(_poorcraft_opengl_available)
    message(STATUS "PoorCraft renderer enabled: OpenGL")
else()
    message(WARNING "All renderer backends are disabled. PoorCraft will build without rendering support.")
endif()

include(GNUInstallDirs)
include(CMakePackageConfigHelpers)

if(MSVC)
    add_compile_options(/W4 /permissive- /Zc:__cplusplus)
else()
    add_compile_options(-Wall -Wextra -Wpedantic)
endif()

if(POORCRAFT_BUILD_TESTS)
    include(CTest)
endif()

add_subdirectory(src)

if(POORCRAFT_BUILD_TESTS AND BUILD_TESTING)
    add_subdirectory(tests)
endif()

install(
    DIRECTORY ${PROJECT_SOURCE_DIR}/assets/
    DESTINATION ${CMAKE_INSTALL_DATADIR}/poorcraft/assets
    FILES_MATCHING PATTERN "*"
)

install(
    FILES ${PROJECT_SOURCE_DIR}/README.md
    DESTINATION ${CMAKE_INSTALL_DATADIR}/poorcraft
)
