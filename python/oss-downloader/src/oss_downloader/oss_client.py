from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime
from typing import Iterator

import oss2


@dataclass(frozen=True)
class ObjectItem:
    key: str
    size: int
    etag: str
    last_modified: datetime | None


class OssClient:
    def __init__(self, endpoint: str, access_key_id: str, access_key_secret: str, bucket: str) -> None:
        auth = oss2.Auth(access_key_id, access_key_secret)
        self.bucket = oss2.Bucket(auth, endpoint, bucket)

    def list_objects(self, prefix: str) -> Iterator[ObjectItem]:
        token: str | None = ""
        while True:
            result = self.bucket.list_objects_v2(prefix=prefix, continuation_token=token or None)
            for obj in result.object_list:
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