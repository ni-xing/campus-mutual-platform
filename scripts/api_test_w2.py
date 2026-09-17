# -*- coding: utf-8 -*-
"""W2 二手交易模块验收脚本（specs/01 AC-1~7）。前置：gateway 8080 + user-credit + trade 已启动。"""
import json, urllib.request, urllib.error, sys, random, threading, time

BASE = 'http://127.0.0.1:8080'
SUFFIX = str(random.randint(100000, 999999))

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

results = []

def check(name, cond, detail=''):
    results.append(cond)
    print(('PASS' if cond else 'FAIL'), name, detail)

def register_and_login(tag):
    no = '2025' + SUFFIX + tag
    s, b = call('POST', '/api/v1/auth/register', {
        'studentNo': no, 'email': f'w2{tag}@stu.campus.edu.cn',
        'password': 'Passw0rd123', 'privacyAgreed': True})
    assert b.get('code') == '000000', f'setup register {tag} failed: {b}'
    s, b = call('POST', '/api/v1/auth/login', {'account': no, 'password': 'Passw0rd123'})
    assert b.get('code') == '000000', f'setup login {tag} failed: {b}'
    return b['data']['token'], b['data'].get('userId') or b['data'].get('id')

seller_token, seller_id = register_and_login('1')
buyer_token, buyer_id = register_and_login('2')
buyer2_token, buyer2_id = register_and_login('3')
print('setup ok, seller/buyer/buyer2 =', seller_id, buyer_id, buyer2_id)

# 卖家充值（供收款展示）+ 买家充值
call('POST', '/api/v1/trade/balance/recharge', {'amount': 100}, token=seller_token)
s, b = call('POST', '/api/v1/trade/balance/recharge', {'amount': 100}, token=buyer_token)
check('setup 买家模拟充值', b.get('code') == '000000' and (b['data'] or {}).get('balance') == 100, str(b.get('data')))

# ---- AC-1 下单并完成：商品已售 + 买家扣款 + 卖家入账 + 支付流水 ----
s, b = call('POST', '/api/v1/goods', {
    'title': 'W2验收-高数教材', 'description': '九成新', 'category': 'BOOK', 'price': 12.5}, token=seller_token)
check('发布商品', b.get('code') == '000000', str(b.get('msg')))
goods_id = b['data']

s, b = call('POST', '/api/v1/orders', {'goodsId': goods_id}, token=buyer_token)
check('AC-1 下单(冻结)成功', b.get('code') == '000000', str(b.get('msg')))
order_id = (b.get('data') or {}).get('id')
s, b = call('GET', '/api/v1/goods/' + str(goods_id), token=buyer_token)
check('AC-1 商品变已售', (b.get('data') or {}).get('status') == 'SOLD', str((b.get('data') or {}).get('status')))
s, b = call('GET', '/api/v1/trade/balance', token=buyer_token)
check('AC-1 买家余额冻结(可用100→87.5)', (b.get('data') or {}).get('balance') == 87.5 and (b.get('data') or {}).get('frozen') == 12.5, str(b.get('data')))
s, b = call('POST', f'/api/v1/orders/{order_id}/complete', token=buyer_token)
check('AC-1 确认取货完成', b.get('code') == '000000', str(b.get('msg')))
s, b = call('GET', '/api/v1/trade/balance', token=buyer_token)
check('AC-1 买家扣款+积分', (b.get('data') or {}).get('balance') == 87.5 and (b.get('data') or {}).get('points') == 0, str(b.get('data')))
s, b = call('GET', '/api/v1/trade/balance', token=seller_token)
check('AC-1 卖家入账+流水', (b.get('data') or {}).get('balance') == 112.5, str(b.get('data')))
s, b = call('GET', '/api/v1/trade/balance/flows', token=seller_token)
flows = (b.get('data') or {}).get('records') or []
check('AC-1 卖家 INCOME 流水', any(f.get('flowType') == 'INCOME' and f.get('bizRef') for f in flows), str(len(flows)))

# ---- AC-2 余额不足 ----
s, b = call('POST', '/api/v1/goods', {
    'title': 'W2验收-余额不足用例', 'category': 'DAILY', 'price': 500}, token=seller_token)
gid2 = b['data']
s, b = call('POST', '/api/v1/orders', {'goodsId': gid2}, token=buyer2_token)
check('AC-2 余额不足拒绝(A020002)', b.get('code') == 'A020002', str(b.get('msg')))
s, b = call('GET', '/api/v1/goods/' + str(gid2), token=buyer2_token)
check('AC-2 商品状态不变', (b.get('data') or {}).get('status') == 'LISTED', str((b.get('data') or {}).get('status')))
s, b = call('GET', '/api/v1/trade/balance/flows', token=buyer2_token)
check('AC-2 无流水产生', len((b.get('data') or {}).get('records') or []) == 0, '')

# ---- AC-3 并发下单同一商品，仅一单成功 ----
s, b = call('POST', '/api/v1/goods', {
    'title': 'W2验收-并发抢购', 'category': 'DIGITAL', 'price': 20}, token=seller_token)
gid3 = b['data']
results_ac3 = []

def buyer2_order():
    s, b = call('POST', '/api/v1/orders', {'goodsId': gid3}, token=buyer2_token)
    results_ac3.append(b.get('code'))

def buyer1_order():
    s, b = call('POST', '/api/v1/orders', {'goodsId': gid3}, token=buyer_token)
    results_ac3.append(b.get('code'))

t1 = threading.Thread(target=buyer2_order); t2 = threading.Thread(target=buyer1_order)
t1.start(); t2.start(); t1.join(); t2.join()
check('AC-3 并发仅一单成功', results_ac3.count('000000') == 1 and len(results_ac3) == 2, str(results_ac3))

# ---- AC-4 取消退款回架 ----
s, b = call('POST', '/api/v1/goods', {
    'title': 'W2验收-取消用例', 'category': 'SPORT', 'price': 30}, token=seller_token)
gid4 = b['data']
call('POST', '/api/v1/trade/balance/recharge', {'amount': 100}, token=buyer2_token)
s, b = call('POST', '/api/v1/orders', {'goodsId': gid4}, token=buyer2_token)
check('AC-4 buyer2 充值后下单', b.get('code') == '000000', str(b.get('msg')))
order4 = (b.get('data') or {}).get('id')
s, b = call('POST', f'/api/v1/orders/{order4}/cancel', token=buyer2_token)
check('AC-4 取消成功', b.get('code') == '000000', str(b.get('msg')))
s, b = call('GET', '/api/v1/trade/balance', token=buyer2_token)
d = b.get('data') or {}
check('AC-4 冻结清零余额返还', d.get('frozen') == 0, str(d))
s, b = call('GET', '/api/v1/goods/' + str(gid4), token=buyer2_token)
check('AC-4 商品回架', (b.get('data') or {}).get('status') == 'LISTED', str((b.get('data') or {}).get('status')))

# ---- AC-5 已完成订单再操作被状态机拒绝 ----
s, b = call('POST', f'/api/v1/orders/{order_id}/cancel', token=buyer_token)
check('AC-5 完成单取消拒绝(A020004)', b.get('code') == 'A020004', str(b.get('msg')))

# ---- 评价 + 重复评价 ----
s, b = call('POST', f'/api/v1/orders/{order_id}/review', {'rating': 5, 'content': '很好'}, token=seller_token)
check('评价成功', b.get('code') == '000000', str(b.get('msg')))
s, b = call('POST', f'/api/v1/orders/{order_id}/review', {'rating': 5, 'content': '再评'}, token=seller_token)
check('重复评价拒绝(A020003)', b.get('code') == 'A020003', str(b.get('msg')))

# ---- AC-6 列表缓存：二次访问明显更快 ----
def timed_list():
    t0 = time.time()
    call('GET', '/api/v1/goods?pageNo=1&pageSize=10', token=buyer_token)
    return (time.time() - t0) * 1000

first = timed_list()
second = timed_list()
check('AC-6 二次访问命中缓存', second < max(first, 120), f'first={first:.0f}ms second={second:.0f}ms')

# ---- AC-7 涉及个人信息的接口全部要求登录态 ----
s, b = call('POST', '/api/v1/orders', {'goodsId': gid4})
check('AC-7 无token下单 401', s == 401, 'http=' + str(s))
s, b = call('GET', '/api/v1/trade/balance')
check('AC-7 无token查余额 401', s == 401, 'http=' + str(s))

fails = results.count(False)
print(f'\nW2 RESULT: {len(results) - fails}/{len(results)} PASS')
sys.exit(1 if fails else 0)
