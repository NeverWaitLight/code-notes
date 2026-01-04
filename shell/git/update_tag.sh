#!/usr/bin/env bash

branch_name="master"

# 判断参数来决定更新部分
while [[ $# -gt 0 ]]; do
    case $1 in
    --major) major=$((major + 1)) ;;
    --minor) minor=$((minor + 1)) ;;
    --patch) patch=$((patch + 1)) ;;
    -b | --branch) branch_name=$2 ;;
    esac
    shift
done

# 获取当前的最大的版本标签
git fetch --all
current_version=$(git describe --tags $(git rev-list --tags --max-count=1))

# 如果没有找到任何标签则定义初始标签
if [[ -z "$current_version" ]]; then
    current_version="v0.0.0"
else
    # Check whether the latest commit is not already tagged
    head_hash=$(git rev-parse "$branch_name")
    latest_tag_hash=$(git rev-parse $current_version)
    if [ "$head_hash" == "$latest_tag_hash" ]; then
        echo "The latest commit is already tagged. Aborting."
        exit 0
    fi
fi

# Remove the 'v' from version if it's there
current_version=${current_version#v}

# 将当前版本号拆分成 major, minor 和 patch
IFS='.' read -ra version_parts <<<"${current_version}"

major=${version_parts[0]}
minor=${version_parts[1]}
patch=${version_parts[2]}
if [[ $major -eq ${version_parts[0]} ]] && [[ $minor -eq ${version_parts[1]} ]] && [[ $patch -eq ${version_parts[2]} ]]; then
    patch=$((patch + 1))
fi

new_version="v$major.$minor.$patch"

git tag "$new_version"
git push --tags
echo -e "\033[32m>>> Upgrade tag to $new_version OK\033[0m"
