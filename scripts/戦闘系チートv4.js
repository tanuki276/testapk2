// 戦闘系チートv.4 — Titan Core 安全シミュレーション用
// 実際のゲーム・他プロセス・メモリにはアクセスしません。
(function () {
  const state = {
    浮遊: false,
    高速移動: false,
    無敵: false,
    無限ジャンプ: false
  };

  function logState() {
    log("\n=== 戦闘系チートv.4 ===");
    log("浮遊: " + (state.浮遊 ? "ON" : "OFF"));
    log("高速移動: " + (state.高速移動 ? "ON" : "OFF"));
    log("無敵: " + (state.無敵 ? "ON" : "OFF"));
    log("無限ジャンプ: " + (state.無限ジャンプ ? "ON" : "OFF"));
    log("[SAFE] すべてローカルUI上のシミュレーションです。");
  }

  document.body.innerHTML = `
    <div style="font-family:sans-serif;background:#0a0e12;color:#e8f0f7;padding:18px;min-height:100vh;box-sizing:border-box">
      <div style="font-size:22px;font-weight:800;letter-spacing:1px">戦闘系チートv.4</div>
      <div style="color:#8fa1b3;font-size:12px;margin:5px 0 16px">TITAN CORE · SAFE SIMULATION</div>
      <div id="status" style="padding:10px;border:1px solid #2a3948;border-radius:8px;margin-bottom:12px;color:#5be58b">● ローカルシミュレーション</div>
      <div id="buttons"></div>
      <button id="reset" style="width:100%;margin-top:14px;padding:13px;border:1px solid #2a3948;border-radius:8px;background:#18212b;color:#e8f0f7;font-size:15px">全てOFF</button>
      <div style="font-size:11px;color:#8fa1b3;margin-top:14px;line-height:1.5">※ このスクリプトはゲーム改造・メモリ操作・プロセス操作を行わず、項目の状態だけを安全にシミュレートします。</div>
    </div>`;

  const buttons = document.getElementById("buttons");
  const status = document.getElementById("status");

  Object.keys(state).forEach(function (name) {
    const button = document.createElement("button");
    button.style.cssText = "display:block;width:100%;margin:8px 0;padding:14px;border:1px solid #2a3948;border-radius:8px;background:#121820;color:#e8f0f7;text-align:left;font-size:16px";
    button.textContent = name + "　[OFF]";
    button.onclick = function () {
      state[name] = !state[name];
      button.textContent = name + "　[" + (state[name] ? "ON" : "OFF") + "]";
      button.style.borderColor = state[name] ? "#5be58b" : "#2a3948";
      status.textContent = "● " + name + " → " + (state[name] ? "ON" : "OFF");
      log("[GUI] " + name + " = " + (state[name] ? "ON" : "OFF"));
    };
    buttons.appendChild(button);
  });

  document.getElementById("reset").onclick = function () {
    Object.keys(state).forEach(function (name, i) {
      state[name] = false;
      buttons.children[i].textContent = name + "　[OFF]";
      buttons.children[i].style.borderColor = "#2a3948";
    });
    status.textContent = "● 全項目OFF";
    log("[GUI] 全項目をOFFにしました");
  };

  logState();
})();
