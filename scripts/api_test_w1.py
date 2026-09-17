import json, urllib.request, urllib.error, sys, random

BASE = 'http://127.0.0.1:8080'
# 随机学号：支持重复执行（DB 数据残留时会撞唯一索引，属预期）
NO = str(random.randint(2025000000, 2025999999))[:10]

def call(method, path, body=None, token=None):
    req = urllib.request.Request(BASE + path, method=method)
    req.add_header('Content-Type', 'application/json')
    if token:
        req.add_header('Authorization', 'Bearer ' + token)
    data = json.dumps(body).encode() if body is not None else None
    try:
        r = urllib.request.urlopen(req, data=data, timeout=10)
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

# 1 AC-1 正常注册（含隐私同意）
s, b = call('POST', '/api/v1/auth/register', {'studentNo': NO, 'email': 'test@stu.campus.edu.cn', 'password': 'Passw0rd123', 'privacyAgreed': True})
check('AC-1 注册成功', s == 200 and b.get('code') == '000000', 'code=' + str(b.get('code')))
token1 = (b.get('data') or {}).get('token', '')

# 2 AC-4 学号重复
s, b = call('POST', '/api/v1/auth/register', {'studentNo': NO, 'email': 'test@stu.campus.edu.cn', 'password': 'Passw0rd123', 'privacyAgreed': True})
check('AC-4 学号重复拒绝', b.get('code') == 'A010001', 'code=' + str(b.get('code')))

# 3 AC-2 非校园邮箱
s, b = call('POST', '/api/v1/auth/register', {'studentNo': str(int(NO)+1), 'email': 'test@qq.com', 'password': 'Passw0rd123', 'privacyAgreed': True})
check('AC-2 非校园邮箱拒绝', b.get('code') == 'C000001' and '校园' in b.get('msg', ''), 'msg=' + str(b.get('msg')))

# 4 AC-3 纯数字密码
s, b = call('POST', '/api/v1/auth/register', {'studentNo': str(int(NO)+2), 'email': 't3@stu.campus.edu.cn', 'password': '12345678', 'privacyAgreed': True})
check('AC-3 纯数字密码拒绝', b.get('code') == 'C000001', 'msg=' + str(b.get('msg')))

# 5 AC-8 未勾选隐私同意
s, b = call('POST', '/api/v1/auth/register', {'studentNo': str(int(NO)+3), 'email': 't4@stu.campus.edu.cn', 'password': 'Passw0rd123', 'privacyAgreed': False})
check('AC-8 未勾选同意拒绝', b.get('code') == 'C000001' and '同意' in b.get('msg', ''), 'msg=' + str(b.get('msg')))

# 6 AC-9 无 token 401
s, b = call('GET', '/api/v1/users/me')
check('AC-9 无token 401', s == 401, 'http=' + str(s))

# 7 AC-9 /internal 404
s, b = call('GET', '/internal/credit/1')
check('AC-9 /internal 404', s == 404, 'http=' + str(s))

# 8 主页（含脱敏断言 AC-12）
s, b = call('GET', '/api/v1/users/me', token=token1)
d = b.get('data') or {}
check('主页返回+学号脱敏', s == 200 and b.get('code') == '000000' and d.get('studentNoMasked') == NO[:4] + '****' + NO[-2:], 'masked=' + str(d.get('studentNoMasked')))

# 9 AC-5 错误密码防枚举
s, b = call('POST', '/api/v1/auth/login', {'account': NO, 'password': 'WrongPass123'})
check('AC-5 错误密码防枚举', b.get('code') == 'C000005' and b.get('msg') == '账号或密码错误', 'msg=' + str(b.get('msg')))

# 10 AC-7 互踢
s, b = call('POST', '/api/v1/auth/login', {'account': NO, 'password': 'Passw0rd123'})
token2 = (b.get('data') or {}).get('token', '')
s, b = call('GET', '/api/v1/users/me', token=token1)
check('AC-7 互踢旧token 401', s == 401, 'http=' + str(s))
s, b = call('GET', '/api/v1/users/me', token=token2)
check('新token可用', s == 200, 'http=' + str(s))

# 11 AC-6 登出
call('POST', '/api/v1/auth/logout', token=token2)
s, b = call('GET', '/api/v1/users/me', token=token2)
check('AC-6 登出后 401', s == 401, 'http=' + str(s))

# 12 内部信用回流（直连服务网络内验证）
print('\n--- internal credit adjust (via user-credit container) ---')

fails = results.count(False)
print(f'\nAPI RESULT: {len(results) - fails}/{len(results)} PASS')
sys.exit(1 if fails else 0)
