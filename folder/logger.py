import json
import time
import os
import logging
from logging.handlers import RotatingFileHandler

from mitmproxy import http

# ========= 加载配置 =========
with open("config.json") as f:
    CONFIG = json.load(f)

URL_PREFIXES = CONFIG["prefixes"]
LOG_FILE = CONFIG["logFile"]
MAX_LOG_SIZE = CONFIG["maxLogSizeMB"] * 1024 * 1024
RETENTION_DAYS = CONFIG["retentionDays"]
MAX_BODY_SIZE = CONFIG["maxBodyKB"] * 1024

# ========= 日志 =========
logger = logging.getLogger("proxy_logger")
logger.setLevel(logging.INFO)

handler = RotatingFileHandler(
    LOG_FILE,
    maxBytes=MAX_LOG_SIZE,
    backupCount=20,
    encoding="utf-8"
)

logger.addHandler(handler)


# ========= 工具 =========
def build_match_url(req: http.Request):
    return f"{req.host}{req.path}".lower()

def match_prefix(req: http.Request):
    target = build_match_url(req)
    return any(target.startswith(p.lower()) for p in URL_PREFIXES)


def is_multipart(req: http.Request):
    content_type = req.headers.get("Content-Type", "")
    return "multipart/form-data" in content_type.lower()


def too_large(flow: http.HTTPFlow):
    req_len = len(flow.request.raw_content or b"")
    resp_len = len(flow.response.raw_content or b"")
    return req_len > MAX_BODY_SIZE or resp_len > MAX_BODY_SIZE


def safe_text(message):
    try:
        return message.get_text(strict=False)
    except:
        return ""


# ========= 生命周期 =========
def request(flow: http.HTTPFlow):
    flow.metadata["start_time"] = time.time()


def response(flow: http.HTTPFlow):
    req = flow.request
    resp = flow.response

    # 1️⃣ URL 前缀过滤
    if not match_prefix(req):
        return

    # 2️⃣ 上传文件过滤
    if is_multipart(req):
        return

    # 3️⃣ 大报文过滤
    if too_large(flow):
        return

    # 4️⃣ 计算耗时
    start = flow.metadata.get("start_time", time.time())
    duration = int((time.time() - start) * 1000)

    # 5️⃣ 只保留指定 header
    headers = {}
    token = req.headers.get("x-meta-token")
    if token:
        headers["x-meta-token"] = token

    log = {
        "method": req.method,
        "host": req.host,
        "path": req.path,
        "headers": headers,
        "queryString": dict(req.query),
        "requestBody": safe_text(req),
        "statusCode": resp.status_code,
        "responseBody": safe_text(resp),
        "duration": duration
    }

    logger.info(json.dumps(log, ensure_ascii=False))


# ========= 日志清理 =========
def cleanup_logs():
    now = time.time()
    for f in os.listdir("."):
        if f.startswith(LOG_FILE):
            path = os.path.join(".", f)
            age = (now - os.stat(path).st_mtime) / 86400
            if age > RETENTION_DAYS:
                try:
                    os.remove(path)
                except:
                    pass


def load(loader):
    cleanup_logs()