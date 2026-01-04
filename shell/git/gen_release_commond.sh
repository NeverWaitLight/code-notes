#!/usr/bin/env bash

script_path=$0
folder_path=$(pwd)
while [[ $# -gt 0 ]]; do
    case $1 in
    -fb)
        feat_branch="$2"
        shift 2
        ;;
    -rb)
        release_branch="$2"
        shift 2
        ;;
    esac
done

if [[ "${script_path:0:1}" = "/" ]]; then
    script_dir=$(dirname $script_path)
else
    script_dir="$(pwd)"/"$(dirname $script_path)"
fi
command="bash $script_dir/release.sh -d -fb $feat_branch -rb $release_branch -p"

for folder in $(find "$folder_path" -maxdepth 1 -type d); do
    if [[ "$folder" == "$folder_path" || "$folder" == "" ]]; then
        continue
    fi

    if [[ ! -d "${folder}/.git" ]]; then
        continue
    fi

    echo -e "\033[32m>>> Find in $folder\033[0m"
    git -C "$folder" fetch -a
    branch_exist=$(git -C "$folder" branch -a | grep "${feat_branch}")
    if [[ ! -z "$branch_exist" ]]; then
        git -C "$folder" branch -a | grep "${feat_branch}"
        project=$(mvn -f "$folder/pom.xml" help:evaluate -Dexpression=project.artifactId -q -DforceStdout)
        command+=" $project"
    fi
done

echo -e "\033[33m==================================================\033[0m"
echo -e "\033[33m$command\033[0m"
echo -e "\033[33m==================================================\033[0m"
