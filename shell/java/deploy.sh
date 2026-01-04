#!/bin/bash
nignxWorkDir="/var/www"

GREEN="\033[0;32m"
YELLOW="\033[0;33m"
NC="\033[0m"

# 1. project name
# 2. git url
# 3. tech (tech stack)
# 4. type: app\dep
projects=(
  "project-web web app https://username:password@url.git dictName"
  "project-server java app https://username:password@url.git"
)

function run() {
  local max_project_name_len=0
  for i in ${!projects[@]}; do
    local p=(${projects[$i]})
    local len=${#p[0]}
    if [ $len -gt $max_project_name_len ]; then
      max_project_name_len=$len
    fi
  done

  echo "- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -"
  let max_project_name_len=$max_project_name_len+5
  for i in ${!projects[@]}; do
    local p=(${projects[$i]})
    local num=$(expr $i + 1)
    printf "%-2s ${GREEN}%-${max_project_name_len}s${NC}  ${YELLOW}%-5s${NC} %5s\n" $num "${p[0]}" "${p[1]}" "${p[2]}"
  done
  echo "- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -"
  echo "Deploy all projects Press $(tput bold)Enter$(tput sgr0)"
  echo "Please select project to deploy: "

  read index

  if [ -z "$index" ]; then
    for i in ${!projects[@]}; do
      local p=(${projects[$i]})
      do_task ${p[*]}
    done
  else
    if [ $index -lt 0 ] || [ $index -gt ${#projects[@]} ]; then
      echo "$(tput setaf 1)Error value, valid value is [0, ${#projects[@]}]$(tput sgr0)"
      return
    fi
    let index=$index-1
    local p=(${projects[$index]})
    do_task ${p[*]}
  fi

  echo "$(tput setaf 2)"All task done!"$(tput sgr0)"
}

function update() {
  if [ ! -d "${project_name}" ]; then
    git clone "${git_url}"
  fi

  git -C "${project_name}" fetch -a
  if git show-ref --quiet refs/heads/dev; then
    git -C "${project_name}" checkout dev
  else
    git -C "${project_name}" checkout --track origin/dev
  fi

  git -C "${project_name}" merge

  echo "$(tput setaf 2)"${project_name} already updated ..."$(tput sgr0)"
}

function package_web() {
  if [ ! -d "${project_name}" ]; then
    echo "${project_name} directory not found"
    return
  fi

  npm --registry https://registry.npm.taobao.org --prefix "${project_name}" install
  if ! npm --prefix "${project_name}" run build:dev; then
    echo "$(tput setaf 1)${project_name} build has error!!!$(tput sgr0)"
    return
  fi
  echo "$(tput setaf 2)Package ${project_name} ok ...$(tput sgr0)"

  rsync -a --delete "${project_name}/dist/" "${nignxWorkDir}/${short_name}"
  nginx -s reload
  echo "$(tput setaf 2)Copy ${project_name} ok ...$(tput sgr0)"
}

function package_java() {
  if [ ! -d "${project_name}" ]; then
    echo "$(tput bold setaf 1)${project_name} directory not found !!!$(tput sgr0)"
    return
  fi

  if ! mvn -Djar.finalName="${project_name}" -f "${project_name}/pom.xml" clean package deploy -U -DskipTests; then
    echo "$(tput bold setaf 1)Package ${project_name} has error, stop !!!$(tput sgr0)"
    return
  fi

  echo "$(tput setaf 2)Package ${project_name} ok ...$(tput sgr0)"
}

function restart_java() {
  cp -f "${project_name}"/server/target/*.jar /opt/services/"${project_name}".jar
  echo "$(tput setaf 2)Copy jar ok ...$(tput sgr0)"

  pidList=$(ps -ef | grep java | grep -v grep | grep "${project_name}".jar | awk '{print $2}')
  for pid in $pidList; do
    kill "${pid}"
    echo "$(tput setaf 1)Kill ${pid} ...$(tput sgr0)"
    echo "$(tput setaf 2)"The server is too weak, please wait 30 seconds ..."$(tput sgr0)"
    progress_bar 30
  done

  if [ -f /opt/logs/"${project_name}"-console.log ]; then
    rm /opt/logs/"${project_name}"-console.log
    echo "$(tput setaf 2)Remove ${project_name} console log ok ...$(tput sgr0)"
  fi

  nohup java -Xmx512m -server -Djava.security.egd=file:/dev/./urandom -jar /opt/services/"${project_name}".jar --spring.profiles.active=local >/opt/logs/"${project_name}"-console.log 2>&1 &
  echo "$(tput setaf 2)Run ${project_name} jar on background ...$(tput sgr0)"
}

function progress_bar() {
  local duration=${1}

  already_done() { for ((done = 0; done < $elapsed; done++)); do printf "▇"; done; }
  remaining() { for ((remain = $elapsed; remain < $duration; remain++)); do printf " "; done; }
  percentage() { printf "| %s%%" $(((($elapsed) * 100) / ($duration) * 100 / 100)); }
  clean_line() { printf "\r"; }

  for ((elapsed = 1; elapsed <= $duration; elapsed++)); do
    already_done
    remaining
    percentage
    sleep 1
    clean_line
  done
}

function do_task() {
  local project_name="$1"
  local tech="$2"
  local type="$3"
  local git_url="$4"
  local short_name="$5"

  exec {lock_fd}>"/tmp/${project_name}.lock"
  flock -nx "${lock_fd}" || {
    echo "Script is running by other user, please wait and retry."
    exit 1
  }

  update ${project_name} ${git_url}

  if [ "java" == "${tech}" ]; then
    package_java ${project_name}
    if [ "app" == "${type}" ]; then
      restart_java ${project_name}
    fi
  fi

  if [ "web" == "${tech}" ]; then
    package_web ${project_name} ${short_name}
  fi

  flock -u "${lock_fd}"
  return
}

run
