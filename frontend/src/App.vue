<template>
  <div class="page">
    <header class="topbar">
      <span class="brand">校园互助生活平台</span>
      <span class="env-tag">模拟环境</span>
    </header>
    <router-view />
  </div>
</template>

<style>
* { box-sizing: border-box; margin: 0; padding: 0; }
body { font-family: system-ui, sans-serif; background: #f5f6f8; color: #222; }
.page { max-width: 520px; margin: 0 auto; padding: 24px 16px; }
.topbar { display: flex; align-items: center; gap: 8px; margin-bottom: 24px; }
.brand { font-weight: 600; font-size: 17px; }
.env-tag { font-size: 12px; color: #b45309; background: #fef3c7; padding: 2px 8px; border-radius: 10px; }
.card { background: #fff; border: 1px solid #e5e7eb; border-radius: 12px; padding: 20px; }
.field { margin-bottom: 14px; }
.field label { display: block; font-size: 13px; margin-bottom: 4px; color: #555; }
.field input { width: 100%; padding: 9px 10px; border: 1px solid #d1d5db; border-radius: 8px; font-size: 14px; }
.btn { width: 100%; padding: 10px; border: none; border-radius: 8px; background: #2563eb; color: #fff; font-size: 15px; cursor: pointer; }
.btn:disabled { background: #9ca3af; cursor: not-allowed; }
.btn-ghost { background: #fff; color: #374151; border: 1px solid #d1d5db; }
.err { color: #dc2626; font-size: 13px; margin-bottom: 10px; }
.ok { color: #16a34a; font-size: 13px; margin-bottom: 10px; }
.link { font-size: 13px; color: #2563eb; text-decoration: none; }
</style>
