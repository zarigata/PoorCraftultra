cmake_minimum_required(VERSION 3.31)

add_executable(
    poorcraft
    main.cpp
    core/Window.cpp
    core/GPUInfo.cpp
    core/Timer.cpp
    core/Input.cpp
    core/Camera.cpp
    core/Player.cpp
    rendering/VulkanRenderer.cpp
    rendering/OpenGLRenderer.cpp
    rendering/RendererFactory.cpp
    world/Block.cpp
    world/Chunk.cpp
    world/ChunkMesh.cpp
    world/ChunkMesher.cpp
    world/TerrainGenerator.cpp
    world/ChunkManager.cpp
)

set_target_properties(
    poorcraft
    PROPERTIES
        UNITY_BUILD ${POORCRAFT_UNITY_BUILD}
)

if(APPLE)
    set_target_properties(poorcraft PROPERTIES MACOSX_BUNDLE ON)
endif()

target_compile_features(poorcraft PRIVATE cxx_std_17)

target_include_directories(
    poorcraft
    PRIVATE
        ${PROJECT_SOURCE_DIR}/include
)

target_link_libraries(
    poorcraft
    PRIVATE
        SDL2::SDL2
        SDL2::SDL2main
        glm::glm-header-only
        FastNoise
)

if(POORCRAFT_ENABLE_VULKAN AND _poorcraft_vulkan_available)
    target_link_libraries(poorcraft PRIVATE Vulkan::Vulkan)
endif()

if(POORCRAFT_ENABLE_OPENGL)
    if(UNIX AND NOT APPLE)
        find_package(OpenGL REQUIRED)
        target_link_libraries(poorcraft PRIVATE OpenGL::GL ${CMAKE_DL_LIBS})
    elseif(WIN32)
        find_package(OpenGL REQUIRED)
        target_link_libraries(poorcraft PRIVATE OpenGL::GL)
    endif()
endif()

target_compile_definitions(
    poorcraft
    PRIVATE
        $<$<AND:$<BOOL:${POORCRAFT_ENABLE_VULKAN}>,$<BOOL:${_poorcraft_vulkan_available}>>:POORCRAFT_VULKAN_ENABLED>
        $<$<AND:$<BOOL:${POORCRAFT_ENABLE_VULKAN}>,$<BOOL:${_poorcraft_vulkan_available}>>:GLM_FORCE_DEPTH_ZERO_TO_ONE>
        $<$<BOOL:${POORCRAFT_ENABLE_OPENGL}>:POORCRAFT_OPENGL_ENABLED>
)

install(
    TARGETS poorcraft
    RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
    LIBRARY DESTINATION ${CMAKE_INSTALL_LIBDIR}
    ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
)
