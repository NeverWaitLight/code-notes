#!/usr/bin/env bash

keep_count=10

delete_branches() {
    local repo_path=$1
    local approximately=$2

    if [[ ! -d "${repo_path}/.git" ]]; then
        return
    fi

    echo -e "\033[32m>>> $repo_path\033[0m"
    git -C "$repo_path" fetch
    tag_list=$(git -C "$repo_path" tag --sort=-committerdate | grep -E '^[0-9v\.]+$')

    for tag in $tag_list; do
        keep_count=$((keep_count - 1))

        if [[ $keep_count -lt 0 ]]; then
            git -C "$repo_path" tag -d "$tag"
            git -C "$repo_path" push origin :refs/tags/"$tag"
        fi
    done

}

# 默认设置
folder_path=$(pwd)
traverse_subfolders=0

# 处理命令行选项
while [[ $# -gt 0 ]]; do
    case $1 in
    -r)
        traverse_subfolders=1 # 遍历子文件夹的开关
        shift
        ;;
    esac
done

if [[ "$traverse_subfolders" -eq 1 ]]; then
    for folder in $(find "$folder_path" -maxdepth 1 -type d); do
        if [[ "$folder" == "$folder_path" || "$folder" == "" ]]; then
            continue
        fi

        delete_branches "$folder"
    done
else
    delete_branches "$folder_path"
fi
