import json
import time
import gzip
import logging
import os
import shutil
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


# ========= 自定义 handler（核心） =========
class GzipRotatingFileHandler(RotatingFileHandler):

    def doRollover(self):
        """
        1. 关闭当前文件
        2. 文件切分
        3. 压缩旧文件
        """

        if self.stream:
            self.stream.close()

        # 1️⃣ 删除最老的
        if self.backupCount > 0:
            oldest = f"{self.baseFilename}.{self.backupCount}.gz"
            if os.path.exists(oldest):
                os.remove(oldest)

        # 2️⃣ 往后移动 .gz 文件
        for i in range(self.backupCount - 1, 0, -1):
            src = f"{self.baseFilename}.{i}.gz"
            dst = f"{self.baseFilename}.{i+1}.gz"

            if os.path.exists(src):
                os.rename(src, dst)

        # 3️⃣ 压缩最新的 .1
        if os.path.exists(self.baseFilename):
            with open(self.baseFilename, "rb") as f_in:
                with gzip.open(f"{self.baseFilename}.1.gz", "wb") as f_out:
                    shutil.copyfileobj(f_in, f_out)

            os.remove(self.baseFilename)

        # 4️⃣ 重新打开日志文件
        self.mode = "a"
        self.stream = self._open()


# ========= logger =========
def get_logger(log_file):
    if log_file in LOGGER_MAP:
        return LOGGER_MAP[log_file]

    logger = logging.getLogger(log_file)
    logger.setLevel(logging.INFO)

    handler = GzipRotatingFileHandler(
        log_file,
        maxBytes=MAX_LOG_SIZE,
        backupCount=50,
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
    flow.metadata["start"] = time.time()


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

        duration = int((time.time() - flow.metadata.get("start", time.time())) * 1000)

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