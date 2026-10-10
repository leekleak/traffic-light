cmake_minimum_required(VERSION 3.22.1)

project("iperf3" C)

set(IPERF_SRC_DIR "${CMAKE_CURRENT_SOURCE_DIR}/iperf/src")
set(IPERF_GEN_DIR "${CMAKE_CURRENT_BINARY_DIR}/generated")
set(PACKAGE_VERSION "3.22")

configure_file(
    "${IPERF_SRC_DIR}/version.h.in"
    "${IPERF_GEN_DIR}/version.h"
    @ONLY
)

configure_file(
    "iperf_config.h"
    "${IPERF_GEN_DIR}/iperf_config.h"
    @ONLY
)

set(CMAKE_RUNTIME_OUTPUT_DIRECTORY "${CMAKE_LIBRARY_OUTPUT_DIRECTORY}")

set(IPERF_TARGET libiperf3.so)

add_executable(${IPERF_TARGET}
    "${IPERF_SRC_DIR}/main.c"
    "${IPERF_SRC_DIR}/cjson.c"
    "${IPERF_SRC_DIR}/dscp.c"
    "${IPERF_SRC_DIR}/iperf_api.c"
    "${IPERF_SRC_DIR}/iperf_auth.c"
    "${IPERF_SRC_DIR}/iperf_client_api.c"
    "${IPERF_SRC_DIR}/iperf_error.c"
    "${IPERF_SRC_DIR}/iperf_locale.c"
    "${IPERF_SRC_DIR}/iperf_sctp.c"
    "${IPERF_SRC_DIR}/iperf_server_api.c"
    "${IPERF_SRC_DIR}/iperf_tcp.c"
    "${IPERF_SRC_DIR}/iperf_time.c"
    "${IPERF_SRC_DIR}/iperf_udp.c"
    "${IPERF_SRC_DIR}/iperf_util.c"
    "${IPERF_SRC_DIR}/iperf_pthread.c"
    "${IPERF_SRC_DIR}/net.c"
    "${IPERF_SRC_DIR}/tcp_info.c"
    "${IPERF_SRC_DIR}/timer.c"
    "${IPERF_SRC_DIR}/units.c"
)

target_include_directories(${IPERF_TARGET} PRIVATE
    "${IPERF_GEN_DIR}"
    "${IPERF_SRC_DIR}"
    "${CMAKE_CURRENT_SOURCE_DIR}"
)

target_compile_definitions(${IPERF_TARGET} PRIVATE HAVE_CONFIG_H HAVE_PTHREAD)

set_target_properties(${IPERF_TARGET} PROPERTIES POSITION_INDEPENDENT_CODE ON)

target_link_libraries(${IPERF_TARGET} m log)