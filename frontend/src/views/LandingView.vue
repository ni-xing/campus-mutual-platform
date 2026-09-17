<template>
  <div class="landing">
    <!-- Hero -->
    <section class="wrap hero">
      <div>
        <h1>宿舍楼下的事，<br>就在<span class="hand-note">楼下</span>解决。</h1>
        <p class="lede">闲置流转、失物找回、拼车拼单——把需求贴上布告栏，等对面楼的人搭把手。不用加好友，不用拉群，留言即达。</p>
        <div class="hero-cta">
          <router-link class="btn hero-btn" to="/login?mode=register">发布第一条布告</router-link>
          <router-link class="btn hero-btn yellow" to="/market">先逛逛集市</router-link>
        </div>
      </div>
      <div class="board" aria-label="布告栏示例">
        <article class="note n1">
          <span class="tag">二手集市</span>
          <h3>《数据结构与算法》第九版</h3>
          <p class="price">¥15</p>
          <p class="meta">9成新 · 3号楼自取 · 可小刀</p>
        </article>
        <article class="note n2">
          <span class="stamp">急</span>
          <span class="tag">失物招领</span>
          <h3>捡到一张校园卡</h3>
          <p class="meta">二食堂门口 · 卡尾号 2107 · 认领请留言</p>
        </article>
        <article class="note n3">
          <span class="tag">跑腿拼单</span>
          <h3>周六 10:00 拼车去高铁站</h3>
          <p class="meta">还差 2 人 · 东门集合 · 人均 ¥18</p>
        </article>
        <article class="note n4">
          <span class="tag">AI 助手</span>
          <h3>帮我找一台二手显示器</h3>
          <p class="meta">预算 200 内 · 24 寸左右 · 已帮你盯着了</p>
        </article>
      </div>
    </section>

    <!-- 今日布告滚动条 -->
    <div class="ticker" aria-label="今日最新布告">
      <div class="ticker-track">
        <span v-for="(t, i) in tickerItems" :key="i">{{ t }}</span>
      </div>
    </div>

    <!-- 四板块 -->
    <section class="wrap sections">
      <h2>一块布告栏，四件事</h2>
      <p class="sub">每件事都是同一套动作：贴上去，等回应，线下交割。平台只管让信息找到人。</p>
      <div class="sec-grid">
        <section id="market" class="sec market">
          <h3>二手集市</h3>
          <p>毕业季甩卖的、换宿舍出不掉的，都贴在这里。按楼栋浏览，下楼就能取。</p>
          <p class="sample">「kindle 3 代，9 成新，4 号楼 502 自取」</p>
          <router-link class="sec-link" to="/market">去逛逛</router-link>
        </section>
        <section id="lost" class="sec lost">
          <span class="stamp-big" aria-hidden="true">急</span>
          <h3>失物招领</h3>
          <p>捡到的贴出来等失主，丢了的挂上去等消息。校园卡、耳机、钥匙，最快的 20 分钟就回家。</p>
          <p class="sample">「一食堂三楼捡到蓝牙耳机，白色充电盒」</p>
          <router-link class="sec-link" to="/lost">找找看</router-link>
        </section>
        <section id="errand" class="sec errand">
          <h3>跑腿拼单</h3>
          <p>顺路带个快递、拼一车去高铁站、凑单点奶茶——谁顺路谁来接，一块钱也是人情。</p>
          <p class="sample">「明早 8 点有谁去南门取快递？帮我捎一个」</p>
          <router-link class="sec-link" to="/errand">喊个人</router-link>
        </section>
        <section id="ai" class="sec ai">
          <h3>AI 助手</h3>
          <p>说不出想要什么？告诉它预算和用途，它帮你盯帖子、比价格、提醒你出手时机。</p>
          <p class="sample">「帮我留意 200 以内的二手显示器，到了叫我」</p>
          <router-link class="sec-link" to="/ai">使唤它</router-link>
        </section>
      </div>
    </section>

    <!-- 三步 -->
    <section class="wrap steps">
      <h2>贴一张布告，只要三步</h2>
      <div class="step-row">
        <div class="step">
          <span class="num" aria-hidden="true">1</span>
          <h3>拍张照，写两行</h3>
          <p>选好板块，填上价格和楼栋，30 秒贴上去。不强制实名，楼栋就是信用。</p>
        </div>
        <div class="step">
          <span class="num" aria-hidden="true">2</span>
          <h3>等人搭把手</h3>
          <p>有人感兴趣会在帖子下留言，AI 助手也会帮你盯着匹配的帖子。</p>
        </div>
        <div class="step">
          <span class="num" aria-hidden="true">3</span>
          <h3>楼下交割</h3>
          <p>约在宿舍楼下或食堂门口，当面看货付款。平台只留记录，不抽成。</p>
        </div>
      </div>
    </section>

    <!-- CTA -->
    <section class="cta">
      <h2>把你的布告贴上来</h2>
      <p>楼下永远有人在看布告栏，差的就是你这一张。</p>
      <router-link class="btn hero-btn yellow" to="/login?mode=register">贴出第一条布告</router-link>
    </section>

    <footer class="foot">
      <span class="foot-logo">搭把手。</span><br>
      校园互助生活平台 · 楼栋信用 · 零抽成 · 由学生自己维护
    </footer>
  </div>
</template>

<script setup>
const tickerItems = [
  '李同学刚发布了：考研数学全套资料 ¥40',
  '你的校园卡在体育馆前台被捡到了',
  '新拼单：今晚 6 点拼奶茶，满 4 杯免配送费',
  '王同学刚发布了：小米台灯 Pro ¥59',
  '李同学刚发布了：考研数学全套资料 ¥40',
  '你的校园卡在体育馆前台被捡到了',
  '新拼单：今晚 6 点拼奶茶，满 4 杯免配送费',
  '王同学刚发布了：小米台灯 Pro ¥59',
]
</script>

<style scoped>
.wrap { max-width: 1080px; margin: 0 auto; padding: 0 24px; }

/* ---------- hero ---------- */
.hero { padding: 64px 24px 80px; display: grid; grid-template-columns: 1.05fr 1fr; gap: 56px; align-items: center; }
.hero h1 {
  font-family: var(--font-hand);
  font-size: clamp(36px, 5.4vw, 60px);
  line-height: 1.28; font-weight: 700; letter-spacing: .01em;
}
.lede { margin-top: 22px; font-size: 17px; color: var(--muted); max-width: 30em; }
.hero-cta { margin-top: 32px; display: flex; gap: 16px; flex-wrap: wrap; }
.hero-btn { width: auto; padding: 11px 24px; }
.hand-note {
  display: inline-block; font-family: var(--font-hand);
  background: var(--highlight); padding: 2px 12px;
  transform: rotate(-1.5deg); box-shadow: 2px 3px 0 rgba(43, 42, 36, .18);
}

/* ---------- 布告栏 ---------- */
.board {
  background: var(--board);
  border: 10px solid #8A6B4A;
  border-radius: 8px;
  padding: 26px 22px;
  min-height: 400px;
  position: relative;
  display: grid; grid-template-columns: repeat(2, 1fr); gap: 20px;
  box-shadow: 0 14px 34px rgba(43, 42, 36, .22);
}
.note {
  position: relative;
  padding: 18px 16px 14px;
  font-family: var(--font-hand);
  line-height: 1.55;
  box-shadow: 0 5px 12px rgba(0, 0, 0, .28);
  animation: pin .55s cubic-bezier(.2, .9, .3, 1.2) backwards;
}
.n1 { background: #FFF8E1; transform: rotate(-2.4deg); animation-delay: .05s; }
.n2 { background: #E8F1EC; transform: rotate(1.8deg); animation-delay: .18s; }
.n3 { background: #FFF8E1; transform: rotate(1.2deg); animation-delay: .31s; }
.n4 { background: #EAF0FB; transform: rotate(-1.6deg); animation-delay: .44s; }
.note .tag { font-size: 12.5px; font-weight: 700; letter-spacing: .05em; color: var(--ink); }
.note h3 { font-size: 17.5px; margin: 4px 0 2px; font-weight: 700; }
.note .meta { font-size: 13px; color: var(--muted); }
.note .price { font-size: 17px; color: var(--pin); font-weight: 700; }
.note::before {
  content: ""; position: absolute; top: -7px; left: 50%; margin-left: -7px;
  width: 14px; height: 14px; border-radius: 50%;
  background: radial-gradient(circle at 34% 30%, #F08A76, var(--pin) 62%, #8E2417);
  box-shadow: 0 2px 3px rgba(0, 0, 0, .4);
}
.note .stamp {
  position: absolute; right: 10px; top: 8px;
  border: 2.5px solid var(--pin); color: var(--pin);
  font-size: 14px; font-weight: 700; padding: 0 7px; border-radius: 4px;
  transform: rotate(9deg); opacity: .85;
}
@keyframes pin {
  from { opacity: 0; transform: translateY(-26px) rotate(0) scale(1.12); }
}
.n1 { animation-name: pin1; }
.n2 { animation-name: pin2; }
.n3 { animation-name: pin3; }
.n4 { animation-name: pin4; }
@keyframes pin1 { from { opacity: 0; transform: translateY(-26px) rotate(0) scale(1.12); } to { transform: translateY(0) rotate(-2.4deg) scale(1); } }
@keyframes pin2 { from { opacity: 0; transform: translateY(-26px) rotate(0) scale(1.12); } to { transform: translateY(0) rotate(1.8deg) scale(1); } }
@keyframes pin3 { from { opacity: 0; transform: translateY(-26px) rotate(0) scale(1.12); } to { transform: translateY(0) rotate(1.2deg) scale(1); } }
@keyframes pin4 { from { opacity: 0; transform: translateY(-26px) rotate(0) scale(1.12); } to { transform: translateY(0) rotate(-1.6deg) scale(1); } }

/* ---------- ticker ---------- */
.ticker {
  border-top: 2px solid var(--charcoal);
  border-bottom: 2px solid var(--charcoal);
  background: var(--charcoal); color: var(--paper);
  overflow: hidden; white-space: nowrap;
  font-family: var(--font-hand); font-size: 16px;
}
.ticker-track { display: inline-block; padding: 12px 0; animation: slide 30s linear infinite; }
.ticker span { margin: 0 26px; position: relative; padding-left: 20px; }
.ticker span::before { content: "●"; position: absolute; left: 0; color: var(--highlight); font-size: 11px; top: 8px; }
@keyframes slide { from { transform: translateX(0); } to { transform: translateX(-50%); } }

/* ---------- 板块 ---------- */
.sections { padding: 80px 24px 56px; }
.sections h2 { font-family: var(--font-hand); font-size: clamp(28px, 3.6vw, 40px); margin-bottom: 10px; }
.sections .sub { color: var(--muted); margin-bottom: 40px; max-width: 34em; }
.sec-grid { display: grid; grid-template-columns: 1.2fr .9fr; grid-auto-rows: minmax(200px, auto); gap: 26px; }
.sec {
  padding: 30px 28px; position: relative;
  border: 2px solid var(--charcoal);
  box-shadow: 5px 6px 0 rgba(43, 42, 36, .16);
}
.sec h3 { font-family: var(--font-hand); font-size: 25px; margin-bottom: 6px; }
.sec p { font-size: 15px; max-width: 26em; }
.sec .sample {
  margin-top: 16px; font-family: var(--font-hand); font-size: 14.5px;
  border-left: 3px solid currentColor; padding-left: 12px; opacity: .85;
}
.sec.market { background: var(--highlight); }
.sec.lost { background: #FDF6EC; }
.sec.lost .stamp-big {
  position: absolute; top: 20px; right: 22px;
  border: 3px solid var(--pin); color: var(--pin);
  font-family: var(--font-hand); font-weight: 700; font-size: 20px;
  padding: 2px 10px; border-radius: 6px; transform: rotate(8deg);
}
.sec.errand { background: #EAF0FB; }
.sec.ai { background: var(--board); color: #F2F0E8; }
.sec.ai .sample { color: #BFD6C9; }
.sec.ai p { color: #D7DFD8; }
.sec-link {
  position: absolute; right: 20px; bottom: 16px;
  font-size: 13px; font-weight: 700;
}
.sec.market .sec-link, .sec.lost .sec-link { color: var(--ink); }
.sec.errand .sec-link { color: var(--ink); }
.sec.ai .sec-link { color: #FFD84D; }
.sec-link:hover { text-decoration: underline; }

/* ---------- 三步 ---------- */
.steps { padding: 64px 24px 84px; }
.steps h2 { font-family: var(--font-hand); font-size: clamp(28px, 3.6vw, 40px); margin-bottom: 44px; }
.step-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 0; }
.step { padding: 0 30px; position: relative; }
.step + .step { border-left: 2px dashed rgba(43, 42, 36, .35); }
.step .num {
  font-family: var(--font-hand); font-size: 46px; font-weight: 700; color: var(--ink);
  line-height: 1; display: block; margin-bottom: 14px;
}
.step h3 { font-size: 19px; margin-bottom: 8px; }
.step p { font-size: 15px; color: var(--muted); }

/* ---------- CTA / 页脚 ---------- */
.cta {
  background: var(--board); color: #F2F0E8; text-align: center;
  padding: 80px 24px;
  background-image:
    linear-gradient(rgba(255, 255, 255, .045) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, .045) 1px, transparent 1px);
  background-size: 28px 28px;
}
.cta h2 { font-family: var(--font-hand); font-size: clamp(30px, 4.4vw, 48px); margin-bottom: 14px; }
.cta p { color: #C9D6CE; margin-bottom: 32px; }
.foot { padding: 34px 24px 44px; text-align: center; font-size: 13.5px; color: var(--muted); }
.foot-logo { font-family: var(--font-hand); font-size: 19px; font-weight: 700; color: var(--charcoal); }

/* ---------- responsive ---------- */
@media (max-width: 900px) {
  .hero { grid-template-columns: 1fr; gap: 48px; padding: 44px 24px 56px; }
  .board { min-height: 0; }
  .sec-grid { grid-template-columns: 1fr; }
  .step-row { grid-template-columns: 1fr; gap: 34px; }
  .step + .step { border-left: none; border-top: 2px dashed rgba(43, 42, 36, .35); padding-top: 34px; }
}
@media (prefers-reduced-motion: reduce) {
  .note { animation: none; }
  .ticker-track { animation: none; }
}
</style>
