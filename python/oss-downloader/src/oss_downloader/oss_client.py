from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime
from typing import Iterator
from urllib.parse import urlparse

import oss2


@dataclass(frozen=True)
class ObjectItem:
    key: str
    size: int
    etag: str
    last_modified: datetime | None


class OssClient:
    def __init__(
        self,
        endpoint: str,
        region: str,
        access_key_id: str,
        access_key_secret: str,
        bucket: str,
    ) -> None:
        endpoint = _normalize_endpoint(endpoint)
        auth = oss2.AuthV4(access_key_id, access_key_secret)
        self.bucket = oss2.Bucket(auth, endpoint, bucket, region=region)

    def list_objects(self, prefix: str) -> Iterator[ObjectItem]:
        token: str | None = None
        while True:
            # Only pass continuation_token if it's not None to avoid V4 signature issues
            if token:
                result = self.bucket.list_objects_v2(prefix=prefix, continuation_token=token)
            else:
                result = self.bucket.list_objects_v2(prefix=prefix)

            object_list = result.object_list
            if object_list is None:
                object_list = []
            for obj in object_list:
                last_modified = None
                if getattr(obj, "last_modified", None):
                    last_modified = datetime.fromtimestamp(obj.last_modified)
                yield ObjectItem(
                    key=obj.key,
                    size=int(obj.size),
                    etag=str(getattr(obj, "etag", "")),
                    last_modified=last_modified,
                )
            if not result.is_truncated:
                break
            token = result.next_continuation_token

    def download_to_file(self, key: str, dest_path: str) -> None:
        self.bucket.get_object_to_file(key, dest_path)


def _normalize_endpoint(endpoint: str) -> str:
    raw = endpoint.strip()
    if not raw:
        return raw
    if raw.startswith("http://") or raw.startswith("https://"):
        return raw
    parsed = urlparse("https://" + raw)
    if parsed.netloc:
        return "https://" + raw
    return raw
