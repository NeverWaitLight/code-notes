#!/usr/bin/env bash

folder=$(pwd)
tmp="$folder/extra.csv.tmp"
output="$folder/extra.csv"
content_model=1

while [[ $# -gt 0 ]]; do
  case $1 in
  -r)
    traverse_subfolders=1
    shift
    ;;
  --regex)
    regex="$2"
    shift 2
    ;;
  -f | --file-name)
    content_model=0
    shift 1
    ;;
  -ft | --file_types)
    shift
    while [ $# -gt 0 ] && [ "${1:0:1}" != "-" ]; do
      file_types+=("$1")
      shift
    done
    ;;
  esac
done

rm -f "$tmp" && touch "$tmp"
rm -f "$output"

function extra() {
  local curr_folder=$1

  curr_folder_name=$(basename $curr_folder)
  for file_type in "${file_types[@]}"; do
    if [[ "$file_type" =~ "." ]]; then
      file_type="*$file_type"
    else
      file_type="*.$file_type"
    fi
    find "$curr_folder" -type f -name "$file_type" -not -path '*/.*' | while IFS= read -r file; do
      filename=$(basename $file)
      file_relative=$(echo "${file#$curr_folder/}")
      echo "--->>> Check $file_relative"
      if [[ $content_model -eq 1 ]]; then
        contentStr=$(grep -E "$regex" "$file" | sed 's/^[ \t]*//;s/[ \t]*$//')
        if [[ -z "$contentStr" ]]; then
          continue
        fi
        readarray -t contents < <(grep -E "$regex" "$file" | sed 's/,/ /g' | sed 's/^[ \t]*//;s/[ \t]*$//' | sed 's/  */ /g')
        for content in "${contents[@]}"; do
          echo "$curr_folder_name, $content, $file_relative" >>"$tmp"
        done
      else
        lowercase_file=$(echo "$filename" | tr '[:upper:]' '[:lower:]')
        lowercase_regex=$(echo "$regex" | tr '[:upper:]' '[:lower:]')
        if [[ "$lowercase_file" == *"$lowercase_regex"* ]]; then
          echo "$curr_folder_name, $filename, $file_relative" >>"$tmp"
        fi
      fi
    done
  done
}

if [[ "$traverse_subfolders" -eq 1 ]]; then
  for curr_folder in $(find "$folder" -maxdepth 1 -type d); do
    if [[ "$curr_folder" == "$folder" || "$curr_folder" == "" ]]; then
      continue
    fi
    extra "$curr_folder"
  done
else
  extra "$folder"
fi

sort "$tmp" | uniq -i >"$output"
sed -i '1i\folder, content, path' "$output"
rm -f "$tmp"
