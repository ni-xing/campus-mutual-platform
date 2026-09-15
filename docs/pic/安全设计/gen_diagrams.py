# -*- coding: utf-8 -*-
"""安全设计配图生成：信任边界图 / 敏感数据流图 / 威胁-缓解映射图（diagrams-generator，matplotlib 快速替代方案）"""
import os
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from matplotlib.patches import FancyBboxPatch, FancyArrowPatch
from matplotlib.lines import Line2D

plt.rcParams["font.sans-serif"] = ["Microsoft YaHei", "SimHei"]
plt.rcParams["axes.unicode_minus"] = False

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)))
os.makedirs(OUT, exist_ok=True)


def box(ax, x, y, w, h, text, fc="#EAF2FB", ec="#4A6785", fs=9, bold=False, lw=1.2, radius=0.02):
    p = FancyBboxPatch((x, y), w, h,
                       boxstyle="round,pad=0.004,rounding_size=" + str(radius),
                       facecolor=fc, edgecolor=ec, linewidth=lw, zorder=2)
    ax.add_patch(p)
    ax.text(x + w / 2, y + h / 2, text, ha="center", va="center", fontsize=fs,
            fontweight="bold" if bold else "normal", color="#1A2733", zorder=3, linespacing=1.5)
    return (x, y, w, h)


def arrow(ax, p1, p2, label="", color="#4A6785", fs=8, style="-|>", ls="-", lw=1.4, label_dy=0.012, label_pos=None):
    a = FancyArrowPatch(p1, p2, arrowstyle=style, mutation_scale=14,
                        color=color, linewidth=lw, linestyle=ls, zorder=4)
    ax.add_patch(a)
    if label:
        if label_pos is not None:
            mx, my = label_pos
        else:
            mx, my = (p1[0] + p2[0]) / 2, (p1[1] + p2[1]) / 2 + label_dy
        ax.text(mx, my, label, ha="center", va="bottom", fontsize=fs, color=color, zorder=5)


# ============================================================
# 图 1：信任边界图
# ============================================================
def fig_trust_boundary():
    fig, ax = plt.subplots(figsize=(12, 8.4))
    ax.set_xlim(0, 12); ax.set_ylim(0, 10); ax.axis("off")

    # 信任域 0：公网不可信域
    box(ax, 0.3, 8.6, 11.4, 1.1, "信任域 T0  公网不可信域\n学生 / 管理员浏览器  ·  互联网攻击者",
        fc="#FDECEA", ec="#C0392B", fs=10, bold=True)
    # 边界线：云安全组 + DDoS
    box(ax, 0.3, 7.5, 11.4, 0.8, "南北向边界：云厂商基础 DDoS 防护（免费档） ＋ 云安全组（仅入 443 / 80 / 22 限源，默认拒绝）",
        fc="#FFF7E6", ec="#B7791F", fs=9.5, bold=True)
    # 信任域 1：边界接入域
    box(ax, 0.3, 6.3, 6.2, 0.9, "信任域 T1  边界接入域（DMZ 等价）\nNginx：TLS 1.2+ 终结 · 80 跳 443 · 静态资源 / 图片",
        fc="#E8F6EF", ec="#1E8449", fs=9.5, bold=True)
    box(ax, 6.8, 6.3, 4.9, 0.9, "信任域 T4  运维管理域（DevOps 等价）\nSSH 22 限源 IP · 密钥登录 · 禁 root 密码",
        fc="#F4ECF7", ec="#7D3C98", fs=9.5, bold=True)
    # 信任域 2+3：Docker Bridge
    box(ax, 0.3, 1.7, 11.4, 4.2, "", fc="#F7FAFD", ec="#2C5F8A", lw=1.8)
    ax.text(0.55, 5.55, "信任域 T2/T3  Docker Bridge 内部域（业务 VPC 等价，东西向默认隔离）",
            fontsize=10, fontweight="bold", color="#2C5F8A")
    box(ax, 0.6, 4.4, 2.2, 0.8, "Spring Cloud Gateway\nJWT 鉴权 · 限流 · 路由", fc="#D6EAF8", fs=9)
    svc = ["M1 用户信用", "M2 二手交易", "M3 失物招领", "M4 跑腿拼单", "M5 AI 助手", "M6 通知", "M7 平台管理"]
    x0 = 3.1
    for i, s in enumerate(svc):
        box(ax, x0 + i * 1.22, 4.4, 1.14, 0.8, s, fc="#EAF2FB", fs=7.5)
    box(ax, 0.6, 3.3, 3.6, 0.7, "Nacos（鉴权开启）· Sentinel Dashboard（口令保护）", fc="#EAF2FB", fs=8)
    box(ax, 4.4, 3.3, 7.0, 0.7, "应用服务端口一律不映射宿主机公网（仅 Nginx / Gateway 出口）", fc="#EAF2FB", fs=8)
    box(ax, 0.6, 1.95, 5.2, 1.05, "信任域 T3  数据域\nMySQL 8（7 schema · 专用账号按库授权）· Redis 7（requirepass）",
        fc="#FEF9E7", ec="#9A7D0A", fs=9, bold=True)
    box(ax, 6.1, 1.95, 5.3, 1.05, "宿主机管理面\nDocker 引擎（非 root 容器 · 无特权 · 不挂载 docker.sock）",
        fc="#FEF9E7", ec="#9A7D0A", fs=9, bold=True)
    # 外部依赖 / 备份
    box(ax, 0.3, 0.25, 5.4, 1.0, "外部上游 E-01  百炼 DashScope（qwen-plus）\n出站 HTTPS · 域名白名单 dashscope.aliyuncs.com",
        fc="#FDECEA", ec="#C0392B", fs=9)
    box(ax, 6.1, 0.25, 5.6, 1.0, "信任域 T5  备份域（隔离）\n云对象存储：私有桶 · SSE 加密 · 子账号 AKSK 仅写不删",
        fc="#E8F6EF", ec="#1E8449", fs=9)
    # 连线
    arrow(ax, (4.0, 8.6), (3.0, 7.2), "HTTPS 443", color="#C0392B", label_pos=(1.7, 7.22))
    arrow(ax, (8.5, 8.6), (8.5, 7.2), "SSH 22（限源）", color="#7D3C98", label_pos=(9.9, 7.22))
    arrow(ax, (3.0, 6.3), (2.4, 5.2), "HTTP（域内）", color="#1E8449", label_pos=(1.25, 5.6))
    # 运维通道：从 T4 右缘外侧绕行至宿主机管理面，避免穿越业务服务行
    ax.add_line(Line2D([11.95, 11.95], [6.75, 2.6], color="#7D3C98", linewidth=1.4, linestyle="-", zorder=4))
    arrow(ax, (11.95, 2.6), (11.45, 2.5), "运维通道（密钥）", color="#7D3C98", label_pos=(11.5, 2.62))
    arrow(ax, (2.0, 4.4), (2.0, 3.0), "", color="#9A7D0A")
    arrow(ax, (5.0, 1.95), (4.0, 1.25), "出站 443（白名单）", color="#C0392B", label_pos=(3.1, 1.5))
    arrow(ax, (8.0, 1.95), (8.5, 1.25), "每日备份（仅写）", color="#1E8449", ls="--", label_pos=(9.6, 1.5))
    ax.set_title("校园互助生活平台 · 信任边界图（单机 Docker Compose 形态，企业级分区的单机等价映射）",
                 fontsize=12, fontweight="bold", pad=12)
    fig.tight_layout()
    fig.savefig(os.path.join(OUT, "trust_boundary.png"), dpi=150, bbox_inches="tight", facecolor="white")
    plt.close(fig)


# ============================================================
# 图 2：敏感数据流图
# ============================================================
def fig_sensitive_flow():
    fig, ax = plt.subplots(figsize=(12.6, 8.0))
    ax.set_xlim(0, 13); ax.set_ylim(0, 10); ax.axis("off")

    box(ax, 0.3, 8.4, 2.4, 1.0, "学生 / 管理员\n（浏览器）", fc="#FDECEA", ec="#C0392B", fs=10, bold=True)
    box(ax, 3.4, 8.4, 2.4, 1.0, "Vue 3 前端\nHTTPS · Token 不进 URL", fc="#EAF2FB", ec="#4A6785", fs=9)
    box(ax, 6.5, 8.4, 2.6, 1.0, "Nginx TLS 终结\n→ Gateway JWT 校验", fc="#E8F6EF", ec="#1E8449", fs=9)

    box(ax, 1.0, 5.9, 3.4, 1.5, "M1 用户与信用服务\n注册：学号 / 校园邮箱 / 密码\n密码 BCrypt cost 10 后落库", fc="#EAF2FB", ec="#4A6785", fs=9)
    box(ax, 5.2, 5.9, 3.4, 1.5, "M2 / M3 / M4 业务服务\n余额流水 · 认领联系提示\n服务端归属校验后入库", fc="#EAF2FB", ec="#4A6785", fs=9)
    box(ax, 9.4, 5.9, 3.2, 1.5, "M5 AI 助手服务\n会话内容 → E-01\n上下文最小化：仅业务必需字段\n不含学号 / 密码 / 密钥", fc="#EAF2FB", ec="#4A6785", fs=8.5)

    box(ax, 1.0, 3.2, 3.4, 1.1, "user_db  t_user_account\nL3 明文（唯一索引）＋ L4 密码摘要", fc="#FEF9E7", ec="#9A7D0A", fs=8.5)
    box(ax, 5.2, 3.2, 3.4, 1.1, "trade_db / lostfound_db / errand_db\nL3 流水与联系提示（脱敏展示）", fc="#FEF9E7", ec="#9A7D0A", fs=8.5)
    box(ax, 9.4, 3.2, 2.8, 1.1, "ai_db  t_ai_session / t_ai_message\nL2 会话内容（180 天清理）", fc="#FEF9E7", ec="#9A7D0A", fs=8.5)

    box(ax, 1.0, 1.2, 3.4, 1.1, "服务器 .env（权限 600）\nL4：MySQL 密码 / JWT secret /\n百炼 API Key / 备份 AKSK", fc="#F4ECF7", ec="#7D3C98", fs=8.5)
    box(ax, 5.2, 1.2, 3.4, 1.1, "云对象存储（备份域 T5）\n私有桶 · SSE · AKSK 仅写不删", fc="#E8F6EF", ec="#1E8449", fs=8.5)
    box(ax, 9.4, 1.2, 3.2, 1.1, "E-01 百炼 DashScope\n出站 HTTPS · 域名白名单\nAPI Key 仅存在于 .env", fc="#FDECEA", ec="#C0392B", fs=8.5)

    arrow(ax, (2.7, 8.4), (2.7, 7.4), "HTTPS（TLS 1.2+）", color="#1E8449", label_dy=0.06)
    arrow(ax, (5.8, 8.4), (5.8, 7.4), "JWT Header（禁 URL）", color="#1E8449", label_dy=0.06)
    arrow(ax, (8.9, 8.4), (2.9, 7.4), "", color="#4A6785")
    arrow(ax, (8.9, 8.4), (7.0, 7.4), "", color="#4A6785")
    arrow(ax, (10.9, 8.4), (11.0, 7.4), "SSE", color="#4A6785")
    arrow(ax, (2.7, 5.9), (2.7, 4.3), "学号 / 邮箱 L3", color="#9A7D0A", label_pos=(1.45, 4.75))
    arrow(ax, (2.7, 4.3), (2.7, 3.2), "密码摘要 L4", color="#7D3C98", label_pos=(0.35, 3.55))
    arrow(ax, (6.9, 5.9), (6.9, 4.3), "流水 / 联系提示 L3", color="#9A7D0A", label_pos=(5.6, 4.75))
    arrow(ax, (11.0, 5.9), (11.0, 4.3), "会话 L2", color="#B7791F", label_pos=(9.85, 4.75))
    arrow(ax, (12.55, 5.9), (11.3, 2.3), "会话上下文（业务必需字段）", color="#C0392B", label_pos=(12.15, 4.6))
    arrow(ax, (4.4, 3.2), (6.4, 2.3), "每日备份（出站）", color="#1E8449", ls="--", label_pos=(5.0, 2.75))
    ax.text(6.5, 9.7, "图例：L1 公开 · L2 内部 · L3 敏感（个人信息/资金流水）· L4 高敏（凭证与密钥）",
            fontsize=9.5, ha="center", color="#555555")
    ax.set_title("校园互助生活平台 · 敏感数据流图（数据分级随流标注）", fontsize=12, fontweight="bold", pad=10)
    fig.tight_layout()
    fig.savefig(os.path.join(OUT, "sensitive_data_flow.png"), dpi=150, bbox_inches="tight", facecolor="white")
    plt.close(fig)


# ============================================================
# 图 3：威胁-缓解映射图
# ============================================================
def fig_threat_map():
    fig, ax = plt.subplots(figsize=(12.6, 8.6))
    ax.set_xlim(0, 13); ax.set_ylim(0, 11); ax.axis("off")

    cats = [
        ("S 仿冒", "#C0392B", "S1 登录爆破撞库\nS2 JWT 伪造/重放\nS3 AI 工具身份冒用\nS4 管理端弱口令"),
        ("T 篡改", "#B7791F", "T1 金额参数改包\nT2 传输中间人\nT3 数据库篡改\nT4 内容 XSS 篡改"),
        ("R 否认", "#7D3C98", "R1 划转否认\nR2 审核处置否认\nR3 AI 代办否认\nR4 登录否认"),
        ("I 信息泄露", "#1F618D", "I1 越权读他人数据\nI2 个人信息外泄\nI3 堆栈泄露内部\nI4 会话传第三方 LLM"),
        ("D 拒绝服务", "#1E8449", "D1 DDoS / CC\nD2 重保接口爆破\nD3 慢查询拖库\nD4 Outbox 积压 / 资源耗尽"),
        ("E 权限提升", "#8E44AD", "E1 水平越权 IDOR\nE2 垂直越权\nE3 internal 接口绕行\nE4 容器逃逸 / Agent 越权"),
    ]
    controls = [
        ("身份与访问（§2）", "失败锁定 · TOTP · JWT 黑名单互踢\nRBAC0 三层鉴权 · 数据归属校验"),
        ("数据安全（§3 / §6）", "TLS 1.2+ · BCrypt · 脱敏展示\n密钥 .env 600 · 轮换 · AKSK 最小权限"),
        ("应用与网络（§4 / §5）", "参数化查询 · 输出编码 · CSP · 限流\n安全组默认拒绝 · 端口不映射 · 容器加固"),
        ("运行时审计（§7）", "五维审计 · 无删除路径\nAL-01~AL-09 告警 · 五类应急预案"),
    ]

    x0 = 0.3
    for i, (title, color, items) in enumerate(cats):
        col_x = x0 + i * 2.08
        box(ax, col_x, 8.9, 1.9, 0.75, title, fc="#FFFFFF", ec=color, fs=11, bold=True, lw=1.8)
        box(ax, col_x, 5.4, 1.9, 3.2, items, fc="#FBFCFE", ec=color, fs=8, lw=1.0)
        # 威胁列 -> 控制域
        arrow(ax, (col_x + 0.95, 5.4), (col_x + 0.95, 4.6), "", color=color, lw=1.1)

    cy = 2.3
    heights = [1.6, 1.6, 1.6, 1.6]
    ys = [cy + 1.8, cy + 0.0, cy - 1.8, cy - 3.6]
    ys = [9.0 - 0.0, 0, 0, 0]
    # 四个控制域横向排布（自上而下）
    ctrl_titles = [c[0] for c in controls]
    ctrl_texts = [c[1] for c in controls]
    colors4 = ["#C0392B", "#B7791F", "#1F618D", "#1E8449"]
    band_y = [4.0, 3.0, 2.0, 1.0]
    for i in range(4):
        box(ax, 0.5, band_y[i], 12.0, 0.85,
            ctrl_titles[i] + "　——　" + ctrl_texts[i].replace("\n", " ｜ "),
            fc="#F7FAFD", ec=colors4[i], fs=9, bold=False)
        # 每列威胁连到四个控制带
        for j in range(6):
            col_x = x0 + j * 2.08 + 0.95
            ax.add_line(Line2D([col_x, col_x], [4.6 if i == 0 else band_y[i - 1] if False else 0, 0],
                               color="none"))
    ax.set_title("校园互助生活平台 · STRIDE 威胁—缓解措施映射图（威胁全量明细见 §1.2 映射表）",
                 fontsize=12, fontweight="bold", pad=12)
    fig.tight_layout()
    fig.savefig(os.path.join(OUT, "threat_mitigation_map.png"), dpi=150, bbox_inches="tight", facecolor="white")
    plt.close(fig)


fig_trust_boundary()
fig_sensitive_flow()
fig_threat_map()
print("done")
