#!/usr/bin/env bash

discard_local_change() {
    local repo_path=$1

    if [[ ! -d "${repo_path}/.git" ]]; then
        return
    fi

    git -C "$repo_path" fetch --all

    git -C "$repo_path" switch dev
    git -C "$repo_path" merge --abort
    git -C "$repo_path" rebase --abort
    git -C "$repo_path" reset --hard origin/dev

    git -C "$repo_path" switch test
    git -C "$repo_path" merge --abort
    git -C "$repo_path" rebase --abort
    git -C "$repo_path" reset --hard origin/test

    git -C "$repo_path" switch master
    git -C "$repo_path" merge --abort
    git -C "$repo_path" rebase --abort
    git -C "$repo_path" reset --hard origin/master

    echo -e "\033[32m>>>>>>>>>>>>>> $(echo ${folder} | sed 's/.*\///')\033[0m"
}

folder_path=$(pwd)
traverse_subfolders=0

while [[ $# -gt 0 ]]; do
    case $1 in
    -r)
        traverse_subfolders=1 # 遍历子文件夹的开关
        ;;
    esac
    shift
done

if [[ "$traverse_subfolders" -eq 1 ]]; then
    for folder in $(find "$folder_path" -maxdepth 1 -type d); do
        if [[ "$folder" == "$folder_path" || "$folder" == "" ]]; then
            continue
        fi

        discard_local_change "$folder"
    done
else
    discard_local_change "$folder_path"
fi
