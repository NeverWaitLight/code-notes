#!/usr/bin/env bash

# Function control variable
upgrade_to_release=0
upgrade_tag=0
debug=0

# Project.artifactId cache
declare -A cache

function loop() {
    local feat_branch=$1
    local release_branch=$2
    local upgrade_to_release=$3
    local upgrade_tag=$4
    local lambda=$5
    shift 5
    local project_names=($@)

    workspace=$(pwd)
    for element in "${project_names[@]}"; do
        if [[ $element == *":"* ]]; then
            IFS=':' read -ra parts <<<"$element"
            project_name="${parts[0]}"
            feat_branch="${parts[1]}"
        else
            project_name="$element"
        fi

        for project_path in $(find "$workspace" -maxdepth 1 -type d); do
            if [[ "$project_path" == "$workspace" || "$project_path" == "" ]]; then continue; fi
            if [[ ! -f "$project_path/pom.xml" ]]; then continue; fi

            curr_project_name=${cache["$project_path"]}
            if [[ -z "$curr_project_name" ]]; then
                curr_project_name=$(mvn -f "$project_path/pom.xml" help:evaluate -Dexpression=project.artifactId -q -DforceStdout)
                cache["$project_path"]="$curr_project_name"
            fi

            if [[ "$curr_project_name" != "$project_name" ]]; then continue; fi

            echo -e ">>>>>> Serach for $project_name to $lambda ......"
            eval "$lambda $project_path $project_name $feat_branch $release_branch $upgrade_to_release $upgrade_tag"
            break
        done
    done

    echo -e "\033[33m==================================================\033[0m"
    echo -e "\033[33mAll $lambda phase done\033[0m"
    echo -e "\033[33m==================================================\033[0m"
}

function is_in_local() {
    local project_path=$1
    local branch=$2
    local existed_in_local=$(git -C "$project_path" branch --list ${branch})

    if [[ -z ${existed_in_local} ]]; then
        return 0
    fi
    return 1
}

function is_in_remote() {
    local project_path=$1
    local branch=$2
    local existed_in_remote=$(git -C "$project_path" ls-remote --heads origin ${branch})

    if [[ -z ${existed_in_remote} ]]; then
        return 0
    fi
    return 1
}

function check_branch() {
    local project_path=$1
    local project_name=$2
    local branch=$3

    is_in_local "$project_path" "$branch"
    in_local=$?
    is_in_remote "$project_path" "$branch"
    in_remote=$?

    if [[ ! ${in_local} -eq 1 ]] && [[ ! ${in_remote} -eq 1 ]]; then
        echo -e "\033[31m>>> $project_name $branch not found!!!\033[0m"
        exit -1
    fi
}

function stash() {
    local project_path=$1
    if [[ -n $(git -C "$project_path" status -s) ]]; then
        git -C "$project_path" stash -m 'before release'
        git -C "$project_path" stash list
    fi
}

function merge() {
    local project_path=$1
    local project_name=$2
    local feat_branch=$3
    local release_branch=$4

    git -C "$project_path" fetch --all
    check_branch "$project_path" "$project_name" "$feat_branch"
    check_branch "$project_path" "$project_name" "$release_branch"
    git -C "$project_path" switch "$release_branch"
    git -C "$project_path" reset --hard "origin/$release_branch"
    git -C "$project_path" pull --rebase
    git -C "$project_path" merge --strategy-option=theirs --no-ff "origin/$feat_branch"
    if [[ -n $(git -C "$project_path" ls-files --unmerged) ]]; then
        echo -e "\033[0;31mXXX $project_name merge conflict!!!\033[0m"
        exit 1
    fi
    echo -e "\033[32m>>> $project_name merge $feat_branch into $release_branch OK\033[0m"
}

function deploy() {
    local project_path=$1
    local project_name=$2
    local upgrade_to_release=$5

    if [[ $upgrade_to_release -eq 1 ]]; then
        mvn -f "$project_path" versions:set -DremoveSnapshot
        local curr_ver=$(mvn -f "$project_path" help:evaluate -Dexpression=project.version -DforceStdout)
        git -C "$project_path" add .
        git -C "$project_path" commit -m "Upgrade to $curr_ver"
    fi

    if [[ $debug -eq 1 ]]; then
        mvn -f "$project_path" clean install -q -DskipTests -pl model,client
    else
        mvn -f "$project_path" clean deploy -q -DskipTests -pl model,client
    fi

    echo -e "\033[32m>>> $project_name deploy model,client OK\033[0m"
}

function upgrade_tag() {
    local project_path=$1
    local release_branch=$4
    local upgrade_tag=$6

    if [[ "$upgrade_tag" -ne 1 ]]; then
        return
    fi

    local curr_tag=$(git -C "$project_path" describe --tags $(git -C "$project_path" rev-list --tags --max-count=1))

    if [[ -z "$curr_tag" ]]; then
        curr_tag="v0.0.0"
    else
        head_hash=$(git -C "$project_path" rev-parse "$release_branch")
        latest_tag_hash=$(git -C "$project_path" rev-parse $curr_tag)
        if [ "$head_hash" == "$latest_tag_hash" ]; then
            echo "The latest commit is already tagged. Aborting."
            return
        fi
    fi

    curr_tag=${curr_tag#v}

    IFS='.' read -ra version_parts <<<"${curr_tag}"
    major=${version_parts[0]}
    minor=${version_parts[1]}
    patch=${version_parts[2]}

    patch=$((patch + 1))

    new_tag="v$major.$minor.$patch"

    git -C "$project_path" tag "$new_tag" -m "version $new_tag"
    git -C "$project_path" push --tags
    echo -e "\033[32m>>> $project_name upgrade new tag $new_tag OK\033[0m"
}

function upgrade() {
    local project_path=$1
    local project_name=$2

    mvn -f "$project_path" -q versions:use-releases
    if [[ -n $(git -C "$project_path" status -s) ]]; then
        git -C "$project_path" add .
        git -C "$project_path" commit -m "Upgrade dependencies to Releases"
    fi

    upgrade_tag $@

    echo -e "\033[32m>>> $project_name upgrade dependencies OK\033[0m"
}

function commit() {
    local project_path=$1
    local project_name=$2
    local release_branch=$4

    if [[ $debug -eq 0 ]]; then
        git -C "$project_path" push --tags
        git -C "$project_path" push origin "$release_branch"
    fi

    git -C "$project_path" log --graph --oneline --first-parent -n 5

    echo -e "\033[32m>>> $project_name all changes commited OK\033[0m"
}

while [ $# -gt 0 ]; do
    case $1 in
    -fb)
        feat_branch="$2"
        shift 2
        ;;
    -rb)
        release_branch="$2"
        shift 2
        ;;
    -r)
        upgrade_to_release=1
        shift 1
        ;;
    -t)
        upgrade_tag=1
        shift 1
        ;;
    -d)
        debug=1
        shift 1
        ;;
    -p | --project_names)
        shift
        while [ $# -gt 0 ] && [ "${1:0:1}" != "-" ]; do
            project_names+=("$1")
            shift
        done
        ;;
    *)
        echo "Unknown options: $1"
        shift
        ;;
    esac
done

[[ -z "$feat_branch" ]] && echo "feat_branch is empty" && exit -1
[[ -z "$release_branch" ]] && echo "release_branch is empty" && exit -1

loop "$feat_branch" "$release_branch" "$upgrade_to_release" "$upgrade_tag" "stash" "${project_names[@]}"
loop "$feat_branch" "$release_branch" "$upgrade_to_release" "$upgrade_tag" "merge" "${project_names[@]}"
loop "$feat_branch" "$release_branch" "$upgrade_to_release" "$upgrade_tag" "deploy" "${project_names[@]}"
loop "$feat_branch" "$release_branch" "$upgrade_to_release" "$upgrade_tag" "upgrade" "${project_names[@]}"
loop "$feat_branch" "$release_branch" "$upgrade_to_release" "$upgrade_tag" "commit" "${project_names[@]}"
