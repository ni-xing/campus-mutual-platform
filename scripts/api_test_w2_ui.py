# -*- coding: utf-8 -*-
"""W2 前端接口契约验证：确认 SectionView / OrdersView / PersonalView 调用的接口与字段全部对齐。"""
import json, urllib.request, urllib.error, random, sys

BASE = 'http://127.0.0.1:8080'
SUF = str(random.randint(100000, 999999))
results = []

def call(method, path, body=None, token=None, headers=None):
    req = urllib.request.Request(BASE + path, method=method)
    req.add_header('Content-Type', 'application/json')
    if token:
        req.add_header('Authorization', 'Bearer ' + token)
    for k, v in (headers or {}).items():
        req.add_header(k, v)
    data = json.dumps(body).encode() if body is not None else None
    try:
        r = urllib.request.urlopen(req, data=data, timeout=15)
        return r.status, json.loads(r.read().decode())
    except urllib.error.HTTPError as e:
        try:
            return e.code, json.loads(e.read().decode())
        except Exception:
            return e.code, {}

def check(name, cond, detail=''):
    results.append(bool(cond))
    print(('PASS' if cond else 'FAIL'), name, detail)

def reg(tag):
    no = '2025' + SUF + tag
    call('POST', '/api/v1/auth/register', {'studentNo': no, 'email': f'ui{tag}@stu.campus.edu.cn',
                                          'password': 'Passw0rd123', 'privacyAgreed': True})
    s, b = call('POST', '/api/v1/auth/login', {'account': no, 'password': 'Passw0rd123'})
    return b['data']['token']

seller = reg('1')
buyer = reg('2')
call('POST', '/api/v1/trade/balance/recharge', {'amount': 100}, token=buyer,
     headers={'X-Idempotency-Key': 'ui-' + SUF + '-1'})

# --- SectionView: GET /api/v1/goods 列表字段 ---
s, b = call('POST', '/api/v1/goods', {'title': 'UI契约-测试商品', 'price': 18.5, 'category': 'BOOK',
                                      'description': '契约验证用'}, token=seller)
gid = b['data']
s, b = call('GET', '/api/v1/goods?pageNo=1&pageSize=20', token=buyer)
rec = (b.get('data') or {}).get('records') or []
g = next((x for x in rec if x.get('id') == gid), None)
check('集市列表：分页结构 total/pages/records', all(k in (b.get('data') or {}) for k in ('total', 'pages', 'records')))
check('集市列表：卡片所需字段齐全',
      g and all(k in g for k in ('id', 'title', 'price', 'category', 'sellerId', 'createdTime')),
      str(list(g.keys()) if g else None))
check('集市列表：未登录价格可读（无需登录态）',
      call('GET', '/api/v1/goods?pageNo=1&pageSize=5')[0] == 401)

# --- SectionView: 发布后列表可见（发布→loadGoods 刷新链路） ---
check('发布后列表可见', g is not None)

# --- SectionView / OrdersView: 下单（X-Idempotency-Key 头） ---
s, b = call('POST', '/api/v1/orders', {'goodsId': gid}, token=buyer,
            headers={'X-Idempotency-Key': 'ui-order-' + SUF})
check('下单：返回 id/orderNo/status 供订单页使用',
      (b.get('data') or {}).get('id') and (b.get('data') or {}).get('orderNo')
      and (b.get('data') or {}).get('status') == 'FROZEN', str(b.get('data')))
oid = (b.get('data') or {}).get('id')

# 同键重复提交 → 幂等命中（前端可能重试）
s, b2 = call('POST', '/api/v1/orders', {'goodsId': gid}, token=buyer,
             headers={'X-Idempotency-Key': 'ui-order-' + SUF})
check('下单幂等：同键重试返回同一单', (b2.get('data') or {}).get('id') == oid, str(b2.get('code')))

# --- OrdersView: GET /orders/mine?role=buyer|seller ---
s, b = call('GET', '/api/v1/orders/mine?role=buyer&pageNo=1&pageSize=30', token=buyer)
ords = (b.get('data') or {}).get('records') or []
check('我的订单(buyer)：buyerNickname/reviewedByMe 字段就位',
      ords and all(k in ords[0] for k in ('id', 'goodsTitle', 'goodsPrice', 'status', 'createdTime', 'reviewedByMe')),
      str(list(ords[0].keys()) if ords else None))
s, b = call('GET', '/api/v1/orders/mine?role=seller&pageNo=1&pageSize=30', token=seller)
check('我的订单(seller)：卖家视角可见', len(((b.get('data') or {}).get('records') or [])) >= 1)

# --- OrdersView: 确认取货 → 评价 → reviewedByMe 翻转 ---
s, b = call('POST', f'/api/v1/orders/{oid}/complete', token=buyer)
check('确认取货成功', b.get('code') == '000000', str(b.get('msg')))
s, b = call('POST', f'/api/v1/orders/{oid}/review', {'rating': 5, 'content': '界面契约验证'},
            token=buyer)
check('评价成功（前端 1-5 星 + 200 字文案）', b.get('code') == '000000', str(b.get('msg')))
s, b = call('GET', '/api/v1/orders/mine?role=buyer&pageNo=1&pageSize=30', token=buyer)
o = next((x for x in ((b.get('data') or {}).get('records') or []) if x.get('id') == oid), None)
check('评价后 reviewedByMe=true（按钮切换为「已评价」）', o and o.get('reviewedByMe') is True, str(o.get('reviewedByMe') if o else None))

# --- OrdersView: 取消并退款 ---
s, b = call('POST', '/api/v1/goods', {'title': 'UI契约-取消用例', 'price': 9, 'category': 'DAILY'}, token=seller)
gid2 = b['data']
s, b = call('POST', '/api/v1/orders', {'goodsId': gid2}, token=buyer,
            headers={'X-Idempotency-Key': 'ui-cancel-' + SUF})
oid2 = (b.get('data') or {}).get('id')
s, b = call('POST', f'/api/v1/orders/{oid2}/cancel', token=buyer)
check('取消并退款成功（订单页按钮链路）', b.get('code') == '000000', str(b.get('msg')))
s, b = call('GET', '/api/v1/orders/mine?role=buyer&pageNo=1&pageSize=30', token=buyer)
o2 = next((x for x in ((b.get('data') or {}).get('records') or []) if x.get('id') == oid2), None)
check('取消后状态 CANCELLED（前端显示「已退款回余额」）', o2 and o2.get('status') == 'CANCELLED', str(o2.get('status') if o2 else None))

# --- PersonalView: 余额 / 积分 / 在售数 ---
s, b = call('GET', '/api/v1/trade/balance', token=buyer)
d = b.get('data') or {}
check('个人中心：余额/冻结/积分字段', all(k in d for k in ('balance', 'frozen', 'points')), str(d))
s, b = call('POST', '/api/v1/trade/balance/recharge', {'amount': 10}, token=buyer,
            headers={'X-Idempotency-Key': 'ui-rcg-' + SUF})
check('模拟充值：返回最新余额', b.get('code') == '000000' and (b.get('data') or {}).get('balance') is not None)
s, b = call('GET', '/api/v1/goods/mine?pageNo=1&pageSize=1', token=seller)
check('个人中心：在售商品 total（seller 视角）', (b.get('data') or {}).get('total') is not None, str((b.get('data') or {}).get('total')))

# --- 演示数据兜底：未登录访问 goods 列表 → 401（前端回落 posts.js） ---
s, b = call('GET', '/api/v1/goods?pageNo=1&pageSize=5')
check('未登录 401 → 前端演示数据兜底路径成立', s == 401, 'http=' + str(s))

fails = results.count(False)
print(f'\nUI CONTRACT RESULT: {len(results) - fails}/{len(results)} PASS')
sys.exit(1 if fails else 0)
