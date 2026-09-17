"""Nacos SB-01 初始化：将 admin 密码从默认 nacos/nacos 改为 docker/.env 的 NACOS_PASSWORD。

背景：NACOS_AUTH_ENABLE=true 只开启鉴权，admin 初始密码仍为 nacos/nacos；
业务服务客户端使用 .env 的 NACOS_USER/NACOS_PASSWORD 登录，若未初始化会报
"NacosException: user not found!"（W1 实测教训）。容器重建（down -v）后需重跑本脚本。

用法：python scripts/init-nacos-admin.py
"""
import re
import urllib.parse
import urllib.request

ENV = r"C:/Users/27382/Desktop/campus-mutual-platform/docker/.env"
NACOS = "http://127.0.0.1:8848"


def env_value(key: str) -> str:
    m = re.search(key + r"=(.+)", open(ENV, encoding="utf-8").read())
    return m.group(1).strip()


def login(user: str, pwd: str) -> str:
    data = urllib.parse.urlencode({"username": user, "password": pwd}).encode()
    r = urllib.request.urlopen(NACOS + "/nacos/v1/auth/login", data=data, timeout=5)
    return str(json.loads(r.read())["accessToken"])


import json

new_pwd = env_value("NACOS_PASSWORD")
try:
    token = login("nacos", new_pwd)
    print("admin password already set, nothing to do")
except urllib.error.HTTPError:
    token = login("nacos", "nacos")
    body = urllib.parse.urlencode(
        {"username": "nacos", "newPassword": new_pwd, "accessToken": token}).encode()
    req = urllib.request.Request(
        NACOS + "/nacos/v1/auth/users?accessToken=" + token, data=body, method="PUT")
    r = urllib.request.urlopen(req, timeout=5)
    print("password updated:", r.read().decode()[:60])

login("nacos", new_pwd)
print("verify with new password: OK")
