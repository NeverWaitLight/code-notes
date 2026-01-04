#!/usr/bin/env bash

DEF_MEMORY_SIZE="512M"             # 默认内存容量
WAIT_START_UP_INTERVAL=$((5 * 60)) # 等待jar启动就绪时间
SAMPLING_INTERVAL=10               # 采样时间间隔
SAMPLES_MAX_SIZE=30                # 最大样本数量
CPU_THRESHOLD=90                   # CPU占用阈值（百分比）

# func: 启动\重启jar包
# args:
#   - $1: 服务名
#   - $2: 内存容量
start_jar() {
    local service_name=$1
    local memory_size=${2:-"${DEF_MEMORY_SIZE}"}

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
}

# func: 监控cpu占用率
# args:
#   - $1: 服务名
watch_cpu_usage() {
    local service_name=$1
    local samples=() # 采样结果存储的数组

    local pids=$(pgrep -f "${service_name}.jar")

    while [[ ! -z ${pids} ]]; do
        for pid in "${pids}"; do
            if ! kill -0 "${pid}" >/dev/null 2>&1; then
                pids=("${pids[@]:1}")
            fi

            if [[ ${#samples[@]} -gt "${SAMPLES_MAX_SIZE}" ]]; then
                samples=("${samples[@]:1}")
            fi
            sample=$(ps -p "${pid}" -o %cpu | grep -v CPU)
            sample=$(printf "%.0f" "${sample}")
            samples+=("${sample}")

            num_above_threshold=0
            for s in "${samples[@]}"; do
                if [[ $s -ge "${CPU_THRESHOLD}" ]]; then
                    ((num_above_threshold++))
                fi
            done
            if [[ ${num_above_threshold} -ge $((${SAMPLES_MAX_SIZE} / 2)) ]]; then
                output_file="${pid}-jstack-$(date '+%Y-%m-%d-%H-%M-%S').log" # jstack输出文件名
                jstack $pid >$output_file
            fi
            num_above_threshold=0
        done
        sleep "${SAMPLING_INTERVAL}"
    done
}

start_jar $@
sleep ${WAIT_START_UP_INTERVAL}
watch_cpu_usage $@
