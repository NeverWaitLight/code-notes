import csv
import io
from typing import List, Dict, Any

import httpx


def read_urls_from_csv(csv_path: str) -> List[str]:
    """
    从 CSV 文件读取 URL 列表

    参数说明
    csv_path: CSV 文件路径

    返回结果
    URL 列表
    """
    urls = []
    with open(csv_path, "r", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for row in reader:
            url = row.get("url", "").strip()
            if url:
                urls.append(url)
    return urls


def upload_document(
    url: str,
    filename: str,
    api_url: str = "http://localhost:9621",
    timeout: float = 300.0,
) -> Dict[str, Any]:
    """
    从 URL 流式下载文档并上传到 API

    参数说明
    url: 源文档 URL
    filename: 上传时使用的文件名
    api_url: API 基础地址
    timeout: 请求超时时间（秒）

    返回结果
    包含上传结果的字典
    """
    upload_url = f"{api_url.rstrip('/')}/documents/upload"

    try:
        with httpx.stream("GET", url, timeout=timeout) as download_response:
            download_response.raise_for_status()

            file_content = io.BytesIO()
            for chunk in download_response.iter_bytes():
                file_content.write(chunk)

            file_content.seek(0)

            files = {"file": (filename, file_content, "text/plain")}

            with httpx.Client(timeout=timeout) as client:
                upload_response = client.post(upload_url, files=files)
                upload_response.raise_for_status()
                result = upload_response.json()

            return {
                "success": True,
                "filename": filename,
                "url": url,
                "status": result.get("status", "unknown"),
                "message": result.get("message", ""),
                "track_id": result.get("track_id", ""),
            }

    except httpx.HTTPStatusError as e:
        return {
            "success": False,
            "filename": filename,
            "url": url,
            "error": f"HTTP错误: {e.response.status_code} - {e.response.text}",
        }
    except httpx.RequestError as e:
        return {
            "success": False,
            "filename": filename,
            "url": url,
            "error": f"请求错误: {str(e)}",
        }
    except Exception as e:
        return {
            "success": False,
            "filename": filename,
            "url": url,
            "error": f"未知错误: {str(e)}",
        }


def main():
    """主函数"""
    csv_path = "urls.csv"
    api_url = "http://localhost:9621"

    print(f"正在从 {csv_path} 读取 URL 列表...")
    urls = read_urls_from_csv(csv_path)
    total = len(urls)
    print(f"共找到 {total} 个 URL")

    if total == 0:
        print("未找到任何 URL，退出")
        return

    results = []
    success_count = 0
    fail_count = 0

    for index, url in enumerate(urls, start=1):
        filename = f"第{index}章.txt"
        print(f"[{index}/{total}] 正在上传 {filename}...")

        result = upload_document(url, filename, api_url)
        results.append(result)

        if result["success"]:
            success_count += 1
            status = result.get("status", "unknown")
            message = result.get("message", "")
            track_id = result.get("track_id", "")
            print(f"  成功: {status} - {message}")
            if track_id:
                print(f"  Track ID: {track_id}")
        else:
            fail_count += 1
            error = result.get("error", "未知错误")
            print(f"  失败: {error}")

    print("\n" + "=" * 60)
    print("上传完成")
    print(f"总计: {total} 个文件")
    print(f"成功: {success_count} 个")
    print(f"失败: {fail_count} 个")
    print("=" * 60)

    if fail_count > 0:
        print("\n失败的文件:")
        for result in results:
            if not result["success"]:
                print(f"  - {result['filename']}: {result.get('error', '未知错误')}")


if __name__ == "__main__":
    main()
