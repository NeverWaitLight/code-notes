#!/usr/bin/env bash

DEF_MEMORY_SIZE="512M" # 默认内存容量

service_name=$1
memory_size=${2:-"${DEF_MEMORY_SIZE}"}

if [[ -z "${service_name}" ]]; then
    exit 1
fi

latest_jar=$(ls "${service_name}"-*.jar 2>/dev/null | sort -V | tail -n 1)
if [[ -f "${latest_jar}" ]]; then
    mv "${latest_jar}" "${service_name}.jar"
fi

pids=$(pgrep -f "${service_name}.jar")
if [[ ! -z "${pids}" ]]; then
    for pid in "${pids}"; do
        kill "${pid}"
        while kill -0 "${pid}" >/dev/null 2>&1; do
            sleep 0.5
        done
    done
fi

nohup java -Xms"${memory_size}" -Xmx"${memory_size}" -XX:+UseG1GC \
    -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=./heapdump-%t.hprof \
    -jar "${service_name}".jar >/dev/null 2>&1 &
