#!/usr/bin/env bash

excludes=("demo" "guava" "commons-lang" "tutorials" "winutils" "working" "test" "office-tools" "example")

# func：删除指定分支
# args：
#   - $1: Git 仓库路径
#   - $2: 是否模糊匹配分支
# desc：删除指定 Git 仓库中的本地和远程分支
delete_branches() {
    local repo_path=$1
    local approximately=$2

    if [[ ! -d "${repo_path}/.git" ]]; then
        return
    fi

    echo -e "\033[32m>>> $(echo ${folder} | sed 's/.*\///')\033[0m"
    git -C "$repo_path" fetch
    delete_local_branches "$repo_path" "$approximately"  # 调用函数删除本地分支
    delete_remote_branches "$repo_path" "$approximately" # 调用函数删除远程分支
}

# func：删除远程分支
# args：
#   - $1: Git 仓库路径
#   - $2: 是否模糊匹配分支
# desc：根据指定的分支名删除 Git 仓库中的远程分支
delete_remote_branches() {
    local repo_path=$1
    local approximately=$2
    if [[ "$approximately" -eq 1 ]]; then
        git -C "$repo_path" branch -r | sed -e 's/^[ \t\*]*//' | sed 's/[^/]*\///' | grep "^${branch_name}.*" | xargs -I {} git -C "$repo_path" push origin --delete {}
    else
        git -C "$repo_path" branch -r | sed -e 's/^[ \t\*]*//' | sed 's/[^/]*\///' | grep "\\${branch_name}\b" | xargs -I {} git -C "$repo_path" push origin --delete {}
    fi
}

# func：删除本地分支
# args：
#   - $1: Git 仓库路径
#   - $2: 是否模糊匹配分支
# desc：根据指定的分支名删除 Git 仓库中的本地分支
delete_local_branches() {
    local repo_path=$1
    local approximately=$2
    if [[ "$approximately" -eq 1 ]]; then
        git -C "$repo_path" branch | sed -e 's/^[ \t\*]*//' | grep "^${branch_name}.*" | xargs -I {} git -C "$repo_path" branch -D {}
    else
        git -C "$repo_path" branch | sed -e 's/^[ \t\*]*//' | grep "\\${branch_name}\b" | xargs -I {} git -C "$repo_path" branch -D {}
    fi
}

# 默认设置
folder_path=$(pwd)
branch_name=""
traverse_subfolders=0
approximately=0 # 模糊匹配

# 处理命令行选项
while [[ $# -gt 0 ]]; do
    case $1 in
    -b)
        shift
        branch_name="$1" # 设置要删除的分支名
        ;;
    -r)
        traverse_subfolders=1 # 遍历子文件夹的开关
        ;;
    -a)
        approximately=1 # 模糊匹配开关
        ;;
    esac
    shift
done

# 根据遍历子文件夹的开关来选择如何删除分支
if [[ "$traverse_subfolders" -eq 1 ]]; then
    for folder in $(find "$folder_path" -maxdepth 1 -type d); do
        # 排除当前文件夹和上级文件夹
        if [[ "$folder" == "$folder_path" || "$folder" == "" ]]; then
            continue
        fi

        e=false
        for str in "${excludes[@]}"; do
            if [[ "$folder" == *"$str"* ]]; then
                e=true
                break
            fi
        done

        if [[ "$e" == true ]]; then
            continue
        fi

        delete_branches "$folder" "$approximately"
    done
else
    # 只删除当前文件夹的分支
    delete_branches "$folder_path" "$approximately"
fi
