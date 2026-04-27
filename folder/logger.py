import json
import time
import logging
import os
from logging.handlers import RotatingFileHandler

from mitmproxy import http

# ========= 加载配置 =========
with open("logConfig.json") as f:
    CONFIG = json.load(f)

RULES = CONFIG["rules"]
MAX_LOG_SIZE = CONFIG["maxLogSizeMB"] * 1024 * 1024
RETENTION_DAYS = CONFIG["retentionDays"]
MAX_REQUEST_BODY_SIZE = CONFIG["maxRequestBodyKB"] * 1024
MAX_RESPONSE_BODY_SIZE = CONFIG["maxResponseBodyKB"] * 1024

# ========= logger 缓存 =========
LOGGER_MAP = {}


def get_logger(log_file):
    if log_file in LOGGER_MAP:
        return LOGGER_MAP[log_file]

    logger = logging.getLogger(log_file)
    logger.setLevel(logging.INFO)

    handler = RotatingFileHandler(
        log_file,
        maxBytes=MAX_LOG_SIZE,
        backupCount=20,
        encoding="utf-8"
    )

    logger.addHandler(handler)
    logger.propagate = False

    LOGGER_MAP[log_file] = logger
    return logger


# ========= 匹配 =========
def match_rule(req: http.Request, rule):
    prefix = rule["prefix"].lower()

    target = f"{req.host}{req.path}".lower()

    return target.startswith(prefix)


# ========= 工具 =========
def is_multipart(req: http.Request):
    content_type = req.headers.get("Content-Type", "")
    return "multipart/form-data" in content_type.lower()


def too_large(flow: http.HTTPFlow):
    req_len = len(flow.request.raw_content or b"")
    resp_len = len(flow.response.raw_content or b"")
    return req_len > MAX_REQUEST_BODY_SIZE or resp_len > MAX_RESPONSE_BODY_SIZE


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

    for rule in RULES:
        if not match_rule(req, rule):
            continue

        # 过滤
        if is_multipart(req):
            return

        if too_large(flow):
            return

        logger = get_logger(rule["logFile"])

        duration = int((time.time() - flow.metadata.get("start_time", time.time())) * 1000)

        log = {
            "method": req.method,
            "url": req.pretty_url,
            "headers": {
                "x-meta-token": req.headers.get("x-meta-token", "")
            },
            "queryString": dict(req.query),
            "requestBody": safe_text(req),
            "statusCode": resp.status_code,
            "responseBody": safe_text(resp),
            "duration": duration
        }

        logger.info(json.dumps(log, ensure_ascii=False))

        # ✅ 命中一条规则就结束（避免重复写多个文件）
        return